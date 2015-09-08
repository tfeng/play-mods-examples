name := "dust-example"

version := "1.0.0-SNAPSHOT"

lazy val root = project in file(".") enablePlugins(PlayJava)

libraryDependencies += javaWs % "test"

Dust.settings

routesGenerator := InjectedRoutesGenerator
