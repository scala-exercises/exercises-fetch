/*
 * scala-exercises - exercises-fetch
 * Copyright (C) 2015-2016 47 Degrees, LLC. <http://www.47deg.com>
 */

package fetchlib

import cats.data.NonEmptyList
import org.scalatest.{FlatSpec, Matchers, _}
import fetch._
import cats._
import fetch.unsafe.implicits._
import fetch.syntax._

import scala.util.Try
import org.scalaexercises.definitions.Section

/**
 * = Debugging =
 *
 * We have introduced the handy fetch.debug.describe function for debugging errors, but it can do more than that. It can also give you a detailed description of a fetch execution given an environment.
 *
 * Add the following line to your dependencies for including Fetch’s debugging facilities:
 * {{{
 * "com.47deg" %% "fetch-debug" % "0.6.0"
 * }}}
 *
 * @param name Debbuging
 *
 */
object DebuggingSection extends FlatSpec with Matchers with Section {

  import FetchTutorialHelper._

  /**
	  * = Fetch execution =
	  * We are going to create an interesting fetch that applies all the optimizations available (caching, batching and concurrent request) for ilustrating how we can visualize fetch executions using the environment.
	  * {{{
	  * val batched: Fetch[List[User]] = Fetch.multiple(1, 2)(UserSource)
	  * val cached: Fetch[User] = getUser(2)
	  * val concurrent: Fetch[(List[User], List[Post])] = (List(1, 2, 3).traverse(getUser) |@| List(1, 2, 3).traverse(getPost)).tupled
	  * *
	  * val interestingFetch = for {
	  * users <- batched
	  * anotherUser <- cached
	  * _ <- concurrent
	  * } yield "done"
	  * }}}
	  * Now that we have the fetch let’s run it, get the environment and visualize its execution using the describe function:
	  */
  def debugging(res0: List[Int], res1: Int, res2: List[Int]) = {
    import fetch.debug.describe

    val env = interestingFetch.runE[Id]

    println(describe(env))

    // Fetch execution took 0.319514 seconds <- shows the total time that took to run the fetch
    //
    //The nested lines represent the different rounds of execution

    //“Fetch many” rounds are executed for getting a batch of identities from one data source
    /*   [Fetch many] From `User` with ids*/
    res0 /*took 0.000110 seconds*/

    //“Concurrent” rounds are multiple “one” or “many” rounds for different data sources executed concurrently
    //   [Concurrent] took 0.000207 seconds

    //“Fetch one” rounds are executed for getting an identity from one data source
    //     [Fetch one] From `User` with id */
    res1
    //     [Fetch many] From `Post` with ids */
    res2
  }
}
