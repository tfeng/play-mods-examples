name := "oauth2-example"

version := "1.0.0-SNAPSHOT"

lazy val root = project in file(".") enablePlugins(PlayJava)

libraryDependencies ++= Seq(
  "me.tfeng.play-mods" % "oauth2" % "0.4.2-SNAPSHOT",
  "me.tfeng.toolbox" % "common" % "0.4.2-SNAPSHOT" % "test->test",
  javaWs % "test"
)

routesGenerator := InjectedRoutesGenerator
