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
import org.scalatest.{FlatSpec, Matchers}

/**
 * = Caching =
 *
 * As we have learned, Fetch caches intermediate results implicitly. You can
 * provide a prepopulated cache for running a fetch, replay a fetch with the cache of a previous
 * one, and even implement a custom cache.
 *
 * @param name caching
 */
object CachingSection extends FlatSpec with Matchers with Section {

  import FetchTutorialHelper._

  /**
   * = Prepopulating a cache =
   *
   * We'll be using the default in-memory cache, prepopulated with some data. The cache key of an identity
   * is calculated with the `DataSource`'s `identity` method.
   *
   * We can pass a cache as the second argument when running a fetch with `Fetch.run`.
   *
   */
  def prepopulating(res0: Int, res1: String) = {
    val fetchUser: Fetch[User] = getUser(1)
    val cache                  = InMemoryCache(UserSource.identity(1) -> User(1, "@one"))
    Fetch.run[Id](fetchUser, cache) shouldBe User(res0, res1)
  }

  /**
   * And as the first when using fetch syntax:
   * {{{
   *fetchUser.runA[Id](cache)
   * res: cats.Id[User] = User(1,@one)
   * }}}
   * As you can see, when all the data is cached, no query to the data sources is executed since the results are
   *
   * available in the cache.
   *
   * If only part of the data is cached, the cached data won't be asked for:
   *
   */
  def cachePartialHits(res0: String, res1: String) = {

    val fetchUser: Fetch[User]            = getUser(1)
    val cache                             = InMemoryCache(UserSource.identity(1) -> User(1, "@dialelo"))
    val fetchManyUsers: Fetch[List[User]] = List(1, 2, 3).traverse(getUser)
    fetchManyUsers.runA[Id].head.username shouldBe res0
    Fetch.run[Id](fetchUser, cache).username shouldBe res1
  }

  /**
   * = Replaying a fetch without querying any data source =
   *
   * When running a fetch, we are generally interested in its final result.
   * However, we also have access to the cache and information about the executed rounds once we run a fetch.
   * Fetch’s interpreter keeps its state in an environment (implementing the `Env` trait),
   * and we can get both the environment and result after running a fetch using `Fetch.runFetch` instead of `Fetch.run`
   * or `value.runF` via it’s implicit syntax.
   *
   * Knowing this, we can replay a fetch reusing the cache of a previous one. The replayed fetch won't have to call
   * any of the data sources.
   *
   */
  def replaying(res0: Int, res1: Int) = {
    def fetchUsers = List(1, 2, 3).traverse(getUser)

    val firstEnv = fetchUsers.runE[Id]

    firstEnv.rounds.size shouldBe res0

    val secondEnv = fetchUsers.runE[Id](firstEnv.cache)

    secondEnv.rounds.size shouldBe res1
  }

  /**
   *
   * = Implementing a custom cache =
   *
   * The default cache is implemented as an immutable in-memory map,
   * but users are free to use their own caches when running a fetch.
   * Your cache should implement the `DataSourceCache` trait,
   * and after that you can pass it to Fetch's `run` methods.
   *
   * There is no need for the cache to be mutable since fetch
   * executions run in an interpreter that uses the state monad.
   * Note that the `update` method in the `DataSourceCache` trait
   * yields a new, updated cache.
   *
   * {{{
   * trait DataSourceCache {
   * def update[A](k: DataSourceIdentity, v: A): DataSourceCache
   * def get[A](k: DataSourceIdentity): Option[A]
   * }
   * }}}
   *
   * Let's implement a cache that forgets everything we store in it.
   *
   * {{{
   * final case class ForgetfulCache() extends DataSourceCache {
   * override def get[A](k: DataSourceIdentity): Option[A] = None
   * override def update[A](k: DataSourceIdentity, v: A): ForgetfulCache = this
   * }
   * }}}
   *
   * We can now use our implementation of the cache when running a fetch.
   */
  def customCache(res0: Int) = {

    val fetchSameTwice: Fetch[(User, User)] = for {
      one     <- getUser(1)
      another <- getUser(1)
    } yield (one, another)
    val env = fetchSameTwice.runE[Id](ForgetfulCache())
    env.rounds.size shouldBe res0
  }

}
