name := """rx-redis-parent"""

description in ThisBuild := """Reactive Extensions for Redis"""

organization in ThisBuild := """de.knutwalker"""

scalaVersion in ThisBuild := "2.11.2"

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

initialCommands := """|import rx.redis.api.RxRedis
                      |val client = RxRedis("localhost", 6379)""".stripMargin
