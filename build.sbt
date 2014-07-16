name := """rx-redis"""

organization := """de.knutwalker"""

version := "0.1-SNAPSHOT"

scalaVersion := "2.11.1"

libraryDependencies ++= List(
  "org.scala-lang"        %  "scala-reflect"    % scalaVersion.value,
  "com.codahale.metrics"  %  "metrics-core"     % "3.0.2",
  "com.netflix.rxnetty"   %  "rx-netty"         % "0.3.9",
  "com.netflix.rxjava"    %  "rxjava-scala"     % "0.19.1",
  "org.scalatest"        %%  "scalatest"        % "2.2.0" % "test"
)

Revolver.settings
