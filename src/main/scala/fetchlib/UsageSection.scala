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
import org.scalatest.{FlatSpec, Matchers}

/**
 * = Introduction =
 *
 * Fetch is a library that allows your data fetches to be written in a concise,
 * composable way while executing efficiently. You don't need to use any explicit
 * concurrency construct but existing idioms: applicative for concurrency and
 * monad for sequencing.
 *
 * Oftentimes, our applications read and manipulate data from a variety of
 * different sources such as databases, web services or file systems. These data
 * sources are subject to latency, and we'd prefer to query them efficiently.
 *
 * If we are just reading data, we can make a series of optimizations such as:
 *
 * - batching requests to the same data source
 * - requesting independent data from different sources in parallel
 * - caching previously seen results
 *
 * However, if we mix these optimizations with the code that fetches the data
 * we may end up trading clarity for performance. Furthermore, we are
 * mixing low-level (optimization) and high-level (business logic with the data
 * we read) concerns.
 * = Installation =
 *To begin, add the following dependency to your SBT build file:
 *{{{
 *"com.47deg" %% "fetch" % "1.2.1"
 *}}}
 *Or, if using Scala.js:
 *{{{
 *"com.47deg" %%% "fetch" % "1.2.1"
 *}}}
 *Now you’ll have Fetch available in both Scala and Scala.js.
 *
 * = Usage =
 *
 * In order to tell Fetch how to retrieve data, we must implement the `DataSource` typeclass.
 *
 * {{{
 * import cats.effect.Concurrent
 * import cats.data.NonEmptyList
 *
 * trait DataSource[F[_], Identity, Result]{
 *   def data: Data[Identity, Result]
 *
 *   def CF: Concurrent[F]
 *
 *   def fetch(id: Identity): F[Option[Result]]
 *
 *   /* `batch` is implemented in terms of `fetch` by default */
 *   def batch(ids: NonEmptyList[Identity]): F[Map[Identity, Result]]
 * }
 * }}}
 *
 * It takes two type parameters:
 *
 * - `Identity`: the identity we want to fetch (a `UserId` if we were fetching users)
 * - `Result`: the type of the data we retrieve (a `User` if we were fetching users)
 *
 * There are two methods: `fetch` and `batch`. `fetch` receives one identity and must return
 * a `Concurrent` containing
 * an optional result. Returning an `Option` Fetch can detect whether an identity couldn't be
 * fetched or no longer exists.
 *
 * `batch` method takes a non-empty list of identities and must return a `Concurrent` containing
 * a map from identities to results. Accepting a list of identities gives Fetch the ability to batch
 * requests to the same data source, and returning a mapping from identities to results, Fetch can
 * detect whenever an identity couldn’t be fetched or no longer exists.
 *
 * The `data` method returns a `Data[Identity, Result]` instance that Fetch uses to optimize requests to the
 * same data source, and is expected to return a singleton `object` that extends `Data[Identity, Result]`.
 *
 * = Writing your first data source =
 *
 * Now that we know about the DataSource` typeclass, let's write our first data source! We'll start by
 * implementing a data source for fetching users given their id.
 * The first thing we'll do is define the types for user ids and users.
 *
 * {{{
 * type UserId = Int
 * case class User(id: UserId, username: String)
 * }}}
 *
 * We’ll simulate unpredictable latency with this function.
 * {{{
 * import cats.effect._
 * import cats.syntax.all._
 *
 * def latency[F[_] : Concurrent](msg: String): F[Unit] = for {
 *   _ <- Sync[F].delay(println(s"--> [${Thread.currentThread.getId}] $msg"))
 *   _ <- Sync[F].delay(Thread.sleep(100))
 *   _ <- Sync[F].delay(println(s"<-- [${Thread.currentThread.getId}] $msg"))
 * } yield ()
 * }}}
 * And now we're ready to write our user data source;
 * we'll emulate a database with an in-memory map.
 *
 * {{{
 * import cats.data.NonEmptyList
 * import cats.instances.list._
 * import fetch._
 *
 * val userDatabase: Map[UserId, User] = Map(
 *   1 -> User(1, "@one"),
 *   2 -> User(2, "@two"),
 *   3 -> User(3, "@three"),
 *   4 -> User(4, "@four")
 * )
 *
 * object Users extends Data[UserId, User] {
 *   def name = "Users"
 *
 *   def source[F[_] : Concurrent]: DataSource[F, UserId, User] = new DataSource[F, UserId, User] {
 *     override def data = Users
 *
 *     override def CF = Concurrent[F]
 *
 *     override def fetch(id: UserId): F[Option[User]] =
 *       latency[F](s"One User $id") >> CF.pure(userDatabase.get(id))
 *
 *     override def batch(ids: NonEmptyList[UserId]): F[Map[UserId, User]] =
 *       latency[F](s"Batch Users $ids") >> CF.pure(userDatabase.filterKeys(ids.toList.toSet).toMap)
 *   }
 * }
 * }}}
 *
 * Now that we have a data source we can write a function for fetching users
 * given an id, we just have to pass a `UserId` as an argument to `Fetch`.
 *
 * {{{
 * def getUser[F[_] : Concurrent](id: UserId): Fetch[F, User] =
 *   Fetch(id, Users.source)
 * }}}
 *
 * = Optional identities =
 *
 * If you want to create a Fetch that doesn’t fail if the identity is not found, you can use
 * `Fetch#optional` instead of `Fetch#apply`. Note that instead of a `Fetch[F, A]` you will get a
 * `Fetch[F, Option[A]]`.
 *
 * {{{
 * def maybeGetUser[F[_] : Concurrent](id: UserId): Fetch[F, Option[User]] =
 *   Fetch.optional(id, Users.source)
 * }}}
 *
 * = Data sources that don’t support batching =
 *
 * If your data source doesn’t support batching, you can simply leave the `batch` method unimplemented.
 * Note that it will use the `fetch` implementation for requesting identities in parallel.
 * {{{
 * object Unbatched extends Data[Int, Int]{
 *   def name = "Unbatched"
 *
 *   def source[F[_] : Concurrent]: DataSource[F, Int, Int] = new DataSource[F, Int, Int]{
 *     override def data = Unbatched
 *
 *     override def CF = Concurrent[F]
 *
 *     override def fetch(id: Int): F[Option[Int]] =
 *       CF.pure(Option(id))
 *   }
 * }
 * }}}
 *
 * = Batching individuals requests sequentially =
 *
 * The default `batch` implementation run requests to the data source in parallel, but you can easily
 * override it. We can make `batch` sequential using `NonEmptyList.traverse` for fetching individual
 * identities.
 *
 * {{{
 * object UnbatchedSeq extends Data[Int, Int]{
 *   def name = "UnbatchedSeq"
 *
 *   def source[F[_] : Concurrent]: DataSource[F, Int, Int] = new DataSource[F, Int, Int]{
 *     override def data = UnbatchedSeq
 *
 *     override def CF = Concurrent[F]
 *
 *     override def fetch(id: Int): F[Option[Int]] =
 *       CF.pure(Option(id))
 *
 *     override def batch(ids: NonEmptyList[Int]): F[Map[Int, Int]] =
 *       ids.traverse(
 *         (id) => fetch(id).map(v => (id, v))
 *       ).map(_.collect { case (i, Some(x)) => (i, x) }.toMap)
 *   }
 * }
 * }}}
 *
 * = Data sources that only support batching =
 *
 * If your data source only supports querying it in batches, you can implement `fetch` in terms of `batch`.
 * {{{
 * object OnlyBatched extends Data[Int, Int]{
 *   def name = "OnlyBatched"
 *
 *   def source[F[_] : Concurrent]: DataSource[F, Int, Int] = new DataSource[F, Int, Int]{
 *     override def data = OnlyBatched
 *
 *     override def CF = Concurrent[F]
 *
 *     override def fetch(id: Int): F[Option[Int]] =
 *       batch(NonEmptyList(id, List())).map(_.get(id))
 *
 *     override def batch(ids: NonEmptyList[Int]): F[Map[Int, Int]] =
 *       CF.pure(ids.map(x => (x, x)).toList.toMap)
 *   }
 * }
 * }}}
 *
 * @param name usage
 **/
object UsageSection extends FlatSpec with Matchers with Section {

  import FetchTutorialHelper._

  /**
   * = Creating a runtime =
   *
   * Since we’lll use `IO` from the `cats-effect` library to execute our fetches, we’ll need a runtime for
   * executing our `IO` instances. This includes a `ContextShift[IO]` used for running the `IO` instances and
   * a `Timer[IO]` that is used for scheduling, let’s go ahead and create them, we’ll use a
   * `java.util.concurrent.ScheduledThreadPoolExecutor` with a few threads to run our fetches.
   * {{{
   * import cats.effect._
   * import java.util.concurrent._
   * import scala.concurrent.ExecutionContext
   * import scala.concurrent.duration._
   *
   * val executor = new ScheduledThreadPoolExecutor(4)
   * val executionContext: ExecutionContext = ExecutionContext.fromExecutor(executor)
   *
   * implicit val timer: Timer[IO] = IO.timer(executionContext)
   * implicit val cs: ContextShift[IO] = IO.contextShift(executionContext)
   * }}}
   *
   * = Creating and running a fetch =
   *
   * We are now ready to create and run fetches. Note the distinction between Fetch creation and execution.
   * When we are creating `Fetch` values, we are just constructing a recipe of our data
   * dependencies.
   *
   * {{{
   * def fetchUser[F[_] : Concurrent]: Fetch[F, User] =
   *   getUser(1)
   * }}}
   *
   * A Fetch is just a value, and in order to be able to get its value we need to run it to an IO first.
   * {{{
   * import cats.effect.IO
   *
   * Fetch.run[IO](fetchUser)
   * }}}
   *
   * We can now run the IO and see its result:
   **/
  def creatingAndRunning(res0: User) = {
    def fetchUser[F[_]: Concurrent]: Fetch[F, User] = getUser(1)

    Fetch.run[IO](fetchUser).unsafeRunSync() shouldBe res0
  }

  /**
   * = Sequencing =
   *
   * When we have two fetches that depend on each other, we can use `flatMap` to combine them.
   * The most straightforward way is to use a for comprehension.
   *
   * When composing fetches with `flatMap` we are telling Fetch that the second one depends on the previous one,
   * so it isn't able to make any optimizations. When running the below fetch, we will query the user data source
   * in two rounds: one for the user with id 1 and another for the user with id 2.
   */
  def sequencing(res0: (User, User)) = {
    def fetchTwoUsers[F[_]: Concurrent]: Fetch[F, (User, User)] =
      for {
        aUser       <- getUser(1)
        anotherUser <- getUser(aUser.id + 1)
      } yield (aUser, anotherUser)

    Fetch.run[IO](fetchTwoUsers).unsafeRunSync() shouldBe res0
  }

  /**
   * = Batching =
   *
   * If we combine two independent requests to the same data source, Fetch will
   * automatically batch them together into a single request.
   * Applicative operations like the product of two fetches help us tell
   * the library that those fetches are independent, and thus can be batched if they use the same data source:
   *
   * Both ids (1 and 2) are requested in a single query to the data source when executing the fetch.
   */
  def batching(res0: (User, User)) = {
    def fetchProduct[F[_]: Concurrent]: Fetch[F, (User, User)] = (getUser(1), getUser(2)).tupled

    Fetch.run[IO](fetchProduct).unsafeRunSync() shouldBe res0
  }

  /**
   * = Deduplication =
   *
   * If two independent requests ask for the same identity, Fetch will detect it and deduplicate the id.
   * Note that when running the fetch, the identity 1 is only requested once even when it is needed by both fetches.
   *
   */
  def deduplication(res0: (User, User)) = {
    def fetchDuped[F[_]: Concurrent]: Fetch[F, (User, User)] = (getUser(1), getUser(1)).tupled

    Fetch.run[IO](fetchDuped).unsafeRunSync() shouldBe res0
  }

  /**
   * = Caching =
   *
   * During the execution of a fetch, previously requested results are implicitly cached. This allows us to write
   * fetches in a very modular way, asking for all the data they need as if it
   * was in memory; furthermore, it also avoids re-fetching an identity that may have changed
   * during the course of a fetch execution, which can lead to inconsistencies in the data.
   *
   * {{{
   * val fetchCached: Fetch[(User, User)] = for {
   * aUser <- getUser(1)
   * anotherUser <- getUser(1)
   * } yield (aUser, anotherUser)
   * }}}
   *
   * As you can see, the `User` with id 1 is fetched only once in a single round-trip. The next
   * time it was needed we used the cached versions, thus avoiding another request to the user data
   * source.
   */
  def caching(res0: (User, User)) = {
    def fetchCached[F[_]: Concurrent]: Fetch[F, (User, User)] =
      for {
        aUser       <- getUser(1)
        anotherUser <- getUser(1)
      } yield (aUser, anotherUser)

    Fetch.run[IO](fetchCached).unsafeRunSync() shouldBe res0
  }

  /**
   * = Combining data from multiple sources =
   *
   * Now that we know about some of the optimizations that Fetch can perform to read data efficiently,
   * let's look at how we can combine more than one data source.
   *
   *
   * Imagine that we are rendering a blog and have the following types for posts:
   *
   * {{{
   * type PostId = Int
   * case class Post(id: PostId, author: UserId, content: String)
   * }}}
   *
   * As you can see, every `Post` has an author, but it refers to the author by its id.
   * We'll implement a data source for retrieving a post given a post id.
   *
   * {{{
   * val postDatabase: Map[PostId, Post] = Map(
   *   1 -> Post(1, 2, "An article"),
   *   2 -> Post(2, 3, "Another article"),
   *   3 -> Post(3, 4, "Yet another article")
   * )
   *
   * object Posts extends Data[PostId, Post] {
   *   def name = "Posts"
   *
   *   def source[F[_] : Concurrent]: DataSource[F, PostId, Post] = new DataSource[F, PostId, Post] {
   *     override def data = Posts
   *
   *     override def CF = Concurrent[F]
   *
   *     override def fetch(id: PostId): F[Option[Post]] =
   *       latency[F](s"One Post $id") >> CF.pure(postDatabase.get(id))
   *
   *     override def batch(ids: NonEmptyList[PostId]): F[Map[PostId, Post]] =
   *       latency[F](s"Batch Posts $ids") >> CF.pure(postDatabase.filterKeys(ids.toList.toSet).toMap)
   *   }
   * }
   *
   * def getPost[F[_] : Concurrent](id: PostId): Fetch[F, Post] =
   *   Fetch(id, Posts.source)
   * }}}
   *
   * Apart from posts, we are going to add another data source: one for post topics.
   *
   * {{{
   * type PostTopic = String
   * }}}
   *
   * We'll implement a data source for retrieving a post topic given a post id.
   *
   * {{{
   * object PostTopics extends Data[Post, PostTopic] {
   *   def name = "Post Topics"
   *
   *   def source[F[_] : Concurrent]: DataSource[F, Post, PostTopic] = new DataSource[F, Post, PostTopic] {
   *     override def data = PostTopics
   *
   *     override def CF = Concurrent[F]
   *
   *     override def fetch(id: Post): F[Option[PostTopic]] = {
   *       val topic = if (id.id % 2 == 0) "monad" else "applicative"
   *       latency[F](s"One Post Topic $id") >> CF.pure(Option(topic))
   *     }
   *
   *     override def batch(ids: NonEmptyList[Post]): F[Map[Post, PostTopic]] = {
   *       val result = ids.toList.map(id => (id, if (id.id % 2 == 0) "monad" else "applicative")).toMap
   *       latency[F](s"Batch Post Topics $ids") >> CF.pure(result)
   *     }
   *   }
   * }
   *
   * def getPostTopic[F[_] : Concurrent](post: Post): Fetch[F, PostTopic] =
   *   Fetch(post, PostTopics.source)
   * }}}
   *
   * Now that we have multiple sources let's mix them in the same fetch.
   * In the following example, we are fetching a post given its id and then fetching its topic. This
   * data could come from entirely different places, but Fetch makes working with heterogeneous sources
   * of data very easy.
   *
   */
  def combiningData(res0: (Post, PostTopic)) = {
    def fetchMulti[F[_]: Concurrent]: Fetch[F, (Post, PostTopic)] =
      for {
        post  <- getPost(1)
        topic <- getPostTopic(post)
      } yield (post, topic)

    Fetch.run[IO](fetchMulti).unsafeRunSync() shouldBe res0
  }

  /**
   * = Combinators =
   *
   * Besides `flatMap` for sequencing fetches and products for running them concurrently,
   * Fetch provides a number of other combinators.
   *
   * = Sequence =
   *
   * Whenever we have a list of fetches of the same type and want to run them concurrently, we can use the `sequence`
   * combinator. It takes a `List[Fetch[A]]` and gives you back a `Fetch[List[A]]`, batching the fetches to the same
   * data source and running fetches to different sources in parallel.
   * Note that the `sequence` combinator is more general and works not only on lists but on any type that
   * has a [[http://typelevel.org/cats/tut/traverse.html Traverse]] instance.
   *
   * Since `sequence` uses applicative operations internally, the library is able to perform optimizations
   * across all the sequenced fetches.
   * {{{
   * import cats.instances.list._
   * import cats.syntax.traverse._
   * }}}
   */
  def sequence(res0: List[User]) = {
    def fetchSequence[F[_]: Concurrent]: Fetch[F, List[User]] =
      List(getUser(1), getUser(2), getUser(3)).sequence

    Fetch.run[IO](fetchSequence).unsafeRunSync() shouldBe res0
  }

  /**
   * = Traverse =
   *
   * Another interesting combinator is `traverse`, which is the composition of `map` and `sequence`.
   *
   * All the optimizations made by `sequence` still apply when using `traverse`.
   */
  def traverse(res0: List[User]) = {
    def fetchTraverse[F[_]: Concurrent]: Fetch[F, List[User]] =
      List(1, 2, 3).traverse(getUser[F])

    Fetch.run[IO](fetchTraverse).unsafeRunSync() shouldBe res0
  }

}
