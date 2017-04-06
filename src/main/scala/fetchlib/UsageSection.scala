/*
 * scala-exercises - exercises-fetch
 * Copyright (C) 2015-2016 47 Degrees, LLC. <http://www.47deg.com>
 */

package fetchlib

import cats._
import cats.instances.list._
import cats.syntax.cartesian._
import cats.syntax.traverse._
import fetch._
import fetch.syntax._
import fetch.unsafe.implicits._
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
 *"com.47deg" %% "fetch" % "0.6.0"
 *}}}
 *Or, if using Scala.js:
 *{{{
 *"com.47deg" %%% "fetch" % "0.6.0"
 *}}}
 *Now you’ll have Fetch available in both Scala and Scala.js.
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
 * source for fetching users given their id.
 * The first thing we'll do is define the types for user ids and users.
 *
 * {{{
 * type UserId = Int
 * case class User(id: UserId, username: String)
 * }}}
 *
 * We’ll simulate unpredictable latency with this function.
 * {{{
 * def latency[A](result: A, msg: String) = {
 *  val id = Thread.currentThread.getId
 *  println(s"~~> [$id] $msg")
 *  Thread.sleep(100)
 *  println(s"<~~ [$id] $msg")
 *  result
 * }
 * }}}
 * And now we're ready to write our user data source;
 * we'll emulate a database with an in-memory map.
 *
 * {{{
 * import cats.data.NonEmptyList
 * import cats.instances.list._
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
 * override def name = "User"
 *
 * override def fetchOne(id: UserId): Query[Option[User]] = {
 * Query.sync({
 * latency(userDatabase.get(id), s"One User $id")
 * })
 * }
 * override def fetchMany(ids: NonEmptyList[UserId]): Query[Map[UserId, User]] = {
 * Query.sync({
 * latency(userDatabase.filterKeys(ids.toList.contains), s"Many Users $ids")
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
 * = Data sources that don’t support batching =
 *
 * If your data source doesn’t support batching, you can use the `DataSource#batchingNotSupported` method as the implementation of `fetchMany`. Note that it will use the `fetchOne` implementation for requesting identities one at a time.
 * {{{
 * implicit object UnbatchedSource extends DataSource[Int, Int]{
 * override def name = "Unbatched"
 *
 * override def fetchOne(id: Int): Query[Option[Int]] = {
 * Query.sync(Option(id))
 * }
 * override def fetchMany(ids: NonEmptyList[Int]): Query[Map[Int, Int]] = {
 * batchingNotSupported(ids)
 * }
 * }
 * }}}
 * = Data sources that only support batching =
 *
 * If your data source only supports querying it in batches, you can implement `fetchOne` in terms of `fetchMany` using `DataSource#batchingOnly`.
 * {{{
 * implicit object OnlyBatchedSource extends DataSource[Int, Int]{
 * override def name = "OnlyBatched"
 *
 * override def fetchOne(id: Int): Query[Option[Int]] =
 * batchingOnly(id)
 *
 * override def fetchMany(ids: NonEmptyList[Int]): Query[Map[Int, Int]] =
 *Query.sync(ids.toList.map((x) => (x, x)).toMap)
 * }
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
	  * [[https://monix.io/ Monix]] or [[https://github.com/functional-streams-for-scala/fs2 fs2]], both of which are supported
	  * in Fetch.
	  *
	  * We can now run the fetch and see its result:
	  *
	  * {{{
	  * fetchUser.runA[Id]
	  * }}}
	  *
	  */
  def creatingAndRunning(res0: User) = {
    val fetchUser: Fetch[User] = getUser(1)
    fetchUser.runA[Id] shouldBe res0
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
    val fetchTwoUsers: Fetch[(User, User)] = for {
      aUser       <- getUser(1)
      anotherUser <- getUser(aUser.id + 1)
    } yield {
      (aUser, anotherUser)
    }

    val (env, result) = fetchTwoUsers.runF[Id]

    result shouldBe res0
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
  def batching(res0: (User, User)) = {
    val fetchProduct: Fetch[(User, User)] = getUser(1).product(getUser(2))
    //Note how both ids (1 and 2) are requested in a single query to the data source when executing the fetch.
    fetchProduct.runA[Id] shouldBe res0
  }

  /**
	  * = Deduplication =
	  *
	  * If two independent requests ask for the same identity, Fetch will detect it and deduplicate the id.
	  * Note that when running the fetch, the identity 1 is only requested once even when it is needed by both fetches.
	  *
	  */
  def deduplication(res0: (User, User)) = {
    val fetchDuped: Fetch[(User, User)] = getUser(1).product(getUser(1))

    fetchDuped.runA[Id] shouldBe res0
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
    val fetchCached: Fetch[(User, User)] = for {
      aUser       <- getUser(1)
      anotherUser <- getUser(1)
    } yield {
      (aUser, anotherUser)
    }

    fetchCached.runA[Id] shouldBe res0
  }

  /**
	  * = Queries =
	  *
	  * Queries are a way of separating the computation required to read a piece of data from the context in
	  * which is run. Let's look at the various ways we have of constructing queries.
	  *
	  * = Synchronous =
	  *
	  * A query can be synchronous, and we may want to evaluate it when `fetchOne` and `fetchMany`
	  * are called. We can do so with `Query#sync`:
	  * {{{
	  *     Query.sync(42)
	  * }}}
	  *
	  *
	  * {{{
	  *Query.sync({ println("Computing 42"); 42 })
	  * }}}
	  * Synchronous queries simply wrap a Cats’ `Eval` instance, which captures the notion of a lazy synchronous computation. You can lift an `Eval[A]` into a `Query[A]` too:
	  * {{{
	  * import cats.Eval
	  *Query.eval(Eval.always({ println("Computing 42"); 42 }))
	  * }}}
	  *
	  * = Asynchronous =
	  *
	  * Asynchronous queries are constructed passing a function that accepts a callback (`A => Unit`) and an errback
	  * (`Throwable => Unit`) and performs the asynchronous computation. Note that you must ensure that either the
	  * callback or the errback are called.
	  * {{{
	  * def asynchronous(res0: Boolean) = {
	  * Query.async((ok: (Int => Unit), fail) => {
	  * Thread.sleep(100)
	  * ok(42)
	  * })
	  * }
	  * }}}
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
	  * latency(postDatabase.filterKeys(ids.toList.contains), s"Many Posts $ids")
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
	  * override def name = "Post topic"
	  * override def fetchOne(id: Post): Query[Option[PostTopic]] = {
	  * Query.sync({
	  * val topic = if (id.id % 2 == 0) "monad" else "applicative"
	  * Option(topic)
	  * })
	  * }
	  * override def fetchMany(ids: NonEmptyList[Post]): Query[Map[Post, PostTopic]] = {
	  * Query.sync({
	  * val result = ids.toList.map(id => (id, if (id.id % 2 == 0) "monad" else "applicative")).toMap
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
  def combiningData(res0: (Post, PostTopic)) = {
    val fetchMulti: Fetch[(Post, PostTopic)] = for {
      post  <- getPost(1)
      topic <- getPostTopic(post)
    } yield {
      (post, topic)
    }

    fetchMulti.runA[Id] shouldBe res0
  }

  /**
	  * = Concurrency =
	  *
	  * Combining multiple independent requests to the same data source can have two outcomes:
	  *
	  * - if the data sources are the same, the request is batched
	  * - otherwise, both data sources are queried at the same time
	  *
	  * The below example combines data from two different sources, and the library knows they are independent.
	  */
  def concurrency(res0: (Post, User)) = {
    val fetchConcurrent: Fetch[(Post, User)] = getPost(1).product(getUser(2))

    fetchConcurrent.runA[Id] shouldBe res0

  }

  /**
	  * Since we are running the fetch to `Id`, we couldn’t exploit parallelism for reading from both sources at the same time. Let’s do some imports in order to be able to run fetches to a `Future`.
	  * {{{
	  * import scala.concurrent._
	  * import ExecutionContext.Implicits.global
	  * import scala.concurrent.duration._
	  * }}}
	  * Let’s see what happens when running the same fetch to a `Future`, note that you cannot block for a future’s result in Scala.js.
	  * {{{
	  * import fetch.implicits._
	  * *
	  * Await.result(fetchConcurrent.runA[Future], Duration.Inf)
	  * res: (Post, User) = (Post(1,2,An article),User(2,@two))
	  * }}}
	  * As you can see, each independent request ran in its own logical thread.
	  *
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
    val fetchSequence: Fetch[List[User]] = List(getUser(1), getUser(2), getUser(3)).sequence
    fetchSequence.runA[Id] shouldBe res0
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
    fetchTraverse.runA[Id] shouldBe res0
  }

}
