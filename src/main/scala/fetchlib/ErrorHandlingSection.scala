/*
 * Copyright 2016-2020 47 Degrees Open Source <https://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fetchlib

import cats.effect._
import fetch._
import org.scalaexercises.definitions.Section
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

/**
 * =Error handling=
 *
 * Fetch is used for reading data from remote sources and the queries we perform can and will fail
 * at some point. There are many things that can go wrong:
 *
 *   - an exception can be thrown by client code of certain data sources
 *   - an identity may be missing
 *   - the data source may be temporarily available
 *
 * Since the error cases are plenty and can’t be anticipated Fetch errors are represented by the
 * 'FetchException' trait, which extends `Throwable`. Currently fetch defines `FetchException` cases
 * for missing identities and arbitrary exceptions but you can extend `FetchException` with any
 * error you want.
 *
 * @param name
 *   error_handling
 */
object ErrorHandlingSection extends AnyFlatSpec with Matchers with Section {

  import FetchTutorialHelper._

  /**
   * =Exceptions=
   *
   * What happens if we run a fetch and fails with an exception? We’ll create a fetch that always
   * fails to learn about it.
   * {{{
   * def fetchException[F[_] : Concurrent]: Fetch[F, User] =
   *   Fetch.error(new Exception("Oh noes"))
   * }}}
   * If we try to execute to `IO` the exception will be thrown wrapped in a
   * `fetch.UnhandledException`.
   * {{{
   * Fetch.run[IO](fetchException).unsafeRunTimed(5.seconds)
   * //fetch.package$UnhandledException
   * }}}
   * A safer version would use Cats' `.attempt` method:
   */
  def catsEval(res0: Boolean) = {
    val safeResult = Fetch.run[IO](fetchException).attempt.unsafeRunSync()

    safeResult.isLeft shouldBe res0
  }

  /**
   * =Debugging exceptions=
   *
   * Using fetch’s debugging facilities, we can visualize a failed fetch’s execution up until the
   * point where it failed. Let’s create a fetch that fails after a couple rounds to see it in
   * action:
   * {{{
   * def failingFetch[F[_] : Concurrent]: Fetch[F, String] = for {
   *   a <- getUser(1)
   *   b <- getUser(2)
   *   c <- fetchException
   * } yield s"${a.username} loves ${b.username}"
   *
   * val result: IO[Either[Throwable, (Log, String)]] = Fetch.runLog[IO](failingFetch).attempt
   * }}}
   * Now let’s use the `fetch.debug.describe` function for describing the error if we find one:
   */
  def debugDescribe(res0: Boolean) = {
    import fetch.debug.describe

    val value: Either[Throwable, (Log, String)] = result.unsafeRunSync()
    value.isLeft shouldBe res0

    println(value.fold(describe, identity))
  }

  /**
   * {{{
   * // [ERROR] Unhandled `java.lang.Exception`: 'Oh noes', fetch interrupted after 2 rounds
   * // Fetch execution 🕛 0.21 seconds
   * //
   * //     [Round 1] 🕛 0.10 seconds
   * //       [Fetch one] From `Users` with id 1 🕛 0.10 seconds
   * //     [Round 2] 🕛 0.11 seconds
   * //       [Fetch one] From `Users` with id 2 🕛 0.11 seconds
   * }}}
   * As you can see in the output from `describe`, the fetch stopped due to a `java.lang.Exception`
   * after successfully executing two rounds for getting users 1 and 2.
   *
   * =Missing identities=
   *
   * You’ve probably noticed that `DataSource.fetch` and `DataSource.batch` return types help Fetch
   * know if any requested identity was not found. Whenever an identity cannot be found, the fetch
   * execution will fail with an instance of `MissingIdentity`.
   */
  def missing(res0: Boolean) = {
    import fetch.debug.describe

    def missingUser[F[_]: Concurrent] = getUser(5)

    val result: IO[Either[Throwable, (Log, User)]] = Fetch.runLog[IO](missingUser).attempt

    // And now we can execute the fetch and describe its execution:

    val value: Either[Throwable, (Log, User)] = result.unsafeRunSync()
    value.isLeft shouldBe res0
    println(value.fold(describe, identity))

    // [ERROR] Identity with id `5` for data source `Users` not found, fetch interrupted after 1 rounds
    // Fetch execution 🕛 0.11 seconds
    //
    //     [Round 1] 🕛 0.11 seconds
    //       [Fetch one] From `Users` with id 5 🕛 0.11 seconds
  }

  /**
   * As you can see in the output, the identity `5` for the user source was not found, thus the
   * fetch failed without executing any rounds. `MissingIdentity` also allows you to access the
   * fetch request that was in progress when the error happened.
   */
  def missingIdentity(res0: String, res1: Int) = {
    import fetch.debug.describe

    Fetch.runLog[IO](getUser(5)).attempt.unsafeRunSync() match {
      case Left(mi @ MissingIdentity(id, q, log)) =>
        q.data.name shouldBe res0
        id shouldBe res1

        println(describe(log))
      case _ =>
    }
    // Data: Users
    // Identity: 5
    // Fetch execution 🕛 0.11 seconds
    //
    //   [Round 1] 🕛 0.11 seconds
    //     [Fetch one] From `Users` with id 5 🕛 0.11 seconds
  }
}
