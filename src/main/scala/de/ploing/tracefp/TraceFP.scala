package de.ploing.tracefp

import org.rogach.scallop.ScallopConf
import java.io.{InputStreamReader, BufferedReader, FileInputStream}
import de.ploing.tracefp.stacktrace.{StacktraceElement, StacktraceParser}
import de.ploing.tracefp.dependencies.{Library, LibraryParser, Ivy}
import scala.slick.driver.H2Driver.simple._
import de.ploing.tracefp.db.Tables
import scala.slick.jdbc.meta.MTable
import scala.util.{Failure, Success, Try}
import de.ploing.tracefp.classparser.MethodInfo


object TraceFP {
  def setupDb() {
    Database.forURL(Settings.jdbcUrl, driver = Settings.jdbcDriver) withSession {
      implicit session =>
        if (MTable.getTables.list().size==0) {
          (Tables.libraries.ddl ++ Tables.libDependencies.ddl ++ Tables.callTraces.ddl).create
        }
    }
  }

  def scanLibraryToDb(group : String, artifact : String, version : String) {
    Try(LibraryParser.parse(group, artifact, version)) match {
      case Success(libInfo) =>
        Database.forURL(Settings.jdbcUrl, driver = Settings.jdbcDriver) withSession {
          implicit session =>
          // Get id if new or unparsed entry
            val id = Tables.Library.lookup(group, artifact, version) match {
              case None =>
                // No entry found, create new one
                val id = Tables.Library.lookupOrCreate(group, artifact, version, true)
                Some(id)
              case Some((id, false)) =>
                // Uparsed entry found
                Some(id)
              case _ =>
                // Already parsed, skip
                None
            }
            // Process if processing is necessary
            id match {
              case Some(id) =>
                println(s"*** Scanning ${group}:${artifact}:${version}")
                // Set parsed to true
                val entry = for { lib <- Tables.libraries if lib.id === id } yield lib.parsed
                entry.update(true)
                // Insert dependencies
                libInfo.dependencies.foreach { jar =>
                  val jarId = Tables.Library.lookupOrCreate(jar.groupId, jar.artifactId, jar.version, false)
                  Tables.libDependencies += (id, jarId)
                }
                // Insert call traces
                libInfo.classes.values.foreach { classInfo =>
                  classInfo.methods.values.flatMap { l => l }.foreach { methodInfo : MethodInfo =>
                    methodInfo.calls.foreach { callInfo =>
                      val callingMethod = s"${classInfo.packageName}.${classInfo.className}::${methodInfo.methodName}"
                      val calledMethod = s"${callInfo.fqClassName}::${callInfo.methodName}"
                      // println(s"Call: Method ${callingMethod} does in line ${callInfo.callLine} call to ${calledMethod}")
                      Tables.callTraces += (id, callingMethod, callInfo.callLine, calledMethod)
                    }
                  }
                }
                // Recursively call for dependencies
                libInfo.dependencies.foreach { jar =>
                  scanLibraryToDb(jar.groupId, jar.artifactId, jar.version)
                }
              case None =>
            }
        }
      case Failure(e) =>
        println(s"Failure downloading dependencies of ${group}:${artifact}:${version}")
    }
  }

  def scanLibrary(args : Array[String]) {
    // Parse options
    val opts = new ScallopConf(args) {
      val group = trailArg[String](required = true)
      val artifact = trailArg[String](required = true)
      val version = trailArg[String](required = false)
    }
    val group = opts.group.apply()
    val artifact = opts.artifact.apply()

    setupDb()

    val versions = if (opts.version.isEmpty) {
      Ivy.getVersions(opts.group.apply(), opts.artifact.apply())
    } else {
      List(opts.version.apply())
    }

    versions.foreach { version =>
      println(s"*** Checking ${group}:${artifact}:${version}")
      scanLibraryToDb(group, artifact, version)
    }
  }



  def analyzeStacktrace(args : Array[String]) {
    // Parse options
    val opts = new ScallopConf(args) {
      val input = trailArg[String](required = false)
    }
    // Get input stream (file/stdin)
    val inStream = opts.input.orElse(Some("-")).apply match {
      case "-" => System.in
      case filename => new FileInputStream(filename)
    }
    // Parse stacktrace
    val reader = new BufferedReader(new InputStreamReader(inStream))
    val traces = StacktraceParser.parseAll(reader)

    setupDb()

    val candidates = new VersionCandidates()

    Database.forURL(Settings.jdbcUrl, driver = Settings.jdbcDriver) withSession {
      implicit session =>
        for (trace <- traces) {
          var prevTrace : Option[StacktraceElement] = None
          for (el <- trace) {
            prevTrace match {
              case Some(prev) =>
                val fromMethod = s"${el.fqClassName}::${el.methodName}"
                val toMethod = s"${prev.fqClassName}::${prev.methodName}"
                val matchingLibs = for {
                  callTrace <- Tables.callTraces if (callTrace.methodName===fromMethod && callTrace.line===el.lineNumber && callTrace.calledMethod===toMethod)
                  library <- Tables.libraries if (library.id===callTrace.libId)
                } yield (library)
                println(s"Call to ${toMethod} from ${fromMethod} in line ${el.lineNumber}")
                val candidateBundle = new CandidateBundle()
                matchingLibs.mapResult {
                  case (id, group, artifact, version, parsed) => (group,artifact,version)
                }.build().foreach { case (group,artifact,version) =>
                  println(s"  ${group}:${artifact}:${version}")
                  candidateBundle.addCandidate(group, artifact, version)
                }
                candidates.addCandidateBundle(candidateBundle)
              case None =>
            }
            prevTrace = Some(el)
          }
        }
    }

    println
    println("*** Result from Stacktrace:")
    candidates.candidates.foreach { case ((group,artifact), versions) =>
      println(s"  ${group}:${artifact} ${versions.toList.sorted.mkString(", ")}")
    }

    println
    println("*** Cross-checking with dependencies...")
    val mavenCandidates = new VersionCandidates()
    Database.forURL(Settings.jdbcUrl, driver = Settings.jdbcDriver) withSession {
      implicit session =>
        candidates.candidates.foreach { case ((group,artifact), versions) =>
          versions.foreach {
            version =>
              println(s"Cross-checking ${group}:${artifact}:${version}")
              val libraryId = (for {
                library <- Tables.libraries if (library.group === group && library.artifact === artifact && library.version === version)
              } yield library.id).build().head
              val dependsOn = for {
                dependency <- Tables.libDependencies if (dependency.libId === libraryId)
                dependentLib <- Tables.libraries if (dependentLib.id === dependency.dependsOnId)
              } yield (dependentLib.group, dependentLib.artifact, dependentLib.version)
              val isDependencyOf = for {
                dependency <- Tables.libDependencies if (dependency.dependsOnId === libraryId)
                superLib <- Tables.libraries if (superLib.id === dependency.libId)
              } yield (superLib.group, superLib.artifact, superLib.version)

              def matchingOrNoCandidate(query : Query[(Column[String],Column[String],Column[String]), (String,String,String)]) = {
                val matchList = query.build().map { case (depGroup : String, depArtifact : String, depVersion : String) =>
                  if (candidates.candidates.contains((depGroup, depArtifact)) && !candidates.candidates((depGroup, depArtifact)).contains(depVersion)) {
                    println(s"- Ruled out by ${depGroup}:${depArtifact}:${depVersion}")
                  }
                  !candidates.candidates.contains((depGroup, depArtifact)) || candidates.candidates((depGroup, depArtifact)).contains(depVersion)
                }
                matchList.isEmpty || matchList.reduce(_ & _)
              }

              if (matchingOrNoCandidate(dependsOn) && matchingOrNoCandidate(isDependencyOf)) {
                mavenCandidates.addCandidate(group, artifact, version)
              }
            }
        }
    }
    println
    println("*** Cross-checked with Maven dependencies:")
    mavenCandidates.candidates.foreach { case ((group,artifact), versions) =>
      println(s"  ${group}:${artifact} ${versions.toList.sorted.mkString(", ")}")
    }
  }



  def main(args : Array[String]) : Unit = {
    println("tracefp - the Java stacktrace fingerprinter")
    val mode = if (args.length>0) {
      args(0)
    } else {
      ""
    }
    mode match {
      case "analyze" => analyzeStacktrace(args.tail)
      case "scanlib" => scanLibrary(args.tail)
      case _ =>
        println("Wrong mode parameter")
        println("tracefp scanlib ... - Download and scan a library")
        println("tracefp analyze ... - Analyze a stacktrace")
    }
  }
}
