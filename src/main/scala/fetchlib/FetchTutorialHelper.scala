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

import java.util.concurrent.ScheduledThreadPoolExecutor

import scala.concurrent.ExecutionContext
import cats.{Applicative, Monad}
import cats.data.NonEmptyList
import cats.effect._

import fetch._
import cats.implicits._

object FetchTutorialHelper {

  val executor = new ScheduledThreadPoolExecutor(4)
  val executionContext: ExecutionContext =
    ExecutionContext.fromExecutor(new java.util.concurrent.ForkJoinPool(2))

  implicit val timer: Timer[IO]     = IO.timer(executionContext)
  implicit val cs: ContextShift[IO] = IO.contextShift(executionContext)

  type UserId = Int

  case class User(id: UserId, username: String)

  def latency[F[_]: Concurrent](msg: String): F[Unit] =
    for {
      _ <- Sync[F].delay(println(s"--> [${Thread.currentThread.getId}] $msg"))
      _ <- Sync[F].delay(Thread.sleep(100))
      _ <- Sync[F].delay(println(s"<-- [${Thread.currentThread.getId}] $msg"))
    } yield ()

  val userDatabase: Map[UserId, User] = Map(
    1 -> User(1, "@one"),
    2 -> User(2, "@two"),
    3 -> User(3, "@three"),
    4 -> User(4, "@four")
  )

  def fetchException[F[_]: Concurrent]: Fetch[F, User] = Fetch.error(new Exception("Oh noes"))

  object Users extends Data[UserId, User] {
    def name = "Users"

    def source[F[_]: Concurrent]: DataSource[F, UserId, User] =
      new DataSource[F, UserId, User] {
        override def data = Users

        def CF = Concurrent[F]

        override def fetch(id: UserId): F[Option[User]] =
          latency[F](s"One User $id") >> CF.pure(userDatabase.get(id))

        override def batch(ids: NonEmptyList[UserId]): F[Map[UserId, User]] =
          latency[F](s"Batch Users $ids") >> CF.pure(
            userDatabase.view.filterKeys(ids.toList.toSet).toMap
          )
      }
  }

  def getUser[F[_]: Concurrent: Monad](id: UserId): Fetch[F, User] =
    Fetch[F, UserId, User](id, Users.source)

  def cache[F[_]: Concurrent] =
    InMemoryCache.from[F, UserId, User](
      (Users, 1) -> User(1, "@dialelo")
    )

  type PostId = Int

  case class Post(id: PostId, author: UserId, content: String)

  val postDatabase: Map[PostId, Post] = Map(
    1 -> Post(1, 2, "An article"),
    2 -> Post(2, 3, "Another article"),
    3 -> Post(3, 4, "Yet another article")
  )

  object Posts extends Data[PostId, Post] {
    def name = "Posts"

    def source[F[_]: Concurrent]: DataSource[F, PostId, Post] =
      new DataSource[F, PostId, Post] {
        override def data = Posts

        override def CF = Concurrent[F]

        override def fetch(id: PostId): F[Option[Post]] =
          latency[F](s"One Post $id") >> CF.pure(postDatabase.get(id))

        override def batch(ids: NonEmptyList[PostId]): F[Map[PostId, Post]] =
          latency[F](s"Batch Posts $ids") >> CF.pure(
            postDatabase.view.filterKeys(ids.toList.toSet).toMap
          )
      }
  }

  def getPost[F[_]: Concurrent](id: PostId): Fetch[F, Post] =
    Fetch(id, Posts.source)

  type PostTopic = String

  object PostTopics extends Data[Post, PostTopic] {
    def name = "Post Topics"

    def source[F[_]: Concurrent]: DataSource[F, Post, PostTopic] =
      new DataSource[F, Post, PostTopic] {
        override def data = PostTopics

        override def CF = Concurrent[F]

        override def fetch(id: Post): F[Option[PostTopic]] = {
          val topic = if (id.id % 2 == 0) "monad" else "applicative"
          latency[F](s"One Post Topic $id") >> CF.pure(Option(topic))
        }

        override def batch(ids: NonEmptyList[Post]): F[Map[Post, PostTopic]] = {
          val result =
            ids.toList.map(id => (id, if (id.id % 2 == 0) "monad" else "applicative")).toMap
          latency[F](s"Batch Post Topics $ids") >> CF.pure(result)
        }
      }
  }

  def getPostTopic[F[_]: Concurrent](post: Post): Fetch[F, PostTopic] =
    Fetch(post, PostTopics.source)

  case class ForgetfulCache[F[_]: Monad]() extends DataCache[F] {
    def insert[I, A](i: I, v: A, d: Data[I, A]): F[DataCache[F]] =
      Applicative[F].pure(this)

    def lookup[I, A](i: I, ds: Data[I, A]): F[Option[A]] =
      Applicative[F].pure(None)
  }

  def forgetfulCache[F[_]: Concurrent] = ForgetfulCache[F]()

  object BatchedUsers extends Data[UserId, User] {
    def name = "Batched Users"

    def source[F[_]: Concurrent]: DataSource[F, UserId, User] =
      new DataSource[F, UserId, User] {
        override def data = BatchedUsers

        override def CF = Concurrent[F]

        override def maxBatchSize: Option[Int] = Some(2)

        override def fetch(id: UserId): F[Option[User]] =
          latency[F](s"One User $id") >> CF.pure(userDatabase.get(id))

        override def batch(ids: NonEmptyList[UserId]): F[Map[UserId, User]] =
          latency[F](s"Batch Users $ids") >> CF.pure(
            userDatabase.view.filterKeys(ids.toList.toSet).toMap
          )
      }
  }

  def getBatchedUser[F[_]: Concurrent](id: Int): Fetch[F, User] =
    Fetch(id, BatchedUsers.source)

  object SequentialUsers extends Data[UserId, User] {
    def name = "Sequential Users"

    def source[F[_]: Concurrent]: DataSource[F, UserId, User] =
      new DataSource[F, UserId, User] {
        override def data = SequentialUsers

        override def CF = Concurrent[F]

        override def maxBatchSize: Option[Int]      = Some(2)
        override def batchExecution: BatchExecution = Sequentially // defaults to `InParallel`

        override def fetch(id: UserId): F[Option[User]] =
          latency[F](s"One User $id") >> CF.pure(userDatabase.get(id))

        override def batch(ids: NonEmptyList[UserId]): F[Map[UserId, User]] =
          latency[F](s"Batch Users $ids") >> CF.pure(
            userDatabase.view.filterKeys(ids.toList.toSet).toMap
          )
      }
  }

  def getSequentialUser[F[_]: Concurrent](id: Int): Fetch[F, User] =
    Fetch(id, SequentialUsers.source)

  def failingFetch[F[_]: Concurrent: Monad]: Fetch[F, String] = {
    for {
      a <- getUser(1)
      b <- getUser(2)
      c <- fetchException
    } yield s"${a.username} loves ${b.username}"
  }

  val result: IO[Either[Throwable, (Log, String)]] = Fetch.runLog[IO](failingFetch).attempt

}
