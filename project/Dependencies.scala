import sbt._

object Version {
  val scala = "2.11.2"
  val netty = "4.0.21.Final"
  val rxJava = "0.19.1"
  val rxNetty = "0.3.10"
  val scalaTest = "2.2.0"
}

object Library {
  val scalaReflect   = "org.scala-lang"      % "scala-reflect"    % Version.scala
  val nettyBuffer    = "io.netty"            % "netty-buffer"     % Version.netty
  val nettyCodec     = "io.netty"            % "netty-codec"      % Version.netty
  val nettyCommon    = "io.netty"            % "netty-common"     % Version.netty
  val nettyTransport = "io.netty"            % "netty-transport"  % Version.netty
  val rxJava         = "com.netflix.rxjava"  % "rxjava-core"      % Version.rxJava
  val rxScala        = "com.netflix.rxjava"  % "rxjava-scala"     % Version.rxJava exclude("org.scala-lang", "scala-library")
  val rxNetty        = "com.netflix.rxnetty" % "rx-netty"         % Version.rxNetty
  val scalaTest      = "org.scalatest"      %% "scalatest"        % Version.scalaTest
}

object Dependencies {

  import Library._

  val coreDeps = List(scalaReflect % "compile", nettyBuffer % "test")
  val pipelineDeps = List(nettyCodec, scalaTest % "test")
  val clientDeps = List(rxJava, scalaTest % "it,test")
  val scalaApiDeps = List(rxScala)
}
