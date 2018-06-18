name := "Wallet"
version := "0.1"
scalaVersion := "2.12.6"
organization in ThisBuild := "org.encryfoundation"

val akkaVersion = "10.0.9"

libraryDependencies ++= Seq (
  "com.typesafe.akka" %% "akka-http" % akkaVersion,
  "com.lihaoyi" %% "scalatags" % "0.6.7"
)
