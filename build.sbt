name := "scalataiwan17"

scalaVersion := "2.12.2"
//scalaVersion := "0.1.2-RC1"

resolvers += Resolver.sonatypeRepo("releases")

addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.3")

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats" % "0.9.0",
  "io.frees" %% "freestyle" % "0.1.1",
  "io.frees" %% "freestyle-tagless" % "0.1.1",
  "org.scalaz" %% "scalaz-core" % "7.2.13",
  "com.typesafe.play" %% "play" % "2.6.0-RC1",
  "com.typesafe.play" %% "play-iteratees" % "2.6.1")


 scalacOptions ++= Seq("-Ypartial-unification")
