name := "spark-example"

version := "1.0.0-SNAPSHOT"

lazy val root = project in file(".") enablePlugins(PlayJava)

libraryDependencies ++= Seq(
  "me.tfeng.play-mods" % "kafka" % "0.4.2-SNAPSHOT",
  "me.tfeng.play-mods" % "spark" % "0.4.2-SNAPSHOT",
  "org.apache.spark" % "spark-streaming_2.10" % "1.3.1",
  "org.apache.spark" % "spark-streaming-kafka_2.10" % "1.3.1",
  javaWs % "test",
  "org.hamcrest" % "hamcrest-all" % "1.3" % "test"
)

routesGenerator := InjectedRoutesGenerator
