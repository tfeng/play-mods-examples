name := "schema-evolution-example"

version := "1.0.0-SNAPSHOT"

lazy val root = project in file(".") enablePlugins(PlayJava)

libraryDependencies ++= Seq(
  "commons-io" % "commons-io" % "2.4",
  "com.google.guava" % "guava" % "18.0",
  javaWs % "test"
)

unmanagedResourceDirectories in Compile <+= baseDirectory / "protocols"

AvroD2.settings

routesGenerator := InjectedRoutesGenerator
