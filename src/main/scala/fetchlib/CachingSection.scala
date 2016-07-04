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

/**
 * = Caching =
 *
 * As we have learned, Fetch caches intermediate results implicitly. You can
 * provide a prepopulated cache for running a fetch, replay a fetch with the cache of a previous
 * one, and even implement a custom cache.
 *
 * @param name caching
 */
object CachingSection extends FlatSpec with Matchers with exercise.Section {

  import FetchTutorialHelper._

  /**
   * = Prepopulating a cache =
   *
   * We'll be using the default in-memory cache, prepopulated with some data. The cache key of an identity
   * is calculated with the `DataSource`'s `identity` method.
   * {{{
   * val cache = InMemoryCache(UserSource.identity(1) -> User(1, "@dialelo"))
   * }}}
   * We can pass a cache as argument when running a fetch
   */
  def prepopulating(res0: Int) = {

    val env = getUser(1).runE[Id](cache)
    env.rounds.size should be(res0)

  }

  /**
   * As you can see, when all the data is cached, no query to the data sources is executed since the results are available
   * in the cache.
   * If only part of the data is cached, the cached data won't be asked for:
   *
   */
  def cachePartialHits(res0: Int) = {
    val env = List(1, 2, 3).traverse(getUser).runE[Id](cache)
    env.rounds.size should be(res0)
  }

}
