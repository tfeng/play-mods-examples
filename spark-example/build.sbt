name := "spark-example"

version := "1.0.0-SNAPSHOT"

lazy val root = project in file(".") enablePlugins(PlayJava)

libraryDependencies ++= Seq(
  "me.tfeng.play-mods" % "spring" % "0.5.10",
  "me.tfeng.toolbox" % "kafka" % "0.5.9",
  "org.apache.spark" % "spark-streaming_2.10" % "1.3.1",
  "org.apache.spark" % "spark-streaming-kafka_2.10" % "1.3.1",
  "org.hamcrest" % "hamcrest-all" % "1.3" % "test",
  javaWs % "test"
)

routesGenerator := InjectedRoutesGenerator
