package roach.tests

import roach.*
import roach.codecs.*

class SqlInterpolatorTests extends munit.FunSuite, TestHarness:
  override def tableCreationSQL = Some(tableName => s"""
    create table $tableName(
      key int not null,
      value text not null
    );
    insert into $tableName values(25, 'hello');
    insert into $tableName values(42, 'bye');
    """)

  test("fragments") {
    zone {
      withDB {
        val fr1 = Fragment("key,value")
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

  test("basics") {
    zone {
      withDB {

        case class Data(key: Int, value: String)
        val rc = (int4 ~ text).as[Data]
        sql"insert into $tableName values ($rc)".exec(Data(150, "howdies"))

        assertEquals(
          sql"select * from $tableName where key = $int4 and value = $text"
            .count(
              150 -> "howdies"
            ),
          1
        )
        assertEquals(
          sql"select value from $tableName where key = $int4".one(150, text),
          Some("howdies")
        )

        sql"select * from $tableName ".exec()

        sql"select * from pg_type ".exec()
      }
    }
  }

end SqlInterpolatorTests
