package roach.tests

import roach.*
import roach.codecs.*
import scala.util.Using
import scala.scalanative.unsafe.Zone
import scala.scalanative.unsigned.*
import libpq.types.Oid

object Tests extends verify.BasicTestSuite:
  val connectionString =
    "postgresql://postgres:mysecretpassword@localhost:5432/postgres"

  inline def zone[A](inline f: Zone ?=> A) = Zone.apply(z => f(using z))

  def withDB(f: Database ?=> Unit)(using Zone) =
    Using.resource(Database(connectionString).getOrThrow)(db => f(using db))

  def query(q: String)(f: Result => Unit)(using Zone)(using db: Database) =
    Using.resource(db.execute(q).getOrThrow)(f)

  test("super basics") {
    zone {
      withDB { db ?=>
        query(
          "SELECT oid::int4, typname from pg_type where typname in ('bool', 'int4', 'varchar')"
        ) { result =>
          val retrieved = result.readAll(oid ~ name).toMap

          assert(retrieved(Oid(16.toUInt)) == "bool")
          assert(retrieved(Oid(23.toUInt)) == "int4")
          assert(retrieved(Oid(1043.toUInt)) == "varchar")
        }
      }
    }
  }
end Tests
