
import uk.gov.hmrc.DefaultBuildSettings

val appName = "income-tax-dividends"

ThisBuild / majorVersion := 0
ThisBuild / scalaVersion := "2.13.12"

lazy val coverageSettings: Seq[Setting[?]] = {
  import scoverage.ScoverageKeys

  val excludedPackages = Seq(
    "<empty>",
    ".*Reverse.*",
    ".*standardError*.*",
    ".*govuk_wrapper*.*",
    ".*main_template*.*",
    "uk.gov.hmrc.BuildInfo",
    "app.*",
    "prod.*",
    "config.*",
    "testOnly.*",
    "testOnlyDoNotUseInAppConf.*",
    "controllers.testOnly.*",
  )

  Seq(
    ScoverageKeys.coverageExcludedPackages := excludedPackages.mkString(";"),
    ScoverageKeys.coverageMinimumStmtTotal := 95,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
}

lazy val microservice = Project(appName, file("."))
  .enablePlugins(PlayScala, SbtDistributablesPlugin)
  .settings(
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    scalacOptions += s"-Wconf:src=${target.value}/.*:s"
  )
  .configs(Test)
  .settings(PlayKeys.playDefaultPort := 9307)
  .settings(resolvers += Resolver.jcenterRepo)
  .settings(coverageSettings *)
  .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)
  .disablePlugins(JUnitXmlReportPlugin)

lazy val itSettings = DefaultBuildSettings.itSettings() ++ Seq(
  unmanagedSourceDirectories.withRank(KeyRanks.Invisible) := Seq(
    baseDirectory.value / "it"
  )
)

lazy val it = project
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test") // the "test->test" allows reusing test code and test dependencies
  .settings(itSettings)