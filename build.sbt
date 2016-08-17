name := """chronos"""

scalaVersion := "2.11.8"

resolvers ++= Seq(
  "Sonatype OSS Release Repository" at "https://oss.sonatype.org/content/repositories/releases/",
  "Sonatype OSS Snapshot Repository" at "https://oss.sonatype.org/content/repositories/snapshots/",
  "Artima Maven Repository" at "http://repo.artima.com/releases"
)

libraryDependencies ++= Seq(
  "org.sisioh" %% "baseunits-scala" % "0.1.18-SNAPSHOT",
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4",
  "org.scalactic" %% "scalactic" % "3.0.0",
  "org.scalatest" %% "scalatest" % "3.0.0" % "test",
  "com.typesafe.akka" %% "akka-testkit" % "2.4.8" % "test",
  "org.slf4j" % "slf4j-simple" % "1.7.21" % "test",
  "org.quartz-scheduler" % "quartz" % "2.2.3",
  "com.typesafe.akka" %% "akka-actor" % "2.4.8",
  "com.typesafe.akka" %% "akka-cluster-sharding" % "2.4.8",
  "com.typesafe.akka" %% "akka-persistence" % "2.4.8",
  "com.typesafe.akka" %% "akka-stream" % "2.4.8",
  "org.apache.commons" % "commons-lang3" % "3.4"
)


