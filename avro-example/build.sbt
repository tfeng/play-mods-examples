name := "avro-example"

version := "1.0.0-SNAPSHOT"

lazy val root = project in file(".") enablePlugins(PlayJava)

Avro.settings

routesGenerator := InjectedRoutesGenerator
