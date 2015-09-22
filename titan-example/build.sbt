name := "titan-example"

version := "1.0.0-SNAPSHOT"

lazy val root = project in file(".") enablePlugins(PlayJava)

libraryDependencies ++= Seq(
  "me.tfeng.play-mods" % "spring" % "0.5.4",
  "me.tfeng.toolbox" % "titan" % "0.5.1",
  "org.hamcrest" % "hamcrest-all" % "1.3" % "test",
  javaWs % "test"
)

routesGenerator := InjectedRoutesGenerator
