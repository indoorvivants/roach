package roach

import libpq.functions.*
import libpq.types.*

import scala.scalanative.unsigned.*
import scala.scalanative.unsafe.*
import scala.scalanative.libc.*
import scala.util.Using
import scala.util.Using.Releasable
import java.util.UUID
import scala.util.Try

class RoachException(msg: String) extends Exception(msg)
class RoachFatalException(msg: String) extends Exception(msg)

opaque type Database = Ptr[PGconn]

object Database:
  def apply(connString: String)(using Zone): Validated[Database] =
    apply(toCString(connString))

  def apply(connString: CString): Validated[Database] =
    val conn = PQconnectdb(connString)

    if PQstatus(conn) != ConnStatusType.CONNECTION_OK then
      val res = conn.currentError
      PQfinish(conn)
      res
    else Validated(conn)

  import Validated.*
  import Result.given

  extension (d: Database)
    private[roach] def currentError: Validated[Nothing] =
      val str = fromCString(PQerrorMessage(d))
      Validated.error(str)

    def executePrepared(
        statementName: String,
        nParams: Int,
        values: ParamValues
    )(using Zone): Validated[Result] =
      executePrepared(toCString(statementName), nParams, values)

    def executePrepared(
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

    def connectionIsOkay: Boolean =
      val status = PQstatus(d)
      status != ConnStatusType.CONNECTION_NEEDED && status != ConnStatusType.CONNECTION_BAD

    def checkConnection(): Unit =
      val status = PQstatus(d)
      if status == ConnStatusType.CONNECTION_NEEDED || status == ConnStatusType.CONNECTION_BAD
      then throw new RoachFatalException("Postgres connection is down")

    def execute(query: String)(using Zone): Validated[Result] =
      checkConnection()
      val cstr = toCString(query)
      val res = PQexec(d, cstr)
      val status = PQresultStatus(res)
      import ExecStatusType.*
      val failed =
        status == PGRES_BAD_RESPONSE ||
          status == PGRES_NONFATAL_ERROR ||
          status == PGRES_FATAL_ERROR

      if failed then
        val ret = currentError
        PQclear(res)
        ret
      else Validated(Result(res))
    end execute

    def command(query: String)(using Zone): Unit =
      checkConnection()
      Using.resource(d.execute(query).getOrThrow) { res =>
        res.status
      }

    private[roach] def result(res: Result): Validated[Result] =
      val status = res.status
      import ExecStatusType.*

      val failed =
        status == PGRES_BAD_RESPONSE ||
          status == PGRES_NONFATAL_ERROR ||
          status == PGRES_FATAL_ERROR

      if failed then
        res.clear()
        currentError
      else Validated(res)
    end result

    def prepare[T](
        query: String,
        statementName: String,
        codec: Codec[T]
    )(using z: Zone, oids: OidMapping = OidMapping): Validated[Prepared[T]] =
      checkConnection()
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
      checkConnection()
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
      checkConnection()
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

    private[roach] def closeConnection() =
      if d != null && PQstatus(d) == ConnStatusType.CONNECTION_OK then
        PQfinish(d)

  end extension

  given Releasable[Database] with
    def release(db: Database) = db.closeConnection()
end Database
