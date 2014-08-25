import sbt._

object Version {
  val scala = "2.11.2"
  val netty = "4.0.23.Final"
//  val netty = "4.1.0.Beta1"
//  val netty = "5.0.0.Alpha1"
  // val rxJava = "0.19.6"
  val rxJava = "0.20.0"
  val scalaTest = "2.2.1"
  val scalaCheck = "1.11.5"
}

object Library {
  val scalaReflect   = "org.scala-lang"      % "scala-reflect"    % Version.scala
  val nettyCommon    = "io.netty"            % "netty-common"     % Version.netty withSources() withJavadoc()
  val nettyBuffer    = "io.netty"            % "netty-buffer"     % Version.netty withSources() withJavadoc()
  val nettyTransport = "io.netty"            % "netty-transport"  % Version.netty withSources() withJavadoc()
  val rxJava         = "com.netflix.rxjava"  % "rxjava-core"      % Version.rxJava withSources() withJavadoc()
  val rxScala        = "com.netflix.rxjava"  % "rxjava-scala"     % Version.rxJava exclude("org.scala-lang", "scala-library") withSources() withJavadoc()
  val scalaTest      = "org.scalatest"      %% "scalatest"        % Version.scalaTest
  val scalaCheck     = "org.scalacheck"     %% "scalacheck"       % Version.scalaCheck
}

object Dependencies {

  import Library._

  val netty = List(nettyTransport, nettyBuffer, nettyCommon)

  val coreDeps = scalaReflect % "compileonly" :: List(nettyBuffer, scalaTest, scalaCheck).map(_  % "test")
  val commandsDeps = List(scalaTest % "test")
  val clientDeps = netty ::: List(rxJava, scalaTest % "it,test")
  val scalaApiDeps = List(rxScala)
}
