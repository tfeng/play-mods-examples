name := "oauth2-avro-d2-example"

version := "1.0.0-SNAPSHOT"

scalaVersion := "2.12.2"

lazy val root = project in file(".") enablePlugins(PlayJava)

libraryDependencies ++= Seq(
  "commons-io" % "commons-io" % "2.5",
  "me.tfeng.play-mods" % "avro-d2" % "0.10.0-SNAPSHOT",
  "me.tfeng.play-mods" % "oauth2" % "0.10.0-SNAPSHOT",
  javaWs % "test"
)

Avro.settings
