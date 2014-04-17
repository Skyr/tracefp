package de.ploing.tracefp.dependencies

import org.apache.ivy.core.settings.IvySettings
import org.apache.ivy.plugins.resolver.{IBiblioResolver, URLResolver}
import java.io.File
import org.apache.ivy.core.module.id.ModuleRevisionId
import org.apache.ivy.core.module.descriptor.{Artifact, Configuration, DefaultDependencyDescriptor, DefaultModuleDescriptor}
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorWriter
import org.apache.ivy.core.resolve.ResolveOptions
import java.nio.file.{Paths, Path, Files}
import org.apache.ivy.core.LogOptions
import org.apache.ivy.core.retrieve.RetrieveOptions
import org.apache.ivy.util.filter.FilterHelper
import org.apache.ivy.core.search.OrganisationEntry
import org.apache.ivy.core.report.ArtifactDownloadReport
import de.ploing.tracefp.Settings


case class Jar(groupId : String, artifactId : String, version : String, file : Option[File])


case class Library(lib : Jar, dependencies : List[Jar])


object Ivy {
  lazy val mavenResolver = {
    /*
    // URLResolver understands maven repo format, but does not honor POM dependencies
    val resolver = new URLResolver()
    resolver.setM2compatible(true)
    resolver.setName("central")
    resolver.addArtifactPattern("http://repo1.maven.org/maven2/[organisation]/[module]/[revision]/[artifact](-[revision]).[ext]")
    */

    val resolver = new IBiblioResolver()
    resolver.setM2compatible(true)
    resolver.setName("maven2")
    resolver
  }

  lazy val ivySettings = {
    val ivySettings = new IvySettings()
    val configFile = new File(Settings.ivyConfig)
    if (configFile.exists()) {
      ivySettings.load(configFile)
    }
    if (ivySettings.getResolver(mavenResolver.getName())==null) {
      ivySettings.addResolver(mavenResolver)
      ivySettings.setDefaultResolver(mavenResolver.getName())
    }
    ivySettings.setDefaultCache(new File(Settings.ivyCache))
    ivySettings
  }

  def createIvyConfigFile(groupId : String, artifactId : String, version : String) = {
    val ivyConfigFile = File.createTempFile("ivy", ".xml")
    ivyConfigFile.deleteOnExit()
    val md = DefaultModuleDescriptor.newDefaultInstance(ModuleRevisionId.newInstance(groupId, artifactId + "-caller", "working"))
    val dd = new DefaultDependencyDescriptor(md, ModuleRevisionId.newInstance(groupId, artifactId, version), false, false, true)
    dd.addDependencyConfiguration("default", "*")
    md.addDependency(dd)
    XmlModuleDescriptorWriter.write(md, ivyConfigFile)

    // println(new String(Files.readAllBytes(Paths.get(ivyConfigFile.toURI))))

    ivyConfigFile
  }

  def getVersions(groupId : String, artifactId : String) : List[String] = {
    val ivy = org.apache.ivy.Ivy.newInstance(ivySettings)
    ivy.listRevisions(groupId, artifactId).toList
  }

  def resolveDependencies(groupId : String, artifactId : String, version : String) : Library = {
    def getReportInfos(report : ArtifactDownloadReport) = {
      val a = report.getArtifact.getId
      val moduleId = a.getArtifactId.getModuleId
      val group = moduleId.getOrganisation
      val artifact = moduleId.getName
      val version = a.getRevision
      (group, artifact, version)
    }


    // Inspired by http://developers-blog.org/blog/def/2010/11/08/Embed-Ivy-How-to-use-Ivy-with-Java

    val ivy = org.apache.ivy.Ivy.newInstance(ivySettings)
    val ivyConfigFile = createIvyConfigFile(groupId, artifactId, version)

    val resolveOptions = new ResolveOptions()
      .setConfs(Array("default"))
      .setValidate(true)
      .setResolveMode(ResolveOptions.RESOLVEMODE_DEFAULT)
      .setArtifactFilter(FilterHelper.NO_FILTER)
      .setRefresh(true)
      .setTransitive(true)
      .setOutputReport(false)
    resolveOptions.setLog(LogOptions.LOG_QUIET)
    val report = ivy.resolve(ivyConfigFile.toURI.toURL, resolveOptions)

    // println("xxx - " + ivy.listModuleEntries(new OrganisationEntry(mavenResolver, "org.springframework")).mkString(","))

/*
    val retrieveOptions = new RetrieveOptions()
      .setMakeSymlinks(true)
      .setConfs(Array("default"))
    ivy.retrieve(report.getModuleDescriptor.getModuleRevisionId, "/tmp/ivytest/[artifact]-[type]-[revision].[ext]", retrieveOptions)
*/

    val jars = report.getAllArtifactsReports.map { report =>
      val (group, artifact, ver) = getReportInfos(report)
      new Jar(group, artifact, ver, Option(report.getLocalFile))
    }.toList

    new Library(jars.find { jar =>
      jar.groupId==groupId && jar.artifactId==artifactId && jar.version==version
    }.get, jars.filterNot { jar =>
      jar.groupId==groupId && jar.artifactId==artifactId && jar.version==version
    })
  }
}
