package roach.tests

import roach.*
import roach.codecs.*
import scala.util.Using
import scala.scalanative.unsafe.Zone
import scala.scalanative.unsigned.*
import libpq.types.Oid
import scala.util.Try.apply
import scala.util.Try

class Tests extends munit.FunSuite:
  val connectionString =
    "postgresql://postgres:mysecretpassword@localhost:5432/postgres?application_name=roach_tests"

  inline def zone[A](inline f: Zone ?=> A) = Zone.apply(z => f(using z))

  def withDB(f: Database ?=> Unit)(using Zone) =
    Using.resource(Database(connectionString).getOrThrow)(db => f(using db))

  def query[A](q: String)(f: Result => A)(using Zone)(using db: Database): A =
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

  private def findPid()(using Database, Zone) =
    query(
      "select pid::int4 from pg_stat_activity where application_name = 'roach_tests'"
    ) { res =>
      res.readOne(int4).get
    }

  private def terminatePid(pid: Int)(using db: Database)(using Zone) =
    Try {
      db.command(s"select pg_terminate_backend($pid)")
    }

  test("connection status and termination") {
    zone {
      withDB { db ?=>
        assert(db.connectionIsOkay)

        val pid = findPid()
        terminatePid(pid)
        assert(!db.connectionIsOkay)

      }
    }
  }

  test("single slot pool") {
    zone {
      Pool.single(connectionString) { pool =>
        var old: Database | Null = null

        val pid = pool.lease { db =>
          old = db
          findPid()(using db)
        }

        pool.lease { db =>
          terminatePid(pid)(using db)
        }

        assert(!old.nn.connectionIsOkay)

        val pid2 = pool.lease { db =>
          old = db
          findPid()(using db)
        }

        assert(old.nn.connectionIsOkay)

        val pid3 = pool.lease { db =>
          findPid()(using db)
        }

        assert(pid != pid2) // make sure we reconnected
        assert(pid2 == pid3) // make sure we don't reconnect all the time
      }
    }
  }

  test("execute params") {
    zone {
      withDB { db ?=>
        db.executeParams(
          "select oid::int4 from pg_type where typname = $1",
          varchar,
          "bool"
        ).getOrThrow
          .use { res =>
            val row: Option[Int] = res.readOne(int4)

            assert(row.contains(16))
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
