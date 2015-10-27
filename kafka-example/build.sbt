name := "kafka-example"

version := "1.0.0-SNAPSHOT"

lazy val root = project in file(".") enablePlugins(PlayJava)

libraryDependencies ++= Seq(
  "commons-io" % "commons-io" % "2.4",
  "me.tfeng.play-mods" % "spring" % "0.5.12",
  "me.tfeng.toolbox" % "kafka" % "0.5.12",
  "org.apache.zookeeper" % "zookeeper" % "3.4.6",
  "org.hamcrest" % "hamcrest-all" % "1.3" % "test"
)

Avro.settings

routesGenerator := InjectedRoutesGenerator
