package de.ploing.tracefp.classparser

import java.io.{File, FileInputStream}
import java.util.zip.{ZipEntry, ZipInputStream}
import scala.annotation.tailrec
import javassist.ClassPool


object JarParser {
  def classesInJarFile(jarFile : File) : List[String] = {
    val classSuffix = ".class"
    val zip = new ZipInputStream(new FileInputStream(jarFile))
    @tailrec def getClassNames(initialList : List[String]) : List[String] = {
      val entry = zip.getNextEntry
      if (entry!=null) {
        if (!entry.isDirectory && entry.getName.endsWith(classSuffix)) {
          val className = entry.getName.replace('/','.').substring(0, entry.getName.length-classSuffix.length)
          getClassNames(className :: initialList)
        } else {
          getClassNames(initialList)
        }
      } else {
        initialList
      }
    }
    getClassNames(List())
  }

  def parse(jarFile : File, dependencies : Option[List[File]]) : Map[String, ClassInfo] = {
    val cp = new ClassPool()
    val mainJarPath = jarFile.getAbsolutePath
    cp.appendClassPath(mainJarPath)
    dependencies match {
      case Some(list) =>
        list.map(_.getAbsolutePath)
          .filterNot(_==mainJarPath)
          .foreach(cp.appendClassPath(_))
      case None =>
    }
    cp.appendSystemPath()
    classesInJarFile(jarFile)
      .map(ClassParser.parse(cp, _))
      .map { classInfo => s"${classInfo.packageName}.${classInfo.className}" -> classInfo }
      .toMap
  }

  def parse(jarFile : File, dependencies : List[File]) : Map[String, ClassInfo] = {
    parse(jarFile, Some(dependencies))
  }

  def parse(jarFile : File) : Map[String, ClassInfo] = {
    parse(jarFile, None)
  }
}
