package de.ploing.tracefp.stacktrace


case class StacktraceElement(packageName : String, className : String, methodName : String, fileName : String, lineNumber : Int) {
  val fqClassName = s"${packageName}.${className}"
  override def toString() = s"$fqClassName::$methodName ($fileName:$lineNumber)"
}


object StacktraceElement {
  val pattern = """(\w.*)\.([^.]*)\.([^.]*)\((.*):(\d*)\)""".r

  def parse(line : String) : Option[StacktraceElement] = {
    pattern findFirstIn line match {
      case Some(pattern(packageName, className, methodName, fileName, lineNumber)) =>
        Some(new StacktraceElement(packageName, className, methodName, fileName, Integer.parseInt(lineNumber)))
      case None => None
    }
  }
}
