/*
 * scala-exercises - exercises-fetch
 * Copyright (C) 2015-2016 47 Degrees, LLC. <http://www.47deg.com>
 */

package fetchlib

import cats._
import cats.syntax.cartesian._
import fetch._
import fetch.syntax._
import fetch.unsafe.implicits._
import org.scalaexercises.definitions.Section
import org.scalatest.{FlatSpec, Matchers}

/**
 * = Cats =
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

  import FetchTutorialHelper._

  /**
   * = Applicative =
   *
   * The `|@|` operator (cartesian builder) allows us to combine multiple independent fetches, even when they
   * are from different types, and apply a pure function to their results. We can use it
   * as a more powerful alternative to the `product` method or `Fetch#join`:
   *
   * Notice how the queries to posts are batched.
   *
   * {{{
   *   import cats.syntax.cartesian._
   *
   *   val fetchThree: Fetch[(Post, User, Post)] = (getPost(1) |@| getUser(2) |@| getPost(2)).tupled
   *
   *    fetchThree.runA[Id]
   *   // res: (Post(1,2,An article),User(2,@two),Post(2,3,Another article))
   * }}}
   *
   * More interestingly, we can use it to apply a pure function to the results of various fetches.
   */
  def applicative(res0: String) = {
    val fetchFriends: Fetch[String] = (getUser(1) |@| getUser(2)).map({ (one, other) =>
      {
        s"${one.username} is friends with ${other.username}"
      }
    })

    fetchFriends.runA[Id] shouldBe res0
  }

  /**
   * The above example is equivalent to the following using the `Fetch#join` method:
   */
  def similarToJoin(res0: String) = {
    val fetchLoves: Fetch[String] = Fetch
      .join(getUser(1), getUser(2))
      .map({
        case (one, other) => {
          s"${one.username} loves ${other.username}"
        }
      })

    fetchLoves.runA[Id] shouldBe res0
  }

}
