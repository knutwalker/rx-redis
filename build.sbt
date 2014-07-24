name := """rx-redis-parent"""

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

initialCommands := """|import rx.redis.RxRedis
                      |val client = RxRedis("localhost", 6379)""".stripMargin
