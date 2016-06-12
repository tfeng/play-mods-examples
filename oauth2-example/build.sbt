name := "oauth2-example"

version := "1.0.0-SNAPSHOT"

scalaVersion := "2.11.7"

lazy val root = project in file(".") enablePlugins(PlayJava)

libraryDependencies ++= Seq(
  "me.tfeng.play-mods" % "oauth2" % "0.8.7",
  "org.hamcrest" % "hamcrest-all" % "1.3" % "test",
  javaWs % "test"
)
