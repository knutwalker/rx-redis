import com.typesafe.sbt.SbtPgp.PgpKeys._
import com.typesafe.sbt.SbtScalariform._
import sbt.Defaults._
import sbt.Keys._
import sbt._
import sbtbuildinfo.Plugin._
import sbtrelease.ReleasePlugin.ReleaseKeys._
import sbtrelease.ReleasePlugin._
import sbtrelease.ReleaseStateTransformations._
import sbtrelease._
import xerial.sbt.Sonatype.SonatypeKeys._
import scalariform.formatter.preferences._


object Common {
  lazy val RegressionTest = config("reg").extend(Test)
  lazy val CompileTimeOnly = config("compileonly").hide

  def regFilter(name: String): Boolean = name endsWith "RegressionSpec"
  def unitFilter(name: String): Boolean = (name endsWith "Spec") && !regFilter(name)

  private lazy val publishSignedArtifacts = publishArtifacts.copy(action = { st: State =>
    val extracted = Project.extract(st)
    val ref = extracted.get(Keys.thisProjectRef)
    extracted.runAggregated(publishSigned in Global in ref, st)
  })

  private def runTestIn(conf: Configuration) = ReleaseStep(action = { st: State =>
    val shouldSkipTests = st.get(skipTests).getOrElse(false)
    if (!shouldSkipTests) {
      val extracted = Project.extract(st)
      val ref = extracted.get(Keys.thisProjectRef)
      extracted.runAggregated(Keys.test in conf in ref, st)
    } else st
  })

  private lazy val runIntegrationTest = runTestIn(IntegrationTest)
  private lazy val runRegressionTest = runTestIn(RegressionTest)

  private lazy val releaseToCentral = ReleaseStep(action = { st: State =>
    val extracted = Project.extract(st)
    val ref = extracted.get(Keys.thisProjectRef)
    extracted.runAggregated(sonatypeReleaseAll in Global in ref, st)
  })

  lazy val signedReleaseSettings = releaseSettings ++ List(
    releaseProcess := List[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      setReleaseVersion,
      runClean,
      runTest,
      runRegressionTest,
      runIntegrationTest,
      commitReleaseVersion,
      tagRelease,
      publishSignedArtifacts,
      releaseToCentral,
      setNextVersion,
      commitNextVersion,
      pushChanges,
      publishArtifacts
    ).map(_.copy(enableCrossBuild = false)),
    tagComment <<= (Keys.version in ThisBuild) map (v => s"Release version $v"),
    commitMessage <<= (Keys.version in ThisBuild) map (v => s"Set version to $v"),
    versionBump := sbtrelease.Version.Bump.Bugfix
  )

  private lazy val buildKeys = List[BuildInfoKey](
    BuildInfoKey.map(organization) { case (k, v) => "groupId" -> v},
    BuildInfoKey.map(name) { case (k, v) => "artifactId" -> v },
    version,
    scalaVersion,
    sbtVersion,
    buildInfoBuildNumber,
    BuildInfoKey.action("buildTimeMillis") {
      System.currentTimeMillis
    },
    BuildInfoKey.action("buildTime") {
      val format = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
      format.setTimeZone(java.util.TimeZone.getTimeZone("UTC"))
      format.format(new java.util.Date())
    },
    BuildInfoKey.action("gitCommit") {
      "git describe --always".!!.trim
    },
    BuildInfoKey.action("gitCommitSha1") {
      "git rev-parse HEAD".!!.trim
    }
  )

  lazy val mainBuildInfoSettings = buildInfoSettings ++List(
    sourceGenerators in Compile <+= buildInfo,
    buildInfoKeys := buildKeys,
    buildInfoPackage := "rx.redis"
  )

  lazy val mainItSettings = itSettings ++ inConfig(IntegrationTest)(List(
    logBuffered := false,
    fork := true
  ))

  // Like to use testSettings, but IntelliJ doesn't get it right
  lazy val mainRegSettings = inConfig(RegressionTest)(Defaults.testTasks)

  lazy val formatterSettings = scalariformSettings ++ List(
    ScalariformKeys.preferences := ScalariformKeys.preferences.value.
      setPreference(AlignSingleLineCaseStatements, true).
      setPreference(DoubleIndentClassDeclaration, true).
      setPreference(PreserveDanglingCloseParenthesis, true).
      setPreference(RewriteArrowSymbols, true)
  )
}
