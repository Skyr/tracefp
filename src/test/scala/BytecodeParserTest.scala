import bytecodeparser.analysis.decoders.DecodedMethodInvocationOp
import bytecodeparser.analysis.stack.StackAnalyzer
import bytecodeparser.utils.Utils
import javassist.{CtMethod, ClassPool}

import scala.collection.JavaConversions._


object BytecodeParserTest {
  def methodTest1(method : CtMethod) : Unit =  {
    if (method.getMethodInfo.getCodeAttribute!=null) {
      println(s"Analyzing ${method.getName} (${method.getLongName}), starting at line ${Utils.getLineNumberAttribute(method).lineNumber(0)}")
      val stackAnalyzer = new StackAnalyzer(method)
      stackAnalyzer.analyze().iterator().foreach { frame =>
        frame.decodedOp match {
          case dmio : DecodedMethodInvocationOp =>
            val linenumber = Utils.getLineNumberAttribute(method).toLineNumber(frame.index)
            println(s"  method '${dmio.getDeclaringClassName}::${dmio.getName()}' is being called at line $linenumber")
          case _  =>
        }
      }
    } else {
      println(s"${method.getLongName} has no code attribute (native method?), skipping")
    }
  }

  def methodTest2(method : CtMethod) : Unit =  {
    try {
      val lineNumbers = Utils.getLineNumberAttribute(method)
      println("  lineNumber table length = " + lineNumbers.tableLength())
    } catch {
      case _ : NullPointerException => println("  Whoopsie...")
    }
  }

  def main(args : Array[String]) : Unit = {
    val cp = new ClassPool()
    cp.appendSystemPath()
    // val ctClass = cp.getCtClass("java.util.Arrays")
    val ctClass = cp.getCtClass("bytecodeparser.analysis.stack.StackAnalyzer")
    ctClass.getMethods().foreach { method =>
      methodTest1(method)
    }
  }
}
