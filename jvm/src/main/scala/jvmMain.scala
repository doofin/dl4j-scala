import java.nio.ByteBuffer


import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.util.ByteString
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor, Future}
import scala.concurrent.duration.Duration

import boopickle.Default._


import sharedUtil._
import Api.logInfo




object Main {
  implicit val system: ActorSystem = ActorSystem("my-system")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  // needed for the future flatMap/onComplete in the end
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  def main(args: Array[String]): Unit = {
    dl.dl_main()
  }



  implicit class FutureMonad[R](o: Future[R]) { // observable = [(a,Int)]
  type ob[S] = Future[S]

    //def ~~>(nxt:R=>Unit): Subscription =o.subscribe(nxt)//subscribe
    def ~>[S](f: R => S): ob[S] = o.map(f)

    //def *[S](p:ob[S]): ob[(R, S)] =o.zip(p)//product type
    def >>=[S](f: R => ob[S]): ob[S] = o.flatMap(f)

    def forceRun: R = Await.result(o, Duration.Inf) // Future t => t
  }


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

    object autowireServer { // for future visualization usage

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



}
