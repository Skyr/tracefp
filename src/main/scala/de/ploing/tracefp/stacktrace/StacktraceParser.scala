package de.ploing.tracefp.stacktrace

import java.io.BufferedReader


object StacktraceParser {
  def skipToMatchingLine(reader : BufferedReader) : Option[StacktraceElement] = {
    val line = reader.readLine()
    if (line==null) {
      None
    } else {
      StacktraceElement.parse(line) match {
        case Some(el) => Some(el)
        case None => skipToMatchingLine(reader)
      }
    }
  }

  def parseElements(reader : BufferedReader) : Stacktrace = {
    val line = reader.readLine()
    if (line==null) {
      List()
    } else {
      StacktraceElement.parse(line) match {
        case None => List()
        case Some(el) => el +: parseElements(reader)
      }
    }
  }

  def parse(reader : BufferedReader) : Option[Stacktrace] = {
    val firstMatch = skipToMatchingLine(reader)
    firstMatch match {
      case None => None
      case Some(el) => Some(el +: parseElements(reader))
    }
  }

  def parseAll(reader : BufferedReader) : List[Stacktrace] = parse(reader) match {
    case None => List()
    case Some(el : Stacktrace) => el +: parseAll(reader)
  }
}
