import java.nio.ByteBuffer
import java.time.Instant
import java.util.concurrent.TimeUnit
import scala.io.StdIn


import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.Route
import akka.util.ByteString
import akka.actor._
import akka.http.scaladsl.model.headers.{HttpCookie, HttpCookiePair}
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor, Future}
import scala.concurrent.duration.Duration

import boopickle.Default._
import slick.jdbc.meta.MTable
import slick.jdbc.SQLiteProfile.api._


import rpc_server.{autowireServer, autowireServerUnauth}
import sharedType.{studentDBColType, studentDBColTypeId}
import sharedUtil._
import Api.logInfo


object rpc_server {

  import autowire._

  import scala.concurrent.ExecutionContext.Implicits.global //required!

  object autowireServerUnauth {

    private object autowireSerialization_
      extends autowire.Server[ByteBuffer, Pickler, Pickler] {
      override def read[R: Pickler](p: ByteBuffer) = Unpickle[R].fromBytes(p)

      override def write[R: Pickler](r: R) = Pickle.intoBytes(r)
    }

    def run(apiImpl: unauthApi,
            reqPathList: List[String],
            reqBodyBytes: ByteString): Future[ByteBuffer] = {
      autowireSerialization_.route[unauthApi](apiImpl)(
        autowire.Core.Request(
          reqPathList,
          Unpickle[Map[String, ByteBuffer]].fromBytes(reqBodyBytes.asByteBuffer))
      )
    }

  }

  object autowireServer {

    private object autowireSerialization_
      extends autowire.Server[ByteBuffer, Pickler, Pickler] {
      override def read[R: Pickler](p: ByteBuffer) = Unpickle[R].fromBytes(p)

      override def write[R: Pickler](r: R) = Pickle.intoBytes(r)
    }

    def run(apiImpl: Api,
            reqPathList: List[String],
            reqBodyBytes: ByteString): Future[ByteBuffer] = {
      autowireSerialization_.route[Api](apiImpl)(
        autowire.Core.Request(
          reqPathList,
          Unpickle[Map[String, ByteBuffer]].fromBytes(reqBodyBytes.asByteBuffer))
      )
    }

  }

}


object Main {
  val path_tablet = "/home/d/Desktop/syncthing/code/scala/scalaCross/"
  val path_pc = "/home/dubt/Desktop/syncthing/code/scala/scalaCross/"
  val pathInUse = path_tablet
  implicit val system: ActorSystem = ActorSystem("my-system")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  // needed for the future flatMap/onComplete in the end
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  implicit class FutureMonad[R](o: Future[R]) { // observable = [(a,Int)]
  type ob[S] = Future[S]

    //def ~~>(nxt:R=>Unit): Subscription =o.subscribe(nxt)//subscribe
    def ~>[S](f: R => S): ob[S] = o.map(f)

    //def *[S](p:ob[S]): ob[(R, S)] =o.zip(p)//product type
    def >>=[S](f: R => ob[S]): ob[S] = o.flatMap(f)

    def forceRun: R = Await.result(o, Duration.Inf) // Future t => t
  }



  val studentsQueryList = TableQuery[db.Student]

  def main(args: Array[String]): Unit = {
    dl.dl_main()
  }
  def server(): Unit ={
    println(sharedPure.sq(111))
    import sys.process._

    studentsQueryList.result
    val thisDir = System.getProperty("user.dir")
    println("user.dir::" + thisDir) //user.dir::/home/d/work/scalaCross
    val db = Database.forURL("jdbc:sqlite:sqlite.db",
      driver = "org.sqlite.JDBC",
      keepAliveConnection = true)
    //val db=Database.forConfig("h2mem1")
    val sqlinit = DBIO.seq(
      studentsQueryList.schema.create,
      studentsQueryList ++= Seq(
        (1, "name1", "cls1", "20180101", 3, 100, 200, "8789-898", "extrainfo"))
    )

    val dblist = db.run(MTable.getTables).forceRun.toList

    if (dblist.isEmpty) {
      db.run(sqlinit).forceRun
      println("no existing tables," + dblist.toString() + ",init finished")
    } else {
      println("existing tables:" + dblist.toString())
    }

    Future {
      Seq(1, 2, 3)
    }.foreach(print(_))
    db.run(studentsQueryList.result).map(println(_)).forceRun
    println("studentsQueryList.result ")


    val livereloadThread = stringToProcess("livereloadx " + pathInUse + "js/target/scala-2.12/scalajs-bundler/main/").run()

    //import scorex.crypto
    var logInfo_inMemory: Seq[Api.logInfo] = Seq()

    def token2logInfo(tk: String): Option[logInfo] = logInfo_inMemory.find(tk == _.tk)

    val mainHtml = path("") {
      get {
        import scalatags.Text.all._
        println("enter hello")
        complete(
          HttpEntity(
            ContentTypes.`text/html(UTF-8)`,
            html(
              head(
                title := "wheatedu",
                script(src := "http://localhost:35729/livereload.js"),
                link(rel := "stylesheet", href := "js/bulma.css")
              ),
              body(
                div(id := sharedStr.domDivContainer1),
                div(id := sharedStr.domDivContainer2),
                script(src := "js/target/scala-2.12/scalajs-bundler/main/js-fastopt-bundle.js")
              )
            ).toString()
          ))
      }
    }

    val atwUnauthRoute = path(sharedStr.autowirePathUnauth / Segments) { subpaths =>
      lg("enter autowirePathUnauth with cookie")
      entity(as[ByteString]) { entity_ : ByteString =>
        optionalCookie(sharedStr.cookie_get_name_path) { cke =>
          (cke match {
            case Some(ck) =>
              lg("have cookie:")
              onSuccess(autowireServerUnauth.run(
                new unauthApi {
                  override def getLogInfo(): Option[logInfo] = {
                    token2logInfo(ck.value)
                  }
                },
                subpaths,
                entity_
              ))
            case None =>
              lg("no cookie")
              onSuccess(autowireServerUnauth.run(
                new unauthApi {
                  override def getLogInfo(): Option[logInfo] = {
                    None
                  }
                },
                subpaths,
                entity_
              ))
          }) { b: ByteBuffer => complete(ByteString(b)) }

        }
      }
    }
    val cookieRoute = path(sharedStr.cookie_get_name_path) {
      post {
        parameters("nm", "pass") {
          case x@("a", "p") =>
            val tok = System.currentTimeMillis().toString
            logInfo_inMemory ++= Seq(logInfo(x._1,
              tok,
              Api.authLevel.admin)
            )
            setCookie(HttpCookie(sharedStr.cookie_get_name_path, tok)) {
              complete("log in as " + x._1)
            }
          case x@("t", "p") =>
            val tok = System.currentTimeMillis().toString
            logInfo_inMemory ++= Seq(logInfo(x._1,
              tok,
              Api.authLevel.teacher)
            )
            setCookie(HttpCookie(sharedStr.cookie_get_name_path, tok)) {
              complete("log in as " + x._1)
            }

          case x =>
            complete("auth failed: " + x.toString())
        } ~
          parameters("del") { _ =>
            lg("@ deleteCookie")
            deleteCookie(sharedStr.cookie_get_name_path) {
              complete("cookie deleted")
            }
          } ~
          complete("cookie_get_name_path params error")
      }
    }

    val atwAuthedPath: Route =
      path(sharedStr.autowirePath / Segments) { subpaths =>
        println(s"enter api/ (autowire)@ ${Instant.now().toString}")
        val c = complete("auth autowirePath")
        optionalCookie(sharedStr.cookie_get_name_path) { cko: Option[HttpCookiePair] =>
          (cko, cko.flatMap(ck => logInfo_inMemory.find(x => x.tk == ck.value))) match {
            case (Some(tk), _) =>
              lg("@ cookie: " + cko.toString)
              entity(as[ByteString]) { entity_ : ByteString =>
                onSuccess(
                  autowireServer.run(
                    new Api {
                      //works only on values other than expressions! inline also not working
                      //change to generic thing breaks,may relate to dependency injection!!!

                      override def userInfo(): String = "username"

                      override def getStudents(): Seq[studentDBColType] = {
                        db.run(studentsQueryList.result).forceRun
                      }

                      override def updateStudents(x: studentDBColType): Unit = {
                        db.run(
                          studentsQueryList
                            .filter(col => col.id === x._1)
                            .update(x))
                          .forceRun
                      }


                      override def addStudent(s: sharedType.studentDBColType): Unit = {
                        db.run(studentsQueryList += s).forceRun
                      }

                      override def delStudent(s: studentDBColTypeId): Boolean = {
                        token2logInfo(tk.value) match {
                          case Some(i)=> i.pr match {
                            case Api.authLevel.admin=>
                              db.run(studentsQueryList.filter(x => x.id === s).delete).forceRun
                              true
                            case Api.authLevel.teacher=>false
                          }
                          case _=>false
                        }

                      }
                    },
                    subpaths,
                    entity_
                  )) { b: ByteBuffer => complete(ByteString(b)) }

              }
            case _ =>
              lg("@ cookie yes  but none userinfo in mem!!: " + cko.toString)
              complete("error")

          }
        }
        // https://doc.akka.io/docs/akka-http/current/scala/http/routing-dsl/directives/marshalling-directives/entity.html
      }

    val getfileRoute = pathPrefix("") { //for resources
      getFromDirectory(pathInUse)
    }

    /*
        val sheduledWorks = util.intervaledWork(() => { // causing termination bug!
          logInfo_inMemory = Seq()
        }, 30)
    */



    val bindingFuture =
      Http().bindAndHandle(
        mainHtml ~
          atwAuthedPath ~
          atwUnauthRoute ~
          cookieRoute ~
          getfileRoute,
        "0.0.0.0", 8081)

    println(s"Server online at http://localhost:8081/\nPress RETURN to stop...")

    StdIn.readLine() // let it run until user presses return

    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete { _ =>
      lg("terminating")
      db.close()
      livereloadThread.destroy()
      //sheduledWorks.cancel() // cause java.util.concurrent.RejectedExecutionException: Task java.util.concurrent.ExecutorCompletionService
      system.terminate()
      lg("termination finished")
    }
    lg("main exited")

  }

}

object util {
  def intervaledWork(x: Runnable, interval: Int): Cancellable = {
    val actorSystem = ActorSystem()
    val scheduler = actorSystem.scheduler
    implicit val executor = actorSystem.dispatcher

    scheduler.schedule(initialDelay = Duration(interval, TimeUnit.HOURS),
      interval = Duration(interval, TimeUnit.HOURS),
      runnable = x)
  }
}

