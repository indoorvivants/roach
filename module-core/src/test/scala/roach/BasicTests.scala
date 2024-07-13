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
import scala.util.Random

class BasicTests extends munit.FunSuite, TestHarness:
  override protected def tableCreationSQL: Option[String => String] =
    Some(tableName => s"""
    CREATE TABLE $tableName(
      id varchar not null,
      f_int2 int2 not null,
      f_int4 int4 not null,
      f_int8 int8 not null,
      f_varchar varchar not null,
      f_text text not null,
      f_uid uuid not null,
      f_float float4 not null,
      f_double float8 not null,
      f_bool bool not null,
      o_int2 int2 ,
      o_int4 int4 ,
      o_int8 int8 ,
      o_varchar varchar ,
      o_text text ,
      o_uid uuid ,
      o_float float4 ,
      o_double float8 ,
      o_bool bool 
    )
    """)

  test("super basics") {
    Zone {
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
    Zone {
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
        id: String,
        short: Short,
        int: Int,
        long: Long,
        vc: String,
        txt: String,
        uid: UUID,
        float: Float,
        double: Double,
        bool: Boolean,
        o_short: Option[Short],
        o_int: Option[Int],
        o_long: Option[Long],
        o_vc: Option[String],
        o_txt: Option[String],
        o_uid: Option[UUID],
        o_float: Option[Float],
        o_double: Option[Double],
        o_bool: Option[Boolean]
    )
    val codec =
      (varchar ~ int2 ~ int4 ~ int8 ~ varchar ~ text ~ uuid ~ float4 ~ float8 ~ bool ~
        int2.opt ~ int4.opt ~ int8.opt ~ varchar.opt ~ text.opt ~ uuid.opt ~ float4.opt ~ float8.opt ~ bool.opt)

    val rowCodec = codec.as[Row]

    Zone {
      withDB { db ?=>
        def row(opt: Boolean) = Row(
          id = s"opt_$opt",
          short = Short.MaxValue,
          int = Int.MaxValue,
          long = Long.MaxValue,
          vc = "varchar",
          txt = "text",
          uid = UUID.fromString("5D7BC610-17DF-460F-BCA9-AADB391A252D"),
          float = Float.MaxValue,
          double = Double.MaxValue,
          bool = false,
          o_short = Some(Short.MaxValue).filter(_ => opt),
          o_int = Some(Int.MaxValue).filter(_ => opt),
          o_long = Some(Long.MaxValue).filter(_ => opt),
          o_vc = Some("varchar").filter(_ => opt),
          o_txt = Some("text").filter(_ => opt),
          o_uid = Some(UUID.fromString("5D7BC610-17DF-460F-BCA9-AADB391A252D"))
            .filter(_ => opt),
          o_float = Some(Float.MaxValue).filter(_ => opt),
          o_double = Some(Double.MaxValue).filter(_ => opt),
          o_bool = Some(true).filter(_ => opt)
        )

        List(row(true), row(false)).foreach { r =>
          // TODO: figure out a better way of doing this
          db.executeParams(
            s"insert into $tableName values(${List
                .tabulate(rowCodec.length)(n => "$" + (n + 1))
                .mkString(", ")})",
            rowCodec,
            r
          ).getOrThrow

          val result = sql"select * from $tableName where id = $varchar"
            .one(r.id, rowCodec)

          assertEquals(clue(result), Some(clue(r)))
        }
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
      sql"select pg_terminate_backend(${pid.toString()})".one(bool)
    }

  test("connection status and termination") {
    Zone {
      withDB { db ?=>
        assert(db.connectionIsOkay)

        val pid = findPid()
        terminatePid(pid)
        assert(!db.connectionIsOkay)

      }
    }
  }

  test("single slot pool") {
    Zone {
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
          sql"select * from pg_type".count()
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

  test("pool notice processor") {
    val messages = List.newBuilder[String]
    val tableName = s"my_table_bla_${Random.nextInt().abs}"
    Zone {
      Pool.single(connectionString, s => messages.addOne(s.trim)) { pool =>
        pool.withLease {

          sql"create table if not exists $tableName(id varchar not null)"
            .exec()
          sql"create table if not exists $tableName(id varchar not null)"
            .exec()
        }
      }
    }

    assertEquals(
      messages.result(),
      List(
        s"NOTICE:  relation \"$tableName\" already exists, skipping"
      )
    )
  }

  test("execute error params") {
    Zone {
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

  test("execute error capturing") {
    Zone {
      withDB { db ?=>
        val msg = db
          .executeParams(
            "select oid::int4! from pg_type where typname = $1",
            varchar,
            "bool"
          )
          .either
          .left
          .get
          .message
          .toLowerCase()

        assert(msg.contains("syntax error at or near"))
      }
    }
  }

  test("prepared") {
    Zone {
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
