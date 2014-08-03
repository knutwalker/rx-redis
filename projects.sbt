import sbt._
import Keys._
import Common._
import Dependencies._
import com.typesafe.sbt.pgp.PgpKeys._
import spray.revolver.RevolverPlugin.Revolver

lazy val core = {
  project.in(file("modules") / "core").
    configs(RegressionTest).
    settings(formatterSettings: _*).
    settings(mainRegSettings: _*).
    settings(
      name := "rx-redis-core",
      ivyConfigurations += CompileTimeOnly,
      libraryDependencies ++= coreDeps,
      unmanagedClasspath in Compile ++= update.value.select(configurationFilter(CompileTimeOnly.name)),
      testOptions in Test := List(Tests.Filter(unitFilter)),
      testOptions in RegressionTest := List(Tests.Filter(regFilter))
  )
}

lazy val commands = {
  project.in(file("modules") / "commands").
    dependsOn(core).
    settings(formatterSettings: _*).
    settings(
      name := "rx-redis-commands",
      libraryDependencies ++= commandsDeps
    )
}

lazy val client = {
  project.in(file("modules") / "client").
    dependsOn(core, commands).
    configs(IntegrationTest).
    settings(mainItSettings: _*).
    settings(formatterSettings: _*).
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
    dependsOn(api, japi, client, commands, core).
    aggregate(api, japi, client, commands, core).
    settings(signedReleaseSettings: _*).
    settings(sonatypeSettings: _*).
    settings(
      publishSigned := {}
    )
}
