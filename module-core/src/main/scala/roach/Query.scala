package roach

import scala.scalanative.unsafe.Zone
import scala.util.NotGiven

// class Query:
//   def all[T](using NotGiven[T])

opaque type Query[T] = T => (Database, Zone) ?=> roach.Result
object Query:
  def apply[T](q: String, codecIn: Codec[T]): Query[T] =
    data => (db, z) ?=> db.executeParams[T](q, codecIn, data).getOrThrow

  def apply(q: String): Query[Unit] =
    data => (db, z) ?=> db.execute(q).getOrThrow

  private[roach] def applyTransformed[Positional, UserSupplied](
      q: String,
      codecIn: Codec[Positional],
      transform: UserSupplied => Positional
  ): Query[UserSupplied] =
    data => (db, z) ?=> db.executeParams(q, codecIn, transform(data)).getOrThrow

  extension [T](q: Query[T])(using
      NotGiven[T =:= Unit]
  )
    def all[X](data: T, codec: Codec[X])(using
        iz: Zone,
        db: Database
    ): Vector[X] =
      q(data).use(_.readAll(codec)(using iz))

    def one[X](data: T, codec: Codec[X])(using
        iz: Zone,
        db: Database
    ): Option[X] =
      q(data).use(_.readOne(codec)(using iz))

    def exec(data: T)(using
        iz: Zone,
        db: Database
    ): Unit =
      q(data).use { _ => }

    def count(data: T)(using
        iz: Zone,
        db: Database
    ): Int =
      q(data).use(_.count())
  end extension

  extension (q: Query[Unit])
    def all[X](codec: Codec[X])(using iz: Zone, db: Database): Vector[X] =
      q(()).use(_.readAll(codec)(using iz))

    def one[X](codec: Codec[X])(using iz: Zone, db: Database): Option[X] =
      q(()).use(_.readOne(codec)(using iz))

    def exec()(using iz: Zone, db: Database): Unit =
      q(()).use { _ => }

    def count()(using
        iz: Zone,
        db: Database
    ): Int =
      q(()).use(_.count())
  end extension
end Query
