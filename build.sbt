scalaVersion := "2.13.7"

name := "scala-shopping-cart"
organization := "com.playground"
version := "1.0"

fork in run := true

lazy val logbackVersion = "1.2.10"
lazy val log4cats = "2.1.1"
lazy val catsVersion = "2.7.0"
lazy val catsEffectVersion = "3.3.1"
lazy val fs2Version = "3.2.4"
lazy val http4sVersion = "1.0.0-M30"
lazy val doobieVersion = "1.0.0-RC1"
lazy val flywayVersion = "8.3.0"
lazy val circeVersion = "0.15.0-M1"
lazy val circeConfigVersion = "0.8.0"

libraryDependencies += "ch.qos.logback" % "logback-classic" % logbackVersion
libraryDependencies += "org.typelevel" %% "log4cats-slf4j" % log4cats

// cats
libraryDependencies += "org.typelevel" %% "cats-core" % catsVersion withSources () withJavadoc ()
libraryDependencies += "org.typelevel" %% "cats-effect" % catsEffectVersion withSources () withJavadoc ()

// circe
libraryDependencies ++= Seq(
  "io.circe" %% "circe-generic" % circeVersion,
  "org.http4s" %% "http4s-circe" % http4sVersion,
  // Optional for auto-derivation of JSON codecs
  "io.circe" %% "circe-generic" % circeVersion,
  // Optional for string interpolation to JSON model
  "io.circe" %% "circe-literal" % circeVersion,
  "io.circe" %% "circe-config" % circeConfigVersion
)

// fs2
libraryDependencies += "co.fs2" %% "fs2-core" % fs2Version withSources () withJavadoc ()
//libraryDependencies += "co.fs2" %% "fs2-io" % fs2Version withSources() withJavadoc()

// http4s
libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "org.http4s" %% "http4s-blaze-server" % http4sVersion,
  "org.http4s" %% "http4s-blaze-client" % http4sVersion,
)

// doobie
libraryDependencies ++= Seq(
  "org.tpolecat" %% "doobie-core" % doobieVersion,
  "org.tpolecat" %% "doobie-hikari" % doobieVersion, // HikariCP transactor.
  "org.tpolecat" %% "doobie-postgres" % doobieVersion, // Postgres driver 42.2.23 + type mappings.
  "org.tpolecat" %% "doobie-specs2" % doobieVersion % "test", // Specs2 support for typechecking statements.
  "org.tpolecat" %% "doobie-scalatest" % doobieVersion % "test", // ScalaTest support for typechecking statements.
)

// flyway
libraryDependencies += "org.flywaydb" % "flyway-core" % flywayVersion