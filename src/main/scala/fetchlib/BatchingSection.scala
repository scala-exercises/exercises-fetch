/*
 * scala-exercises - exercises-fetch
 * Copyright (C) 2015-2016 47 Degrees, LLC. <http://www.47deg.com>
 */

package fetchlib

import cats._
import cats.instances.list._
import cats.syntax.traverse._
import fetch._
import fetch.syntax._
import fetch.unsafe.implicits._
import org.scalaexercises.definitions.Section
import org.scalatest.{Assertion, FlatSpec, Matchers}

/**
 * = Batching =
 *
 * As we have learned, Fetch performs batched requests whenever it can.
 * It also exposes a couple knobs for tweaking the maximum batch size
 * and whether multiple batches are run in parallel or sequentially.
 *
 * @param name batching
 */
object BatchingSection extends FlatSpec with Matchers with Section {

  import FetchTutorialHelper._

  /**
   *
   * = Maximum batch size =
   * When implementing a `DataSource`, there is a method we can override called `maxBatchSize`.
   * When implementing it we can specify the maximum size of the batched requests to this data source,
   * let’s try it out:
   *
   * {{{+
   *
   * implicit object BatchedUserSource extends DataSource[UserId, User]{
   * override def name = "BatchedUser"
   *
   * override def maxBatchSize: Option[Int] = Some(2)
   *
   * override def fetchOne(id: UserId): Query[Option[User]] = {
   *       Query.sync({
   * latency(userDatabase.get(id), s"One User $id")
   * })
   * }
   * override def fetchMany(ids: NonEmptyList[UserId]): Query[Map[UserId, User]] = {
   *       Query.sync({
   * latency(userDatabase.filterKeys(ids.toList.contains), s"Many Users $ids")
   * })
   * }
   * }
   *
   * def getBatchedUser(id: Int): Fetch[User] = Fetch(id)(BatchedUserSource)
   *
   * }}}
   *
   * We have defined the maximum batch size to be 2,
   * let’s see what happens when running a fetch that needs more than two users:
   */
  def maximumSize(res0: Int) = {
    val fetchManyBatchedUsers: Fetch[List[User]] = List(1, 2, 3, 4).traverse(getBatchedUser)
    fetchManyBatchedUsers.runE[Id].rounds.size shouldBe res0
  }

  /**
   * Batch execution strategy
   * In the presence of multiple concurrent batches,
   * we can choose between a sequential or parallel execution strategy.
   * By default they will be run in parallel,
   * but you can tweak it by overriding `DataSource#batchExection`.
   *
   * {{{
   *
   * implicit object SequentialUserSource extends DataSource[UserId, User]{
   * override def name = "SequentialUser"
   *
   * override def maxBatchSize: Option[Int] = Some(2)
   *
   * override def batchExecution: ExecutionType = Sequential
   *
   * override def fetchOne(id: UserId): Query[Option[User]] = {
   *       Query.sync({
   * latency(userDatabase.get(id), s"One User $id")
   * })
   * }
   * override def fetchMany(ids: NonEmptyList[UserId]): Query[Map[UserId, User]] = {
   *       Query.sync({
   * latency(userDatabase.filterKeys(ids.toList.contains), s"Many Users $ids")
   * })
   * }
   * }
   *
   * def getSequentialUser(id: Int): Fetch[User] = Fetch(id)(SequentialUserSource)
   *
   * }}}
   *
   * We have defined the maximum batch size to be 2 and the batch execution to be sequential,
   * let’s see what happens when running a fetch that needs more than one batch:
   *
   */
  def executionStrategy(res0: Int) = {
    val fetchManySeqBatchedUsers: Fetch[List[User]] = List(1, 2, 3, 4).traverse(getSequentialUser)
    fetchManySeqBatchedUsers.runE[Id].rounds.size shouldBe res0
  }
}
