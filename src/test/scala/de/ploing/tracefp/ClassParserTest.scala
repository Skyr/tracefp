package de.ploing.tracefp

import org.scalatest.{Matchers, FlatSpec}
import de.ploing.tracefp.classparser.{JarParser, ClassParser}
import javassist.ClassPool
import java.io.File


class ClassParserTest extends FlatSpec with Matchers {
  val springJar = new File("src/test/resources/spring-web-3.0.0.RELEASE.jar")

  "A full-qualified class name" should "consist of a package name and a class name" in {
    val (packageName, className) = ClassParser.splitFQClassName("java.net.URLClassLoader")
    packageName should equal ("java.net")
    className should equal ("URLClassLoader")
  }

  it should "return an empty package name for the default package" in {
    val (packageName, className) = ClassParser.splitFQClassName("SomeClass")
    packageName should equal ("")
    className should equal ("SomeClass")
  }

  "Class StackAnalyzer" should "contain method analyze" in {
    val cp = new ClassPool()
    cp.appendSystemPath()
    val info = ClassParser.parse(cp, "bytecodeparser.analysis.stack.StackAnalyzer")
    info.methods.keySet should contain ("analyze")
  }

  "Jar spring-web-3.0.0.RELEASE.jar" should "contain class org.springframework.web.util.WebUtils" in {
    val classes = JarParser.classesInJarFile(springJar)
    classes should contain ("org.springframework.web.util.WebUtils")
  }

  it should "get the parsed class information of org.springframework.web.util.WebUtils" in {
    val classes = JarParser.parse(springJar)
    classes.keySet should contain ("org.springframework.web.util.WebUtils")
  }
}
