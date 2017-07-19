name := "avro-example"

version := "1.0.0-SNAPSHOT"

scalaVersion := "2.12.2"

lazy val root = project in file(".") enablePlugins(PlayJava)

libraryDependencies += "me.tfeng.play-mods" % "avro" % "0.10.0-SNAPSHOT"

Avro.settings
