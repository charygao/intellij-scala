resolvers += Resolver.url("jetbrains-bintray",
  url("https://dl.bintray.com/jetbrains/sbt-plugins/"))(Resolver.ivyStylePatterns)

addSbtPlugin("org.jetbrains" % "sbt-ide-settings" % "1.0.0")