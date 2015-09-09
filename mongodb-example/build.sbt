name := "mongodb-example"

version := "1.0.0-SNAPSHOT"

lazy val root = project in file(".") enablePlugins(PlayJava)

libraryDependencies ++= Seq(
  "me.tfeng.play-mods" % "spring" % "0.5.1-SNAPSHOT",
  "me.tfeng.toolbox" % "mongodb" % "0.5.1-SNAPSHOT",
  javaWs % "test"
)

Avro.settings

routesGenerator := InjectedRoutesGenerator
