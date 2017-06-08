import scalaz._, Scalaz._

//val l = List(1,2,3,4,5)
//val o = Some(4)
//
//val FL = Foldable[List]
//FL.foldLeftM[Option, Int, String](l, "")((s, i) => if(i == 4) None else Some(s + i))
//
//FL.fold(l)
//FL.foldMap(l)(_.toString)
//Foldable[Option].fold(o)
//
//l mappend List(0,9,8,17)



val m = Map(1 -> List(1,2), 2 -> List(2,3), 4 -> List(6,7,8))
val m1 = Map(1 -> List(4), 4 -> List(10,44))

m |+| m1

val mm = Map("a" -> Map(1 -> 3, 2 -> 4), "b" -> Map(2 -> 2), "c" -> Map(4 -> 6, 8 -> 5), "d" -> Map(8 -> 9999))
val mm1 = Map("a" -> Map(1 -> 2), "b" -> Map(3 -> 20), "c" -> Map(5 -> 6, 8 -> 5))
mm |+| mm1

val f: Int => Int = _ + 1

val f1: Int => Int = _ * 2

val fm = f |+| f1

val a = fm(3)

val fl: String => String = 42 + _

val fl1: String => String = 99 + _

val flm = fl |+| fl1

val x = flm("abc")


val fli: List[Int] => List[Int] = 42 :: _

val fli1: List[Int] => List[Int] = 99 :: _



