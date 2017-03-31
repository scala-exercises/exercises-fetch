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
 *
 * = Error handling =
 *
 * Fetch is used for reading data from remote sources and the queries we perform
 * can and will fail at some point.
 *
 * @param name error_handling
 */
object ErrorHandlingSection extends FlatSpec with Matchers with Section {

  import FetchTutorialHelper._

  /**
   * What happens if we run a fetch and fails? We'll create a fetch that always fails to learn about it.
   * {{{
   * val fetchError: Fetch[User] = (new Exception("Oh noes")).fetch
   * }}}
   */
  def failedFetch(res0: Boolean) =
    Try(fetchError.runA[Id]).isFailure should be(res0)

  /**
   * Since `Id` runs the fetch eagerly, the only way to recover from errors is to capture the exception.
   * We'll use Cats' `Eval` type as the target monad which, instead of evaluating the fetch eagerly, gives us
   * an `Eval[A]` that we can run anytime with its `.value` method.
   *
   * We can use the `FetchMonadError[Eval]#attempt` to convert a fetch result into a disjuntion and avoid
   * throwing exceptions. Fetch provides an implicit instance of `FetchMonadError[Eval]` that we can import
   * from `fetch.unsafe.implicits._` to have it available.
   *
   * Now we can convert `Eval[User]` into `Eval[Either[Throwable, User]]` and capture exceptions as values
   * in the left of the disjunction.
   */
  def attemptFailedFetch(res0: Boolean) = {
    import fetch.unsafe.implicits._
    import cats.Eval

    val safeResult: Eval[Either[Throwable, User]] =
      FetchMonadError[Eval].attempt(fetchError.runA[Eval])

    safeResult.value.isLeft should be(res0)
  }

  /**
   * And more succintly with Cats' applicative error syntax.
   */
  def attemptFailedFetchSyntax(res0: Boolean) = {
    import cats.syntax.applicativeError._

    fetchError.runA[Eval].attempt.value.isLeft should be(res0)
  }

}
