package roach

import scalanative.unsafe.Zone

object Migrate:
  def all(pool: Pool, tableName: String = defaultTableName)(
      files: ResourceFile*
  ): MigrationResult = Zone { implicit z =>
    pool.withLease(createTable(tableName))

    var rollback = false

    val applied = Vector.newBuilder[String]
    val present = Vector.newBuilder[String]

    pool.withLease { db ?=>

      db.command("BEGIN")
      db.command(s"LOCK TABLE $tableName IN ACCESS EXCLUSIVE MODE")
      try
        val current = readAll(tableName)
        present.addAll(current.map(_._2))

        val expected =
          files
            .map(_.filename)
            .zipWithIndex
            .map((el, i) => (i + 1, el))
            .toVector

        val state =
          s"Recorded migrations in DB: `${current.render}`, " + s"migrations expected: `${expected.render}`"

        assert(
          expected.startsWith(current),
          s"Migration state is inconsistent!\n$state"
        )

        assert(
          current.length <= expected.length,
          s"Migration state is inconsistent (database is AHEAD and contains migrations NOT passed to Migrate)\n$state"
        )

        val unapplied = files.drop(current.length)

        unapplied.foreach { rf =>
          if !rollback then
            val sql = read(rf)

            try
              db.command(sql)
              Query(
                s"insert into $tableName (filename, applied) values ($$1, now()) returning id",
                codecs.varchar
              ).exec(rf.filename)
              applied.addOne(rf.filename)
            catch
              case exc: RoachFatalException =>
                rollback = true
                throw exc
            end try

        }
      catch
        case exc: RoachFatalException =>
          throw new RoachFatalException(
            s"Migration was aborted due to a failure, the following migrations will be rolled back: `${applied.result().mkString(", ")}`",
            Some(exc)
          )
      finally db.execute("COMMIT")
      end try
    }

    MigrationResult(applied.result(), present.result())
  }

  case class MigrationResult(applied: Vector[String], present: Vector[String])

  private def read(rf: ResourceFile) =
    val resourceName = getClass().getResourceAsStream(rf.value)
    scala.io.Source
      .fromInputStream(resourceName)
      .getLines()
      .mkString(System.lineSeparator())

  extension (v: Vector[(Int, String)])
    private def render: String = v.mkString("[", ", ", "]")

  private val defaultTableName = "__roach_migrations"

  private inline def createTable(tableName: String)(using Database, Zone) =
    Query(s"""
    create table if not exists $tableName(
      id serial not null,
      filename varchar(255) not null,
      applied timestamptz not null
    )
    """).exec()

  private inline def readAll(tableName: String)(using Database, Zone) =
    import codecs.*
    Query(s"select id, filename from $tableName order by applied asc").all(
      int4 ~ varchar
    )
end Migrate
