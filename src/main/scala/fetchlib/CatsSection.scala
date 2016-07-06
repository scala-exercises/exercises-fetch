package fetchlib

import cats.data.{NonEmptyList, Xor}
import org.scalatest._
import fetch._

import cats._
import fetch.unsafe.implicits._
import fetch.syntax._
import scala.util.Try


import org.scalaexercises.definitions._

/**
 * = cats =
 *
 * Fetch is built using Cats' Free monad construction and thus works out of the box with
 * cats syntax. Using Cats' syntax, we can make fetch declarations more concise, without
 * the need to use the combinators in the `Fetch` companion object.
 *
 * Fetch provides its own instance of `Applicative[Fetch]`. Whenever we use applicative
 * operations on more than one `Fetch`, we know that the fetches are independent meaning
 * we can perform optimizations such as batching and concurrent requests.
 *
 * If we were to use the default `Applicative[Fetch]` operations, which are implemented in terms of `flatMap`,
 * we wouldn't have information about the independency of multiple fetches.
 *
 * @param name cats
 */
object CatsSection extends FlatSpec with Matchers with Section {

  /**
   * = Applicative =
   *
   * The `|@|` operator (cartesian builder) allows us to combine multiple independent fetches, even when they
   * are from different types, and apply a pure function to their results. We can use it
   * as a more powerful alternative to the `product` method or `Fetch#join`:
   *
   * ```tut:silent
   * ```
   *
   * Notice how the queries to posts are batched.
   *
   * ```tut:book
   * fetchThree.runA[Id]
   * ```
   *
   */
  def applicative(res0: Int) = {
    import cats.syntax.cartesian._

    val ops =
      (1.fetch |@| 2.fetch).map((a, b) => a + b)

    ops.runA[Id] should be(res0)
  }

  /**
   * The above example is equivalent to the following using the `Fetch#join` method:
   */
  def similarToJoin(res0: Int) = {
    val ops =
      Fetch.join(1.fetch, 2.fetch).map { case (a, b) => a + b }

    ops.runA[Id] should be(res0)
  }

}
