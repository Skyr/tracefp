name := "tracefp"

version := "1.0"

scalaVersion := "2.10.3"

resolvers += "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"

libraryDependencies ++= Seq(
  // "ch.qos.logback" % "logback-classic" % "1.0.13",
  "org.slf4j" % "slf4j-log4j12" % "1.7.6",
  "log4j" % "log4j" % "1.2.17",
  "org.rogach" %% "scallop" % "0.9.4",
  "org.sgodbillon" % "bytecode-parser" % "1.0-SNAPSHOT",
  "org.apache.ivy" % "ivy" % "2.3.0",
  "com.typesafe.slick" % "slick_2.10" % "2.0.1",
  "com.h2database" % "h2" % "1.3.175",
  "org.scalatest" % "scalatest_2.10" % "2.1.3" % "test"
)
