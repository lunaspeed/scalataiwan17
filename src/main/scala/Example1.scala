import Implicits._

object Example1 {
  //attempt1

  def sum(li: List[Int]): Int = {
    var total = 0
    for (i <- li) {
      total = total + i
    }
    total
  }

  //-------------------------------------------------

  //can I sum types other than Int?
  //attempt2
  //  def sum[A](l: List[A]): A = {
  //    var total: A = ???
  //    for(a <- l) {
  //      total = total + a
  //    }
  //    total
  //  }

  //-------------------------------------------------

  //doesnt work
  //we need something that can add anything of the same type together
  trait Abstraction1[A] {
    def add(a: A, b: A): A
  }


  def sum1[A](l: List[A])(abs: Abstraction1[A]): A = {
    //we will use the first item of the list as initial item
    var total = l.head
    for (a <- l.tail) {
      total = abs.add(total, a)
    }
    total
  }

  //-------------------------------------------------

  //we are functional programmer we cannot use var, and we can have function as a parameter!!!
  //we need something that can accumulate over a list
  def accumulate[A](l: List[A], total: Option[A] = None)(acc: (A, A) => A): Option[A] = l match {
    case Nil => total
    case h :: tail => total match {
      case None => accumulate(tail, Some(h))(acc)
      case Some(t) => accumulate(tail, Some(acc(t, h)))(acc)
    }
  }

  def sum1a[A](l: List[A])(abs: Abstraction1[A]): A = {
    accumulate(l, None)(abs.add) get
  }

  //-------------------------------------------------

  //are you kidding me? `get` on Option?
  //OK we need a default or initial value.
  //let's make it a initial value instead of default value
  trait Algebra[A] {
    def add(a: A, b: A): A

    def zero: A
  }

  def accumulate2[A](l: List[A], total: A)(acc: (A, A) => A): A = l match {
    case Nil => total
    case h :: tail => accumulate2(tail, acc(total, h))(acc)
  }

  def sum2[A](l: List[A])(abs: Algebra[A]): A = {
    accumulate2(l, abs.zero)(abs.add)
  }

  //what have we achieved?
  //with this simple abstraction we can add any data type that has an Algebra's implementation
  //However we can only do it for simple data types?
  //Going a bit more abstract, we can do it for all data structure. List, Set, Map

  //actual use case
  //https://stackoverflow.com/questions/7076128/best-way-to-merge-two-maps-and-sum-the-values-of-same-key


  //Any more?
  //we can append functions (results)
  //We can do this "add" to any thing that we see fit.
  //And this is being generalized to be call Semigroup and with the `zero` as Monoid with some special rules. will skip for now.
  //Semigroup is anything with the `append`/`combine` method. Monoid is a Semigroup with a `zero`/`empty`.
  //This concept doesnt seem to be useful for now, but it appears in many places to allow you do more things easily.
  // ?Feeling the need to use `+` on something other than number?

  //-------------------------------------------------

  //but why can we only "sum" from type A to A? Can't we get a B out of all those A's?
  //No!!! this called a "sum" you cannot "sum" two different things together.
  //Let's abstract
  //change "sum" to "fold"

  //we need a new type of function
  type Folder[A, B] = (B, A) => B

  type Monoid[A] = Algebra[A]

  //accumulate function only needs an addition type to work
  def accumulate3[A, B](l: List[A], total: B)(acc: (B, A) => B): B = l match {
    case Nil => total
    case h :: tail => accumulate3(tail, acc(total, h))(acc)
  }

  //AND the original sum can still be used
  def sum3[A](l: List[A])(M: Monoid[A]): A = {
    accumulate3(l, M.zero)(M.add)
  }

  //making `sum` a bit more general and change it to fold
  def fold[A, B](l: List[A], init: B)(folder: Folder[A, B]): B = {
    accumulate3(l, init)(folder)
  }


  //with the help of Monoid we can safely create a `sum` without directly taking a initial value can sum up a list.
  //currently `fold` is a more generalized method for `sum3`
  //let's properly define the functions and this is called `Foldable`

  trait Algebra2 {

    //sum3 becomes fold
    def fold[A](l: List[A])(M: Monoid[A]): A

    //with the help of Monoid, we can define the above fold as foldMap
    def foldMap[A, B](l: List[A])(f: A => B)(M: Monoid[B]): B

    //since we not have help from monoid
    def foldLeft[A, B](l: List[A], init: B)(f: (B, A) => B): B

    //for some special reason we might want to fold from the right and never evaluate the left if right is not defined
    def foldRight[A, B](l: List[A], init: => B)(f: (A, => B) => B): B

  }

  //let's generalize this into any type that is a type container and give it a proper name

  trait Foldable[F[_]] {

    def fold[A](l: F[A])(M: Monoid[A]): A

    def foldMap[A, B](l: F[A])(f: A => B)(M: Monoid[B]): B

    def foldLeft[A, B](l: F[A], init: B)(f: (B, A) => B): B

    def foldRight[A, B](l: F[A], init: => B)(f: (A, => B) => B): B

  }

  //in Cats the `=> B` is encapsulated in a structure called `Eval` for lazy evaluation


  //-------------------------------------------------


  //Lets examine the case a little bit more.
  //What we are actually doing?
  //We are iterating over a source (the List) and doing some calculation over the values in the source.
  //With the Foldable abstraction, it looks like we can `fold` over anything.
  //However is that true?
  //Can we actually iterate over a Stream? (InputStream, ResultSet)
  //First issue is our summing calculation's pretty tightly coupled with the source especially in terms of life-cycle.
  //
  //Second if we want to iterate over a Stream (whatever that means) we cannot do it asynchronously.

  //To do this we separate the Source/Foldable and the Logic into two separate types.

  object Attempt1 {

    trait Producer[A] {
      //for the sake of simplicity, will ignore asynchronous
      def run[A, B](consumer: Consumer[A, B]): B
    }

    trait Consumer[A, B] {

      def fold[A, B](init: B)(f: (B, A) => B): B
    }

  }

  //ok that's basically the same as before

}
