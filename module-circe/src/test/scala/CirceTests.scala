package roach.circe.tests

import roach.circe.*
import roach.codecs.*
import io.circe.Json
import io.circe.JsonNumber
import roach.Pool
import scala.scalanative.unsafe.Zone

class CirceTests extends munit.FunSuite with roach.tests.TestHarness:

  override def tablePrefix: String = "roach_test_circe"
  override def tableCreationSQL =
    Some(tableName => s"""
          CREATE TABLE $tableName (
            id serial NOT NULL PRIMARY KEY,
            info json NOT NULL
          );
          """)

  test("read: raw json") {
    Zone {
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

    Zone {
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

    Zone {
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

    Zone {
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
