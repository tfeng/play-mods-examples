name := "avro-example"

version := "1.0.0-SNAPSHOT"

lazy val root = project in file(".") enablePlugins(PlayJava)

libraryDependencies ++= Seq(
  "me.tfeng.toolbox" % "common" % "0.4.2-SNAPSHOT" % "test->test"
)

Avro.settings

routesGenerator := InjectedRoutesGenerator
