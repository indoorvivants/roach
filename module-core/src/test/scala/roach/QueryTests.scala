package roach.tests

import roach.*
import roach.codecs.*
import scala.scalanative.unsafe.Zone

class QueryTests extends munit.FunSuite, TestHarness:
  override def tableCreationSQL = Some(tableName => s"""
    create table $tableName(
      key int not null,
      value text not null
    );
    insert into $tableName values(25, 'hello');
    insert into $tableName values(42, 'bye');
    """)

  test("with input parameters") {
    val q = Query(s"select * from $tableName where key = $$1", int4)
    val qMore = Query(s"select * from $tableName where key >= $$1", int4)
    Zone {
      withDB {
        assertEquals(q.all(25, int4 ~ text), Vector(25 -> "hello"))
        assertEquals(q.all(150, int4 ~ text), Vector.empty)

        assertEquals(q.one(25, int4 ~ text), Some(25 -> "hello"))
        assertEquals(q.one(150, int4 ~ text), None)

        Query(s"insert into $tableName values (150, $$1) returning key", text)
          .exec("testies")

        assertEquals(qMore.count(25), 3)
        assertEquals(qMore.count(10000), 0)
      }
    }
  }

  test("without input parameters") {
    val q =
      Query(s"select * from $tableName where value != 'testies' order by key")

    Zone {
      withDB {
        assertEquals(q.all(int4 ~ text), Vector(25 -> "hello", 42 -> "bye"))

        assertEquals(q.one(int4 ~ text), Some(25 -> "hello"))

        Query(s"insert into $tableName values (1500, 'besties') returning key")
          .exec()

        assertEquals(q.count(), 3)
      }
    }
  }

end QueryTests
