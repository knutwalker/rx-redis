name := """rx-redis"""

organization in ThisBuild := """de.knutwalker"""

version in ThisBuild := "0.1-SNAPSHOT"

scalaVersion in ThisBuild := "2.11.1"

libraryDependencies ++= List(
  "com.netflix.rxnetty"   %  "rx-netty"         % "0.3.9",
  "org.scalatest"        %%  "scalatest"        % "2.2.0" % "test"
)

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

lazy val rxRedis = project.in(file(".")).dependsOn(serialization)

lazy val example = project.dependsOn(rxRedis)
