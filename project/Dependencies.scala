import sbt._

object Version {
  val scala = "2.11.2"
  val rxJava = "0.19.1"
  val rxNetty = "0.3.10"
  val scalaTest = "2.2.0"
}

object Library {
  val scalaReflect = "org.scala-lang" % "scala-reflect" % Version.scala
  val rxJava  = "com.netflix.rxjava"  % "rxjava-core"   % Version.rxJava
  val rxScala = "com.netflix.rxjava"  % "rxjava-scala"  % Version.rxJava exclude("org.scala-lang", "scala-library")
  val rxNetty = "com.netflix.rxnetty" % "rx-netty"      % Version.rxNetty
  val scalaTest = "org.scalatest"    %% "scalatest"     % Version.scalaTest
}

object Dependencies {

  import Library._

  val serializationDeps = List(scalaReflect % "compile")
  val coreDeps = List(rxNetty, scalaTest % "test")
  val scalaApiDeps = List(rxScala)
}
