# Roach

A toy database access "library" for Postgres, in Scala Native.

See [blog post](https://blog.indoorvivants.com/2022-03-04-twotm8-part-2-postgres-and-openssl.html) for details.

This is pre-pre-pre-pre-alpha software - it was enough to create a functioning app, but it's tremendously 
incomplete and it will take a lot of time and effort to make it usable.

Pre-requisites:

- For runtime, `libpq5` (debian) or similar has to be installed 
   - Unless you are linking your binary statically, of course
- For Scala Native, LLVM has to be installed.

## Usage

Assuming you have postgres running:

```bash 
$ docker run -p 5432:5432 -e POSTGRES_PASSWORD=mysecretpassword -d postgres
```


### Basics

```scala mdoc:compile-only
//> using platform "scala-native"
//> using lib "com.indoorvivants.roach::core::0.0.2"

import roach.*
import scala.util.Using
import scala.scalanative.unsafe.Zone

import roach.codecs.*

val connectionString =
  "postgresql://postgres:mysecretpassword@localhost:5432/postgres"

Zone { implicit z =>
  Pool.single(connectionString) { pool => 
    pool.lease { db => 
      db.execute("select typname, typisdefined from pg_type").getOrThrow.use {
        res =>
          val rows: Vector[(String, Boolean)] = res.readAll(name ~ bool)

          rows.foreach(println)
      }

      db.executeParams("select oid::int4 from pg_type where typname = $1", varchar, "bool").getOrThrow.use { res =>
        val row: Option[Int] = res.readOne(int4)

        assert(row.contains(16))
      }
    }
  }
}
```


### Gratuitous `Query` abstraction

This is the raw API without any macro magic. For macro magic see the `sql"..."` interpolator section below.

```scala mdoc:compile-only
import roach.*
import roach.codecs.*
import scalanative.unsafe.*

def example(using Database, Zone) = 
  // a query with input parameters and a result
  Query("select count(*) from my_table where x = $1", int4).all(25, int4)
  Query("select count(*) from my_table where x = $1", int4).one(25, int4)

  // a query with no input parameters and a result
  Query("select count(*) from my_table").all(int4)
  Query("select count(*) from my_table").one(int4)

  // a query with no input parameters and no result
  Query("select count(*) from my_table").exec()
```

### `sql"..."` interpolator

Similar to [Skunk](https://tpolecat.github.io/skunk/tutorial/Query.html)'s, just a lot less advanced.

```scala mdoc:compile-only
import roach.*
import roach.codecs.*
import scalanative.unsafe.*

def example(using Database, Zone) = 
  sql"select count(*) from my_table where x = $int4".all(25, int4)
  sql"select count(*) from my_table where x = $int4 and y = $text".count(25 -> "hello")
  sql"select count(*) from my_table".count()
  sql"select count(*) from my_table where x = $int4 and y = $text".count(25 -> "hello")

  case class Data(key: Int, value: String)
  val rc = (int4 ~ text).as[Data]
  val tableName = "my_table"
  sql"insert into $tableName values ($rc)".exec(Data(150, "howdies"))
```

Note that the parameters to the query are verified at compile time, not runtime, 
i.e. these usages will not compile:

```scala mdoc:fail
import roach.*
import roach.codecs.*
import scalanative.unsafe.*

def example_failure(using Database, Zone) = 
  //  Found:    ("hello" : String)
  //  Required: Int
  //    sql"select count(*) from my_table where x = $int4".count("hello")
  //                                                             ^^^^^^^
  sql"select count(*) from my_table where x = $int4".count("hello")

  // Found:    (String, Int)
  // Required: (Int, String)
  //   sql"select count(*) from my_table where x = $int4 and y = $text".count("hello" -> 25)
  //                                                                          ^^^^^^^^^^^^^
  sql"select count(*) from my_table where x = $int4 and y = $text".count("hello" -> 25)
```

`sql` interpolator produces a `Query`, so you can use it as described in the previous section

### Simple JSON module (with Circe)

Available at `com.indoorvivants.roach::circe::0.0.2` maven coordinates.

```scala mdoc:compile-only
import roach.*
import roach.codecs.*
import scalanative.unsafe.*

import roach.circe.*

def json_example(using Database, Zone) = 
  // returning io.circe.Json directly
  Query("""SELECT '{"bar": "baz", "balance": 7.77, "active": false}'::json""").one(json)

  // returning a class with a derived codec
  case class Test(hello: String, bla: Int) derives io.circe.Codec.AsObject
  Query("""SELECT '{"hello": "baz", "bla": 7}'::json""").one(jsonOf[Test])
```

### Migrations

For this to work, make sure you have [Resource Embedding](https://scala-native.org/en/stable/lib/javalib.html?highlight=resources#embedding-resources) enabled.

Because in Scala Native we currently cannot have a resource listing, we need to specify all the migration files manually:

```scala mdoc:compile-only
import roach.*
import roach.codecs.*
import scalanative.unsafe.*

def migrate_example(pool: Pool)(using Zone) = 
  Migrate.all(pool)(
    ResourceFile("/v001.sql"),
    ResourceFile("/v002.sql"),
    ResourceFile("/v003.sql")
  )
```

The existence of those resource files will be checked at compile time, and in case the file doesn't exist, you will get a _compilation_ error:

```
/v003.sql doesn't exist as a resource
    ResourceFile("/v003.sql")
    ^^^^^^^^^^^^^^^^^^^^^^^^^
```

The migrations state will be persisted in a table named `__roach_migrations` (unless you override it using second parameter to `all`. 

Name of the file (not the full path) is used to identify the migration.

Note that `Migrate.all` takes an exclusive lock of its internal table and runs all migrations within that - to prevent any concurrent modification.
