import sharedType.studentDBColType

//database migration https://github.com/nafg/slick-migration-api
object db {
  import slick.jdbc.meta.MTable
  import slick.jdbc.SQLiteProfile.api._
  import slick.lifted.QueryBase

  class Student(tag: Tag) extends Table[studentDBColType] (tag,"Student") {
    def id =column[Int]("SUP_ID", O.PrimaryKey,O.AutoInc) // This is the primary key column
    def name = column[String]("NAME")
    def cls = column[String]("class")
    def participatedTimeStamp = column[String]("participatedTimeStamp") //remaining classes
    def rclasses = column[Int]("rclasses") //remaining classes
    def startdate = column[Int]("startdate")
    def enddate = column[Int]("enddate")
    def phone = column[String]("phone")
    def extra = column[String]("extra")
    def * = (id, name, cls,participatedTimeStamp,rclasses ,startdate, enddate, phone,extra)// Every table needs a * projection with the same type as the table's type parameter
  }
}

/*
  import slick.migration.api.SQLiteDialect
def sqliteMigration(): Unit ={
implicit val dialect: SQLiteDialect =new SQLiteDialect

implicit val dialect = new H2Dialect

val init =
TableMigration(myTable)
.create
.addColumns(_.col1, _.col2)
.addIndexes(_.index1)
.renameColumn(_.col03, "col3")
val seed =
SqlMigration("insert into myTable (col1, col2) values (10, 20)")

val migration = init & seed

db.run(migration())*/
