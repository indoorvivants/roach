package roach

import scala.scalanative.unsafe.*
import scala.scalanative.libc.*
import scala.util.Using
import scala.util.Using.Releasable
import libpq.functions.*
import libpq.types.*

opaque type Result = Ptr[PGresult]
object Result:
  inline def apply(raw: Ptr[PGresult]): Result = raw

  extension (r: Result)
    inline def status: ExecStatusType = PQresultStatus(r)

    inline def clear(): Unit = if r != null then PQclear(r)

    inline def count(): CInt = 
      PQntuples(r)

    private[roach] def rows: (Vector[(Oid, String)], Vector[Vector[String]]) =
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

    inline def use[A](f: Result => A) = 
      Using.resource(r)(f)
  end extension

  given Releasable[Result] with
    def release(res: Result) =
      if res != null then res.clear()
end Result
