name := "avro-d2-example"

version := "1.0.0-SNAPSHOT"

lazy val root = project in file(".") enablePlugins(PlayJava)

libraryDependencies ++= Seq(
  "commons-io" % "commons-io" % "2.4",
  "me.tfeng.toolbox" % "common" % "0.4.2-SNAPSHOT" % "test->test",
  javaWs % "test"
)

AvroD2.settings

routesGenerator := InjectedRoutesGenerator
