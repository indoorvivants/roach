val BindgenVersion =
  sys.env.getOrElse("SN_BINDGEN_VERSION", "0.0.11")

resolvers += Resolver.sonatypeRepo("snapshots")

addSbtPlugin("com.indoorvivants" % "bindgen-sbt-plugin" % BindgenVersion)
addSbtPlugin("org.scala-native" % "sbt-scala-native" % "0.4.4")
addSbtPlugin("com.geirsson" % "sbt-ci-release" % "1.5.7")
