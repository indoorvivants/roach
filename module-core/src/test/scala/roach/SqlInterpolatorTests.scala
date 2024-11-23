package roach.tests

import roach.*
import roach.codecs.*
import scala.scalanative.unsafe.Zone

class SqlInterpolatorTests extends munit.FunSuite, TestHarness:
  override def tableCreationSQL = Some(tableName => s"""
    create table $tableName(
      key int not null,
      value text not null
    );
    insert into $tableName values(25, 'hello');
    insert into $tableName values(42, 'bye');
    """)

  case class Data(key: Int, value: String)
  val rc = (int4 ~ text).as[Data]

  test("fragments") {
    Zone {
      withDB {
        val fr1 = Fragment("key,value")
        val b = sql"select $text, $int2 from table"

        // type T = ("label1", Int) *: ("label2", Int) *: String *: Short *: EmptyTuple
        val q = sql"select $fr1 from $tableName where key = 25".all(int4 ~ text)

        assertEquals(q, Vector(25 -> "hello"))

        val fr2 = fr1.applied(int4 ~ text)

        val q1 =
          sql"insert into $tableName(${fr2.sql}) values ($fr2) returning key"

        val result = q1.one(125 -> "yo", int4)

        assertEquals(result, Some(125))
      }
    }
  }

  test("labelled queries") {
    Zone:
      withDB:
        val key1 = int4.label["key1"]
        val key2 = int4.label["key2"]
        val value1 = text.label["value1"]
        val value2 = text.label["value2"]

        val ql = sql"""
            select * from $tableName 
            where
              (key = $key1 and value = $value1) OR
              (key = 312 and value = $text) OR
              (key = $key2 and value = $value2) OR
              (key = $key1 and value = 'test') OR
              (key = $int4 and value = $text) OR
              (key = $key2 and value = 'test')
            """
        val rc = int4 ~ text
        val insert =
          sql"insert into $tableName values ($rc), ($rc), ($rc), ($rc), ($rc), ($rc)"

        insert.exec(
          (
            (150, "hello"),
            (256, "world"),
            (150, "test"),
            (256, "test"),
            (312, "test2"),
            (25, "yo")
          )
        )

        val expected = Vector(
          (150, "hello"),
          (256, "world"),
          (150, "test"),
          (256, "test"),
          (312, "test2"),
          (25, "yo")
        )

        assertEquals(
          ql.all(
            (
              "key1" -> 150,
              "value1" -> "hello",
              "test2",
              "key2" -> 256,
              "value2" -> "world",
              25,
              "yo"
            ),
            rc
          ),
          expected
        )

  }

  test("basics") {
    Zone {
      withDB {

        sql"insert into $tableName values ($rc)".exec(Data(1500, "howdies"))

        assertEquals(
          sql"select * from $tableName where key = $int4 and value = $text"
            .count(
              1500 -> "howdies"
            ),
          1
        )
        assertEquals(
          sql"select value from $tableName where key = $int4".one(1500, text),
          Some("howdies")
        )

        sql"select * from $tableName ".exec()

        sql"select * from pg_type ".exec()
      }
    }
  }

end SqlInterpolatorTests
