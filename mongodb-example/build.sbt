name := "mongodb-example"

version := "1.0.0-SNAPSHOT"

scalaVersion := "2.11.7"

lazy val root = project in file(".") enablePlugins(PlayJava)

libraryDependencies ++= Seq(
  "me.tfeng.play-mods" % "avro" % "0.8.5",
  "me.tfeng.toolbox" % "mongodb" % "0.8.5",
  javaWs % "test"
)

Avro.settings
