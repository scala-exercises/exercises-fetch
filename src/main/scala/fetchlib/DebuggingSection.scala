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
