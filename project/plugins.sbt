val BindgenVersion =
  sys.env.getOrElse("SN_BINDGEN_VERSION", "0.1.4")

val VcpkgVersion =
  sys.env.getOrElse("SBT_VCPKG_VERSION", "0.0.18")

resolvers += Resolver.sonatypeRepo("snapshots")
resolvers += Resolver.sonatypeRepo("releases")

addSbtPlugin("com.indoorvivants" % "bindgen-sbt-plugin" % BindgenVersion)
addSbtPlugin("com.indoorvivants.vcpkg" % "sbt-vcpkg-native" % VcpkgVersion)

addSbtPlugin("org.scala-native" % "sbt-scala-native" % "0.5.6")

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.2")

addSbtPlugin("org.scalameta" % "sbt-mdoc" % "2.6.1")

addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.9.0")

libraryDependencies += "com.indoorvivants.detective" %% "platform" % "0.1.0"
