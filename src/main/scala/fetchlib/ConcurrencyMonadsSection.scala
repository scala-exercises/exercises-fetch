package fetchlib

import cats.data.{NonEmptyList, Xor}
import org.scalatest._
import fetch._

import cats._
import fetch.syntax._
import scala.util.Try

import org.scalaexercises.definitions._

/**
 * = Concurrency monads =
 *
 * Fetch lets you choose the concurrency monad you want for running fetches, supporting the Scala and Scala.js
 * standard library concurrency primitives. However not everyone is using `Future` and Fetch acknowledges it,
 * providing support for the most widespread concurrency monads and making it easy for users to run a fetch to a
 * custom type.
 *
 * For supporting running a fetch to a monad `M[_]` an instance of `FetchMonadError[M]` must be available.
 *
 * We'll use the following fetches for the examples. They show how we can combine independent fetches both for
 * batching and exploiting the concurrency of independent data.
 *
 * {{{
 * val postsByAuthor: Fetch[List[Post]] = for {
 * posts <- List(1, 2).traverse(getPost)
 * authors <- posts.traverse(getAuthor)
 * ordered = (posts zip authors).sortBy({ case (_, author) => author.username }).map(_._1)
 * } yield ordered
 *
 * val postTopics: Fetch[Map[PostTopic, Int]] = for {
 * posts <- List(2, 3).traverse(getPost)
 * topics <- posts.traverse(getPostTopic)
 * countByTopic = (posts zip topics).groupBy(_._2).mapValues(_.size)
 * } yield countByTopic
 *
 * val homePage = (postsByAuthor |@| postTopics).tupled
 * }}}
 *
 * @param name concurrency_monads
 */
object ConcurrencyMonadsSection extends FlatSpec with Matchers with Section {

  import FetchTutorialHelper._

  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.{Await, Future}
  import scala.concurrent.duration._

  import monix.eval.Task
  import monix.execution.Scheduler

  import fetch.monixTask.implicits._

  /**
   * = Future =
   *
   * You can run a fetch into a `Future` simply by importing `fetch.implicits`. It
   * contains an instance of `FetchMonadError[Future]` given that you provide an implicit `ExecutionContext`.
   *
   * For the sake of the examples we'll use the global `ExecutionContext`.
   *
   */
  def stdFutures(res0: Tuple2[Int, Int]) = {
    import fetch.implicits._

    import scala.concurrent.ExecutionContext.Implicits.global
    import scala.concurrent.{Await, Future}
    import scala.concurrent.duration._

    val op = homePage.runA[Future] map {
      case (posts, topics) =>
        (posts.size, topics.size)
    }

    val result = Await.result(op, 5 seconds)
 
    result should be(res0)
  }

  /**
   * ## Monix Task
   *
   * The [Monix](https://monix.io/) library provides an abstraction for lazy, asynchronous computations with its [Task](https://monix.io/docs/2x/eval/task.html) type.
   *
   * For using `Task` as the target concurrency monad of a fetch, add the following dependency to your build file:
   *
   * {{{
   * "com.fortysevendeg" %% "fetch-monix" % _fetchVersion_
   * }}}
   *
   * And do some standard imports, we'll need an Scheduler for running our tasks as well as the instance of `FetchMonadError[Task]` that `fetch-monix` provides:
   *
   * {{{
   * import monix.eval.Task
   * import monix.execution.Scheduler
   *
   * import fetch.monixTask.implicits._
   * }}}
   * Note that running a fetch to a `Task` doesn't trigger execution. We can interpret a task to a `Future` with the `Task#runAsync` method. We'll use the global scheduler for now.
   *
   */
  def monixTask(res0: Tuple2[Int, Int]) = {

    val scheduler = Scheduler.Implicits.global
    val task = Fetch.run[Task](homePage)

    val op = homePage.runA[Task] map {
      case (posts, topics) =>
        (posts.size, topics.size)
    }

    val result = Await.result(op.runAsync(scheduler), 5 seconds)

    result should be(res0)
  }

  /**
   * = Custom types =
   *
   * If you want to run a fetch to a custom type `M[_]`, you need to implement the `FetchMonadError[M]` typeclass. `FetchMonadError[M]` is simply a `MonadError[M, Throwable]` from cats augmented
   * with a method for running a `Query[A]` in the context of the monad `M[A]`.
   *
   * For ilustrating integration with an asynchronous concurrency monad we'll use the implementation of Monix Task.
   *
   * = Running queries =
   *
   * First of all, we need to run queries in our target type. As we have learned, queries can be synchronous (simply wrapping an `Eval` from Cats) or asynchronous. Since we'll need to lift
   * `Eval[A]` values to `Task[A]`, let's write a function for doing so first. Note that Monix's `Task` supports the same evaluation strategies of `Eval` in Cats, so the conversion is very
   * direct:
   *
   * {{{
   * import cats.{Eval, Now, Later, Always}
   * import monix.eval.Task
   *
   * def evalToTask[A](e: Eval[A]): Task[A] = e match {
   * case Now(x) => Task.now(x)
   * case l: Later[A]  => Task.evalOnce(l.value)
   * case a: Always[A] => Task.evalAlways(a.value)
   * case other => Task.evalOnce(other.value)
   * }
   * }}}
   *
   * Now that we can run synchronous queries to `Task`, we'll use `Task#create` for running asynchronous computations. Queries also have a third option: `Ap`, which delegates the applicative combination of independent queries to the target monad.
   *
   * {{{
   * import monix.execution.Cancelable
   * import scala.concurrent.duration._
   *
   * def queryToTask[A](q: Query[A]): Task[A] = q match {
   * case Sync(e) => evalToTask(e)
   * case Async(action, timeout) => {
   * val task: Task[A] = Task.create((scheduler, callback) => {
   * scheduler.execute(new Runnable {
   * def run() = action(callback.onSuccess, callback.onError)
   * })
   *
   * Cancelable.empty
   * })
   *
   * timeout match {
   * case finite: FiniteDuration => task.timeout(finite)
   * case _                      => task
   * }
   * }
   * case Ap(qf, qx) => Task.zip2(queryToTask(qf), queryToTask(qx)).map({ case (f, x) => f(x) })
   * }
   * }}}
   *
   * The asynchronous action was built using `Task#create`; it receives the used scheduler and a callback, runs
   * the async action in the scheduler passing the success and error versions of the callback and returns an empty
   * cancelable (it can not be canceled); if we encounter a finite duration timeout, we set it on the task.
   *
   * The applicative action used `Task#zip2` to combine two tasks and apply the function contained in one of them
   * to the other. We used `Task#zip2` for expressing the independence between the two tasks, which can potentially
   * be evaluated in parallel.
   *
   * = Writing the FetchMonadError instance =
   *
   * Now we're ready for implementing the FetchMonadError instance for `Task`, we need to define it as an implicit.
   * Note that Cats' typeclass hierarchy is expressed with inheritance and methods from weaker typeclasses like `Functor` or `Applicative`
   * in more powerful typeclasses like `Monad` are implemented in terms of the operations of the latter.
   * In practice, this means that if you just implement `pure` and `flatMap` the rest of the combinators like `map` are going to be implemented in terms of them.
   * Because of this we'll override `map` for not using `flatMap` and `product` for expressing the independence of two computations.
   */
  def customTypes(res0: Tuple2[Int, Int]) = {
    implicit val taskFetchMonadError: FetchMonadError[Task] = new FetchMonadError[Task] {
      override def map[A, B](fa: Task[A])(f: A => B): Task[B] =
        fa.map(f)

      override def product[A, B](fa: Task[A], fb: Task[B]): Task[(A, B)] =
        Task.zip2(Task.fork(fa), Task.fork(fb)) // introduce parallelism with Task#fork

      override def pureEval[A](e: Eval[A]): Task[A] = evalToTask(e)

      def pure[A](x: A): Task[A] =
        Task.now(x)

      def handleErrorWith[A](fa: Task[A])(f: Throwable => Task[A]): Task[A] =
        fa.onErrorHandleWith(f)

      def raiseError[A](e: Throwable): Task[A] =
        Task.raiseError(e)

      def flatMap[A, B](fa: Task[A])(f: A => Task[B]): Task[B] =
        fa.flatMap(f)

      override def runQuery[A](q: Query[A]): Task[A] = queryToTask(q)
    }

    val scheduler = Scheduler.Implicits.global

    val op = homePage.runA[Task](taskFetchMonadError) map {
      case (posts, topics) =>
        (posts.size, topics.size)
    }

    val result = Await.result(op.runAsync(scheduler), 5 seconds)

    result should be(res0)

  }

}
