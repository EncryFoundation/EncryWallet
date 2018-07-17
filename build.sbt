name := """wallet"""
organization := "com.encry"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.6"

val circeVersion = "0.9.3"

libraryDependencies += guice

libraryDependencies ++= Seq (
  "javax.xml.bind" % "jaxb-api" % "2.1",
  "com.dripower" %% "play-circe" % "2609.1" exclude("io.circe", "*"),
  "net.codingwell" %% "scala-guice" % "4.2.1",
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "org.scorexfoundation" %% "scrypto" % "2.1.1",
  "org.scorexfoundation" %% "iodb" % "0.3.2",
  "com.iheart" %% "ficus" % "1.4.3",
  "com.github.oskin1" %% "prism" % "0.2.2",
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test
) map ( _ exclude("ch.qos.logback", "*") exclude("ch.qos.logback", "*") )


libraryDependencies ++= Seq (
  "org.slf4j" % "slf4j-api" % "1.7.25",
  "ch.qos.logback" % "logback-classic" % "1.2.3"
)
