name := """wallet"""
organization := "com.encry"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.6"

val circeVersion = "0.9.3"

libraryDependencies += guice

libraryDependencies ++= Seq (
  "javax.xml.bind" % "jaxb-api" % "2.1",
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "org.scorexfoundation" %% "scrypto" % "2.1.1",
  "org.scorexfoundation" %% "iodb" % "0.3.2",
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test
)
