name := "oauth2-avro-d2-example"

version := "1.0.0-SNAPSHOT"

scalaVersion := "2.11.7"

lazy val root = project in file(".") enablePlugins(PlayJava)

libraryDependencies ++= Seq(
  "commons-io" % "commons-io" % "2.5",
  "me.tfeng.play-mods" % "avro-d2" % "0.8.9",
  "me.tfeng.play-mods" % "oauth2" % "0.8.9",
  javaWs % "test"
)

Avro.settings
