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
import cats.syntax.all._

import fetch._

object FetchTutorialHelper {

  private def monadForF[F[_]: Concurrent: Sync] = new Monad[F] {
    def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B] = Concurrent[F].flatMap(fa)(f)
  }

  val executor = new ScheduledThreadPoolExecutor(4)
  val executionContext: ExecutionContext =
    ExecutionContext.fromExecutor(new java.util.concurrent.ForkJoinPool(2))

  type UserId = Int

  case class User(id: UserId, username: String)

  // this method + the implicit Concurrent[F] defined in Datasource leads to
  // _a lot_ of implicit resolution problems. We _must_ have Sync for the delay()
  // call here, and we _must_ have concurrent because the Concurrent algebra requires
  // it for its provided implementation of batch()
  // https://github.com/47degrees/fetch/blob/v3.1.0/fetch/src/main/scala/datasource.scala#L50-L66
  // the upshot of this is a lot of ambiguous monads, syntax not working without explicit
  // references to a value that should have the syntax available, and for comprehensions don't work
  // without a closer implicit definition
  def latency[F[_]: Monad: Sync](msg: String): F[Unit] =
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

    def source[F[_]: Monad: Sync]: DataSource[F, UserId, User] =
      new DataSource[F, UserId, User] {
        implicit def monadForConcurrentSync[F[_]: Sync: Concurrent]: Monad[F] = new Monad[F] {
          def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B] = Concurrent[F].flatMap(fa)(f)
        }
        override def data = Users

        override def fetch(id: UserId): F[Option[User]] =
          monadForF.*>(latency[F](s"One User $id")(monadForF[F], Sync[F]))(
            Concurrent[F].pure(userDatabase.get(id))
          )

        override def batch(ids: NonEmptyList[UserId]): F[Map[UserId, User]] =
          monadForF.*>(latency[F](s"Batch Users $ids")(monadForF[F], Sync[F]))(
            CF.pure(
              userDatabase.view.filterKeys(ids.toList.toSet).toMap
            )
          )
      }
  }

  def getUser[F[_]: Concurrent: Sync](id: UserId): Fetch[F, User] =
    Fetch[F, UserId, User](id, Users.source(monadForF[F], Sync[F]))

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

    def source[F[_]: Concurrent: Monad: Sync]: DataSource[F, PostId, Post] =
      new DataSource[F, PostId, Post] {
        override def data = Posts

        override def CF = Concurrent[F]

        override def fetch(id: PostId): F[Option[Post]] =
          monadForF.*>(latency[F](s"One Post $id")(monadForF[F], Sync[F]))(
            CF.pure(postDatabase.get(id))
          )

        override def batch(ids: NonEmptyList[PostId]): F[Map[PostId, Post]] =
          monadForF.*>(latency[F](s"Batch Posts $ids")(monadForF[F], Sync[F]))(
            CF.pure(
              postDatabase.view.filterKeys(ids.toList.toSet).toMap
            )
          )
      }
  }

  def getPost[F[_]: Concurrent: Sync](id: PostId): Fetch[F, Post] = {
    // implicit definition of Concurrent in datasource causes some implicit conflicts
    // so we can provide this value explicitly to help the compiler out
    // (it doesn't know that we're just going to use IO eventually)
    val monadForF: Monad[F] = new Monad[F] {
      def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B] = Concurrent[F].flatMap(fa)(f)
    }
    Fetch(id, Posts.source[F](Concurrent[F], monadForF, Sync[F]))
  }

  type PostTopic = String

  object PostTopics extends Data[Post, PostTopic] {
    def name = "Post Topics"

    def source[F[_]: Monad: Sync]: DataSource[F, Post, PostTopic] =
      new DataSource[F, Post, PostTopic] {
        override def data = PostTopics

        override def fetch(id: Post): F[Option[PostTopic]] = {
          val topic = if (id.id % 2 == 0) "monad" else "applicative"
          monadForF.*>(latency[F](s"One Post Topic $id")(monadForF[F], Sync[F]))(
            CF.pure(Option(topic))
          )
        }

        override def batch(ids: NonEmptyList[Post]): F[Map[Post, PostTopic]] = {
          val result =
            ids.toList.map(id => (id, if (id.id % 2 == 0) "monad" else "applicative")).toMap
          monadForF.*>(latency[F](s"Batch Post Topics $ids")(monadForF[F], Sync[F]))(
            CF.pure(result)
          )
        }
      }
  }

  def getPostTopic[F[_]: Concurrent: Monad: Sync](post: Post): Fetch[F, PostTopic] =
    Fetch(post, PostTopics.source(monadForF[F], Sync[F]))

  case class ForgetfulCache[F[_]: Monad]() extends DataCache[F] {
    def insert[I, A](i: I, v: A, d: Data[I, A]): F[DataCache[F]] =
      Applicative[F].pure(this)

    def lookup[I, A](i: I, ds: Data[I, A]): F[Option[A]] =
      Applicative[F].pure(None)
  }

  def forgetfulCache[F[_]: Concurrent] = ForgetfulCache[F]()

  object BatchedUsers extends Data[UserId, User] {
    def name = "Batched Users"

    def source[F[_]: Monad: Sync]: DataSource[F, UserId, User] =
      new DataSource[F, UserId, User] {
        override def data = BatchedUsers

        override def maxBatchSize: Option[Int] = Some(2)

        override def fetch(id: UserId): F[Option[User]] =
          monadForF.*>(latency[F](s"One User $id")(monadForF[F], Sync[F]))(
            CF.pure(userDatabase.get(id))
          )

        override def batch(ids: NonEmptyList[UserId]): F[Map[UserId, User]] =
          monadForF.*>(latency[F](s"Batch Users $ids")(monadForF[F], Sync[F]))(
            CF.pure(
              userDatabase.view.filterKeys(ids.toList.toSet).toMap
            )
          )
      }
  }

  def getBatchedUser[F[_]: Concurrent: Monad: Sync](id: Int): Fetch[F, User] =
    Fetch(id, BatchedUsers.source(monadForF[F], Sync[F]))

  object SequentialUsers extends Data[UserId, User] {
    def name = "Sequential Users"

    def source[F[_]: Monad: Sync]: DataSource[F, UserId, User] =
      new DataSource[F, UserId, User] {
        override def data = SequentialUsers

        override def maxBatchSize: Option[Int]      = Some(2)
        override def batchExecution: BatchExecution = Sequentially // defaults to `InParallel`

        override def fetch(id: UserId): F[Option[User]] =
          monadForF.*>(latency[F](s"One User $id")(monadForF[F], Sync[F]))(
            CF.pure(userDatabase.get(id))
          )

        override def batch(ids: NonEmptyList[UserId]): F[Map[UserId, User]] =
          monadForF.*>(latency[F](s"Batch Users $ids")(monadForF[F], Sync[F]))(
            CF.pure(
              userDatabase.view.filterKeys(ids.toList.toSet).toMap
            )
          )
      }
  }

  def getSequentialUser[F[_]: Concurrent: Monad: Sync](id: Int): Fetch[F, User] =
    Fetch(id, SequentialUsers.source(monadForF[F], Sync[F]))

  def failingFetch[F[_]: Sync: Concurrent]: Fetch[F, String] = {
    implicit val fetchMonad: Monad[Fetch[F, *]] = fetchM(monadForF[F])
    for {
      a <- getUser(1)
      b <- getUser(2)
      c <- fetchException
    } yield s"${a.username} loves ${b.username}"
  }

  val result: IO[Either[Throwable, (Log, String)]] = Fetch.runLog[IO](failingFetch).attempt

}
