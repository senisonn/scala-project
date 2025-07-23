ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.6"

lazy val root = (project in file("."))
  .settings(
    name := "scala-project",
    libraryDependencies ++= Seq(
    "org.postgresql" % "postgresql" % "42.7.3",
    "org.mindrot" % "jbcrypt" % "0.4"
    )
  )
