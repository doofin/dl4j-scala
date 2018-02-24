import com.raquo.domtypes.generic.Modifier
import com.raquo.domtypes.generic.keys.EventProp
import com.raquo.laminar.emitter.{EventPropEmitter, EventPropOps, EventPropTransformation}
import com.raquo.laminar.nodes.{ReactiveChildNode, ReactiveElement, ReactiveRoot, ReactiveText}
import com.raquo.xstream
import com.raquo.xstream.{MetaStream, Producer}

import scala.scalajs.js
import org.scalajs.dom
import org.scalajs.dom.html.Div
import org.scalajs.dom.raw.{HTMLInputElement, Storage}
import org.scalajs.dom.{Element, Event, MouseEvent, Text}

import scala.concurrent.Future

//import scala.Predef.{ArrowAssoc => _ ,_}

object commons {
  val unit: Unit = ()
  //def constf[T](x:T) =
}

object jsApi {
  import scala.scalajs.js.{Date, Dynamic}

  val localStorage: Storage = dom.window.localStorage
  val jo: Dynamic.literal.type = js.Dynamic.literal
  val ja: js.Array.type = js.Array
  def toBase64(s:String): String = js.Dynamic.global.btoa(s).asInstanceOf[String]
  def yyyymmddhh(): String = {
    val d=new Date()
    s"${d.getFullYear()}-${(d.getMonth()+1)}-${d.getDate()}@${d.getHours()}"
  }
}

object lam {
  /**
    * [[com.raquo.laminar.emitter.EventPropEmitter]] with Modifier[El]
    * [[com.raquo.domtypes.generic.Modifier]] in elements(Mod*) vararg
    **/

  import com.raquo.laminar.bundle._
  import com.raquo.laminar.nodes.ReactiveNode
  import com.raquo.laminar.streams.EventBus
  import com.raquo.xstream.{MemoryStream, XStream}
  import org.scalajs.dom
  import com.raquo.laminar.render
  import com.raquo.xstream.Listener

  type timeList[X] = XStream[X]
  type Out[X] = XStream[X]
  type In[X] = EventBus[X]

  type pipe[T] = (EventBus[T], XStream[T]) // sink,source
  type pipe2[I, O] = (EventBus[I], XStream[O]) // sink,source

  def embedRaw(f: Element => Unit): ReactiveElement[Div] = {
    val d: ReactiveElement[Div] = div()
    f(d.ref)
    d
  }

  def newPipe[T](): pipe[T] = {
    val x = new EventBus[T]
    (x, x.$)
  }

  def newPipeVar[T](): pipe[T] = {
    var x = new EventBus[T]
    (x, x.$)
  }

  def lam_render(container: dom.Element, rootNode: ReactiveChildNode[dom.Element]): ReactiveRoot = render(container, rootNode)

  def f2Out[T](pd: ((T => Unit) => Unit, () => Unit)): Out[T] = {
    XStream.create(Producer({
      l =>
        pd._1 { v: T => l.next(v)
        }
    },
      pd._2))
  }

  def newOut[T](hf: (T => Unit) => Unit, onStop: () => Unit = () => {}): Out[T] = {
    XStream.create(Producer({
      l =>
        hf { v: T => l.next(v)
        }
    },
      onStop))
  }

  def join[T](x: XStream[XStream[T]]): XStream[T] = new MetaStream[T](x).flatten

  def cst[T](s: Seq[T]): XStream[T] = XStream.fromSeq(s)

  def cst[T](s: T): XStream[T] = XStream.of(s)

  def eventPropEnriched[Ev <: dom.Event, X](eventProp: EventProp[Ev], f: Ev => X): EventPropOps[Ev] = {
    toEventPropOps(eventProp)
  }

  import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

  def future2xstream[T](x: Future[T]): XStream[T] = XStream.create[T](Producer[T]({ ls: Listener[T] => {
    x.foreach(y => ls.next(y))
  }
  }, () => {}))

  implicit class xstreamOps[I](x: Out[I]) {
    def ++(y: Out[I]): Out[I] = XStream.merge(x, y)

    def *[R](y: Out[R]): Out[(I, R)] = XStream.combine[I, R](x, y)

    def >[R](f: I => R): Out[R] = x.map(f)
    //def >[R](f: I => R): XStream[R] = x.map(f) //overloadding not working

    def ~~>(f: I => Unit): xstream.Subscription[I] = x.subscribe[I](Listener[I](onNext = f))

    def >>=[O](f: I => Out[O]): Out[O] = x.map(f).flatten
  }

  implicit class pipeOps[I](x: pipe[I]) {
    def ++(y: pipe[I]): XStream[I] = XStream.merge(x._2, y._2)

    def *[R](y: pipe[R]): XStream[(I, R)] = XStream.combine[I, R](x._2, y._2)

    def >[R](f: I => R): XStream[R] = x._2.map(f)

    def ~~>(f: I => Unit): xstream.Subscription[I] = x._2.subscribe[I](Listener[I](onNext = f))

    def >>=[O](f: I => timeList[O]): pipe2[I, O] = (x._1, x._2 >>= f)

  }

  implicit class evtemitters[Ev <: dom.Event, V, El <: ReactiveElement[dom.Element]]
  (e: EventPropTransformation[Ev, V, El]) {
    def >[V2](f: V => V2): EventPropTransformation[Ev, V2, El] = e.map(f) //fmap
    def >[V2](v: V2): EventPropTransformation[Ev, V2, El] = e.mapTo(v) //fmap

  }

  implicit class srcs[Ev <: dom.Event](x: EventPropOps[Ev]) {
    //def -->[R](writeBus: WriteBus[R]) = writeBus
    //def --->[R](writeBus: WriteBus[R])=toEventPropOps(x)-->writeBus
    def --->[El <: ReactiveElement[dom.Element], BusEv >: Ev](writeBus: pipe[BusEv]): EventPropEmitter[Ev, Ev, BusEv, El]
    = x --> writeBus._1
  }



  def tst[T](e: dom.Element, atw: Future[T]): Unit = {
    val p2: XStream[Int] = XStream.create(new Producer[Int] {
      override protected def start(listener: Listener[Int]): Unit = {
        e.addEventListener("click", { e: Event =>
          listener.next(100)
        })
      }

      override protected def stop(): Unit = {}
    })
    val p3 = newOut[Int]({ f: (Int => Unit) =>
      e.addEventListener("click", { e: Event =>
        f(10000)
      })
    })
    p3 ~~> (println(_: Int))
  }

}

object bulma_lam {

  import com.raquo.laminar.bundle._
  import org.scalajs.dom.html._
  import org.scalajs.dom.html.{Div, Element, Input}

  //className:="input is-primary"
  val b: ReactiveElement[Button] = button()
  val i: ReactiveElement[Input] = input()

  def rootContainer(modifier: Modifier[ReactiveElement[Div]]*) =
    div(modifier.+:(className := "container"): _*)

  def modal(modifier: Modifier[ReactiveElement[Element]]*): ReactiveElement[Div] =
    div(className := "modal is-active", //modal
      div(className := "modal-background"),
      div(
        className := "modal-content",
        whitebox(modifier: _*)
      ),
    )

  def whitebox(modifier: Modifier[ReactiveElement[Element]]*) = div(
    modifier.+:(className := "box"): _*
  )

  def btngrp(modifier: Modifier[ReactiveElement[Element]]*) = div(
    modifier.+:(className := "buttons"): _*
  )

  def navbar(modifier: Modifier[ReactiveElement[Element]]*) = nav(
    modifier.+:(className := "navbar is-primary").++(Seq()): _*
  )

  def btn(modifier: Modifier[ReactiveElement[Button]]*): ReactiveElement[Button] =
    button(modifier.+:(className := "button is-primary"): _*)

  def row(modifier: Modifier[ReactiveElement[Div]]*) =
    div(modifier.+:(className := "column"): _*)

  def empty() =
    div()

  def tb(modifier: Modifier[ReactiveElement[Table]]*) =
    table(modifier.+:(className := "table is-bordered"): _*)

  def trow(modifier: Modifier[ReactiveElement[TableRow]]*) = tr(modifier: _*)

  def tcol(modifier: Modifier[ReactiveElement[TableCell]]*) = td(modifier: _*)

  def col(modifier: Modifier[ReactiveElement[Div]]*) =
    div(modifier.+:(className := "columns"): _*)

  def ipt(modifier: Modifier[ReactiveElement[Input]]*) =
    input(modifier.+:(className := "input is-primary"): _*)
}


