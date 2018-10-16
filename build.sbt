name := "small-mtproto"

version := "0.1"

scalaVersion := "2.12.7"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.5.17",
  "com.typesafe.akka" %% "akka-stream" % "2.5.17",
  "org.scodec" %% "scodec-bits" % "1.1.6",
  "org.scodec" %% "scodec-core" % "1.10.3"
)