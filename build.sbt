import java.nio.file.Paths

Global / onChangedBuildSource := ReloadOnSourceChanges

val Versions = new {
  val Scala = "3.2.0"
}

import bindgen.interface.*

lazy val roach =
  project
    .in(file("."))
    .enablePlugins(
      ScalaNativePlugin,
      ScalaNativeJUnitPlugin,
      BindgenPlugin,
      VcpkgPlugin
    )
    .settings(
      scalaVersion := Versions.Scala,
      vcpkgDependencies := Set("libpq"),
      bindgenBindings += {
        Binding(
          vcpkgManager.value.includes("libpq") / "libpq-fe.h",
          "libpq",
          linkName = Some("pq"),
          cImports = List("libpq-fe.h"),
          clangFlags = vcpkgConfigurator.value
            .updateCompilationFlags(List("-std=gnu99"), "libpq")
            .toList
        )
      }
    )
    .settings(vcpkgNativeConfig())
    .settings(vcpkgNativeConfig(conf = Test))
    .settings(
      organization := "com.indoorvivants.roach",
      moduleName := "core",
      scalaVersion := Versions.Scala,
      resolvers += Resolver.sonatypeRepo("snapshots"),
      libraryDependencies += "com.eed3si9n.verify" %%% "verify" % "1.0.0" % Test,
      testFrameworks += new TestFramework("verify.runner.Framework"),
      Compile / packageSrc / mappings ++= {
        val base = (Compile / sourceManaged).value
        val files = (Compile / managedSources).value
        files.map { f => (f, f.relativeTo(base).get.getPath) }
      }
    )

def vcpkgNativeConfig(rename: String => String = identity, conf: Configuration = Compile) = Seq(
  conf / nativeConfig := {
    import com.indoorvivants.detective.Platform
    val configurator = vcpkgConfigurator.value
    val manager = vcpkgManager.value
    val conf = nativeConfig.value
    val deps = vcpkgDependencies.value.toSeq.map(rename)

    val files = deps.map(d => manager.files(d))

    val compileArgsApprox = files.flatMap { f =>
      List("-I" + f.includeDir.toString)
    }
    val linkingArgsApprox = files.flatMap { f =>
      List("-L" + f.libDir) ++ f.staticLibraries.map(_.toString)
    }

    import scala.util.control.NonFatal

    def updateLinkingFlags(current: Seq[String], deps: String*) =
      try {
        configurator.updateLinkingFlags(
          Seq.empty,
          deps*
        ) ++ current
      } catch {
        case NonFatal(exc) =>
          linkingArgsApprox ++ current
      }

    def updateCompilationFlags(current: Seq[String], deps: String*) =
      try {
        configurator.updateCompilationFlags(
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
