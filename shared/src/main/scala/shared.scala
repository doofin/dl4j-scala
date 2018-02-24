object sharedStr{
  val autowirePath="api"
  val autowirePathUnauth="apiUn"
  val domDivContainer1="div1"
  val domDivContainer2="div2"
  val cookie_get_name_path="cookie_get"
  val cookie_delPath="cookie_del"
}

object sharedType{
  type studentDBColTypeId=Int
  type studentDBColType=(studentDBColTypeId,
    String,//name
    String,//class name
    String,//participated timestamp
    Int, Int, Int, String,String)
  case class name_password(nm:String,passwd:String)
}

object sharedPure {
  def sq(x: Int): Int = x * x
  implicit class Bool(o:Boolean){
  }
}

object sharedUtil {
  import upickle.default._
  def lg(x:Any)=println(x)
  }




import Api.logInfo
import sharedType.{studentDBColType, studentDBColTypeId}

trait Api {
  import Api.{logInfo, token}
  def userInfo(): String
  def getStudents():Seq[studentDBColType]
  def updateStudents(s:studentDBColType):Unit
  def addStudent(s:studentDBColType):Unit
  def delStudent(s:studentDBColTypeId):Boolean
  //def modifyStudent(s:studentCol)(implicit t: token):Unit //need to verify token ,not working in autowire ,implicits not supported
  //def newStudents(s:studentDBColType):Unit
}

object Api {
  object authLevel{
    sealed trait permission
    case object teacher extends permission
    case object admin extends permission
  }
  type token=String
  type studentCol=studentDBColType
  case class logInfo(username:String,tk:token,pr: authLevel.permission)
}

trait unauthApi{
  def getLogInfo():Option[logInfo]
}