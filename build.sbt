import com.typesafe.sbt.pgp.PgpKeys.publishSigned
import de.heikoseeberger.sbtheader.license.Apache2_0
import pl.project13.scala.sbt.SbtJmh.JmhKeys.{outputTarget, generatorType, Jmh}
import sbtrelease.ReleasePlugin.ReleaseKeys._
import sbtrelease.ReleaseStateTransformations._
import sbtrelease.ReleaseStep
import scalariform.formatter.preferences._
import scoverage.ScoverageSbtPlugin.ScoverageKeys.coverageExcludedPackages
import spray.revolver.RevolverPlugin.Revolver
import xerial.sbt.Sonatype.SonatypeKeys.{profileName, sonatypeReleaseAll}


lazy val parent = project in file(".") dependsOn (
  api, japi, client, commands, serialization, core) aggregate (
  api, japi, client, commands, serialization, core, `java-examples`, `scala-examples`, tests) settings (
  buildsUberJar,
  rxRedisSettings,
  doNotPublish,
  name := "rx-redis-parent")

lazy val core = project in file("modules") / "core" enablePlugins AutomateHeaderPlugin settings (
  rxRedisSettings,
  name := "rx-redis-core",
  libraryDependencies += "io.netty" % "netty-buffer" % nettyVersion.value)

lazy val serialization = project in file("modules") / "serialization" enablePlugins AutomateHeaderPlugin dependsOn core settings (
  rxRedisSettings,
  name := "rx-redis-serialization",
  libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value % "provided")

lazy val commands = project in file("modules") / "commands" enablePlugins AutomateHeaderPlugin dependsOn serialization settings (
  rxRedisSettings,
  name := "rx-redis-commands")

lazy val client = project in file("modules") / "client" enablePlugins AutomateHeaderPlugin dependsOn commands settings (
  buildsUberJar,
  rxRedisSettings,
  name := "rx-redis-client",
  libraryDependencies ++= List(
    "io.netty"     % "netty-transport" % nettyVersion.value,
    "io.reactivex" % "rxjava"          % rxJavaVersion.value))

lazy val api = project in file("language-bindings") / "scala" enablePlugins AutomateHeaderPlugin dependsOn client settings (
  buildsUberJar,
  rxRedisSettings,
  name := "rx-redis-scala",
  libraryDependencies += "io.reactivex" %% "rxscala" % rxScalaVersion.value exclude("org.scala-lang", "scala-library"))

lazy val japi = project in file("language-bindings") / "java" enablePlugins AutomateHeaderPlugin dependsOn client settings (
  buildsUberJar,
  rxRedisSettings,
  name := "rx-redis-java",
  libraryDependencies += "org.scala-lang.modules" %% "scala-java8-compat" % "0.4.0")

lazy val `scala-examples` = project in file("examples") / "scala" enablePlugins AutomateHeaderPlugin dependsOn api settings (
  Revolver.settings,
  rxRedisSettings,
  name := "rx-redis-example")

lazy val `java-examples` = project in file("examples") / "java" enablePlugins AutomateHeaderPlugin dependsOn japi settings (
  rxRedisSettings,
  name := "rx-redis-java-example")

lazy val tests = project enablePlugins AutomateHeaderPlugin configs IntegrationTest dependsOn (client, api, japi) settings (
  rxRedisSettings,
  doNotPublish,
  Defaults.itSettings,
  inConfig(IntegrationTest)(List(fork := true)),
  parallelExecution in Test := true,
  parallelExecution in IntegrationTest := true,
  scalacOptions in Test += "-Yrangepos",
  scalacOptions in IntegrationTest += "-Yrangepos",
  name := "rx-redis-tests",
  libraryDependencies ++= List(
    "org.scalatest"  %% "scalatest"         % "2.2.4"  % "it,test",
    "org.scalacheck" %% "scalacheck"        % "1.12.2" % "test"))

lazy val benchmarks = project enablePlugins AutomateHeaderPlugin dependsOn client settings (
  rxRedisSettings,
  doNotPublish,
  jmhSettings,
  outputTarget in Jmh := target.value / s"scala-${scalaBinaryVersion.value}",
  generatorType in Jmh := "asm",
  mainClass in (Compile, run) := Some("rx.redis.Main"),
  libraryDependencies ++= List(
    "redis.clients"  % "jedis"       % "2.7.0",
    "org.redisson"   % "redisson"    % "1.2.1",
    "net.debasishg" %% "redisclient" % "2.15"))

lazy val `benchmarks-finagle` = project in file("benchmarks") / "finagle" enablePlugins AutomateHeaderPlugin settings (
  rxRedisSettings,
  doNotPublish,
  scalaVersion := "2.10.4",
  jmhSettings,
  outputTarget in Jmh := target.value / s"scala-${scalaBinaryVersion.value}",
  generatorType in Jmh := "asm",
  mainClass in (Compile, run) := Some("rx.redis.Main"),
  libraryDependencies += "com.twitter" %% "finagle-redis" % "6.24.0")

lazy val `benchmarks-scala-redis-nb` = project in file("benchmarks") / "scala-redis-nb" enablePlugins AutomateHeaderPlugin dependsOn core settings (
  rxRedisSettings,
  doNotPublish,
  jmhSettings,
  outputTarget in Jmh := target.value / s"scala-${scalaBinaryVersion.value}",
  generatorType in Jmh := "asm",
  mainClass in (Compile, run) := Some("rx.redis.Main"),
  libraryDependencies += "net.debasishg" %% "redisreact"  % "0.7")

lazy val dist = project disablePlugins AssemblyPlugin settings (
  scalaVersion := "2.11.6",
  target := baseDirectory.value)

// ====================================================================

lazy val rxRedisSettings =
  buildSettings ++ commonSettings ++
  publishSettings ++ formatterSettings

lazy val buildSettings  = List(
          organization := "de.knutwalker",
  organizationHomepage := Some(url("http://knutwalker.de/")),
           description := "Reactive Extension for Redis",
          scalaVersion := "2.11.6",
          nettyVersion := "4.0.28.Final",
         rxJavaVersion := "1.0.8",
        rxScalaVersion := "0.24.1")

lazy val commonSettings = List(
  scalacOptions ++= {
    val crossOpts = scalaBinaryVersion.value match {
      case "2.11" ⇒ List(
        "-Xlint:_", "-Yconst-opt", "-Ywarn-infer-any",
        "-Ywarn-unused", "-Ywarn-unused-import")
      case _      ⇒ List("-Xlint")
    }
    crossOpts ++ List(
      "-deprecation",
      "-encoding",  "UTF-8",
      "-feature",
      "-language:existentials",
      "-language:higherKinds",
      "-language:implicitConversions",
      "-language:postfixOps",
      "-unchecked",
      "-target:jvm-1.7",
      "-Xcheckinit",
      "-Xfatal-warnings",
      "-Xfuture",
      "-Yclosure-elim",
      "-Ydead-code",
      "-Yno-adapted-args",
      "-Ywarn-adapted-args",
      "-Ywarn-dead-code",
      "-Ywarn-inaccessible",
      "-Ywarn-nullary-override",
      "-Ywarn-nullary-unit",
      "-Ywarn-numeric-widen")},
  scalacOptions in (Compile, console) ~= (_ filterNot (x ⇒ x == "-Xfatal-warnings" || x.startsWith("-Ywarn"))),
  shellPrompt := { state ⇒
    import scala.Console._
    val name = Project.extract(state).currentRef.project
    val color = name match {
      case "core" | "commands" | "client"  ⇒ GREEN
      case "api"  | "scala-examples"       ⇒ CYAN
      case "japi" | "java-examples"        ⇒ MAGENTA
      case "shapeless"                     ⇒ YELLOW
      case "tests"                         ⇒ BLUE
      case x if x.startsWith("benchmarks") ⇒ RED
      case _                               ⇒ WHITE
    }
    (if (name == "parent") "" else s"[$color$name$RESET] ") + "> "
  },
  coverageExcludedPackages := """com.example.*|rx.redis.util.pool.*|rx.redis.j?api.*""",
  headers := {
    val thisYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
    val years = List(startYear.value.getOrElse(thisYear), thisYear).distinct.mkString(" – ")
    Map(
      "java"  -> Apache2_0(years, maintainer.value),
      "scala" -> Apache2_0(years, maintainer.value))
  },
  initialCommands in      console := """import rx.redis.api.RxRedis; val client = RxRedis("localhost", 6379)""",
  initialCommands in consoleQuick := "",
  logBuffered := false)

lazy val publishSettings = releaseSettings ++ sonatypeSettings ++ List(
                 startYear := Some(2014),
         publishMavenStyle := true,
   publishArtifact in Test := false,
      pomIncludeRepository := { _ => false },
                maintainer := "Paul Horn",
               profileName := "knutwalker",
                githubUser := "knutwalker",
                githubRepo := "rx-redis",
                  homepage := Some(url(s"https://github.com/${githubUser.value}/${githubRepo.value}")),
               tagComment <<= version map (v => s"Release version $v"),
            commitMessage <<= version map (v => s"Set version to $v"),
               versionBump := sbtrelease.Version.Bump.Minor,
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },
  pomExtra := {
    <licenses>
      <license>
        <name>Apache License, Version 2.0</name>
        <url>https://www.apache.org/licenses/LICENSE-2.0</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <scm>
      <connection>scm:git:https://github.com/{githubUser.value}/{githubRepo.value}.git</connection>
      <developerConnection>scm:git:ssh://git@github.com:{githubUser.value}/{githubRepo.value}.git</developerConnection>
      <tag>master</tag>
      <url>https://github.com/{githubUser.value}/{githubRepo.value}</url>
    </scm>
    <developers>
      <developer>
        <id>{githubUser.value}</id>
        <name>{maintainer.value}</name>
        <url>{organizationHomepage.value.get}</url>
      </developer>
    </developers>
  },
  pomPostProcess := { (node) =>
    val rewriteRule = new scala.xml.transform.RewriteRule {
      override def transform(n: scala.xml.Node): scala.xml.NodeSeq =
        if (n.label == "dependency" && (n \ "scope").text == "provided" && ((n \ "groupId").text == "org.scoverage" || (n \ "artifactId").text == "scala-reflect"))
          scala.xml.NodeSeq.Empty
        else n
    }
    val transformer = new scala.xml.transform.RuleTransformer(rewriteRule)
    transformer.transform(node).head
  },
  releaseProcess := List[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    runTest,
    runIntegrationTest,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    publishSignedArtifacts,
    releaseToCentral,
    setNextVersion,
    commitNextVersion,
    pushChanges,
    publishArtifacts
  ))

lazy val doNotPublish = List(
          publish := (),
     publishLocal := (),
  publishArtifact := false)

lazy val formatterSettings = scalariformSettings ++ List(
  ScalariformKeys.preferences := ScalariformKeys.preferences.value.
    setPreference(AlignSingleLineCaseStatements, true).
    setPreference(DoubleIndentClassDeclaration, true).
    setPreference(RewriteArrowSymbols, true))

lazy val buildsUberJar = List(
        assemblyJarName in assembly := s"${name.value}_${scalaBinaryVersion.value}-${version.value}.jar",
     assemblyOutputPath in assembly := (target in dist).value / (assemblyJarName in assembly).value,
         assemblyOption in assembly ~= { _.copy(includeScala = false, includeDependency = false) },
  assemblyMergeStrategy in assembly := {
    case PathList("META-INF", "changes.txt") => MergeStrategy.rename
    case PathList("META-INF", "licenses.txt") => MergeStrategy.rename
    case PathList("META-INF", "io.netty.versions.properties") => MergeStrategy.rename
    case "CHANGES.txt" | "LICENSE.txt"   => MergeStrategy.rename
    case PathList("org", "cyberneko", "html", xs @ _*) => MergeStrategy.first
    case x =>
      val oldStrategy = (assemblyMergeStrategy in assembly).value
      oldStrategy(x)
  })

// ====================================================================

lazy val maintainer = SettingKey[String]("Maintainer")
lazy val githubUser = SettingKey[String]("Github username")
lazy val githubRepo = SettingKey[String]("Github repository")

lazy val nettyVersion = SettingKey[String]("Version of Netty")
lazy val rxJavaVersion = SettingKey[String]("Version of RxJava")
lazy val rxScalaVersion = SettingKey[String]("Version of RxScala")

lazy val runIntegrationTest = ReleaseStep(
  action = { state =>
    val shouldSkipTests = state get skipTests getOrElse false
    if (!shouldSkipTests) {
      val extracted = Project extract state
      val ref = extracted get thisProjectRef
      extracted.runAggregated(test in IntegrationTest in ref, state)
    } else state
  },
  enableCrossBuild = true)

lazy val publishSignedArtifacts = publishArtifacts.copy(
  action = { state =>
    val extracted = Project extract state
    val ref = extracted get thisProjectRef
    extracted.runAggregated(publishSigned in Global in ref, state)
  },
  enableCrossBuild = true)

lazy val releaseToCentral = ReleaseStep(
  action = { state =>
    val extracted = Project extract state
    val ref = extracted get thisProjectRef
    extracted.runAggregated(sonatypeReleaseAll in Global in ref, state)
  },
  enableCrossBuild = true)

addCommandAlias("travis", ";clean;coverage;test;it:test;coverageReport;coverageAggregate")
addCommandAlias("ping-benchmark", ";benchmarks/clean;benchmarks-finagle/clean;benchmarks-scala-redis-nb/clean;benchmarks/run Ping;benchmarks-finagle/run Ping;benchmarks-scala-redis-nb/run Ping")
