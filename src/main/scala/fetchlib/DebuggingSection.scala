/*
 * scala-exercises - exercises-fetch
 * Copyright (C) 2015-2016 47 Degrees, LLC. <http://www.47deg.com>
 */

package fetchlib

import cats.data.NonEmptyList
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
object DebuggingSection extends FlatSpec with Matchers with Section {

  /**
	  * = Applicative =
	  *
	  * The `|@|` operator (cartesian builder) allows us to combine multiple independent fetches, even when they
	  * are from different types, and apply a pure function to their results. We can use it
	  * as a more powerful alternative to the `product` method or `Fetch#join`:
	  *
	  * Notice how the queries to posts are batched.
	  */
  {
    import cats.syntax.cartesian._

    val fetchThree: Fetch[(Post, User, Post)] = (getPost(1) |@| getUser(2) |@| getPost(2)).tupled

    fetchThree.runA[Id]
    // ~~> [46] Many Posts NonEmptyList(1, 2)
    // <~~ [46] Many Posts NonEmptyList(1, 2)
    // ~~> [46] One User 2
    // <~~ [46] One User 2
    // res54: cats.Id[(Post, User, Post)] = (Post(1,2,An article),User(2,@two),Post(2,3,Another article))
  }

  /**
	  * More interestingly, we can use it to apply a pure function to the results of various fetches.
	  */
  {
    val fetchFriends: Fetch[String] = (getUser(1) |@| getUser(2)).map({ (one, other) =>
      {
        s"${one.username} is friends with ${other.username}"
      }
    })
    // fetchFriends: fetch.Fetch[String] = Free(...)

    fetchFriends.runA[Id]
    // ~~> [46] Many Users NonEmptyList(1, 2)
    // <~~ [46] Many Users NonEmptyList(1, 2)
    // res55: cats.Id[String] = @one is friends with @two
  }

  /**
	  * The above example is equivalent to the following using the `Fetch#join` method:
	  */
  def similarToJoin(res0: Int) = {
    val ops =
      Fetch.join(1.fetch, 2.fetch).map {
        case (a, b) => {
          a + b
        }
      }

    ops.runA[Id] should be(res0)
  }

}
