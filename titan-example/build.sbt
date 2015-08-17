name := "titan-example"

version := "1.0.0-SNAPSHOT"

lazy val root = project in file(".") enablePlugins(PlayJava)

libraryDependencies ++= Seq(
  "me.tfeng.play-mods" % "spring" % "0.4.2-SNAPSHOT",
  "me.tfeng.toolbox" % "common" % "0.4.2-SNAPSHOT" % "test->test",
  "me.tfeng.toolbox" % "titan" % "0.4.2-SNAPSHOT",
  javaWs % "test"
)

routesGenerator := InjectedRoutesGenerator
