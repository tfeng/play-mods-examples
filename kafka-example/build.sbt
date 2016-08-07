name := "kafka-example"

version := "1.0.0-SNAPSHOT"

scalaVersion := "2.11.7"

lazy val root = project in file(".") enablePlugins(PlayJava)

libraryDependencies ++= Seq(
  "commons-io" % "commons-io" % "2.5",
  "me.tfeng.play-mods" % "avro" % "0.9.8",
  "me.tfeng.toolbox" % "kafka" % "0.9.8",
  "org.apache.zookeeper" % "zookeeper" % "3.4.8",
  "org.hamcrest" % "hamcrest-all" % "1.3" % "test"
)

Avro.settings
