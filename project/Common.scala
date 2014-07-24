import sbt._
import Keys._
import Defaults._
import sbtrelease._
import sbtrelease.ReleasePlugin._
import sbtrelease.ReleasePlugin.ReleaseKeys._
import com.typesafe.sbt.SbtPgp.PgpKeys._
import sbtrelease.ReleaseStateTransformations._


object Common {

  lazy val publishSignedAction = { st: State =>
    val extracted = Project.extract(st)
    val ref = extracted.get(Keys.thisProjectRef)
    extracted.runAggregated(publishSigned in Global in ref, st)
  }

  lazy val signedReleaseSettings = releaseSettings ++ Seq(
    releaseProcess ~= { s: Seq[ReleaseStep] =>
      s map {
        case `publishArtifacts` => publishArtifacts.copy(action = publishSignedAction)
        case s => s
      } map (_.copy(enableCrossBuild = false))
    }
  )
}
