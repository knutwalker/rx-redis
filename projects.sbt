import sbt._
import Keys._
import Common._
import Dependencies._
import com.typesafe.sbt.pgp.PgpKeys._

lazy val core = {
  project.in(file("modules") / "core").
    settings(formatterSettings: _*).
    settings(
      name := "rx-redis-core",
      libraryDependencies ++= coreDeps
  )
}

lazy val pipeline = {
  project.in(file("modules") / "pipeline").
    dependsOn(core).
    settings(formatterSettings: _*).
    settings(
      name := "rx-redis-pipeline",
      libraryDependencies ++= pipelineDeps
    )
}

lazy val client = {
  project.in(file("modules") / "client").
    dependsOn(pipeline).
    configs(IntegrationTest).
    settings(IntegrationTests.integrationTestsSettings: _*).
    settings(formatterSettings: _*).
    settings(mainBuildInfoSettings: _*).
    settings(
      name := "rx-redis-client",
      libraryDependencies ++= clientDeps
    )
}

lazy val api = {
  project.in(file("language-bindings") / "scala").
    dependsOn(client).
    settings(formatterSettings: _*).
    settings(
      name := "rx-redis-scala",
      libraryDependencies ++= scalaApiDeps
    )
}

lazy val japi = {
  project.in(file("language-bindings") / "java").
    dependsOn(client).
    settings(
      name := "rx-redis-java"
    )
}

lazy val example = {
  project.in(file("examples") / "scala").
    dependsOn(api).
    settings(Revolver.settings: _*).
    settings(
      name := "rx-redis-example"
    )
}

lazy val `java-example` = {
  project.in(file("examples") / "java").
    dependsOn(japi).
    settings(
      name := "rx-redis-java-example"
    )
}

lazy val rxRedis = {
  project.in(file(".")).
    dependsOn(api, japi, client, pipeline, core).
    aggregate(api, japi, client, pipeline, core).
    settings(signedReleaseSettings: _*).
    settings(sonatypeSettings: _*).
    settings(
      publishSigned := {}
    )
}
