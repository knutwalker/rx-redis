import sbt._

object Version {
  val scala = "2.11.2"
  val netty = "4.0.21.Final"
  val rxJava = "0.19.1"
  val scalaTest = "2.2.0"
  val scalaCheck = "1.11.4"
}

object Library {
  val scalaReflect   = "org.scala-lang"      % "scala-reflect"    % Version.scala
  val nettyBuffer    = "io.netty"            % "netty-buffer"     % Version.netty
  val nettyCodec     = "io.netty"            % "netty-codec"      % Version.netty
  val rxJava         = "com.netflix.rxjava"  % "rxjava-core"      % Version.rxJava
  val rxScala        = "com.netflix.rxjava"  % "rxjava-scala"     % Version.rxJava exclude("org.scala-lang", "scala-library")
  val scalaTest      = "org.scalatest"      %% "scalatest"        % Version.scalaTest
  val scalaCheck     = "org.scalacheck"     %% "scalacheck"       % Version.scalaCheck
}

object Dependencies {

  import Library._

  val coreDeps = scalaReflect % "compile" :: List(nettyBuffer, scalaTest, scalaCheck).map(_  % "test")
  val pipelineDeps = List(nettyCodec, scalaTest % "test")
  val clientDeps = List(rxJava, scalaTest % "it,test")
  val scalaApiDeps = List(rxScala)
}
