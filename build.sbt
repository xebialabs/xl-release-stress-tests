name := "xlr-stress-tests"

version := "0.1"

scalaVersion := "2.12.4"

scalacOptions ++= Seq(
  "-feature",
  "-Xplugin-require:macroparadise",
  "-Ypartial-unification",
  "-language:higherKinds"
)


//resolvers ++= Seq(
//  "XebiaLabs Releases" at "https://nexus.xebialabs.com/nexus/content/repositories/releases/",
//  "XebiaLabs Snapshots" at "https://nexus.xebialabs.com/nexus/content/repositories/snapshots/"
//)

addCompilerPlugin("org.scalameta" % "paradise" % "3.0.0-M10" cross CrossVersion.full)
addSbtPlugin("org.lyranthe.sbt" % "partial-unification" % "1.1.0")


libraryDependencies ++= Seq(
  "io.frees" %% "frees-core" % "0.8.0",
  "org.typelevel" %% "cats-core" % "1.0.1",
  "com.typesafe.akka" %% "akka-http" % "10.1.1",
  "com.typesafe.akka" %% "akka-stream" % "2.5.11",
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.1",
  "commons-io" % "commons-io" % "2.5"
)


publishTo := {
  val nexus = "https://nexus.xebialabs.com/nexus"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "content/repositories/releases")
}