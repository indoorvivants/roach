import scala.scalanative.build.SourceLevelDebuggingConfig
import bindgen.plugin.BindgenMode
import java.nio.file.Paths

Global / onChangedBuildSource := ReloadOnSourceChanges

val Versions = new {
  val Scala = "3.3.3"

  val circe = "0.14.9"

  val munit = "1.0.3"

  val upickle = "4.1.0"
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
      Test / nativeConfig ~= (_.withEmbedResources(true)
        .withSourceLevelDebuggingConfig(SourceLevelDebuggingConfig.enabled)),
      Compile / bindgenBindings += {
        val configurator = vcpkgConfigurator.value

        Binding(configurator.includes("libpq") / "libpq-fe.h", "libpq")
          .withLinkName("pq")
          .addCImport("libpq-fe.h")
          .withClangFlags(
            configurator.pkgConfig
              .updateCompilationFlags(List("-std=gnu99"), "libpq")
              .toList
          )
          .withNoLocation(true)
      },
      bindgenMode := BindgenMode.Manual(
        sourceDirectory.value / "main" / "scala" / "generated",
        (Compile / resourceDirectory).value / "scala-native"
      ),
      moduleName := "core",
      configurePlatform
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
    .settings(configurePlatform)

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
    .settings(configurePlatform)

val common = Seq(
  organization := "com.indoorvivants.roach",
  scalaVersion := Versions.Scala,
  libraryDependencies += "org.scalameta" %%% "munit" % Versions.munit % Test,
  resolvers ++= Resolver.sonatypeOssRepos("snapshots"),
  vcpkgDependencies := VcpkgDependencies("libpq")
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

val configurePlatform = Seq(
  nativeConfig := {
    import com.indoorvivants.detective.*
    val conf = nativeConfig.value
    val arch64 =
      if (
        Platform.arch == Platform.Arch.Arm && Platform.bits == Platform.Bits.x64
      )
        List("-arch", "arm64")
      else Nil

    conf
      .withLinkingOptions(
        conf.linkingOptions ++ arch64
      )
      .withCompileOptions(
        conf.compileOptions ++ arch64
      )
      .withIncrementalCompilation(true)
      .withMultithreading(true)
  }
)
