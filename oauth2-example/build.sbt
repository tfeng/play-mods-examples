name := "oauth2-example"

version := "1.0.0-SNAPSHOT"

scalaVersion := "2.12.2"

lazy val root = project in file(".") enablePlugins(PlayJava)

libraryDependencies ++= Seq(
  ehcache,
  "me.tfeng.play-mods" % "oauth2" % "0.11.0",
  "org.hamcrest" % "hamcrest-all" % "1.3" % "test",
  javaWs % "test"
)
