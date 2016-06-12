name := "avro-d2-example"

version := "1.0.0-SNAPSHOT"

scalaVersion := "2.11.7"

lazy val root = project in file(".") enablePlugins(PlayJava)

libraryDependencies ++= Seq(
  "commons-io" % "commons-io" % "2.5",
  "me.tfeng.play-mods" % "avro-d2" % "0.8.7",
  javaWs % "test"
)

Avro.settings
