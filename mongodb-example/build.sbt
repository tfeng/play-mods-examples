name := "mongodb-example"

version := "1.0.0-SNAPSHOT"

lazy val root = project in file(".") enablePlugins(PlayJava)

libraryDependencies ++= Seq(
  "me.tfeng.play-mods" % "spring" % "0.5.5",
  "me.tfeng.toolbox" % "mongodb" % "0.5.5",
  javaWs % "test"
)

Avro.settings

routesGenerator := InjectedRoutesGenerator
