import bindgen.plugin.BindgenMode
import java.nio.file.Paths

Global / onChangedBuildSource := ReloadOnSourceChanges

val Versions = new {
  val Scala = "3.3.1"

  val circe = "0.14.6"

  val munit = "1.0.0-M8"

  val upickle = "3.1.3"
}

import bindgen.interface.*

lazy val root =
  project
    .in(file("."))
    .aggregate(core, circe, upickle)
    .settings(publish / skip := true)

lazy val core =
  project
    .in(file("module-core"))
    .enablePlugins(
      ScalaNativePlugin,
      ScalaNativeJUnitPlugin,
      BindgenPlugin,
      VcpkgNativePlugin
    )
    .settings(common)
    .settings(
      Test / nativeConfig ~= (_.withEmbedResources(true)),
      Compile / bindgenBindings += {
        val configurator = vcpkgConfigurator.value

        Binding
          .builder(configurator.includes("libpq") / "libpq-fe.h", "libpq")
          .withLinkName("pq")
          .addCImport("libpq-fe.h")
          .withClangFlags(
            configurator.pkgConfig
              .updateCompilationFlags(List("-std=gnu99"), "libpq")
              .toList
          )
          .build
      },
      bindgenMode := BindgenMode.Manual(
        sourceDirectory.value / "main" / "scala" / "generated",
        (Compile / resourceDirectory).value / "scala-native"
      ),
      moduleName := "core"
    )

lazy val upickle =
  project
    .in(file("module-upickle"))
    .dependsOn(core % "compile->compile;test->test")
    .enablePlugins(ScalaNativePlugin, VcpkgNativePlugin, BindgenPlugin)
    .settings(
      libraryDependencies += "com.lihaoyi" %%% "upickle" % Versions.upickle
    )
    .settings(moduleName := "upickle")
    .settings(common)

lazy val circe =
  project
    .in(file("module-circe"))
    .dependsOn(core % "compile->compile;test->test")
    .enablePlugins(ScalaNativePlugin, VcpkgNativePlugin, BindgenPlugin)
    .settings(
      libraryDependencies += "io.circe" %%% "circe-parser" % Versions.circe
    )
    .settings(moduleName := "circe")
    .settings(common)

val common = Seq(
  organization := "com.indoorvivants.roach",
  scalaVersion := Versions.Scala,
  libraryDependencies += "org.scalameta" %%% "munit" % Versions.munit % Test,
  resolvers ++= Resolver.sonatypeOssRepos("snapshots"),
  vcpkgDependencies := Set("libpq")
)

lazy val docs =
  project
    .in(file("target/.docs-target"))
    .enablePlugins(MdocPlugin)
    .settings(scalaVersion := Versions.Scala)
    .dependsOn(core, circe, upickle)
    .settings(
      publish / skip := true,
      Compile / resourceGenerators += Def.task {
        val folder = (Compile / unmanagedResourceDirectories).value.head

        for {
          i <- (1 to 3).toSeq
          name = s"v00$i.sql"
          _ = IO.write(folder / name, "")
        } yield folder / name

      }
    )

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

addCommandAlias("checkDocs", "docs/mdoc --in README.md")
