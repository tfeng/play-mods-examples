name := "kafka-example"

version := "1.0.0-SNAPSHOT"

scalaVersion := "2.11.7"

lazy val root = project in file(".") enablePlugins(PlayJava)

libraryDependencies ++= Seq(
  "commons-io" % "commons-io" % "2.4",
  "me.tfeng.play-mods" % "avro" % "0.8.0",
  "me.tfeng.toolbox" % "kafka" % "0.8.0",
  "org.apache.zookeeper" % "zookeeper" % "3.4.8",
  "org.hamcrest" % "hamcrest-all" % "1.3" % "test"
)

Avro.settings
