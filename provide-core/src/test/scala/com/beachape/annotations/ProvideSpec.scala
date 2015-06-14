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
        |  def j: String
        |}
        |
        |trait Boom extends A with B {
        |  @provide val i = 3
        |  @provide val j = "boom"
        |}
      """.stripMargin should compile
    }

  }

}
