name := "titan-example"

version := "1.0.0-SNAPSHOT"

lazy val root = project in file(".") enablePlugins(PlayJava)

libraryDependencies ++= Seq(
  "me.tfeng.play-mods" % "titan" % "0.4.2-SNAPSHOT",
  javaWs % "test",
  "org.hamcrest" % "hamcrest-all" % "1.3" % "test"
)

routesGenerator := InjectedRoutesGenerator
