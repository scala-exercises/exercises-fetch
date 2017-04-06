/*
 * scala-exercises - exercises-fetch
 * Copyright (C) 2015-2016 47 Degrees, LLC. <http://www.47deg.com>
 */

package fetchlib

import cats._
import fetch._
import fetch.syntax._
import fetch.unsafe.implicits._
import org.scalaexercises.definitions.Section
import org.scalatest.{FlatSpec, Matchers}

import scala.util.Try

/**
 * = Syntax =
 *
 * @param name syntax
 */
object SyntaxSection extends FlatSpec with Matchers with Section {

  import FetchTutorialHelper._

  /**
   * = Implicit syntax =
   *
   * Fetch provides implicit syntax to lift any value to the context of a `Fetch`
   * in addition to the most common used combinators active within `Fetch` instances.
   *
   * = Pure =
   *
   * Plain values can be lifted to the Fetch monad with `value.fetch`:
   *
   * Executing a pure fetch doesn't query any data source, as expected.
   *
   */
  def implicitSyntax(res0: Int) =
    42.fetch.runA[Id] shouldBe res0

  /**
   * = Error =
   *
   * Errors can also be lifted to the Fetch monad via `exception.fetch`.
   *
   */
  def errorSyntax(res0: Boolean) = {
    val fetchFail: Fetch[Int] = new Exception("Something went terribly wrong").fetch
    Try(fetchFail.runA[Id]).isFailure shouldBe res0
  }

  /**
   * = Join =
   *
   * We can compose two independent fetches with `fetch1.join(fetch2)`.
   */
  def join(res0: (Post, User)) = {
    val fetchJoined: Fetch[(Post, User)] = getPost(1).join(getUser(2))
    fetchJoined.runA[Id] shouldBe res0
  }

  /**
   * = RunA =
   *
   * Run directly any fetch with `fetch1.runA[M[_]]`.
   *
   */
  def runA(res0: Int) =
    1.fetch.runA[Id] shouldBe res0

  /**
   * = RunE =
   *
   * Discard results and get the fetch environment with `fetch1.runE[M[_]]`.
   *
   */
  def runE(res0: Boolean) =
    1.fetch.runE[Id].isInstanceOf[FetchEnv] shouldBe res0

  /**
   * = RunF =
   *
   * Obtain results and get the fetch environment with `fetch1.runF[M[_]]`.
   *
   */
  def runF(res0: Int, res1: Boolean) = {
    val (env, result) = 1.fetch.runF[Id]

    result shouldBe res0
    env.isInstanceOf[FetchEnv] shouldBe res1
  }

  /**
   * = Companion object =
   *
   * We’ve been using Cats’ syntax and `fetch.syntax` throughout the examples since it’s more concise and general than the methods in the `Fetch` companion object. However, you can use the methods in the companion object directly.
   *
   * Note that using cats syntax gives you a plethora of combinators, much richer that what the companion object provides.
   *
   * = Pure =
   *
   * Plain values can be lifted to the Fetch monad with `Fetch#pure`:
   *
   * Executing a pure fetch doesn’t query any data source, as expected.
   */
  def companionPure(res0: Int) = {
    val fetchPure: Fetch[Int] = Fetch.pure(42)

    Fetch.run[Id](fetchPure) shouldBe res0
  }

  /**
   *
   * = Error =
   *
   * Errors can also be lifted to the Fetch monad via `Fetch#error`.
   *
   * val fetchFail: Fetch[Int] = Fetch.error(new Exception("Something went terribly wrong"))
   * Note that interpreting an errorful fetch to Id will throw the exception.
   * {{{
   *  Fetch.run[Id](fetchFail)
   * fetch.UnhandledException: java.lang.Exception: Something went terribly wrong
   * }}}
   *
   * = Join =
   *
   * We can compose two independent fetches with `Fetch#join`.
   *
   * If the fetches are to the same data source they will be batched; if they aren’t, they will be evaluated at the same time.
   */
  def companionJoin(res0: (Post, User)) = {
    val fetchJoined: Fetch[(Post, User)] = Fetch.join(getPost(1), getUser(2))

    Fetch.run[Id](fetchJoined) shouldBe res0
  }

  /**
   *
   * = Sequence =
   *
   * The `Fetch#sequence` combinator turns a `List[Fetch[A]]` into a `Fetch[List[A]]`, running all the fetches concurrently and batching when possible.
   *
   * Note that `Fetch#sequence` is not as general as the `sequence` method from `Traverse`, but performs the same optimizations.
   */
  def companionSequence(res0: List[User]) = {
    val fetchSequence: Fetch[List[User]] = Fetch.sequence(List(getUser(1), getUser(2), getUser(3)))

    Fetch.run[Id](fetchSequence) shouldBe res0
  }

  /**
   * = Traverse =
   *
   * The `Fetch#traverse` combinator is a combination of `map` and `sequence`.
   *
   * Note that `Fetch#traverse` is not as general as the `traverse` method from `Traverse`, but performs the same optimizations.
   */
  def companionTraverse(res0: List[User]) = {

    val fetchTraverse: Fetch[List[User]] = Fetch.traverse(List(1, 2, 3))(getUser)

    Fetch.run[Id](fetchTraverse) shouldBe res0
  }
}
