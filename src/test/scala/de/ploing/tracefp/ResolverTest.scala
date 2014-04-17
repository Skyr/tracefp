package de.ploing.tracefp

import org.scalatest.{Matchers, FlatSpec}
import de.ploing.tracefp.dependencies.Ivy


class ResolverTest extends FlatSpec with Matchers {
  "3.0.0.RELEASE" should "be a version of org.springframework:spring-web" in {
    val versions = Ivy.getVersions("org.springframework", "spring-web")
    versions should not be empty
    versions should contain("3.0.0.RELEASE")
  }

  "Jar spring-web-3.0.0.RELEASE.jar" should "be resolved by Ivy" in {
    val jarFiles = Ivy.resolveDependencies("org.springframework", "spring-web", "3.0.0.RELEASE")
    jarFiles should not be null
    jarFiles.lib.groupId should equal("org.springframework")
    jarFiles.lib.artifactId should equal("spring-web")
    jarFiles.lib.version should equal("3.0.0.RELEASE")
    jarFiles.lib.file should not be null
  }
}
