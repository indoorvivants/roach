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
