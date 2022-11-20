package roach

import libpq.functions.*
import libpq.types.*

import java.util.UUID
import scala.scalanative.libc.*
import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.*
import scala.util.Try
import scala.util.Using
import scala.util.Using.Releasable

opaque type Database = Ptr[PGconn]

object Database:
  def apply(connString: String)(using Zone): Validated[Database] =
    apply(toCString(connString))

  def apply(connString: CString): Validated[Database] =
    val conn = PQconnectdb(connString)

    if PQstatus(conn) != ConnStatusType.CONNECTION_OK then
      val res = Validated.error(
        RoachError.ConnectionIsDown(conn.currentConnectionError)
      )
      PQfinish(conn)
      res
    else Validated(conn)
  end apply

  import Validated.*
  import Result.given

  extension (d: Database)

    def connectionIsOkay: Boolean =
      val status = PQstatus(d)
      status == ConnStatusType.CONNECTION_OK

    def checkConnection(): Unit =
      val status = PQstatus(d)
      if status == ConnStatusType.CONNECTION_BAD
      then RoachError.ConnectionIsDown(currentConnectionError).raise

    def execute(query: String)(using Zone): Validated[Result] =
      val cstr = toCString(query)
      val res = PQexec(d, cstr)
      val status = PQresultStatus(res)
      import ExecStatusType.*
      val failed =
        status == PGRES_BAD_RESPONSE ||
          status == PGRES_NONFATAL_ERROR ||
          status == PGRES_FATAL_ERROR

      val wrapped = Result(res)

      if failed then
        val ret =
          Validated.error(
            RoachError.QueryExecutionFailed(
              wrapped.resultError,
              wrapped.sqlstate
            )
          )
        PQclear(res)
        ret
      else Validated(wrapped)
      end if
    end execute

    def command(query: String)(using Zone): Unit =
      Using.resource(d.execute(query).getOrThrow) { res =>
        res.status
      }

    def prepare[T](
        query: String,
        statementName: String,
        codec: Codec[T]
    )(using z: Zone, oids: OidMapping = OidMapping): Validated[Prepared[T]] =
      val nParams = codec.length
      val paramTypes = stackalloc[Oid](nParams)
      for l <- 0 until nParams do paramTypes(l) = oids.map(codec.accepts(l))

      val res = PQprepare(
        d,
        toCString(statementName),
        toCString(query),
        nParams,
        paramTypes
      )

      result(Result(res)).map(_ => Prepared[T](d, codec, statementName))
    end prepare

    def executeParams[T](
        query: String,
        codec: Codec[T],
        data: T
    )(using z: Zone, oids: OidMapping = OidMapping): Validated[Result] =
      val nParams = codec.length
      val paramTypes = stackalloc[Oid](nParams)
      for l <- 0 until nParams do paramTypes(l) = oids.map(codec.accepts(l))

      val paramValues = stackalloc[CString](nParams)
      val encoder = codec.encode(data)
      for i <- 0 until nParams do paramValues(i) = encoder(i)

      val res = PQexecParams(
        d,
        toCString(query),
        nParams,
        paramTypes,
        paramValues,
        null,
        null,
        0
      )

      result(Result(res))

    end executeParams

    def execute[T](query: String, codec: Codec[T], values: T)(using
        z: Zone,
        oids: OidMapping = OidMapping
    ): Validated[Result] =
      val nParams = codec.length
      val paramTypes = stackalloc[Oid](nParams)
      val encoder = codec.encode(values)
      for l <- 0 until nParams do paramTypes(l) = oids.map(codec.accepts(l))

      val paramValues = stackalloc[CString](nParams)
      for i <- 0 until nParams do paramValues(i) = encoder(i)

      val res = PQexecParams(
        d,
        toCString(query),
        nParams,
        paramTypes,
        paramValues,
        null,
        null,
        0
      )
      result(Result(res))
    end execute

    inline def unsafely[A](f: Ptr[PGconn] => A): A =
      f(d)

    private[roach] def currentConnectionError: String =
      fromCString(PQerrorMessage(d))

    private[roach] def executePrepared(
        statementName: String,
        nParams: Int,
        values: ParamValues
    )(using Zone): Validated[Result] =
      executePrepared(toCString(statementName), nParams, values)

    private[roach] def executePrepared(
        statementName: CString,
        nParams: Int,
        values: ParamValues
    ): Validated[Result] =
      val res = PQexecPrepared(
        d,
        statementName,
        nParams,
        values.asInstanceOf[Ptr[CString]],
        null,
        null,
        0
      )
      d.result(Result(res))
    end executePrepared

    private[roach] def result(res: Result): Validated[Result] =
      val status = res.status
      import ExecStatusType.*

      val failed =
        status == PGRES_BAD_RESPONSE ||
          status == PGRES_NONFATAL_ERROR ||
          status == PGRES_FATAL_ERROR

      if failed then
        res.clear()
        Validated.error(
          RoachError.QueryExecutionFailed(res.resultError, res.sqlstate)
        )
      else Validated(res)
    end result

    /** Why is this method private? running PQfinish both shuts down the
      * connection and _frees the memory_. This means that the pointer we are
      * wrapping is no longer valid - any attempts to run things like PQstatus
      * on it lead to a segfault.
      *
      * Therefore I made the decision to not expose a way to terminate the
      * connection this way - otherwise it will lead to all sorts of memory
      * shenanigans.
      *
      * Users can still use the unsafely block to perform raw operations on the
      * pointer.
      */
    private[roach] def closeConnection() =
      if d != null && PQstatus(d) == ConnStatusType.CONNECTION_OK then
        PQfinish(d)

  end extension

  given Releasable[Database] with
    def release(db: Database) = db.closeConnection()
end Database
