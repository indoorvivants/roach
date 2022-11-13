package roach.tests

import scala.util.Using
import scala.scalanative.unsafe.Zone

import roach.*
import scala.util.Try

trait TestHarness:
  self: munit.FunSuite =>
  protected def appName =
    s"roach_tests${getClass().getSimpleName().replaceAllLiterally("$", "_")}"
  val connectionString =
    s"postgresql://postgres:mysecretpassword@localhost:5432/postgres?application_name=$appName"

  inline def zone[A](inline f: Zone ?=> A) = Zone.apply(z => f(using z))

  def withDB(f: Database ?=> Unit)(using Zone) =
    Using.resource(Database(connectionString).getOrThrow)(db => f(using db))

  def query[A](q: String)(f: Result => A)(using Zone)(using db: Database): A =
    Using.resource(db.execute(q).getOrThrow)(f)

  protected def tablePrefix: String = "roach_tests"
  protected def tableCreationSQL: Option[String => String] = None

  protected var tableName: String | Null = null

  def cleanup(p: Pool)(tableName: String*)(f: () => Unit)(using z: Zone) =
    try f()
    finally tableName.foreach(t => Try(p.lease(_.command(s"drop table $t"))))

  // Runs once before all tests start.
  override def beforeAll(): Unit =
    tableCreationSQL.foreach { sql =>
      zone {
        withDB { db ?=>
          val prepTableName = s"${tablePrefix}_${util.Random().nextLong.abs}"
          db.execute(sql(prepTableName)).getOrThrow

          tableName = prepTableName
        }
      }
    }

  override def afterAll(): Unit =
    zone {
      withDB { db ?=>
        if tableName != null then
          db.execute(s"drop table $tableName").getOrThrow
      }
    }
end TestHarness
