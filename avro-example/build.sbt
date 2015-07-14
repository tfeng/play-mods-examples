name := "avro-example"

version := "1.0.0-SNAPSHOT"

lazy val root = project in file(".") enablePlugins(PlayJava)

libraryDependencies ++= Seq(
  javaWs % "test",
  "org.hamcrest" % "hamcrest-all" % "1.3" % "test"
)

Avro.settings

routesGenerator := InjectedRoutesGenerator
