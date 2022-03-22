# Roach

A toy database access "library" for Postgres, in Scala Native.

See [blog post](https://blog.indoorvivants.com/2022-03-04-twotm8-part-2-postgres-and-openssl.html) for details.

This is pre-pre-pre-pre-alpha software - it was enough to create a functioning app, but it's tremendously 
incomplete and it will take a lot of time and effort to make it usable.

Pre-requisites:

1. For compilation and linking, `libpq-dev` (debian) or similar package has to be installed
2. For runtime, `libpq5` (debian) or similar has to be installed 
3. For Scala Native, LLVM has to be installed.

## Example using [scala-cli](https://scala-cli.virtuslab.org/)

Assuming you have postgres running:

```bash 
$ docker run -p 5432:5432 -e POSTGRES_PASSWORD=mysecretpassword -d postgres
```

```scala 
//> using repository "sonatype:snapshots"
//> using platform "scala-native"
//> using lib "com.indoorvivants.roach::core::0.0.0+11-5fe82ae3-SNAPSHOT"

import roach.*
import scala.util.Using
import scala.scalanative.unsafe.Zone

@main def hello =
  val connectionString =
    "postgresql://postgres:mysecretpassword@localhost:5432/postgres"
  Zone { implicit z =>
    Using.resource(Database(connectionString).getOrThrow) { db =>
      Using.resource(db.execute("select typname from pg_type").getOrThrow) {
        res =>
          val rows = res.readAll(codecs.name)

          rows.foreach(println)
      }
    }
  }
end hello
```
