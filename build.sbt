name := """tips"""
organization := "com.livesafemobile"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.8"

libraryDependencies += filters
libraryDependencies += jdbc
libraryDependencies += evolutions
libraryDependencies += "org.postgresql" % "postgresql" % "9.4.1212"
libraryDependencies += "org.mindrot" % "jbcrypt" % "0.4"
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.livesafemobile.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.livesafemobile.binders._"
