import sbt._
import Keys._
import Common._
import Dependencies._
import com.typesafe.sbt.pgp.PgpKeys._

lazy val serialization = project.settings(
  name := "rx-redis-serialization",
  libraryDependencies ++= serializationDeps)

lazy val core = project.dependsOn(serialization).configs(
  IntegrationTest).settings(Defaults.itSettings: _*).settings(
    name := "rx-redis-core",
    libraryDependencies ++= coreDeps,
    logBuffered in IntegrationTest := false,
    fork in IntegrationTest := true)

lazy val api = project.in(file("language-bindings") / "scala").dependsOn(core).settings(
  name := "rx-redis-scala",
  libraryDependencies ++= scalaApiDeps)

lazy val japi = project.in(file("language-bindings") / "java").dependsOn(core).settings(
  name := "rx-redis-java")

lazy val example = project.in(file("examples") / "scala").dependsOn(api).
  settings(Revolver.settings: _*).settings(name := "rx-redis-example")

lazy val `java-example` = project.in(file("examples") / "java").dependsOn(japi).settings(
  name := "rx-redis-java-example")

lazy val rxRedis = project.in(file(".")).
  settings(signedReleaseSettings: _*).settings(publishSigned := {}).
  dependsOn(example, `java-example`).
  aggregate(example, `java-example`, api, japi, core, serialization)
