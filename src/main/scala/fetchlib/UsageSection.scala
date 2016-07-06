package fetchlib

import cats.data.NonEmptyList
import org.scalatest._
import fetch._

import cats._
import fetch.unsafe.implicits._
import fetch.syntax._
import cats.std.list._
import cats.syntax.cartesian._
import cats.syntax.traverse._

import org.scalaexercises.definitions._

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
 *
 * = Usage =
 *
 * In order to tell Fetch how to retrieve data, we must implement the `DataSource` typeclass.
 *
 * {{{
 * import cats.data.NonEmptyList
 *
 * trait DataSource[Identity, Result]{
 * def fetchOne(id: Identity): Query[Option[Result]]
 * def fetchMany(ids: NonEmptyList[Identity]): Query[Map[Identity, Result]]
 * }
 * }}}
 *
 * It takes two type parameters:
 *
 * - `Identity`: the identity we want to fetch (a `UserId` if we were fetching users)
 * - `Result`: the type of the data we retrieve (a `User` if we were fetching users)
 *
 * There are two methods: `fetchOne` and `fetchMany`. `fetchOne` receives one identity and must return
 * a `Query` containing
 * an optional result. Returning an `Option` Fetch can detect whether an identity couldn't be fetched or no longer exists.
 *
 * `fetchMany` method takes a non-empty list of identities and must return a `Query` containing
 * a map from identities to results. Accepting a list of identities gives Fetch the ability to batch requests to
 * the same data source, and returning a mapping from identities to results, Fetch can detect whenever an identity
 * couldn't be fetched or no longer exists.
 *
 * Returning `Query` makes it possible to run a fetch independently of the target monad.
 *
 * = Writing your first data source =
 *
 * Now that we know about the `DataSource` typeclass, let's write our first data source! We'll start by implementing a data
 * source for fetching users given their id. The first thing we'll do is define the types for user ids and users.
 *
 * {{{
 * type UserId = Int
 * case class User(id: UserId, username: String)
 * }}}
 *
 *
 * And now we're ready to write our user data source; we'll emulate a database with an in-memory map.
 *
 * {{{
 * import cats.data.NonEmptyList
 * import cats.std.list._
 *
 * import fetch._
 *
 * val userDatabase: Map[UserId, User] = Map(
 * 1 -> User(1, "@one"),
 * 2 -> User(2, "@two"),
 * 3 -> User(3, "@three"),
 * 4 -> User(4, "@four")
 * )
 *
 * implicit object UserSource extends DataSource[UserId, User]{
 * override def fetchOne(id: UserId): Query[Option[User]] = {
 * Query.sync({
 * userDatabase.get(id)
 * })
 * }
 * override def fetchMany(ids: NonEmptyList[UserId]): Query[Map[UserId, User]] = {
 * Query.sync({
 * userDatabase.filterKeys(ids.unwrap.contains)
 * })
 * }
 * }
 * }}}
 *
 * Now that we have a data source we can write a function for fetching users
 * given an id, we just have to pass a `UserId` as an argument to `Fetch`.
 *
 * {{{
 * def getUser(id: UserId): Fetch[User] = Fetch(id) // or, more explicitly: Fetch(id)(UserSource)
 * }}}
 *
 * @param name usage
 */
object UsageSection extends FlatSpec with Matchers with Section {

  import FetchTutorialHelper._

  /**
   * = Creating and running a fetch
   *
   * We are now ready to create and run fetches. Note the distinction between Fetch creation and execution.
   * When we are creating and combining `Fetch` values, we are just constructing a recipe of our data
   * dependencies.
   *
   * {{{
   * val fetchUser: Fetch[User] = getUser(1)
   * }}}
   *
   * A `Fetch` is just a value, and in order to be able to get its value we need to run it to a monad first. The
   * target monad `M[_]` must be able to lift a `Query[A]` to `M[A]`, evaluating the query in the monad's context.
   *
   * We'll run `fetchUser` using `Id` as our target monad, so let's do some imports first. Note that interpreting
   * a fetch to a non-concurrency monad like `Id` or `Eval` is only recommended for trying things out in a Scala
   * console, that's why for using them you need to import `fetch.unsafe.implicits`.
   *
   * {{{
   * import cats.Id
   * import fetch.unsafe.implicits._
   * import fetch.syntax._
   * }}}
   *
   * Note that running a fetch to non-concurrency monads like `Id` or `Eval` is not supported in Scala.js.
   * In real-life scenarios you'll want to run your fetches to `Future` or a `Task` type provided by a library like
   * [Monix](https://monix.io/) or [fs2](https://github.com/functional-streams-for-scala/fs2), both of which are supported
   * in Fetch.
   *
   * We can now run the fetch and see its result:
   *
   * ```tut:book
   * fetchUser.runA[Id]
   * ```
   *
   */
  def creatingAndRunning(res0: User) = {
    val fetchUser: Fetch[User] = getUser(1)
    fetchUser.runA[Id] should be(res0)
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
  def sequencing(res0: Tuple2[User, User], res1: Int) = {
    val fetchTwoUsers: Fetch[(User, User)] = for {
      aUser <- getUser(1)
      anotherUser <- getUser(aUser.id + 1)
    } yield (aUser, anotherUser)

    val (env, result) = fetchTwoUsers.runF[Id]

    result should be(res0)
    env.rounds.size should be(res1)
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
   * {{{
   * import cats.syntax.cartesian._
   * }}}
   */
  def batching(res0: Tuple2[User, User], res1: Int) = {
    val fetchProduct: Fetch[(User, User)] = getUser(1).product(getUser(2))

    val (env, result) = fetchProduct.runF[Id]

    result should be(res0)
    env.rounds.size should be(res1)
  }

  /**
   * = Deduplication =
   *
   * If two independent requests ask for the same identity, Fetch will detect it and deduplicate the id.
   * Note that when running the fetch, the identity 1 is only requested once even when it is needed by both fetches.
   *
   */
  def deduplication(res0: Tuple2[User, User], res1: Int) = {
    val fetchDuped: Fetch[(User, User)] = getUser(1).product(getUser(1))

    val (env, result) = fetchDuped.runF[Id]

    result should be(res0)
    env.rounds.size should be(res1)
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
  def caching(res0: Tuple2[User, User], res1: Int) = {
    val fetchCached: Fetch[(User, User)] = for {
      aUser <- getUser(1)
      anotherUser <- getUser(1)
    } yield (aUser, anotherUser)

    val (env, result) = fetchCached.runF[Id]

    result should be(res0)
    env.rounds.size should be(res1)
  }

  /**
   * = Queries =
   *
   * Queries are a way of separating the computation required to read a piece of data from the context in
   * which is run. Let's look at the various ways we have of constructing queries.
   *
   * @param name queries
   * = synchronous =
   *
   * A query can be synchronous, and we may want to evaluate it when `fetchOne` and `fetchMany`
   * are called. We can do so with `Query#sync`:
   */
  def synchronous(res0: Boolean) = {
    val threadSyncSource = new DataSource[Unit, Long] {
      override def fetchOne(id: Unit): Query[Option[Long]] = {
        Query.sync(Some(Thread.currentThread.getId))
      }
      override def fetchMany(ids: NonEmptyList[Unit]): Query[Map[Unit, Long]] =
        batchingNotSupported(ids)
    }

    val threadId = Fetch(())(threadSyncSource).runA[Id]

    (threadId == Thread.currentThread.getId) should be(res0)
  }

  /**
   * = asynchronous =
   *
   * Asynchronous queries are constructed passing a function that accepts a callback (`A => Unit`) and an errback
   * (`Throwable => Unit`) and performs the asynchronous computation. Note that you must ensure that either the
   * callback or the errback are called.
   */
  def asynchronous(res0: Boolean) = {
    val threadAsyncSource = new DataSource[Unit, Long] {
      override def fetchOne(id: Unit): Query[Option[Long]] = {
        Query.async((ok, fail) => ok(Some(Thread.currentThread.getId)))
      }
      override def fetchMany(ids: NonEmptyList[Unit]): Query[Map[Unit, Long]] =
        batchingNotSupported(ids)
    }

    val threadId = Fetch(())(threadAsyncSource).runA[Id]

    (threadId == Thread.currentThread.getId) should be(res0)
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
   * As you can see, every `Post` has an author, but it refers to the author by its id. We'll implement a data source for retrieving a post given a post id.
   *
   * {{{
   * val postDatabase: Map[PostId, Post] = Map(
   * 1 -> Post(1, 2, "An article"),
   * 2 -> Post(2, 3, "Another article"),
   * 3 -> Post(3, 4, "Yet another article")
   * )
   *
   * implicit object PostSource extends DataSource[PostId, Post]{
   * override def fetchOne(id: PostId): Query[Option[Post]] = {
   * Query.sync({
   * latency(postDatabase.get(id), s"One Post $id")
   * })
   * }
   * override def fetchMany(ids: NonEmptyList[PostId]): Query[Map[PostId, Post]] = {
   * Query.sync({
   * latency(postDatabase.filterKeys(ids.unwrap.contains), s"Many Posts $ids")
   * })
   * }
   * }
   *
   * def getPost(id: PostId): Fetch[Post] = Fetch(id)
   * }}}
   *
   * We can also implement a function for fetching a post's author given a post:
   *
   * {{{
   * def getAuthor(p: Post): Fetch[User] = Fetch(p.author)
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
   * implicit object PostTopicSource extends DataSource[Post, PostTopic]{
   * override def fetchOne(id: Post): Query[Option[PostTopic]] = {
   * Query.sync({
   * val topic = if (id.id % 2 == 0) "monad" else "applicative"
   * Option(topic)
   * })
   * }
   * override def fetchMany(ids: NonEmptyList[Post]): Query[Map[Post, PostTopic]] = {
   * Query.sync({
   * val result = ids.unwrap.map(id => (id, if (id.id % 2 == 0) "monad" else "applicative")).toMap
   * result
   * })
   * }
   * }
   *
   * def getPostTopic(post: Post): Fetch[PostTopic] = Fetch(post)
   * }}}
   *
   * Now that we have multiple sources let's mix them in the same fetch.
   * In the following example, we are fetching a post given its id and then fetching its topic. This
   * data could come from entirely different places, but Fetch makes working with heterogeneous sources
   * of data very easy.
   *
   */
  def combiningData(res0: Tuple2[Post, PostTopic]) = {
    val fetchMulti: Fetch[(Post, PostTopic)] = for {
      post <- getPost(1)
      topic <- getPostTopic(post)
    } yield (post, topic)

    fetchMulti.runA[Id] should be(res0)
  }

  /**
   * = Concurrency =
   *
   * Combining multiple independent requests to the same data source can have two outcomes:
   *
   * - if the data sources are the same, the request is batched
   * - otherwise, both data sources are queried at the same time
   *
   * In the following example we are fetching from different data sources so both requests will be
   * evaluated together.
   * The below example combines data from two different sources, and the library knows they are independent.
   */
  def concurrency(res0: Tuple2[Post, User], res1: Int) = {
    val fetchConcurrent: Fetch[(Post, User)] = getPost(1).product(getUser(2))

    import scala.concurrent._
    import ExecutionContext.Implicits.global
    import scala.concurrent.duration._

    import fetch.implicits._

    val (env, result) = Await.result(fetchConcurrent.runF[Future], Duration.Inf)
    result should be(res0)
    env.rounds.size should be(res1)

  }

  /**
   * = Combinators =
   *
   * Besides `flatMap` for sequencing fetches and `product` for running them concurrently, Fetch provides a number of
   * other combinators.
   *
   * = Sequence =
   *
   * Whenever we have a list of fetches of the same type and want to run them concurrently, we can use the `sequence`
   * combinator. It takes a `List[Fetch[A]]` and gives you back a `Fetch[List[A]]`, batching the fetches to the same
   * data source and running fetches to different sources in parallel.
   * Note that the `sequence` combinator is more general and works not only on lists but on any type that
   * has a [[http://typelevel.org/cats/tut/traverse.html] Traverse] instance.
   *
   * Since `sequence` uses applicative operations internally, the library is able to perform optimizations
   * across all the sequenced fetches.
   * {{{
   * import cats.std.list._
   * import cats.syntax.traverse._
   * }}}
   */
  def sequence(res0: List[User]) = {
    val fetchSequence: Fetch[List[User]] = List(getUser(1), getUser(2), getUser(3)).sequence
    fetchSequence.runA[Id] should be(res0)
  }

  /**
   * = Traverse =
   *
   * Another interesting combinator is `traverse`, which is the composition of `map` and `sequence`.
   *
   * All the optimizations made by `sequence` still apply when using `traverse`.
   *
   */
  def traverse(res0: List[User]) = {
    val fetchTraverse: Fetch[List[User]] = List(1, 2, 3).traverse(getUser)
    fetchTraverse.runA[Id] should be(res0)
  }

}
