import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContextExecutor, Future}




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
}
