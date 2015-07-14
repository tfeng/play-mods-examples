name := "oauth2-avro-d2-example"

version := "1.0.0-SNAPSHOT"

lazy val root = project in file(".") enablePlugins(PlayJava)

libraryDependencies ++= Seq(
  "commons-io" % "commons-io" % "2.4",
  "me.tfeng.play-mods" % "oauth2" % "0.4.2-SNAPSHOT",
  javaWs % "test",
  "org.hamcrest" % "hamcrest-all" % "1.3" % "test"
)

AvroD2.settings

routesGenerator := InjectedRoutesGenerator
