name := "mongodb-example"

version := "1.0.0-SNAPSHOT"

scalaVersion := "2.12.2"

lazy val root = project in file(".") enablePlugins(PlayJava)

libraryDependencies ++= Seq(
  "me.tfeng.play-mods" % "avro" % "0.11.0",
  "me.tfeng.toolbox" % "mongodb" % "0.11.0",
  javaWs % "test"
)

Avro.settings
