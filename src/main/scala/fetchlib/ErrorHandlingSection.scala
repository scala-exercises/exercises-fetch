package fetchlib

import cats.data.{NonEmptyList, Xor}
import org.scalatest._
import fetch._

import cats._
import fetch.unsafe.implicits._
import fetch.syntax._
import scala.util.Try

/**
 *
 * = Error handling =
 *
 * Fetch is used for reading data from remote sources and the queries we perform
 * can and will fail at some point.
 *
 * @param name error_handling
 */
object ErrorHandlingSection extends FlatSpec with Matchers with exercise.Section {

  import FetchTutorialHelper._

  /**
   * What happens if we run a fetch and fails? We'll create a fetch that always fails to learn about it.
   * {{{
   * val fetchError: Fetch[User] = (new Exception("Oh noes")).fetch
   * }}}
   */
  def failedFetch(res0: Boolean) = {
    Try(fetchError.runA[Id]).isFailure should be(res0)
  }

  /**
   * Since `Id` runs the fetch eagerly, the only way to recover from errors is to capture the exception.
   * We'll use Cats' `Eval` type as the target monad which, instead of evaluating the fetch eagerly, gives us
   * an `Eval[A]` that we can run anytime with its `.value` method.
   *
   * We can use the `FetchMonadError[Eval]#attempt` to convert a fetch result into a disjuntion and avoid
   * throwing exceptions. Fetch provides an implicit instance of `FetchMonadError[Eval]` that we can import
   * from `fetch.unsafe.implicits._` to have it available.
   *
   * Now we can convert `Eval[User]` into `Eval[Throwable Xor User]` and capture exceptions as values
   * in the left of the disjunction.
   */
  def attemptFailedFetch(res0: Exception Xor User) = {
    import fetch.unsafe.implicits._
    import cats.Eval
    import cats.data.Xor

    val safeResult: Eval[Throwable Xor User] =
      FetchMonadError[Eval].attempt(fetchError.runA[Eval])

    safeResult.value should be(res0)
  }

  /**
   * And more succintly with Cats' applicative error syntax.
   */
  def attemptFailedFetchSyntax(res0: Exception Xor User) = {
    import cats.syntax.applicativeError._

    fetchError.runA[Eval].attempt.value should be(res0)
  }

}
