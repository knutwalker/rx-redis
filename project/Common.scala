import com.typesafe.sbt.SbtPgp.PgpKeys._
import sbt.Keys._
import sbt._
import sbtbuildinfo.Plugin._
import sbtrelease.ReleasePlugin.ReleaseKeys._
import sbtrelease.ReleasePlugin._
import sbtrelease.ReleaseStateTransformations._
import sbtrelease._
import xerial.sbt.Sonatype.SonatypeKeys._


object Common {
  import IntegrationTests.{skipTests => skipIts}

  private lazy val publishSignedArtifacts = publishArtifacts.copy(action = { st: State =>
    val extracted = Project.extract(st)
    val ref = extracted.get(Keys.thisProjectRef)
    extracted.runAggregated(publishSigned in Global in ref, st)
  })

  private lazy val runIntegrationTest = ReleaseStep(action = { st: State =>
    val shouldSkipIntegrationTests =
      st.get(skipTests).getOrElse(st.get((skipIts in IntegrationTest).key).getOrElse(false))
    if (!shouldSkipIntegrationTests) {
      val extracted = Project.extract(st)
      val ref = extracted.get(Keys.thisProjectRef)
      extracted.runAggregated(Keys.test in IntegrationTest in ref, st)
    } else st
  })

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

  lazy val coreBuildInfoSettings = buildInfoSettings ++List(
    sourceGenerators in Compile <+= buildInfo,
    buildInfoKeys := buildKeys,
    buildInfoPackage := "rx.redis"
  )
}
