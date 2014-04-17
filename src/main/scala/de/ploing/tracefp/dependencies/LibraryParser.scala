package de.ploing.tracefp.dependencies

import de.ploing.tracefp.classparser.{JarParser, ClassInfo}


case class JarInfo(groupId : String, artifactId : String, version : String)

case class LibraryInfo(groupId : String, artifactId : String, version : String, classes : Map[String, ClassInfo], dependencies : List[JarInfo])


object LibraryParser {
  def parse(groupId : String, artifactId : String, version : String) : LibraryInfo = {
    val jars = Ivy.resolveDependencies(groupId, artifactId, version)
    val classes = JarParser.parse(jars.lib.file.get, jars.dependencies.flatMap(_.file))
    val dependencies = jars.dependencies.map { jar =>
      new JarInfo(jar.groupId, jar.artifactId, jar.version)
    }
    new LibraryInfo(groupId, artifactId, version, classes, dependencies)
  }
}
