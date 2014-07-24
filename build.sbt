name := """rx-redis-parent"""

organization in ThisBuild := """de.knutwalker"""

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

lazy val serialization = project.settings(
  name := "rx-redis-serialization",
  libraryDependencies ++= List(
    "org.scala-lang" % "scala-reflect" % scalaVersion.value % "compile"))

lazy val core = project.dependsOn(serialization).settings(
  name := "rx-redis-core",
  libraryDependencies ++= List(
    "com.netflix.rxnetty"  %  "rx-netty"   % "0.3.9",
    "org.scalatest"       %%  "scalatest"  % "2.2.0" % "test"))

lazy val api = project.in(file("language-bindings") / "scala").dependsOn(core).settings(
  name := "rx-redis-scala",
  libraryDependencies ++= List(
    "com.netflix.rxjava" % "rxjava-scala" % "0.19.1" exclude("org.scala-lang", "scala-library")))

lazy val japi = project.in(file("language-bindings") / "java").dependsOn(core).settings(
  name := "rx-redis-java")

lazy val example = project.in(file("examples") / "scala").dependsOn(api).settings(
  (Seq(name := "rx-redis-example") ++ Revolver.settings): _*)

lazy val `java-example` = project.in(file("examples") / "java").dependsOn(japi).settings(
  name := "rx-redis-java-example")

lazy val rxRedis = project.in(file(".")).dependsOn(example, `java-example`).aggregate(
  example, `java-example`, api, japi, core, serialization)
