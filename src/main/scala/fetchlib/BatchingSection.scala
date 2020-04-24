/*
 * Copyright 2016-2020 47 Degrees <https://47deg.com>
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
 * = Batching =
 *
 * As we have learned, Fetch performs batched requests whenever it can.
 * It also exposes a couple knobs for tweaking the maximum batch size
 * and whether multiple batches are run in parallel or sequentially.
 *
 * @param name batching
 */
object BatchingSection extends AnyFlatSpec with Matchers with Section {

  import FetchTutorialHelper._

  /**
   *
   * = Maximum batch size =
   * When implementing a `DataSource`, there is a method we can override called `maxBatchSize`.
   * When implementing it we can specify the maximum size of the batched requests to this data source,
   * let’s try it out:
   *
   * {{{
   * object BatchedUsers extends Data[UserId, User]{
   *   def name = "Batched Users"
   *
   *   def source[F[_] : Concurrent]: DataSource[F, UserId, User] = new DataSource[F, UserId, User] {
   *     override def data = BatchedUsers
   *
   *     override def CF = Concurrent[F]
   *
   *     override def maxBatchSize: Option[Int] = Some(2)
   *
   *     override def fetch(id: UserId): F[Option[User]] =
   *       latency[F](s"One User $id") >> CF.pure(userDatabase.get(id))
   *
   *     override def batch(ids: NonEmptyList[UserId]): F[Map[UserId, User]] =
   *       latency[F](s"Batch Users $ids") >> CF.pure(userDatabase.filterKeys(ids.toList.toSet).toMap)
   *   }
   * }
   *
   * def getBatchedUser[F[_] : Concurrent](id: Int): Fetch[F, User] =
   *   Fetch(id, BatchedUsers.source)
   * }}}
   *
   * We have defined the maximum batch size to be 2,
   * let’s see what happens when running a fetch that needs more than two users:
   */
  def maximumSize(res0: Int) = {
    def fetchManyBatchedUsers[F[_]: Concurrent]: Fetch[F, List[User]] =
      List(1, 2, 3, 4).traverse(getBatchedUser[F])

    Fetch.run[IO](fetchManyBatchedUsers).unsafeRunSync().size shouldBe res0
  }

  /**
   * Batch execution strategy
   * In the presence of multiple concurrent batches,
   * we can choose between a sequential or parallel execution strategy.
   * By default they will be run in parallel,
   * but you can tweak it by overriding `DataSource#batchExection`.
   *
   * {{{
   * object SequentialUsers extends Data[UserId, User]{
   *   def name = "Sequential Users"
   *
   *   def source[F[_] : Concurrent]: DataSource[F, UserId, User] = new DataSource[F, UserId, User] {
   *     override def data = SequentialUsers
   *
   *     override def CF = Concurrent[F]
   *
   *     override def maxBatchSize: Option[Int] = Some(2)
   *     override def batchExecution: BatchExecution = Sequentially // defaults to `InParallel`
   *
   *     override def fetch(id: UserId): F[Option[User]] =
   *       latency[F](s"One User $id") >> CF.pure(userDatabase.get(id))
   *
   *     override def batch(ids: NonEmptyList[UserId]): F[Map[UserId, User]] =
   *       latency[F](s"Batch Users $ids") >> CF.pure(userDatabase.filterKeys(ids.toList.toSet).toMap)
   *   }
   * }
   *
   * def getSequentialUser[F[_] : Concurrent](id: Int): Fetch[F, User] =
   *   Fetch(id, SequentialUsers.source)
   * }}}
   *
   * We have defined the maximum batch size to be 2 and the batch execution to be sequential,
   * let’s see what happens when running a fetch that needs more than one batch:
   *
   */
  def executionStrategy(res0: Int) = {
    def fetchManySeqBatchedUsers[F[_]: Concurrent]: Fetch[F, List[User]] =
      List(1, 2, 3, 4).traverse(getSequentialUser[F])

    Fetch.run[IO](fetchManySeqBatchedUsers).unsafeRunSync().size shouldBe res0
  }
}
