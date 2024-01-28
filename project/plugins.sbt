val BindgenVersion =
  sys.env.getOrElse("SN_BINDGEN_VERSION", "0.0.24")

val VcpkgVersion =
  sys.env.getOrElse("SBT_VCPKG_VERSION", "0.0.9+25-bb4dee7a-SNAPSHOT")

resolvers += Resolver.sonatypeRepo("snapshots")
resolvers += Resolver.sonatypeRepo("releases")

addSbtPlugin("com.indoorvivants" % "bindgen-sbt-plugin" % BindgenVersion)
addSbtPlugin("com.indoorvivants.vcpkg" % "sbt-vcpkg-native" % VcpkgVersion)

addSbtPlugin("org.scala-native" % "sbt-scala-native" % "0.4.17")

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.2")

addSbtPlugin("org.scalameta" % "sbt-mdoc" % "2.3.8")

addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.5.12")
