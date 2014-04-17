package de.ploing.tracefp.db

import scala.slick.driver.H2Driver.simple._
import scala.slick.jdbc.JdbcBackend


object Tables {
  class Library(tag: Tag) extends Table[(Int, String, String, String, Boolean)](tag, "LIBRARIES") {
    def id = column[Int]("ID", O.PrimaryKey, O.AutoInc) // This is the primary key column
    def group = column[String]("GROUP")
    def artifact = column[String]("ARTIFACT")
    def version = column[String]("VERSION")
    def parsed = column[Boolean]("PARSED")
    // Every table needs a * projection with the same type as the table's type parameter
    def * = (id, group, artifact, version, parsed)
  }
  val libraries = TableQuery[Library]

  object Library {
    def lookup(group : String, artifact : String, version : String)(implicit session: JdbcBackend#Session) : Option[(Int, Boolean)] = {
      val search = libraries.filter { lib =>
        lib.group===group && lib.artifact===artifact && lib.version===version
      }.take(1).build()
      if (search.size>0) {
        val result = search.head
        Some((result._1, result._5))
      } else {
        None
      }
    }
    def lookupOrCreate(group : String, artifact : String, version : String, willBeParsed : Boolean)(implicit session: JdbcBackend#Session) : Int = {
      val search = libraries.filter { lib =>
        lib.group===group && lib.artifact===artifact && lib.version===version
      }.take(1).mapResult(_._1).build()

      if (search.size>0) {
        search.head
      } else {
        (libraries returning libraries.map(_.id)) += (0, group, artifact, version, willBeParsed)
      }
    }
  }


  class LibDependency(tag: Tag) extends Table[(Int, Int)](tag, "LIB_DEPENDENCIES") {
    def libId = column[Int]("LIB_ID")
    def dependsOnId = column[Int]("DEPENDS_ON_ID")
    // Every table needs a * projection with the same type as the table's type parameter
    def * = (libId, dependsOnId)

    def library = foreignKey("FK_LIBDEPENDENCIES_LIBID_LIBRARIES", libId, libraries)(_.id)
    def dependsOn = foreignKey("FK_LIBDEPENDENCIES_DEPENDSONID_LIBRARIES", dependsOnId, libraries)(_.id)
    def libIdIndex = index("I_LIBDEPENDENCIES_LIBID", libId, false)
    def dependsOnIdIndex = index("I_LIBDEPENDENCIES_DEPENDSONID", dependsOnId, false)
  }
  val libDependencies = TableQuery[LibDependency]

  class CallTrace(tag: Tag) extends Table[(Int, String, Int, String)](tag, "CALL_TRACES") {
    def libId = column[Int]("LIB_ID")
    def methodName = column[String]("METHOD_NAME")
    def line = column[Int]("LINE")
    def calledMethod = column[String]("CALLED_METHOD")
    // Every table needs a * projection with the same type as the table's type parameter
    def * = (libId, methodName, line, calledMethod)

    def library = foreignKey("FK_CALLTRACES_LIBID_LIBRARIES", libId, libraries)(_.id)
    def queryIndex = index("I_CALLTRACE", (methodName, line, calledMethod), false)
  }
  val callTraces = TableQuery[CallTrace]
}
