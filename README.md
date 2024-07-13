# Roach

<!--toc:start-->
- [Roach](#roach)
  - [Goals](#goals)
  - [Usage](#usage)
    - [Basics](#basics)
    - [`sql"..."` interpolator](#sql-interpolator)
    - [Fragments](#fragments)
    - [Simple JSON module (with Circe and upickle)](#simple-json-module-with-circe-and-upickle)
    - [Migrations](#migrations)
<!--toc:end-->

A very simple database access library for Postgres, in Scala 3 Native.
It is based on [bindings](./module-core/src/main/scala/generated/libpq.scala) to libpq produced by 
[my binding generator](https://sn-bindgen.indoorvivants.com/)

See [blog post](https://blog.indoorvivants.com/2022-03-04-twotm8-part-2-postgres-and-openssl.html) for details.

This is pre-alpha software - it is enough to create a functioning app, but it's tremendously 
incomplete and it will take a lot of time and effort to make it usable.

Examples:
- [Twotm8](https://github.com/twotm8-com/twotm8.com/blob/main/app/src/main/scala/db.scala) - 
  basic usage, codecs and migrations
- [Scala Boot](https://github.com/indoorvivants/scala-boot/blob/main/mod/server/src/main/scala/Db.scala) - more advanced usage, including fragments

Pre-requisites:

- For runtime, `libpq5` (debian) or similar has to be installed 
   - Unless you are linking your binary statically, of course - in that case 
     I highly recommend using sn-vcpkg: https://github.com/indoorvivants/sn-vcpkg#sbt
- For Scala Native, Clang has to be installed.

## Goals 

- Direct SQL usage, no ORM
- Minimal overhead on top of `libpq` - unless the overhead is required to improve safety and 
  user experience
- [Skunk](https://github.com/typelevel/skunk)-like codecs and `sql"..."` interpolator
- Direct style (no IO monad)
- Having fun

## Usage

Assuming you have postgres running:

```bash 
$ docker run -p 5432:5432 -e POSTGRES_PASSWORD=mysecretpassword -d postgres
```


### Basics

```scala mdoc:compile-only
//> using platform "scala-native"
//> using lib "com.indoorvivants.roach::core::0.1.0"

import roach.*
import scala.util.Using
import scala.scalanative.unsafe.Zone

import roach.codecs.*

val connectionString =
  "postgresql://postgres:mysecretpassword@localhost:5432/postgres"

Zone:
  Pool.single(connectionString): pool => 
    pool.withLease:
       sql"select typname, typisdefined from pg_type".all(name ~ bool).foreach(println)

       // with parameters now
       val oid: Option[Int] = 
          sql"select oid::int4 from pg_type where typname = $varchar".one("bool", int4)
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

### Fragments

Fragments allow you to extract certain parts of the 
SQL query into a separate object and optionally apply 
it to some codecs, which will become inputs in the final query

```scala mdoc:compile-only 
import roach.*
import roach.codecs.*
import scalanative.unsafe.*

def fragments_example(using Database, Zone) = 
    // sharing list of fields
    val normal = Fragment("key, value")

    val q: Vector[(Int, String)] = 
        sql"select $normal from my_table where key = 25".all(int4 ~ text)
    
    // sharing both list of fields, but also 
    // indicating that this fragment 
    // affects the input of the entire query
    val withInput = normal.applied(int4 ~ text)

    val q1 =
      sql"insert into my_table(${withInput.sql}) values ($withInput) returning key"

    val result: Option[Int] = q1.one(125 -> "yo", int4)
```

### Simple JSON module (with Circe and upickle)

- **Circe**: Available at `com.indoorvivants.roach::circe::0.1.0` maven coordinates.
- **Upickle**: Available at `com.indoorvivants.roach::upickle::0.1.0` maven coordinates.

```scala mdoc:compile-only
import roach.{upickle => _, circe => _, *}
import roach.codecs.*
import scalanative.unsafe.*

def circe_example(using Database, Zone) = 
  import roach.circe.*
  // returning io.circe.Json directly
  sql"""SELECT '{"bar": "baz", "balance": 7.77, "active": false}'::json""".one(json)

  // returning a class with a derived codec
  case class Test(hello: String, bla: Int) derives io.circe.Codec.AsObject
  sql"""SELECT '{"hello": "baz", "bla": 7}'::json""".one(jsonOf[Test])


def upickle_example(using Database, Zone) = 
  import roach.upickle.*
  // returning ujson.Value
  sql"""SELECT '{"bar": "baz", "balance": 7.77, "active": false}'::json""".one(json)

  // returning a class with a derived codec
  case class Test(hello: String, bla: Int) derives upickle.default.ReadWriter
  sql"""SELECT '{"hello": "baz", "bla": 7}'::json""".one(jsonOf[Test])
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
