name := "schema-evolution-example"

version := "1.0.0-SNAPSHOT"

scalaVersion := "2.12.2"

lazy val root = project in file(".") enablePlugins(PlayJava)

libraryDependencies ++= Seq(
  "commons-io" % "commons-io" % "2.5",
  "com.google.guava" % "guava" % "23.0",
  "me.tfeng.play-mods" % "avro-d2" % "0.11.0",
  javaWs % "test"
)

unmanagedResourceDirectories in Compile += baseDirectory.value / "protocols"

Avro.settings
