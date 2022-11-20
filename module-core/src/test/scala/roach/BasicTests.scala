package roach.tests

import roach.*
import roach.codecs.*
import scala.util.Using
import scala.scalanative.unsafe.Zone
import scala.scalanative.unsigned.*
import libpq.types.Oid
import scala.util.Try.apply
import scala.util.Try
import java.util.UUID

class BasicTests extends munit.FunSuite, TestHarness:
  override protected def tableCreationSQL: Option[String => String] =
    Some(tableName => s"""
    CREATE TABLE $tableName(
      f_int2 int2 not null,
      f_int4 int4 not null,
      f_int8 int8 not null,
      f_varchar varchar not null,
      f_text text not null,
      f_uid uuid not null,
      f_float float4 not null,
      f_double float8 not null,
      f_bool bool not null
    )
    """)

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

  test("arrays") {
    zone {
      withDB { db ?=>
        query(
          "SELECT ARRAY[ARRAY['hпрd^sâветe\"l,lo'], ARRAY['bla']]"
        ) { result =>
          result.readOne(
            Codec.stringLike[String]("_text")(identity, identity)
          )

        }
        query(
          "SELECT ARRAY[ARRAY[1,2],ARRAY[4,5],ARRAY[1,351]]"
        ) { result =>
          result.readOne(
            Codec.stringLike[String]("_int4")(identity, identity)
          )
        }
      }
    }
  }

  test("codecs") {
    case class Row(
        short: Short,
        int: Int,
        long: Long,
        vc: String,
        txt: String,
        uid: UUID,
        float: Float,
        double: Double,
        bool: Boolean
    )
    val codec =
      int2 ~ int4 ~ int8 ~ varchar ~ text ~ uuid ~ float4 ~ float8 ~ bool

    val rowCodec = codec.as[Row]

    zone {
      withDB { db ?=>
        val row = Row(
          Short.MaxValue,
          Int.MaxValue,
          Long.MaxValue,
          "varchar",
          "text",
          UUID.fromString("5D7BC610-17DF-460F-BCA9-AADB391A252D"),
          Float.MaxValue,
          Double.MaxValue,
          false
        )

        // TODO: figure out a better way of doing this
        db.executeParams(
          s"insert into $tableName values(${List.tabulate(rowCodec.length)(n => "$" + (n + 1)).mkString(", ")})",
          rowCodec,
          row
        ).getOrThrow

        val result = db
          .execute(s"select * from $tableName")
          .getOrThrow
          .use(_.readOne(rowCodec))

        assertEquals(result, Some(row))
      }
    }
  }

  private def findPid()(using Database, Zone) =
    query(
      s"select pid::int4 from pg_stat_activity where application_name = '$appName'"
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

        intercept[RoachError.QueryExecutionFailed] {
          pool.withLease { db ?=>
            terminatePid(pid)(using db)

            val exc = sql"select * from pg_type".count()
          }
        }

        pool.withLease { db ?=>
          println(sql"select * from pg_type".count())
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
end BasicTests
