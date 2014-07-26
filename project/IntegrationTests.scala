import sbt._
import sbt.Keys._
import sbt.Defaults._
import sbt.{SettingKey, Plugin}

object IntegrationTests extends Plugin {
  val skipTests = SettingKey[Boolean]("release-skip-integration-tests")

  private lazy val additionalSettings = List[Setting[_]](
    skipTests := false,
    logBuffered := false,
    fork := true
  )

  lazy val integrationTestsSettings = itSettings ++ inConfig(IntegrationTest)(additionalSettings)
}
