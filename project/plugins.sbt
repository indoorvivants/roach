val BindgenVersion =
  sys.env.getOrElse("SN_BINDGEN_VERSION", "0.0.14")

val VcpkgVersion =
  sys.env.getOrElse("SBT_VCPKG_VERSION", "0.0.9")

resolvers += Resolver.sonatypeRepo("snapshots")
resolvers += Resolver.sonatypeRepo("releases")

addSbtPlugin("com.indoorvivants" % "bindgen-sbt-plugin" % BindgenVersion)
addSbtPlugin("com.indoorvivants.vcpkg" % "sbt-vcpkg" % VcpkgVersion)

addSbtPlugin("org.scala-native" % "sbt-scala-native" % "0.4.9")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.0")
addSbtPlugin("org.scalameta" % "sbt-mdoc" % "2.3.6")
addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.5.11")
