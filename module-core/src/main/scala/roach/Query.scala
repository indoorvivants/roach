package roach

import scala.scalanative.unsafe.Zone
import scala.util.NotGiven

// class Query:
//   def all[T](using NotGiven[T])

trait Query[T]:
  self =>
  protected def execute(data: T)(using db: Database, z: Zone): roach.Result

  def contramap[X](transform: X => T): Query[X] =
    new Query[X]:
      override protected def execute(
          data: X
      )(using db: Database, z: Zone): Result = self.execute(transform(data))

      override def query: String = self.query

  def all[X](data: T, codec: Codec[X])(using
      iz: Zone,
      db: Database
  ): Vector[X] =
    execute(data).use(_.readAll(codec))

  def execute[X](data: T, codec: Codec[X])(using
      iz: Zone,
      db: Database
  ): Option[X] =
    execute(data).use(_.readOne(codec))

  def exec(data: T)(using
      iz: Zone,
      db: Database
  ): Unit =
    execute(data).use { _ => }

  def count(data: T)(using
      iz: Zone,
      db: Database
  ): Int =
    execute(data).use(_.count())

  def one[X](data: T, codec: Codec[X])(using
      iz: Zone,
      db: Database
  ): Option[X] =
    execute(data).use(_.readOne(codec))

  def query: String
end Query

private class QueryImpl[T](val query: String, codec: Codec[T]) extends Query[T]:
  protected def execute(data: T)(using db: Database, z: Zone): roach.Result =
    db.executeParams[T](query, codec, data).getOrThrow

class VoidQuery(str: String):
  private def execute()(using db: Database, z: Zone): roach.Result =
    db.execute(str).getOrThrow

  def all[X](codec: Codec[X])(using iz: Zone, db: Database): Vector[X] =
    execute().use(_.readAll(codec)(using iz))

  def one[X](codec: Codec[X])(using iz: Zone, db: Database): Option[X] =
    execute().use(_.readOne(codec)(using iz))

  def exec()(using iz: Zone, db: Database): Unit =
    execute().use { _ => }

  def count()(using
      iz: Zone,
      db: Database
  ): Int =
    execute().use(_.count())

end VoidQuery

object Query:
  def apply[T](q: String, codecIn: Codec[T]): Query[T] =
    new QueryImpl(q, codecIn)

  def apply(q: String): VoidQuery =
    new VoidQuery(q)

  private[roach] def applyTransformed[Positional, UserSupplied](
      q: String,
      codecIn: Codec[Positional],
      transform: UserSupplied => Positional
  ): Query[UserSupplied] =
    apply(q, codecIn).contramap(transform)
end Query
