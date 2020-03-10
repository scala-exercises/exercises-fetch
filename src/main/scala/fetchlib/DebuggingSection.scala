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
 * = Debugging =
 *
 * We have introduced the handy `fetch.debug.describe` function for debugging errors, but it can do more than that.
 * It can also give you a detailed description of a fetch execution given an execution log.
 *
 * Add the following line to your dependencies for including Fetch’s debugging facilities:
 * {{{
 * "com.47deg" %% "fetch-debug" % "1.2.2"
 * }}}
 *
 * @param name Debugging
 */
object DebuggingSection extends AnyFlatSpec with Matchers with Section {

  import FetchTutorialHelper._

  /**
   * = Fetch execution =
   *
   * We are going to create an interesting fetch that applies all the optimizations available (caching, batching
   * and concurrent request) for ilustrating how we can visualize fetch executions using the execution log.
   */
  def fetchExecution(res0: String) = {
    def batched[F[_]: Concurrent]: Fetch[F, List[User]] =
      List(1, 2).traverse(getUser[F])

    def cached[F[_]: Concurrent]: Fetch[F, User] =
      getUser(2)

    def notCached[F[_]: Concurrent]: Fetch[F, User] =
      getUser(4)

    def concurrent[F[_]: Concurrent]: Fetch[F, (List[User], List[Post])] =
      (List(1, 2, 3).traverse(getUser[F]), List(1, 2, 3).traverse(getPost[F])).tupled

    def interestingFetch[F[_]: Concurrent]: Fetch[F, String] =
      batched >> cached >> notCached >> concurrent >> Fetch.pure("done")

    Fetch.runLog[IO](interestingFetch).unsafeRunSync()._2 shouldBe res0
  }

}
