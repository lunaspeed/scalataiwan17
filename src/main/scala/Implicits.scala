
import Example1.Algebra
import play.api.libs.iteratee.Enumeratee

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

/**
  * Created by Lunaspeed on 26/05/2017.
  */
object Implicits {

//  implicit def toEnumerateeMap[F, T](f: F => T): Enumeratee[F, T] = Enumeratee map f

//  implicit def getFuture[B](f: Future[B]): B = Await.result(f, 300 seconds)

  implicit object IntAbstraction2 extends Algebra[Int] {
    def add(a: Int, b: Int): Int = a + b
    def zero: Int = 0
  }

  implicit object StringAbstraction2 extends Algebra[String] {
    def add(a: String, b: String): String = a + b
    def zero: String = ""
  }

  implicit object DoubleAbstraction2 extends Algebra[Double] {
    def add(a: Double, b: Double): Double = a + b
    def zero: Double = 0D
  }

  implicit object BigDecimalAbstraction2 extends Algebra[BigDecimal] {
    def add(a: BigDecimal, b: BigDecimal): BigDecimal = a + b
    def zero: BigDecimal = BigDecimal("0")
  }


  class ListAbstraction2[A] extends Algebra[List[A]] {
    def add(a: List[A], b: List[A]): List[A] = a ::: b
    def zero: List[A] = Nil
  }


}
