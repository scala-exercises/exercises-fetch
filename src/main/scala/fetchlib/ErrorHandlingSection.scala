/*
 * scala-exercises - exercises-fetch
 * Copyright (C) 2015-2016 47 Degrees, LLC. <http://www.47deg.com>
 */

package fetchlib

import cats._
import cats.instances.list._
import cats.syntax.applicativeError._
import cats.syntax.traverse._
import fetch._
import fetch.syntax._
import fetch.unsafe.implicits._
import org.scalaexercises.definitions.Section
import org.scalatest.{FlatSpec, Matchers}

/**
 *
 * = Error handling =
 *
 * Fetch is used for reading data from remote sources and the queries we perform can and will fail at some point. There are many things that can go wrong:
 *
 * an exception can be thrown by client code of certain data sources
 * an identity may be missing
 * the data source may be temporarily available
 *
 * Since the error cases are plenty and can’t be anticipated Fetch errors are represented by the 'FetchException' trait, which extends `Throwable`.
 * Currently fetch defines `FetchException` cases for missing identities and arbitrary exceptions but you can extend `FetchException` with any error you want.
 *
 * @param name error_handling
 */
object ErrorHandlingSection extends FlatSpec with Matchers with Section {

  import FetchTutorialHelper._

  /**
	  * = Exceptions =
	  *
	  * What happens if we run a fetch and fails with an exception? We’ll create a fetch that always fails to learn about it.
	  * {{{
	  * val fetchException: Fetch[User] = (new Exception("Oh noes")).fetch
	  * }}}
	  * If we try to execute to Id the exception will be thrown wrapped in a FetchException.
	  * {{{
	  * fetchException.runA[Id]
	  * // res: fetch.UnhandledException: java.lang.Exception: Oh noes
	  * }}}
	  * Since Id runs the fetch eagerly, the only way to recover from errors when running it is surrounding it with a try-catch block. We’ll use Cats’ Eval type as the target monad which, instead of evaluating the fetch eagerly, gives us an Eval[A] that we can run anytime with its .value method.
	  *
	  * We can use the FetchMonadError[Eval]#attempt to convert a fetch result into a disjuntion and avoid throwing exceptions. Fetch provides an implicit instance of FetchMonadError[Eval] that we can import from fetch.unsafe.implicits._ to have it available.
	  * {{{
	  * import fetch.unsafe.implicits._
	  * }}}
	  * Now we can convert Eval[User] into Eval[Either[FetchException, User] and capture exceptions as values in the left of the disjunction.
	  */
  def catsEval(res0: String) = {
    import cats.Eval

    val safeResult: Eval[Either[FetchException, User]] =
      FetchMonadError[Eval].attempt(fetchException.runA[Eval])

    safeResult.value.toString shouldBe res0
  }

  /**
	  * And more succintly with Cats’ applicative error syntax.
	  * {{{
	  * import cats.syntax.applicativeError._
	  *
	  * import fetch.unsafe.implicits._
	  *
	  *fetchException.runA[Eval].attempt.value
	  * // res: Either[fetch.FetchException,User] = Left(fetch.UnhandledException)
	  * }}}
	  * = Debugging exceptions =
	  *
	  * Using fetch’s debugging facilities, we can visualize a failed fetch’s execution up until the point where it failed. Let’s create a fetch that fails after a couple rounds to see it in action:
	  * {{{
	  * val failingFetch: Fetch[String] = for {
	  * a <- getUser(1)
	  * b <- getUser(2)
	  * c <- fetchException
	  * } yield s"${a.username} loves ${b.username}"
	  *
	  * val result: Eval[Either[FetchException, String]] = FetchMonadError[Eval].attempt(failingFetch.runA[Eval])
	  * }}}
	  * Now let’s use the fetch.debug.describe function for describing the error if we find one:
	  */
  def debugDescribe(res0: String) = {
    import fetch.debug.describe

    val value: Either[FetchException, String] = result.value
    value.toString shouldBe res0

    println(value.fold(describe, identity))
    // [Error] Unhandled `java.lang.Exception`: 'Oh noes', fetch interrupted after 2 rounds
    // Fetch execution took 0.203559 seconds
    //
    //     [Fetch one] From `User` with id 1 took 0.000102 seconds
    //     [Fetch one] From `User` with id 2 took 0.000101 seconds
  }

  /**
	  * As you can see in the output from describe, the fetch stopped due to a java.lang.Exception after succesfully executing two rounds for getting users 1 and 2.
	  *
	  * = Missing identities =
	  *
	  * You’ve probably noticed that DataSource.fetchOne and DataSource.fetchMany return types help Fetch know if any requested identity was not found. Whenever an identity cannot be found, the fetch execution will fail with an instance of FetchException.
	  * The requests can be of different types, each of which is described below.
	  *
	  * = One request =
	  *
	  * When a single identity is being fetched the request will be a FetchOne; it contains the data source and the identity to fetch so you should be able to easily diagnose the failure. For ilustrating this scenario we’ll ask for users that are not in the database.
	  */
  def oneRequest(res0: String) = {
    import fetch.debug.describe

    val missingUser = getUser(5)

    val result: Eval[Either[FetchException, User]] = missingUser.runA[Eval].attempt

    //And now we can execute the fetch and describe its execution:

    val value: Either[FetchException, User] = result.value
    value.toString shouldBe res0
    println(value.fold(describe, _.toString))

    // [Error] Identity not found: 5 in `User`, fetch interrupted after 0 rounds
  }

  /**
	  * As you can see in the output, the identity 5 for the user source was not found, thus the fetch failed without executing any rounds. NotFound also allows you to access the fetch request that was in progress when the error happened and the environment of the fetch.
	  * {{{
	  * value match {
	  * 	case Left(nf @ NotFound(_, _)) => {
	  * 		println("Request " + nf.request)
	  * 		println("Environment " + nf.env)
	  * 	}
	  * 	case _ =>
	  * }
	  * // Request FetchOne(5,User)
	  * // Environment FetchEnv(InMemoryCache(Map()),Queue())
	  * }}}
	  *
	  * = Multiple requests =
	  *
	  * When multiple requests to the same data source are batched and/or multiple requests are performed at the same time, is possible that more than one identity was missing. There is another error case for such situations: MissingIdentities, which contains a mapping from data source names to the list of missing identities.
	  * {{{
	  * import fetch.debug.describe
	  *
	  * val missingUsers = List(3, 4, 5, 6).traverse(getUser)
	  *
	  *  val result: Eval[Either[FetchException, List[User]]] = missingUsers.runA[Eval].attempt
	  * }}}
	  * And now we can execute the fetch and describe its execution :
	  * {{{
	  *  val value: Either[FetchException, List[User]] = result.value
	  *   // value: Either[fetch.FetchException,List[User]] = Left(fetch.MissingIdentities)
	  *
	  *   println(value.fold(describe, _.toString))
	  *
	  *   //   [Error] Missing identities, fetch interrupted after 0 rounds
	  *   //   `User` missing identities List(5, 6)
	  *
	  * }}}
	  * The .missing attribute will give us the mapping from data source name to missing identities, and .env will give us the environment so we can track the execution of the fetch.
	  */
  def missing(res0: String, res1: String) = {

    val missingUsers: Fetch[List[User]]                  = List(3, 4, 5, 6).traverse(getUser)
    val result: Eval[Either[FetchException, List[User]]] = missingUsers.runA[Eval].attempt
    val value: Either[FetchException, List[User]]        = result.value

    value match {
      case Left(mi @ MissingIdentities(_, _)) =>
        println("Missing identities " + mi.missing) shouldBe res0
        println("Environment " + mi.env) shouldBe res1
      case _ =>
    }

  }

}
