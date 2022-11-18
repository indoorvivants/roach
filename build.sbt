import bindgen.plugin.BindgenMode
import java.nio.file.Paths

Global / onChangedBuildSource := ReloadOnSourceChanges

val Versions = new {
  val Scala = "3.2.1"
}

import bindgen.interface.*

lazy val root =
  project.in(file(".")).aggregate(core, circe).settings(publish / skip := true)

lazy val core =
  project
    .in(file("module-core"))
    .enablePlugins(
      ScalaNativePlugin,
      ScalaNativeJUnitPlugin,
      BindgenPlugin,
      VcpkgPlugin
    )
    .settings(common)
    .settings(
      Test / nativeConfig ~= (_.withEmbedResources(true)),
      Compile / bindgenBindings += {
        val configurator = vcpkgConfigurator.value

        Binding(
          configurator.includes("libpq") / "libpq-fe.h",
          "libpq",
          linkName = Some("pq"),
          cImports = List("libpq-fe.h"),
          clangFlags = configurator.pkgConfig
            .updateCompilationFlags(List("-std=gnu99"), "libpq")
            .toList
        )
      },
      moduleName := "core",
      Compile / packageSrc / mappings ++= {
        val base = (Compile / sourceManaged).value
        val files = (Compile / managedSources).value
        files.map { f => (f, f.relativeTo(base).get.getPath) }
      }
    )

lazy val circe =
  project
    .in(file("module-circe"))
    .dependsOn(core % "compile->compile;test->test")
    .enablePlugins(ScalaNativePlugin, VcpkgPlugin, BindgenPlugin)
    .settings(libraryDependencies += "io.circe" %%% "circe-parser" % "0.14.3")
    .settings(moduleName := "circe")
    .settings(common)

val common = Seq(
  organization := "com.indoorvivants.roach",
  scalaVersion := Versions.Scala,
  libraryDependencies += "org.scalameta" %%% "munit" % "1.0.0-M7" % Test,
  resolvers ++= Resolver.sonatypeOssRepos("snapshots"),
  vcpkgDependencies := Set("libpq")
) ++ vcpkgNativeConfig() ++ vcpkgNativeConfig(conf = Test)

lazy val docs =
  project
    .in(file("target/.docs-target"))
    .enablePlugins(MdocPlugin)
    .settings(scalaVersion := Versions.Scala)
    .dependsOn(core, circe)
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

def vcpkgNativeConfig(
    rename: String => String = identity,
    conf: Configuration = Compile
) = Seq(
  conf / nativeConfig := {
    import com.indoorvivants.detective.Platform
    val configurator = vcpkgConfigurator.value
    val conf = nativeConfig.value
    val deps = vcpkgDependencies.value.toSeq.map(rename)

    val files = deps.map(d => configurator.files(d))

    val compileArgsApprox = files.flatMap { f =>
      List("-I" + f.includeDir.toString)
    }
    val linkingArgsApprox = files.flatMap { f =>
      List("-L" + f.libDir) ++ f.staticLibraries.map(_.toString)
    }

    import scala.util.control.NonFatal

    def updateLinkingFlags(current: Seq[String], deps: String*) =
      try {
        configurator.pkgConfig.updateLinkingFlags(
          Seq.empty,
          deps*
        ) ++ current
      } catch {
        case NonFatal(exc) =>
          linkingArgsApprox ++ current
      }

    def updateCompilationFlags(current: Seq[String], deps: String*) =
      try {
        configurator.pkgConfig.updateCompilationFlags(
          Seq.empty,
          deps*
        ) ++ current
      } catch {
        case NonFatal(exc) =>
          compileArgsApprox ++ current
      }

    val arch64 =
      if (
        Platform.arch == Platform.Arch.Arm && Platform.bits == Platform.Bits.x64
      )
        List("-arch", "arm64")
      else Nil

    conf
      .withLinkingOptions(
        updateLinkingFlags(
          conf.linkingOptions ++ arch64,
          deps*
        )
      )
      .withCompileOptions(
        updateCompilationFlags(
          conf.compileOptions ++ arch64,
          deps*
        )
      )
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
