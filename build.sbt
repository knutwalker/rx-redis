name := """rx-redis-parent"""

organization in ThisBuild := """de.knutwalker"""

version in ThisBuild := "0.1-SNAPSHOT"

scalaVersion in ThisBuild := "2.11.1"

scalacOptions in ThisBuild ++= List(
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature",
  "-language:postfixOps",
  "-target:jvm-1.7",
  "-unchecked",
  "-Xlint",
  "-Ywarn-dead-code"
)

initialCommands in console := """
import rx.redis.RxRedis
val client = RxRedis("localhost", 6379)"""

lazy val serialization = project

lazy val core = project.dependsOn(serialization)

lazy val api = project.in(file("language-bindings") / "scala").dependsOn(core)

lazy val japi = project.in(file("language-bindings") / "java").dependsOn(core)

lazy val example = project.in(file("examples") / "scala").dependsOn(api)

lazy val `java-example` = project.in(file("examples") / "java").dependsOn(japi)

lazy val rxRedis = project.in(file(".")).dependsOn(example, `java-example`).aggregate(example, `java-example`, api, japi, core, serialization)
