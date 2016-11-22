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
   * = pure =
   *
   * Plain values can be lifted to the Fetch monad with `value.fetch`:
   *
   * Executing a pure fetch doesn't query any data source, as expected.
   *
   */
  def implicitSyntax(res0: Int) = {
    42.fetch.runA[Id] should be(res0)
  }

  /**
   * = error =
   *
   * Errors can also be lifted to the Fetch monad via `exception.fetch`.
   *
   */
  def errorSyntax(res0: Boolean) = {
    val fetchFail: Fetch[Int] = new Exception("Something went terribly wrong").fetch
    Try(fetchFail.runA[Id]).isFailure should be(res0)
  }

  /**
   * = join =
   *
   * We can compose two independent fetches with `fetch1.join(fetch2)`.
   */
  def join(res0: Tuple2[Post, User]) = {
    val fetchJoined: Fetch[(Post, User)] = getPost(1).join(getUser(2))
    fetchJoined.runA[Id] should be(res0)
  }

  /**
   * = runA =
   *
   * Run directly any fetch with `fetch1.runA[M[_]]`.
   *
   */
  def runA(res0: Int) = {
    1.fetch.runA[Id] should be(res0)
  }

  /**
   * = runE =
   *
   * Discard results and get the fetch environment with `fetch1.runE[M[_]]`.
   *
   */
  def runE(res0: Boolean) = {
    1.fetch.runE[Id].isInstanceOf[FetchEnv] should be(res0)
  }

  /**
   * = runF =
   *
   * Obtain results and get the fetch environment with `fetch1.runF[M[_]]`.
   *
   */
  def runF(res0: Int, res1: Boolean) = {
    val (env, result) = 1.fetch.runF[Id]

    result should be(res0)
    env.isInstanceOf[FetchEnv] should be(res1)
  }

}
