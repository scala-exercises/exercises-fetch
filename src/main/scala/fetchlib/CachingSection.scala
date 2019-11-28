/*
 *  scala-exercises - exercises-fetch
 *  Copyright (C) 2015-2019 47 Degrees, LLC. <http://www.47deg.com>
 *
 */

package fetchlib

import cats.effect.{Concurrent, IO}
import cats.implicits._
import fetch._
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
    def fetchUser[F[_]: Concurrent]: Fetch[F, User] = getUser(1)

    def cache[F[_]: Concurrent] = InMemoryCache.from[F, UserId, User]((Users, 1) -> User(1, "@one"))

    Fetch.run[IO](fetchUser, cache).unsafeRunSync() shouldBe User(res0, res1)
  }

  /**
   * As you can see, when all the data is cached, no query to the data sources is executed since the results are
   * available in the cache.
   *
   * If only part of the data is cached, the cached data won't be asked for:
   *
   */
  def cachePartialHits(res0: String, res1: String) = {
    def fetchUser[F[_]: Concurrent]: Fetch[F, User] = getUser(1)

    def cache[F[_]: Concurrent] =
      InMemoryCache.from[F, UserId, User]((Users, 1) -> User(1, "@dialelo"))

    def fetchManyUsers[F[_]: Concurrent]: Fetch[F, List[User]] =
      List(1, 2, 3).traverse(getUser[F])

    Fetch.run[IO](fetchManyUsers).unsafeRunSync().head.username shouldBe res0
    Fetch.run[IO](fetchUser, cache).unsafeRunSync().username shouldBe res1
  }

  /**
   * = Replaying a fetch without querying any data source =
   *
   * When running a fetch, we are generally interested in its final result. However, we also have access to the
   * cache once we run a fetch. We can get both the cache and the result using `Fetch.runCache` instead of `Fetch.run`.
   *
   * Knowing this, we can replay a fetch reusing the cache of a previous one. The replayed fetch won't have to call
   * any of the data sources.
   */
  def replaying(res0: Int, res1: Int) = {
    def fetchUsers[F[_]: Concurrent]: Fetch[F, List[User]] = List(1, 2, 3).traverse(getUser[F])

    val (populatedCache, result1) = Fetch.runCache[IO](fetchUsers).unsafeRunSync()

    result1.size shouldBe res0

    val secondEnv = Fetch.run[IO](fetchUsers, populatedCache).unsafeRunSync()

    secondEnv.size shouldBe res1
  }

  /**
   *
   * = Implementing a custom cache =
   *
   * The default cache is implemented as an immutable in-memory map, but users are free to use their own caches
   * when running a fetch. Your cache should implement the `DataCache` trait, and after that you can pass it to
   * Fetch’s `run` methods.
   *
   * There is no need for the cache to be mutable since fetch
   * executions run in an interpreter that uses the state monad.
   * Note that the `update` method in the `DataCache` trait
   * yields a new, updated cache.
   *
   * {{{
   * trait DataCache[F[_]] {
   *   def insert[I, A](i: I, v: A, d: Data[I, A]): F[DataCache[F]]
   *   def lookup[I, A](i: I, d: Data[I, A]): F[Option[A]]
   * }
   * }}}
   *
   * Let's implement a cache that forgets everything we store in it.
   *
   * {{{
   * import cats.{Applicative, Monad}
   *
   * case class ForgetfulCache[F[_] : Monad]() extends DataCache[F] {
   *   def insert[I, A](i: I, v: A, d: Data[I, A]): F[DataCache[F]] =
   *     Applicative[F].pure(this)
   *
   *   def lookup[I, A](i: I, ds: Data[I, A]): F[Option[A]] =
   *     Applicative[F].pure(None)
   * }
   *
   * def forgetfulCache[F[_] : Concurrent] = ForgetfulCache[F]()
   * }}}
   *
   * We can now use our implementation of the cache when running a fetch.
   */
  def customCache(res0: User) = {
    def fetchSameTwice[F[_]: Concurrent]: Fetch[F, (User, User)] =
      for {
        one     <- getUser(1)
        another <- getUser(1)
      } yield (one, another)

    Fetch.run[IO](fetchSameTwice, forgetfulCache).unsafeRunSync()._1 shouldBe res0
  }

}
