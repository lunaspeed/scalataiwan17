import Implicits._
import Example1._
import java.io.InputStream

import play.api.libs.iteratee.{Enumeratee, Enumerator, Iteratee}


/**
  * Add Producer
  * Add Transformer
  *
  */
object Attempt4 {
  //we need the ability for Producer to let the Consumer know some information about the Producer
  sealed trait Input[+A] {

    def map[U](f: A => U): Input[U] = this match {
      case InputVal(e) => InputVal(f(e))
      case InputEmpty => InputEmpty
      case InputEnd => InputEnd
    }
  }

  //we have a value
  case class InputVal[+A](v: A) extends Input[A]
  //we dont have a value, but something is coming in
  case object InputEmpty extends Input[Nothing]
  //we have ended, nothing more
  case object InputEnd extends Input[Nothing]


  //for the same thing we need to give Consumer the ability to terminate or report state of the consuming

  //we give the
  Iteratee
  sealed trait Consumer[I, +A] {
    //runs the consumer
    def run: A = fold({
      case ConsumerFinish(a, _) => a
      case ConsumerContinue(k) => k(InputEnd).fold({
        case ConsumerFinish(a1, _) => a1
          //probably need something like `recover` or `recoverWith`
        case ConsumerContinue(_) => sys.error("diverging after InputEnd")
        case ConsumerError(msg, _) => sys.error(msg)
      })
      case ConsumerError(msg, _) => sys.error(msg)
    })

    def fold[B](folder: Consumer[I, A] => B): B = folder(this)


    def flatMap[B](f: A => Consumer[I, B]): Consumer[I, B] = this match {
      case ConsumerFinish(x, e) => f(x) match {
        case ConsumerFinish(y, _) => ConsumerFinish(y, e)
        case ConsumerContinue(k) => k(e)
        case ce => ce
      }
      case ConsumerContinue(k) => ConsumerContinue(e => k(e) flatMap f)
      case ce: ConsumerError[I] => ce
    }

    def map[B](f: A => B): Consumer[I, B] = this.flatMap(a => ConsumerFinish(f(a), InputEmpty))
  }
  //Consumer in the state of finish
  case class ConsumerFinish[+A, I](a: A, remain: Input[I]) extends Consumer[I, A]
  //Consumer will continue to consume
  case class ConsumerContinue[I, +A](k: Input[I] => Consumer[I, A]) extends Consumer[I, A]
  //Consumer in the state of error
  case class ConsumerError[I](msg: String, input: Input[I]) extends Consumer[I, Nothing]

  //create our own sum Consumer
  def sumConsumer[A](M: Monoid[A]): Consumer[A, A] = {

    def step(v: A)(i: Input[A]): Consumer[A, A] = i match {
        case InputVal(a) => ConsumerContinue(step(M.add(v, a)))
        case InputEmpty => ConsumerContinue(step(v))
        case InputEnd => ConsumerFinish(v, i)
      }

    ConsumerContinue(step(M.zero))
  }


  def sumList(l: List[Int]): Int = {
    val consumer = sumConsumer(IntAbstraction2)
    def run(ll: List[Int], c: Consumer[Int, Int]): Consumer[Int, Int] = c match {
      case ConsumerContinue(k) => ll match {
        case h :: tail => run(tail, k(InputVal(h)))
        case Nil => run(Nil, k(InputEnd))
      }
      case _ => c
    }

    run(l, consumer) match {
      case ConsumerFinish(a, _) => a
      case _ => throw new RuntimeException("oh yeah we throw")
    }
  }

  def dropConsumer[I](n: Int): Consumer[I, Unit] = {

    def step(n: Int)(i: Input[I]): Consumer[I, Unit] = i match {
      case InputVal(_) if n > 0 => ConsumerContinue(step(n -1))
      case iv @ InputVal(_) => ConsumerFinish((), iv)
      case InputEmpty => ConsumerContinue(step(n))
      case InputEnd => ConsumerFinish((), InputEnd)
    }
    ConsumerContinue(step(n))
  }

  //now we have two consumers
  //Whats important in FP? COMPOSE!!

  //actually we can turn Consumer into Monad
  //add map/flatMap

  //!!! Consumer composes
  def dropAndSum(drop: Int)(M: Monoid[Int]): Consumer[Int, Int] = for {
    _ <- dropConsumer(drop)
    s <- sumConsumer(M)
  } yield s

  //apply the consumer
  def dropAndSumList(l: List[Int], drop: Int): Int = {
    val consumer = dropAndSum(drop)(IntAbstraction2)
    def run(ll: List[Int], c: Consumer[Int, Int]): Consumer[Int, Int] = c match {
      case ConsumerContinue(k) => ll match {
        case h :: tail => run(tail, k(InputVal(h)))
        case Nil => run(Nil, k(InputEnd))
      }
      case a => a
    }

    run(l, consumer).run
  }

  //now we have good Consumer, its seems like the way we apply it to a List is always the same
  //we can generalize it to work on any List of any type

  def applyList[A, B](l: List[A])(consumer: Consumer[A, B]): B = {
    def run(ll: List[A], c: Consumer[A, B]): Consumer[A, B] = c match {
      case ConsumerContinue(k) => ll match {
        case h :: tail => run(tail, k(InputVal(h)))
        case Nil => run(Nil, k(InputEnd))
      }
      case a => a
    }

    run(l, consumer).run
  }

  //and create a consumer from general fold function
  def foldConsumer[A, B](init: B)(f: (B, A) => B): Consumer[A, B] = {
    def step(r: B)(i: Input[A]): Consumer[A, B] = i match {
      case InputVal(a)  => ConsumerContinue(step(f(r, a)))
      case InputEmpty => ConsumerContinue(step(r))
      case InputEnd => ConsumerFinish(r, InputEnd)
    }
    ConsumerContinue(step(init))
  }


  //Generalize to List is simply isn't enough
  //what about Stream?
  //let's do the InputStream first
  def applyStream[B](is: InputStream)(consumer: Consumer[Array[Byte], B]): B = {
    val buffer = new Array[Byte](1024)
    def run(c: Consumer[Array[Byte], B]): Consumer[Array[Byte], B] = c match {
      case ConsumerContinue(k) => run(k(InputVal(is.read(buffer) match {
        case -1 => Array.emptyByteArray
        case _ => buffer
      })))
      case a =>
        //finish reading the Stream
        is.close
        a
    }

    run(consumer).run
  }

  //We can abstract these functions into something called Producer
  Enumerator
  trait Producer[E] {

    parent =>
    //the run method will be responsible for iterating/generating input from source
    def run[B](consumer: Consumer[E, B]): B


    def through[To](trans: Transformer[E, To]): Producer[To] = new Producer[To] {

      def run[B](consumer: Consumer[To, B]): B =
        parent.run(trans.apply(consumer))

    }
  }

  class ListProducer[E](l: List[E]) extends Producer[E] {


    def run[B](consumer: Consumer[E, B]): B = run1(l)(consumer)

    def run1[B](ll:List[E])(consumer: Consumer[E, B]): B = consumer match {
      case ConsumerContinue(k) => ll match {
        case h :: tail => run1(tail)(k(InputVal(h)))
        case Nil => run1(Nil)(k(InputEnd))
      }
      case c => c.run
    }
  }


  //what if we have a Consumer[String, String] but a Producer[Int]?
  //we need a map on Producer
  //how about a map on Consumer? the map on Consumer is right-biased and it works on the result not the input.
  //Can we have something work for both?

  //lets call it Transformer
  Enumeratee
  trait Transformer[From, To] {
    val transform: From => To

    def apply[B](i: Consumer[To, B]): Consumer[From, B] = i match {
      case ConsumerContinue(k) => ConsumerContinue(fi => apply(k(fi.map(transform))))
      case ConsumerFinish(a, _) => ConsumerFinish(a, InputEmpty)
      case ConsumerError(e, _) => ConsumerError(e, InputEmpty)
    }
  }


  object StringIntTrans extends Transformer[String, Int] {
    val transform = (_: String).toInt
  }


}

