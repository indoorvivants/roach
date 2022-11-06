package roach.tests

import scala.util.Using
import scala.scalanative.unsafe.Zone

import roach.*

trait TestHarness:
  val connectionString =
    "postgresql://postgres:mysecretpassword@localhost:5432/postgres?application_name=roach_tests"

  inline def zone[A](inline f: Zone ?=> A) = Zone.apply(z => f(using z))

  def withDB(f: Database ?=> Unit)(using Zone) =
    Using.resource(Database(connectionString).getOrThrow)(db => f(using db))

  def query[A](q: String)(f: Result => A)(using Zone)(using db: Database): A =
    Using.resource(db.execute(q).getOrThrow)(f)
end TestHarness
