package roach

import scalanative.unsafe.Zone

object Migrate:
  def all(pool: Pool, tableName: String = defaultTableName)(
      files: ResourceFile*
  ): MigrationResult = Zone:
    pool.withLease(createTable(tableName))

    var rollback = false

    val applied = Vector.newBuilder[String]
    val present = Vector.newBuilder[String]

    pool.withLease { db ?=>
      db.command("BEGIN")

      try
        db.command(s"LOCK TABLE $tableName IN ACCESS EXCLUSIVE MODE")
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

        if !expected.startsWith(current) then
          RoachError.MigrationStateInconsistent.wrongPrefix(state).raise

        if current.length > expected.length then
          RoachError.MigrationStateInconsistent.databaseIsAhead(state).raise

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
              case exc: RoachError =>
                rollback = true
                throw exc
            end try

        }
      catch
        case exc =>
          RoachError
            .MigrationAbortedWithReason(applied.result(), exc)
            .raise
      finally db.execute("COMMIT")
      end try
    }

    MigrationResult(applied.result(), present.result())

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
