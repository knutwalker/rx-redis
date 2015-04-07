import com.typesafe.sbt.pgp.PgpKeys.publishSigned
import de.heikoseeberger.sbtheader.license.Apache2_0
import sbtrelease.ReleasePlugin.ReleaseKeys._
import sbtrelease.ReleaseStateTransformations._
import sbtrelease.ReleaseStep
import scalariform.formatter.preferences._
import scoverage.ScoverageSbtPlugin.ScoverageKeys.coverageExcludedPackages
import spray.revolver.RevolverPlugin.Revolver
import xerial.sbt.Sonatype.SonatypeKeys.{profileName, sonatypeReleaseAll}


lazy val parent = project in file(".") dependsOn (
  api, japi, client, commands, core) aggregate (
  api, japi, client, commands, core, `java-examples`, `scala-examples`) settings (
  buildsUberJar,
  rxRedisSettings,
  doNotPublish,
  name := "rx-redis-parent")

lazy val core = project in file("modules") / "core" enablePlugins AutomateHeaderPlugin configs RegressionTest settings (
  mainRegSettings,
  rxRedisSettings,
  ivyConfigurations += CompileTimeOnly,
  unmanagedClasspath in Compile ++= update.value.select(configurationFilter(CompileTimeOnly.name)),
  testOptions in Test := List(Tests.Filter(unitFilter)),
  testOptions in RegressionTest := List(Tests.Filter(regFilter)),
  name := "rx-redis-core",
  libraryDependencies ++= List(
    "org.scala-lang"  % "scala-reflect" % scalaVersion.value % "compileonly",
    "io.netty"        % "netty-buffer"  % nettyVersion.value % "test" withSources() withJavadoc(),
    "org.scalatest"  %% "scalatest"     % "2.2.2"            % "test",
    "org.scalacheck" %% "scalacheck"    % "1.11.6"           % "test"))

lazy val commands = project in file("modules") / "commands" enablePlugins AutomateHeaderPlugin dependsOn core settings (
  rxRedisSettings,
  name := "rx-redis-commands",
  libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.2" % "test")

lazy val client = project in file("modules") / "client" enablePlugins AutomateHeaderPlugin dependsOn commands configs IntegrationTest settings (
  buildsUberJar,
  mainItSettings,
  rxRedisSettings,
  name := "rx-redis-client",
  libraryDependencies ++= List(
    "io.netty"            % "netty-transport"  % nettyVersion.value withSources() withJavadoc(),
    "io.netty"            % "netty-buffer"     % nettyVersion.value withSources() withJavadoc(),
    "io.netty"            % "netty-common"     % nettyVersion.value withSources() withJavadoc(),
    "com.netflix.rxjava"  % "rxjava-core"      % rxJavaVersion.value withSources() withJavadoc(),
    "org.scalatest"      %% "scalatest"        % "2.2.2" % "it,test"))

lazy val api = project in file("language-bindings") / "scala" enablePlugins AutomateHeaderPlugin dependsOn client settings (
  buildsUberJar,
  rxRedisSettings,
  name := "rx-redis-scala",
  libraryDependencies += "com.netflix.rxjava" % "rxjava-scala" % rxJavaVersion.value exclude("org.scala-lang", "scala-library") withSources() withJavadoc())

lazy val japi = project in file("language-bindings") / "java" enablePlugins AutomateHeaderPlugin dependsOn client settings (
  buildsUberJar,
  rxRedisSettings,
  name := "rx-redis-java")

lazy val `scala-examples` = project in file("examples") / "scala" enablePlugins AutomateHeaderPlugin dependsOn api settings (
  Revolver.settings,
  rxRedisSettings,
  name := "rx-redis-example")

lazy val `java-examples` = project in file("examples") / "java" enablePlugins AutomateHeaderPlugin dependsOn japi settings (
  rxRedisSettings,
  name := "rx-redis-java-example")

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
          nettyVersion := "4.0.27.Final",
         rxJavaVersion := "0.20.6")

lazy val commonSettings = List(
  scalacOptions ++= List(
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
    "-Xlint:_",
    "-Yclosure-elim",
    "-Yconst-opt",
    "-Ydead-code",
    "-Yno-adapted-args",
    "-Ywarn-adapted-args",
    "-Ywarn-dead-code",
    "-Ywarn-inaccessible",
    "-Ywarn-infer-any",
    "-Ywarn-nullary-override",
    "-Ywarn-nullary-unit",
    "-Ywarn-numeric-widen",
    "-Ywarn-unused",
    "-Ywarn-unused-import"),
  scalacOptions in Test += "-Yrangepos",
  scalacOptions in (Compile, console) ~= (_ filterNot (x ⇒ x == "-Xfatal-warnings" || x.startsWith("-Ywarn"))),
  shellPrompt := { state ⇒
    val name = Project.extract(state).currentRef.project
    (if (name == "parent") "" else name + " ") + "> "
  },
  coverageExcludedPackages := "buildinfo",
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
        if (n.label == "dependency" && (n \ "groupId").text == "org.scoverage")
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
    runRegressionTest,
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

lazy val RegressionTest = config("reg").extend(Test)
lazy val CompileTimeOnly = config("compileonly").hide

lazy val runIntegrationTest = runTestIn(IntegrationTest)
lazy val runRegressionTest = runTestIn(RegressionTest)

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


def runTestIn(conf: Configuration) = ReleaseStep(
  action = { state =>
    val shouldSkipTests = state get skipTests getOrElse false
    if (!shouldSkipTests) {
      val extracted = Project extract state
      val ref = extracted get thisProjectRef
      extracted.runAggregated(test in conf in ref, state)
    } else state
  },
  enableCrossBuild = true)

def regFilter(name: String): Boolean = name endsWith "RegressionSpec"
def unitFilter(name: String): Boolean = (name endsWith "Spec") && !regFilter(name)

// Like to use testSettings, but IntelliJ doesn't get it right
lazy val mainRegSettings = inConfig(RegressionTest)(Defaults.testTasks)

lazy val mainItSettings = Defaults.itSettings ++ inConfig(IntegrationTest)(List(
  logBuffered := false,
  fork := true
))
