name := """wallet"""
organization := "com.encry"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.6"

val circeVersion = "0.9.3"

routesGenerator := InjectedRoutesGenerator

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

libraryDependencies += guice
libraryDependencies += filters

libraryDependencies ++= Seq(
  "javax.xml.bind" % "jaxb-api" % "2.1",
  "com.dripower" %% "play-circe" % "2609.1" exclude("io.circe", "*"),
  "net.codingwell" %% "scala-guice" % "4.2.1",
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "org.scorexfoundation" %% "scrypto" % "2.1.1",
  "org.scorexfoundation" %% "iodb" % "0.3.2",
  "com.iheart" %% "ficus" % "1.4.3",
  "org.encry" %% "prism" % "0.2.3",
  "com.adrianhurt" %% "play-bootstrap" % "1.4-P26-B4-SNAPSHOT",
  "org.webjars" % "bootstrap" % "4.1.2",
  "org.webjars" % "jquery" % "3.3.1",
  "org.mockito" % "mockito-core" % "2.19.1" % Test,
  "org.scalacheck" %% "scalacheck" % "1.13.+" % Test,
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test
) map (_ exclude("ch.qos.logback", "*") exclude("ch.qos.logback", "*"))

libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-api" % "1.7.25",
  "ch.qos.logback" % "logback-classic" % "1.2.3"
)
