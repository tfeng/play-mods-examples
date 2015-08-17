name := "spark-example"

version := "1.0.0-SNAPSHOT"

lazy val root = project in file(".") enablePlugins(PlayJava)

libraryDependencies ++= Seq(
  "me.tfeng.play-mods" % "spring" % "0.4.2-SNAPSHOT",
  "me.tfeng.toolbox" % "common" % "0.4.2-SNAPSHOT" % "test->test",
  "me.tfeng.toolbox" % "kafka" % "0.4.2-SNAPSHOT",
  "org.apache.spark" % "spark-streaming_2.10" % "1.3.1",
  "org.apache.spark" % "spark-streaming-kafka_2.10" % "1.3.1",
  javaWs % "test"
)

routesGenerator := InjectedRoutesGenerator
