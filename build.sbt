import java.nio.file.Paths

Global / onChangedBuildSource := ReloadOnSourceChanges

val Versions = new {
  val Scala = "3.1.1"
}

import bindgen.interface.*

lazy val roach =
  project
    .in(file("."))
    .enablePlugins(ScalaNativePlugin, ScalaNativeJUnitPlugin, BindgenPlugin)
    .settings(
      organization := "com.indoorvivants.roach",
      moduleName := "core",
      scalaVersion := Versions.Scala,
      resolvers += Resolver.sonatypeRepo("snapshots"),
      // Scala 3 hack around the issue with docs
      Compile / doc / scalacOptions ~= { opts =>
        opts.filterNot(_.contains("-Xplugin"))
      },
      // end of Scala 3 hack around the issue with docs
      // Generate bindings to Postgres main API
      bindgenBindings += {
        val postgresIfaces =
          (baseDirectory.value / "postgres" / "src" / "interfaces" / "libpq").toPath
        val postgresInclude =
          (baseDirectory.value / "postgres" / "src" / "include").toPath

        Binding(
          postgresIfaces.resolve("libpq-fe.h").toFile(),
          "libpq",
          linkName = Some("pq"),
          cImports = List("libpq-fe.h"),
          clangFlags = List(
            "-std=gnu99",
            s"-I$postgresInclude",
            "-fsigned-char"
          )
        )
      },
      libraryDependencies += "com.eed3si9n.verify" %%% "verify" % "1.0.0" % Test,
      testFrameworks += new TestFramework("verify.runner.Framework"),
      Compile / packageSrc / mappings ++= {
        val base = (Compile / sourceManaged).value
        val files = (Compile / managedSources).value
        files.map { f => (f, f.relativeTo(base).get.getPath) }
      }
    )

def postgresLib = {
  import Platform.*
  (os, arch) match {
    case (OS.MacOS, Arch.aarch64) =>
      Some(Paths.get("/opt/homebrew/opt/libpq/lib/"))
    case (OS.MacOS, Arch.x86_64) =>
      Some(Paths.get("/usr/local/opt/libpq/lib/"))
    case _ => None
  }
}

inThisBuild(
  Seq(
    organization := "com.indoorvivants",
    organizationName := "Anton Sviridov",
    homepage := Some(
      url("https://github.com/indoorvivants/roach")
    ),
    startYear := Some(2022),
    licenses := List(
      "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")
    ),
    developers := List(
      Developer(
        "keynmol",
        "Anton Sviridov",
        "velvetbaldmime@protonmail.com",
        url("https://blog.indoorvivants.com")
      )
    )
  )
)
