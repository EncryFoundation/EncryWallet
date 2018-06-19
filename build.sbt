name := "Wallet"
version := "0.1"
scalaVersion := "2.12.6"
organization in ThisBuild := "org.encryfoundation"

name := "EncryWallet"

version := "0.0.1"

organization := "org.encryfoundation"

scalaVersion := "2.12.6"

resolvers ++= Seq("Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/",
  "SonaType" at "https://oss.sonatype.org/content/groups/public",
  "Typesafe maven releases" at "http://repo.typesafe.com/typesafe/maven-releases/",
  "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/")

val akkaVersion = "10.0.9"
val circeVersion = "0.9.3"

val testingDependencies = Seq(
  "com.typesafe.akka" %% "akka-testkit" % "2.4.+" % "test",
  "com.typesafe.akka" %% "akka-http-testkit" % akkaVersion % "test",
  "org.scalatest" %% "scalatest" % "3.0.3" % "test",
  "org.scalacheck" %% "scalacheck" % "1.13.+" % "test"
)

libraryDependencies ++= Seq (
  "com.typesafe.akka" %% "akka-http" % akkaVersion,
  "com.lihaoyi" %% "scalatags" % "0.6.7",
  "com.github.oskin1" %% "prism" % "0.1.8",
  "org.scorexfoundation" %% "scrypto" % "2.1.1",
  "org.scorexfoundation" %% "iodb" % "0.3.2",
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
) ++ testingDependencies
