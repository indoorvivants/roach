package roach.circe.tests

import roach.circe.*
import roach.codecs.*
import io.circe.Json
import io.circe.JsonNumber
import roach.Pool

class CirceTests extends munit.FunSuite with roach.tests.TestHarness:
  var tableName: String | Null = null

  // Runs once before all tests start.
  override def beforeAll(): Unit =
    zone {
      withDB { db ?=>
        val prepTableName = s"roach_test_circe_${util.Random().nextLong.abs}"
        db.execute(
          s"""
          CREATE TABLE $prepTableName (
            id serial NOT NULL PRIMARY KEY,
            info json NOT NULL
          );
          """
        ).getOrThrow

        tableName = prepTableName
      }
    }

  override def afterAll(): Unit =
    zone {
      withDB { db ?=>
        if tableName != null then
          db.execute(s"drop table $tableName").getOrThrow
      }
    }

  test("read: raw json") {
    zone {
      withDB { db ?=>
        val q =
          """SELECT '{"bar": "baz", "balance": 7.77, "active": false}'::json"""

        db.execute(q).getOrThrow.use { res =>
          val js = res.readOne(json).get
          assertEquals(js \\ "bar", List(Json.fromString("baz")))
          assertEquals(
            js \\ "balance",
            List(Json.fromDouble(7.77)).flatten
          )
          assertEquals(js \\ "active", List(Json.fromBoolean(false)))
        }
      }
    }
  }

  test("write: raw json") {

    import io.circe.syntax.*

    zone {
      withDB { db ?=>
        val row = (512L, Json.obj("hello" := "world", "bla" := 25))
        db.executeParams(
          s"insert into $tableName(id, info) values($$1, $$2) returning id",
          int8 ~ json,
          row
        )

        query(s"select id::int8, info from $tableName") { res =>
          res.readOne(int8 ~ json).contains(row)
        }
      }
    }
  }

  test("write: json codec") {

    import io.circe.syntax.*

    case class Testi(hello: String, bla: Int) derives io.circe.Codec.AsObject

    zone {
      withDB { db ?=>
        val row = (1024L, Testi("yo", 152))

        db.executeParams(
          s"insert into $tableName(id, info) values($$1, $$2) returning id",
          int8 ~ jsonOf[Testi],
          row
        )

        query(s"select id::int8, info from $tableName") { res =>
          res.readOne((int8 ~ jsonOf[Testi])).contains(row)
        }
      }
    }
  }


  test("read: json codec") {
    case class Top(bar: String, balance: Double, active: Boolean)
        derives io.circe.Codec.AsObject

    zone {
      withDB { db ?=>
        val q =
          """SELECT '{"bar": "baz", "balance": 7.77, "active": false}'::json"""

        db.execute(q).getOrThrow.use { res =>
          val js = res.readOne(jsonOf[Top]).get

          assert(!js.active)
          assert(js.bar == "baz")
          assert(js.balance == 7.77)
        }
      }
    }
  }
end CirceTests
