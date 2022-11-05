package roach.tests

import roach.*
import roach.codecs.*
import scala.util.Using
import scala.scalanative.unsafe.Zone
import scala.scalanative.unsigned.*
import libpq.types.Oid

class Tests extends munit.FunSuite:
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

  test("prepared") {
    zone {
      withDB { db ?=>
        val single = db
          .prepare(
            "SELECT oid::int4, typname from pg_type where typname = $1",
            "myq",
            varchar
          )
          .getOrThrow

        val multiple = db
          .prepare(
            "SELECT count(*) from pg_type where typname = $1 and oid::int4 = $2",
            "multiple",
            varchar ~ int4
          )
          .getOrThrow

        assert(
          single.execute("bool").getOrThrow.readAll(oid ~ name).toMap == Map(
            Oid(16.toUInt) -> "bool"
          )
        )

        assert(
          single.execute("int4").getOrThrow.readAll(oid ~ name).toMap == Map(
            Oid(23.toUInt) -> "int4"
          )
        )

        assert(
          single.execute("varchar").getOrThrow.readAll(oid ~ name).toMap == Map(
            Oid(1043.toUInt) -> "varchar"
          )
        )

        assert(
          multiple
            .execute("varchar" -> 1043)
            .getOrThrow
            .readOne(int8)
            .contains(1L)
        )
        assert(
          multiple.execute("bool" -> 16).getOrThrow.readOne(int8).contains(1L)
        )
        assert(
          multiple.execute("int4" -> 23).getOrThrow.readOne(int8).contains(1L)
        )
        assert(
          multiple
            .execute("blablabla" -> 23)
            .getOrThrow
            .readOne(int8)
            .contains(0L)
        )
      }
    }
  }
end Tests
