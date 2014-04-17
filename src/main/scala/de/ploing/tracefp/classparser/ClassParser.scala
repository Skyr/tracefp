package de.ploing.tracefp.classparser

import javassist.{CtMethod, ClassPool}
import bytecodeparser.utils.Utils
import bytecodeparser.analysis.stack.StackAnalyzer
import bytecodeparser.analysis.decoders.DecodedMethodInvocationOp
import org.slf4j.LoggerFactory
import scala.util.{Success, Try}


case class CallInfo(callLine : Int, fqClassName : String, methodName : String)


case class MethodInfo(methodName : String, firstLine : Int, calls : List[CallInfo])


case class ClassInfo(packageName : String, className : String, methods : Map[String, List[MethodInfo]])


object ClassParser {
  val logger = LoggerFactory.getLogger(ClassParser.getClass)

  def splitFQClassName(fqClassName : String) = {
    val parts = fqClassName.split("""\.""")
    (parts.slice(0, parts.length-1).mkString("."), parts.last)
  }

  def parseCalls(method : CtMethod) : List[CallInfo] = {
    import scala.collection.JavaConversions._

    val stackAnalyzer = new StackAnalyzer(method)
    stackAnalyzer.analyze().iterator().flatMap { frame =>
      frame.decodedOp match {
        case dmio : DecodedMethodInvocationOp =>
          val linenumber = Utils.getLineNumberAttribute(method).toLineNumber(frame.index)
          logger.debug(s"Method '${dmio.getDeclaringClassName}::${dmio.getName}' is being called at line $linenumber")
          Some(CallInfo(linenumber, dmio.getDeclaringClassName, dmio.getName))
        case _  =>
          None
      }
    }.toList
  }

  def parseMethod(method : CtMethod) : Option[MethodInfo] = {
    if (method.getMethodInfo.getCodeAttribute!=null) {
      val firstLine = Utils.getLineNumberAttribute(method).lineNumber(0)
      logger.debug(s"Analyzing ${method.getName} (${method.getLongName}), starting at line ${firstLine}")
      Try(parseCalls(method)) match {
        case Success(calls) =>
          Some(MethodInfo(method.getName, firstLine, calls.toList))
        case _ =>
          logger.debug(s"Implementation for ${method.getLongName} not in classpath, skipping")
          None
      }
    } else {
      logger.debug(s"${method.getLongName} has no code attribute (native method?), skipping")
      None
    }
  }

  def parse(cp : ClassPool, fqClassName : String) : ClassInfo = {
    val ctClass = cp.getCtClass(fqClassName)
    val methods = ctClass.getMethods().flatMap { method =>
      parseMethod(method)
    }.map { methodInfo =>
      methodInfo.methodName -> methodInfo
    }.toList.groupBy(_._1).mapValues(_.map(_._2))
    val (packageName, className) = splitFQClassName(fqClassName)
    ClassInfo(packageName, className, methods)
  }
}
