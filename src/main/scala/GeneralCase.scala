import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by Lunaspeed on 05/06/2017.
  */
trait GeneralCase {

  def f1(): Option[String]
  def f2(): Option[Int]
  def f3(s: String): Option[String]

  val ff: Option[String] = for {
    r1 <- f1
    r2 <- f2
    r3 <- f3(r1)
  } yield r1 + r2 + r3

  //f1 and f2 should work in parallel and not sequential


  //solution 1
  val rf1 = f1()
  val rf2 = f2()
  val ff1: Option[String] = for {
    r1 <- rf1
    r2 <- rf2
    r3 <- f3(r1)
  } yield r1 + r2 + r3

//  //Viktor Klang solution 1, doesnt work in dotty
  for {
    _ <- Option.empty
    rf1 = f1()
    rf2 = f2()
    r1 <- rf1
    r2 <- rf2
    r3 <- f3(r1)
  } yield r1 + r2 + r3


  import scalaz._, Scalaz._

  //you can add |@| to any type as you wish
  for {
    (r1, r2) <- (f1() |@| f2()).tupled
    r3 <- f3(r1)
  } yield r1 + r2 + r3


  //simple way
  // only the type you use support .zip
  for {
    (r1, r2) <- f1 zip f2
    r3 <- f3(r1)
  } yield r1 + r2 + r3
}
