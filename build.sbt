name := """wallet"""
organization := "com.encry"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.6"

libraryDependencies += guice

libraryDependencies ++= Seq (
  "javax.xml.bind" % "jaxb-api" % "2.1",
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test
)


// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.encry.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.encry.binders._"
