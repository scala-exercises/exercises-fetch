/*
 *  scala-exercises - exercises-fetch
 *  Copyright (C) 2015-2019 47 Degrees, LLC. <http://www.47deg.com>
 *
 */

package fetchlib

import cats.effect._
import cats.implicits._
import fetch._
import org.scalaexercises.definitions.Section
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

/**
 * = Syntax =
 *
 * @param name syntax
 */
object SyntaxSection extends AnyFlatSpec with Matchers with Section {

  import FetchTutorialHelper._

  /**
   * = Companion object =
   *
   * We’ve been using Cats’ syntax and `fetch.syntax` throughout the examples since it’s more concise and general
   * than the methods in the `Fetch` companion object. However, you can use the methods in the companion object
   * directly.
   *
   * Note that using cats syntax gives you a plethora of combinators, much richer that what the companion object
   * provides.
   *
   * = Pure =
   *
   * Plain values can be lifted to the Fetch monad with `Fetch#pure`:
   */
  def pureSyntax(res0: Int) = {
    def fetchPure[F[_]: Concurrent]: Fetch[F, Int] = Fetch.pure(42)

    Fetch.run[IO](fetchPure).unsafeRunSync() shouldBe res0
  }

  /**
   * = Error =
   *
   * Errors can also be lifted to the Fetch monad via `Fetch#error`.
   *
   */
  def errorSyntax(res0: Boolean) = {
    def fetchFail[F[_]: Concurrent]: Fetch[F, Int] =
      Fetch.error(new Exception("Something went terribly wrong"))

    Fetch.run[IO](fetchFail).attempt.unsafeRunSync().isLeft shouldBe res0
  }

  /**
   * = Cats =
   *
   * Fetch is built using Cats’ data types and typeclasses and thus works out of the box with cats syntax. Using
   * Cats’ syntax, we can make fetch declarations more concise, without the need to use the combinators in the
   * `Fetch` companion object.
   *
   * Fetch provides its own instance of `Applicative[Fetch]`. Whenever we use applicative operations on more than
   * one `Fetch`, we know that the fetches are independent meaning we can perform optimizations such as batching
   * and concurrent requests.
   *
   * If we were to use the default `Applicative[Fetch]` operations, which are implemented in terms of `flatMap`,
   * we wouldn’t have information about the independency of multiple fetches.
   *
   * = Applicative =
   *
   * The tuple apply syntax allows us to combine multiple independent fetches, even when they are from different
   * types, and apply a pure function to their results. We can use it as a more powerful alternative to the product
   * method:
   */
  def applicativeSyntax(res0: User) = {
    def fetchThree[F[_]: Concurrent]: Fetch[F, (Post, User, Post)] =
      (getPost(1), getUser(2), getPost(2)).tupled

    Fetch.run[IO](fetchThree).unsafeRunSync()._2 shouldBe res0
  }

  /**
   * More interestingly, we can use it to apply a pure function to the results of various fetches.
   */
  def applySyntax(res0: String) = {
    def fetchFriends[F[_]: Concurrent]: Fetch[F, String] = (getUser(1), getUser(2)).mapN {
      (one, other) =>
        s"${one.username} is friends with ${other.username}"
    }

    Fetch.run[IO](fetchFriends).unsafeRunSync() shouldBe res0
  }

}
