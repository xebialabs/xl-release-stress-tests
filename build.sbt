name := "xlr-stress-tests"

version := "0.1"

scalaVersion := "2.12.4"

scalacOptions ++= Seq(
  "-feature",
  "-deprecation",
  "-Xplugin-require:macroparadise",
  "-Ypartial-unification",
  "-language:higherKinds"
)


addCompilerPlugin("org.scalameta" % "paradise" % "3.0.0-M10" cross CrossVersion.full)
addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.6")

libraryDependencies ++= Seq(
  "io.frees" %% "frees-core" % "0.8.0",
  "io.frees" %% "frees-logging" % "0.8.0",
  "org.typelevel" %% "cats-core" % "1.1.0",
  "org.typelevel" %% "cats-free" % "1.1.0",
  "org.typelevel" %% "cats-effect" % "1.0.0-RC",
  "com.typesafe.akka" %% "akka-http" % "10.1.1",
  "com.typesafe.akka" %% "akka-stream" % "2.5.11",
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.1",
  "commons-io" % "commons-io" % "2.5",
  "com.github.nscala-time" %% "nscala-time" % "2.18.0"
)


publishTo := {
  val nexus = "https://nexus.xebialabs.com/nexus"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "content/repositories/releases")
}
