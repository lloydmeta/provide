package com.beachape.annotations

import org.scalatest.{ Matchers, FunSpec }

/**
 * Created by Lloyd on 6/13/15.
 */
class ProvideSpec extends FunSpec with Matchers {

  describe("@provide") {

    it("should not compile if there is no super trait with an abstract trait") {
      """
        |class Test {
        |  @provide def i = 3
        |}
      """.stripMargin shouldNot compile
    }

    it("should not compile if prefixing a method that is itself abstract") {
      """
      |trait A {
      |  def i: Int
      |}
      |
      |trait B extends A {
      |  @provide def i: Int
      |}
      |
      """.stripMargin shouldNot compile
    }

    it("should not compile if prefixing a method with the same name but different input type sig") {
      """
      |trait A {
      |  def i(x: Int): Int
      |}
      |
      |class Test extends A {
      |  def i(y: Int) =  4
      |  @provide def i = 3
      |}
      """.stripMargin shouldNot compile
    }

    it("should not compile if prefixing a method with the same name but a different return sig") {
      """
      |trait A {
      |  def i(x: Int): Int
      |}
      |
      |class Test extends A {
      |  @provide def i(y: Int) = "hello"
      |}
      """.stripMargin shouldNot compile
    }

    it("should not compile if prefixing a method that was already implemented in a parent") {
      """
        |trait A {
        |  def i = 3
        |}
        |
        |trait B extends A {
        |  @provide override def i = 9
        |}
      """.stripMargin shouldNot compile
    }

    it("should not compile if the generic params are of the wrong type") {
      """
        |
        |trait A {
        |  def lo(yo: Seq[Int]): String
        |}
        |
        |trait B extends A {
        |  @provide def lo(x: List[String]) = x.head.toString
        |}
      """.stripMargin shouldNot compile
    }

    it("should not compile for methods with parameterised methods with the wrong type") {
      """
        |  trait A {
        |    def lo[X](yo: Seq[X]): X
        |  }
        |
        |  trait B extends A {
        |    @provide def lo[X](x: List[X]) = x.head
        |  }
      """.stripMargin shouldNot compile
    }

    it("should not compile for methods with implicit param lists with the wrong type compared with a parent method") {
      """
        |  case class TaxRate(i: BigDecimal)
        |  case class DiscountRate(i: BigDecimal)
        |
        |  trait A {
        |    def totalPrice(p: BigDecimal)(implicit taxRate: TaxRate): BigDecimal
        |  }
        |
        |  class B extends A {
        |    @provide def totalPrice(n: BigDecimal)(implicit r: DiscountRate): BigDecimal = BigDecimal(3)
        |  }
        |
      """.stripMargin shouldNot compile
    }

    it("should compile if prefixing a def with args") {
      """
        |trait A {
        |  def i(x: Int): Int
        |}
        |
        |class Test extends A {
        |  @provide def i(y: Int) = 3
        |}
      """.stripMargin should compile
    }

    it("should compile if prefixing a def with no args") {
      """
        |trait A {
        |  def i: Int
        |}
        |
        |class Test extends A {
        |  @provide def i = 3
        |}
      """.stripMargin should compile
    }

    it("should compile if prefixing a val that implements a parent method") {
      """
        |trait A {
        |  def i: Int
        |}
        |
        |class Test extends A {
        |  @provide val i = 3
        |}
      """.stripMargin should compile
    }

    it("should compile even if there are multiple traits and abstract traits") {
      """
        |abstract class A {
        |  def i: Int
        |}
        |
        |trait B {
        |  def j(l: Long): String
        |}
        |
        |trait Boom extends A with B {
        |  @provide val i = 3
        |  @provide def j(x: Long) = "boom"
        |}
      """.stripMargin should compile
    }

    it("should compile in a moderately complex case ") {
      """
        |case class Yolo(lo: String)
        |
        |trait A {
        |  def lo(yo: Yolo): String
        |}
        |
        |trait B extends A {
        |  @provide def lo(x: Yolo) = x.lo
        |}
      """.stripMargin should compile
    }

    it("should compile for methods with generic params") {
      """
        |
        |  trait A {
        |    def lo(yo: Seq[Int]): String
        |  }
        |
        |  trait B extends A {
        |    @provide def lo(x: Seq[Int]) = x.head.toString
        |  }
        |
      """.stripMargin should compile
    }

    it("should compile for methods with parameterised methods") {
      """
        |  trait A {
        |    def lo[X](yo: Seq[X]): X
        |  }
        |
        |  trait B extends A {
        |    @provide def lo[N](x: Seq[N]) = x.head
        |  }
      """.stripMargin should compile
    }

    it("should compile for methods with a mix of parameterised and normal params") {
      """
        |  trait A {
        |    def lo[X, Y](yo: Seq[X], thing: Int, arg: Seq[Y]): X
        |  }
        |
        |  trait B extends A {
        |    @provide def lo[Y, Z](x: Seq[Y], y: Int, blah: Seq[Z]) = x.head
        |  }
        |
      """.stripMargin should compile
    }

    it("should work with multiple param lists") {
      """
        |
        |  trait A {
        |    def lo[X, Y, Z](yo: Seq[X], thing: Int, arg: Seq[Y])(another: Z): X
        |  }
        |
        |  trait B extends A {
        |    @provide def lo[X, Y, N](x: Seq[X], y: Int, blah: Seq[Y])(what: N) = x.head
        |  }
        |
      """.stripMargin should compile
    }

    it("should work with implicit params") {
      """
        |  case class TaxRate(i: BigDecimal)
        |
        |  trait A {
        |    def totalPrice(p: BigDecimal)(implicit taxRate: TaxRate): BigDecimal
        |  }
        |
        |  class B extends A {
        |    @provide def totalPrice(n: BigDecimal)(implicit r: TaxRate): BigDecimal = BigDecimal(3)
        |  }
      """.stripMargin should compile
    }

    it("should work with functional params") {
      """
        |
        |trait Hello {
        |  def a(f: Int => String): String
        |}
        |
        |class HelloImp extends Hello {
        |  @provide def a(func: Int => String) = func(3)
        |}
      """.stripMargin should compile
    }

  }

}