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

opaque type Validated[A] = Either[String, A]
object Validated:
  extension [A](v: Validated[A])
    inline def getOrThrow: A =
      v match
        case Left(err) => throw new RoachFatalException(err)
        case Right(r)  => r
    inline def either: Either[String, A] = v

opaque type Result = Ptr[PGresult]
object Result:
  extension (r: Result)
    inline def status: ExecStatusType = PQresultStatus(r)
    def rows: (Vector[(Oid, String)], Vector[Vector[String]]) =
      val nFields = PQnfields(r)
      val nTuples = PQntuples(r)
      val meta = Vector.newBuilder[(Oid, String)]
      val tuples = Vector.newBuilder[Vector[String]]

      // Read all the column names and their types
      for i <- 0 until nFields do
        meta.addOne(PQftype(r, i) -> fromCString(PQfname(r, i)))

      // Read all the rows
      for t <- 0 until nTuples
      do
        tuples.addOne(
          (0 until nFields).map(f => fromCString(PQgetvalue(r, t, f))).toVector
        )

      meta.result -> tuples.result
    end rows

    def readOne[A](
        codec: Codec[A]
    )(using z: Zone): Option[A] = readAll(codec).headOption

    def readAll[A](
        codec: Codec[A]
    )(using z: Zone, oids: OidMapping = OidMapping): Vector[A] =
      val nFields = PQnfields(r)
      val nTuples = PQntuples(r)
      val tuples = Vector.newBuilder[A]

      if (codec.length != PQnfields(r)) then
        throw new RoachException(
          s"Provided codec is for ${codec.length} fields, while the result has ${PQnfields(r)} fields"
        )

      (0 until nFields).foreach { offset =>
        val expectedType = oids.rev(PQftype(r, offset))
        val fieldName = fromCString(PQfname(r, offset))

        if codec.accepts(offset) != expectedType then
          throw new RoachException(
            s"$offset: Field $fieldName is of type '$expectedType', " +
              s"but the decoder only accepts '${codec.accepts(offset)}'"
          )
      }

      (0 until nTuples).foreach { row =>
        val func =
          (i: Int) => PQgetvalue(r, row, i) // -> PQgetlength(r, row, i)
        tuples.addOne(codec.decode(func))
      }

      tuples.result
    end readAll
  end extension

  given Releasable[Result] with
    def release(db: Result) =
      if db != null then PQclear(db)
end Result

opaque type Database = Ptr[PGconn]

object Database:
  def apply(connString: String)(using Zone): Validated[Database] =
    val conn = PQconnectdb(toCString(connString))

    if PQstatus(conn) != ConnStatusType.CONNECTION_OK then
      val res = Left(fromCString(PQerrorMessage(conn)))
      PQfinish(conn)
      res
    else Right(conn)

  import Validated.*
  import Result.given

  class Prepared[T] private[roach] (
      db: Database,
      codec: Codec[T],
      statementName: String
  ):
    private val nParams = codec.length
    def execute(data: T)(using z: Zone): Validated[Result] =
      db.checkConnection()
      val paramValues = stackalloc[CString](nParams)
      val encoder = codec.encode(data)
      for i <- 0 until nParams do paramValues(i) = encoder(i)
      val res = PQexecPrepared(
        db,
        toCString(statementName),
        nParams,
        paramValues,
        null,
        null,
        0
      )

      db.result(res)
    end execute
  end Prepared

  extension (d: Database)
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
        val ret =
          Left(fromCString(PQerrorMessage(d)))
        PQclear(res)
        ret
      else Right(res)
    end execute

    def command(query: String)(using Zone): Unit =
      checkConnection()
      Using.resource(d.execute(query).getOrThrow) { res =>
        PQresultStatus(res)
      }

    private[roach] def result(res: Result): Validated[Result] =
      val status = PQresultStatus(res)
      import ExecStatusType.*

      val failed =
        status == PGRES_BAD_RESPONSE ||
          status == PGRES_NONFATAL_ERROR ||
          status == PGRES_FATAL_ERROR

      if failed then
        PQclear(res) // important!
        Left(fromCString(PQerrorMessage(d)))
      else Right(res)
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

      result(res).map(_ => Prepared[T](d, codec, statementName))
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

      result(res)

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
      result(res)
    end execute

  end extension

  given Releasable[Database] with
    def release(db: Database) =
      if db != null && PQstatus(db) == ConnStatusType.CONNECTION_OK then
        PQfinish(db)
end Database
