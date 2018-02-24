
// misc


import com.raquo.laminar.streams.EventBus
import com.raquo.xstream.{Listener, XStream}
import com.raquo.xstream.rawextras.RawDelay
import commons._
import jsApi._

//rpc
import autowire._
import rpc.{autowireClient, autowireClientUnauth}


import scala.concurrent.Future
//ui
import com.raquo.domtypes.jsdom.defs.events.TypedTargetEvent
import com.raquo.laminar.nodes.ReactiveElement


//js api
import org.scalajs.dom
import org.scalajs.dom.html.{Div, Element, Input}
import org.scalajs.dom.raw.HTMLInputElement
import org.scalajs.dom.{MouseEvent, XMLHttpRequest}

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue // require by autowire for future


object rpc {

  import java.nio.ByteBuffer
  import boopickle.Default._
  import boopickle.Default.{Pickle, Pickler, Unpickle}
  import scala.scalajs.js.typedarray.{ArrayBuffer, TypedArrayBuffer}

  object autowireClientUnauth extends autowire.Client[ByteBuffer, Pickler, Pickler] {

    override def doCall(reqParam: Request): Future[ByteBuffer] = {
      dom.ext.Ajax
        .post(
          url = sharedStr.autowirePathUnauth + "/" + reqParam.path.mkString("/"),
          data = write(reqParam.args),
          responseType = "arraybuffer",
          headers = Map(
            ("Content-Type", "application/octet-stream"),
            ("Authorization", "Basic " + toBase64("a:p"))
          )
        ).map(r => TypedArrayBuffer.wrap(r.response.asInstanceOf[ArrayBuffer]))
    }

    override def read[Result: Pickler](p: ByteBuffer) = Unpickle[Result].fromBytes(p)

    override def write[Result: Pickler](r: Result) = Pickle.intoBytes(r)
  }

  object autowireClient extends autowire.Client[ByteBuffer, Pickler, Pickler] {


    override def doCall(req: Request): Future[ByteBuffer] = {
      dom.ext.Ajax
        .post(
          url = sharedStr.autowirePath + "/" + req.path.mkString("/"),
          data = Pickle.intoBytes(req.args),
          responseType = "arraybuffer",
          headers = Map(("Content-Type", "application/octet-stream"))
        )
        .map(r => TypedArrayBuffer.wrap(r.response.asInstanceOf[ArrayBuffer]))
    }

    override def read[Result: Pickler](p: ByteBuffer) = Unpickle[Result].fromBytes(p)

    override def write[Result: Pickler](r: Result) = Pickle.intoBytes(r)
  }

}


object wheatedu_lam {

  object searchByT extends Enumeration {
    val byName, byClass = Value
    type enum = Int
  }

  import boopickle.Default._ // req by autowire
  import bulma_lam._
  import com.raquo.laminar.bundle._
  import lam._
  import org.scalajs.dom
  import sharedUtil._
  import upickle.default._ //implicit value for evidence parameter of type boopickle.Default.Pickler[String]

  val kvdb_token = "token"


  def inputs2stuCol(inputsval: Seq[String]) = (inputsval(0).toInt, //id
    inputsval(1), //name
    inputsval(2), //classname
    inputsval(3), //participated
    inputsval(4).toInt,
    inputsval(5).toInt,
    inputsval(6).toInt,
    inputsval(7),
    inputsval(8))

  def getUserInfo(): Option[Api.logInfo] = {
    val rd = localStorage.getItem(kvdb_token)
    if (rd != null) Some(read[Api.logInfo](rd)) else None
  }

  def setUserInfo(x: Api.logInfo): Unit = {
    localStorage.setItem(kvdb_token, write[Api.logInfo](x))
  }

  def clearUserInfo(): Unit = {
    localStorage.removeItem(kvdb_token)
  }

  object dbaction {

    sealed trait dbaction

    case object delete extends dbaction

    case object added extends dbaction

  }


  def start(): Unit = {
    println("wheatedu started!!!")
    //XStream.fromSeq(1 to 5).delay(500).subscribe(Listener(onNext = { _: Int => print("hi") })) // not working

    var inputsVNsearch: Seq[ReactiveElement[Element]] = null
    var iptsg: Seq[ReactiveElement[Input]] = null

    //new pipes
    val modifyingCol: pipe[(Int, Boolean)] = newPipe()
    val clickedLogin = newPipe[Boolean]()
    val saveStuColPipe = newPipe[sharedType.studentDBColType]()
    val deletePipe = newPipe[sharedType.studentDBColTypeId]()
    val newStusPipe = newPipe[sharedType.studentDBColType]()
    val loginNamePasswd = newPipe[(String, String)]()
    val searchByPipe = newPipe[(String, searchByT.Value)]()
    val logOutPipe = newPipe[Unit]()
    val row2modifyglobal = newPipe[Option[sharedType.studentDBColType]]()

    //pipe wires

    def rpc() = autowireClient[Api]

    def rpcUnauthGetUserInfo() = future2xstream(autowireClientUnauth[unauthApi].getLogInfo().call())


    def rpcGetStusPipe(): Out[Seq[sharedType.studentDBColType]] =
      future2xstream(rpc().getStudents().call()) //must be def!! create new pipe every time from future!


    def rpcDelStusOut(stu: sharedType.studentDBColTypeId): Out[Boolean] =
      future2xstream(rpc().delStudent(stu).call())

    def rpcUpdateStusOut(stu: sharedType.studentDBColType): Out[Boolean] =
      future2xstream(rpc().updateStudents(stu).call()).mapTo(true)

    val notif_newStusSaved: Out[Boolean] = newStusPipe._2 >>= { stu =>
      lg("notif_newStusSaved")
      future2xstream(rpc().addStudent(stu).call()).mapTo(true)
    }

    def rpcDelCookie(): Out[XMLHttpRequest] = future2xstream(dom.ext.Ajax
      .post(
        url = sharedStr.cookie_get_name_path + "?del=y"
      ))

    def rpcGetLogInfo(): Out[String] = future2xstream(rpc().userInfo().call())

    //derived pipe
    val loginfoFromDialogRpc: Out[Option[String]] =
      loginNamePasswd._2 >>= { x: (String, String) =>
        println(x)
        future2xstream(dom.ext.Ajax
          .post(
            url = sharedStr.cookie_get_name_path + s"?nm=${x._1}&pass=${x._2}"
          )) > { x =>
          lg(x.responseText)
          if (x.responseText.contains("log in")) Some(x.responseText) else None
        }

      }

    val notif_deleted: Out[Boolean] = deletePipe._2 >>= (i => rpcDelStusOut(i))

    val newOrSavedpipe = (cst(unit) ++
      notif_newStusSaved.mapTo(unit) ++
      notif_deleted.mapTo(unit) ++
      saveStuColPipe._2.mapTo(unit)
      ) >>= {
      _ => rpcGetStusPipe()
    }

    val filteredRpcStus: Out[Seq[sharedType.studentDBColType]] = (cst("", searchByT.byName) ++
      searchByPipe._2 ++
      newOrSavedpipe.mapTo(
        ("", searchByT.byClass))
      ) >>= ((s: (String, searchByT.Value)) =>
      rpcGetStusPipe().map { xs: Seq[sharedType.studentDBColType] =>
        xs.filter {
          case (_id, nm, cls, participated, rc, sd, ed, ph, ext) =>
            s._2 match {
              case searchByT.byName => if (s._1 == "") true else nm.contains(s._1)
              case searchByT.byClass => if (s._1 == "") true else cls.contains(s._1)
            }
        }
      })


    val tableChild = child <-- (
      (loginfoFromDialogRpc ++
        logOutPipe._2.mapTo(None) ++
        (cst(unit) >>= (_ => rpcUnauthGetUserInfo().map(_.map(_.username))))
        ) > {
        case None => div() // not logged in
        case Some(_) => table(
          className := "table is-bordered",
          tr(
            td(colSpan := 4,
              ipt(placeholder := "按名字搜索",
                (onInput() map
                  ((x: TypedTargetEvent[Element]) =>
                    (x.target
                      .asInstanceOf[HTMLInputElement]
                      .value
                      , searchByT.byName)
                    )
                  ) --> searchByPipe._1)),
            td(colSpan := 5,
              ipt(placeholder := "按班级搜索",
                (onInput() map
                  ((x: TypedTargetEvent[Element]) =>
                    (x.target
                      .asInstanceOf[HTMLInputElement]
                      .value
                      , searchByT.byClass)
                    )) --> searchByPipe._1))
          ),
          tr(
            Seq("id(_)",
              "name",
              "class（班级）",
              "已上课程日期",
              "remain（剩余课程）",
              "start date（开始日期）",
              "end date（截止日期）",
              "phone",
              "extra info（其他信息）").map(v => td(v)): _*),
          children <-- (cst(unit) > { _ =>
            val inputsVNnew = 0 to 8 map (x =>
              if (x == 0) ipt(value := "0", disabled := true) else ipt())
            Seq(
              tr(inputsVNnew.map(x => td(x)): _*),
              tr(td(
                colSpan := 7,
                btn("new", (onClick() map { _: MouseEvent => //new students
                  val inputsval = inputsVNnew.map(x =>
                    x.ref.asInstanceOf[HTMLInputElement].value)
                  lg(inputsval)
                  inputs2stuCol(inputsval)
                }) --> newStusPipe._1
                )
              )
              )
            )
          }),
          children <--
            (filteredRpcStus > { stus =>
              stus.map { case (cid, nm, clss, participated, rc, sd, ed, ph, ext) =>
                tr(children <-- cst(Seq(false)).map { _ => //edit or view column
                  Seq(cid, nm, clss, participated, rc, sd, ed, ph, ext) map (vv =>
                    td(a(vv.toString)))
                  // self recursive pipes not working yet
                },
                  (onClick() mapTo Some((cid, nm, clss, participated, rc, sd, ed, ph, ext))) --> row2modifyglobal._1
                )
              }
            })
        )
      }) // main table


    val editDialogChild = child <-- (row2modifyglobal._2 ++
      saveStuColPipe._2.>>=(x => future2xstream(rpc().updateStudents(x).call())).mapTo(None) ++
      notif_deleted.filter(x => x).mapTo(None)
      ).map { //edit dialog
      case Some((id, nm, cls, participated, rc, sd, ed, ph, ext)) =>
        val editDialogInputs = Seq(id, nm, cls, participated, rc, sd, ed, ph, ext).zipWithIndex.map { case (vv, i) =>
          ipt(value := vv.toString(), disabled := (if (i == 0) true else false))
        }
        val wrapedInputs = editDialogInputs map (x => tcol(x))
        modal(
          tb(trow(wrapedInputs: _*)),
          btngrp(btn(
            "save",
            (onClick() map { _ =>
              inputs2stuCol(editDialogInputs.map(x =>
                x.ref.asInstanceOf[HTMLInputElement].value)
              )
            }) --> saveStuColPipe._1
          ),
            button("课时-1", className := "button is-info", (onClick() map { _ =>
              val dat = yyyymmddhh()
              lg(dat)
              val tps = inputs2stuCol(editDialogInputs.map(x =>
                x.ref.asInstanceOf[HTMLInputElement].value))
              tps.copy(
                _5 = tps._5 - 1,
                _4 = tps._4 + "," + dat
              )
            }) --> saveStuColPipe._1
            ),
            button("cancel", className := "button", (onClick() mapTo (None)) --> row2modifyglobal._1),
            button("delete", className := "button is-danger", (onClick() mapTo (id)) --> deletePipe._1)
          ),
          child <-- notif_deleted.filter(!_).mapTo(row("No permission!"))
        )
      case None => empty()
    }

    val notifChild = child <-- (
      notif_newStusSaved.mapTo("added ! ") ++
        notif_deleted.map(if (_) "deleted ! " else "no permission !") ++
        loginfoFromDialogRpc.map(_.toString)
      ).map(x => div(x)
    )

    //log in  NoSuchElementException: value qual$9
    val logInfoChild = whitebox(
      children <-- (
        (
          (
            (
              cst(()) ++
                logOutPipe._2 ++
                loginfoFromDialogRpc.mapTo(unit)) >>= (_ => rpcUnauthGetUserInfo())) ++
            (logOutPipe._2 >>= (_ => rpcDelCookie())).mapTo(None)) > {
          case Some(x: Api.logInfo) => Seq(
            div(
              h3(s"Logged in as : ${x.username} , permission : ${x.pr.toString} ", className := "title"),
              btn("Logout", (onClick() mapTo unit) --> logOutPipe._1) // bug here ,mapTo (()) vs () causing no such element bug!!
            )
          )
          case None => Seq(
            btn("login", (onClick() mapTo true) --> clickedLogin._1),
            h3("Not logged in", className := "title")
          )
        })
    )

    val tp = new EventBus[Unit]()

    lam_render(
      dom.document.getElementById(sharedStr.domDivContainer1), rootContainer(
        div(button("click", (onClick() mapTo (unit)) --> tp),
          child <-- (tp.$ mapTo div("clicked1", child <-- (tp.$ mapTo (div("clicked2")))))

        ),
        notifChild,
        tableChild,
        editDialogChild,
        logInfoChild,
        loginDialog(clickedLogin, loginNamePasswd._1, loginfoFromDialogRpc)
      ) // root
    )
    lg("laminar ui loaded")
  }

  //misc
  def loginDialog(show: pipe[Boolean],
                  user_name_pipe: In[(String, String)],
                  logInfo: Out[Option[String]]) = {
    val ipts = 0 to 1 map (_ => ipt())
    child <-- ((show._2 ++ (logInfo.filter(_ != None) > { // if none,do not propageate loginfo to prevent concurrent nested ui update.works!
      case Some(_) => false;
      case None => true
    })) > (b =>
      if (b)
        modal(
          h5("login"),
          ipts(0),
          ipts(1),
          btngrp(
            btn("ok", (onClick() map { _: MouseEvent =>
              val iptsv =
                ipts.map(x => x.ref.asInstanceOf[HTMLInputElement].value)
              (iptsv(0), iptsv(1))
            }) --> user_name_pipe),
            btn("cancel", (onClick() map { _: MouseEvent =>
              false
            }) --> show._1)
          ),
          child <-- (logInfo > { tk =>
            lg("log in modal") ///printed
            tk match {
              case Some(x) =>
                lg("log ok")
                h5(tk.toString)
              case None =>
                lg("log error")
                h5("登录错误")
            }
          })
        )
      else div()))
  }
}

object Main {

  def main(args: Array[String]): Unit = {
    wheatedu_lam.start()
  }

}
