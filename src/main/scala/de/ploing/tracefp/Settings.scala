package de.ploing.tracefp

import java.nio.file.Paths


object Settings {
  lazy val settingsBasePath = if (System.getenv("APPDATA")!=null) {
    Paths.get(System.getenv("APPDATA"), "tracefp").toString
  } else {
    Paths.get(System.getProperty("user.home"), ".local", "share", "tracefp").toString
  }
  lazy val ivyConfig = Paths.get(settingsBasePath, "ivy.xml").toString
  lazy val ivyCache = Paths.get(settingsBasePath, "ivycache").toString
  val jdbcDriver = "org.h2.Driver"
  lazy val jdbcUrl = s"jdbc:h2:file:${Paths.get(settingsBasePath, "tracefp").toString}"
}
