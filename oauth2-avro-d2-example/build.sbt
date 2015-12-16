name := "oauth2-avro-d2-example"

version := "1.0.0-SNAPSHOT"

lazy val root = project in file(".") enablePlugins(PlayJava)

libraryDependencies ++= Seq(
  "commons-io" % "commons-io" % "2.4",
  "me.tfeng.play-mods" % "oauth2" % "0.6.0",
  javaWs % "test"
)

AvroD2.settings

routesGenerator := InjectedRoutesGenerator
