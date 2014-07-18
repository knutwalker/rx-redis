name := """rx-redis"""

organization in ThisBuild := """de.knutwalker"""

version in ThisBuild := "0.1-SNAPSHOT"

scalaVersion in ThisBuild := "2.11.1"

libraryDependencies ++= List(
  "com.netflix.rxnetty"   %  "rx-netty"         % "0.3.9",
  "com.netflix.rxjava"    %  "rxjava-scala"     % "0.19.1",
  "org.scalatest"        %%  "scalatest"        % "2.2.0" % "test"
)

lazy val root = project.in(file("."))

lazy val example = project.dependsOn(root)
