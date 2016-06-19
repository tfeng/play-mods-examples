name := "schema-evolution-example"

version := "1.0.0-SNAPSHOT"

scalaVersion := "2.11.7"

lazy val root = project in file(".") enablePlugins(PlayJava)

libraryDependencies ++= Seq(
  "commons-io" % "commons-io" % "2.5",
  "com.google.guava" % "guava" % "19.0",
  "me.tfeng.play-mods" % "avro-d2" % "0.8.9",
  javaWs % "test"
)

unmanagedResourceDirectories in Compile <+= baseDirectory / "protocols"

Avro.settings
