package roach.tests

import roach.codecs.*
import roach.*

class MigrationTests extends munit.FunSuite, TestHarness:
  test("basic usage") {
    zone {
      Pool.single(connectionString) { pool =>
        cleanup(pool)("migration_test_1", "howdy") { () =>

          Migrate.all(pool, tableName = "migration_test_1")(
            ResourceFile("/test_1.1.sql"),
            ResourceFile("/test_1.2.sql"),
            ResourceFile("/test_1.3.sql")
          )

          assertEquals(
            pool.lease(
              _.execute(
                "insert into howdy (test3, test2) values (now(), 'yoooo') returning id"
              ).getOrThrow.use(_.readOne(int4))
            ),
            Option(1)
          )

        }
      }
    }

  }

  test("migration is idempotent") {
    val tst = "migration_test_2"
    zone {
      Pool.single(connectionString) { pool =>
        cleanup(pool)(tst, "howdy") { () =>

          Migrate.all(pool, tableName = tst)(
            ResourceFile("/test_1.1.sql"),
            ResourceFile("/test_1.2.sql"),
            ResourceFile("/test_1.3.sql")
          )

          assertEquals(
            pool.lease(
              _.execute(
                "insert into howdy (test3, test2) values (now(), 'yoooo') returning id"
              ).getOrThrow.use(_.readOne(int4))
            ),
            Option(1)
          )

          Migrate.all(pool, tableName = tst)(
            ResourceFile("/test_1.1.sql"),
            ResourceFile("/test_1.2.sql"),
            ResourceFile("/test_1.3.sql")
          )

          assertEquals(
            pool.lease(
              _.execute(
                "insert into howdy (test3, test2) values (now(), 'yoooo') returning id"
              ).getOrThrow.use(_.readOne(int4))
            ),
            Option(2)
          )

        }
      }
    }

  }

  test("incorrect states") {
    zone {
      val tst = "migration_test_1"
      Pool.single(connectionString) { pool =>
        cleanup(pool)(tst, "howdy") { () =>
          Migrate.all(pool, tableName = tst)(
            ResourceFile("/test_1.1.sql"),
            ResourceFile("/test_1.2.sql"),
            ResourceFile("/test_1.3.sql")
          )

          // prefixes don't match
          intercept[java.lang.AssertionError] {
            Migrate.all(pool, tableName = tst)(
              ResourceFile("/test_1.2.sql"),
              ResourceFile("/test_1.1.sql"),
              ResourceFile("/test_1.3.sql")
            )
          }

          // database is ahead
          intercept[java.lang.AssertionError] {
            Migrate.all(pool, tableName = tst)(
              ResourceFile("/test_1.1.sql"),
              ResourceFile("/test_1.2.sql")
            )
          }

        }
      }
    }

  }

  test("migration state is recorded") {
    zone {
      val tst = "migration_test_1"
      Pool.single(connectionString) { pool =>
        cleanup(pool)(tst, "howdy") { () =>
          Migrate.all(pool, tableName = tst)(
            ResourceFile("/test_1.1.sql"),
            ResourceFile("/test_1.2.sql"),
            ResourceFile("/test_1.3.sql")
          )

          val rec = pool.withLease(
            Query(s"select id, filename from $tst order by applied asc").all(
              int4 ~ varchar
            )
          )

          assertEquals(
            rec,
            Vector(
              1 -> "test_1.1.sql",
              2 -> "test_1.2.sql",
              3 -> "test_1.3.sql"
            )
          )

        }
      }
    }

  }

  test("migration is aborted entirely if one step fails") {
    zone {
      val tst = "migration_test_1"
      Pool.single(connectionString) { pool =>
        cleanup(pool)(tst, "howdy") { () =>
          intercept[RoachFatalException] {
            Migrate.all(pool, tableName = tst)(
              ResourceFile("/test_1.1.sql"),
              ResourceFile("/test_1.2.sql"),
              ResourceFile("/test_1.bad.sql"),
              ResourceFile("/test_1.3.sql")
            )
          }

          val rec = pool.withLease(
            Query(s"select id, filename from $tst order by applied asc").all(
              int4 ~ varchar
            )
          )

          assertEquals(
            rec,
            Vector()
          )
        }
      }
    }

  }

  test("partial migration is aborted if 1 step failed") {
    zone {
      val tst = "migration_test_1"
      Pool.single(connectionString) { pool =>
        cleanup(pool)(tst, "howdy") { () =>
          Migrate.all(pool, tableName = tst)(
            ResourceFile("/test_1.1.sql"),
            ResourceFile("/test_1.2.sql")
          )

          intercept[RoachFatalException] {
            Migrate.all(pool, tableName = tst)(
              ResourceFile("/test_1.1.sql"),
              ResourceFile("/test_1.2.sql"),
              ResourceFile("/test_1.bad.sql"),
              ResourceFile("/test_1.3.sql")
            )
          }

          assertEquals(
            pool.withLease(
              Query(s"select id, filename from $tst order by applied asc").all(
                int4 ~ varchar
              )
            ),
            Vector(
              1 -> "test_1.1.sql",
              2 -> "test_1.2.sql"
            )
          )

          assertEquals(
            pool.lease(
              _.execute(
                "insert into howdy (test3) values (now()) returning id"
              ).getOrThrow.readOne(int4)
            ),
            Some(1)
          )

          assert(
            pool.lease(
              _.execute(
                "insert into howdy (test3, test2) values (now(), 'yoooo') returning id"
              ).either.isLeft
            )
          )
        }
      }
    }

  }

end MigrationTests