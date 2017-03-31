/*
 * scala-exercises - exercises-fetch
 * Copyright (C) 2015-2016 47 Degrees, LLC. <http://www.47deg.com>
 */

package fetchlib

object FetchTutorialHelper {

  import fetch._
  import cats.instances.list._

  type UserId = Int
  case class User(id: UserId, username: String)

  def latency[A](result: A, msg: String) = {
    val id = Thread.currentThread.getId
    println(s"~~> [$id] $msg")
    Thread.sleep(100)
    println(s"<~~ [$id] $msg")
    result
  }

  import cats.data.NonEmptyList

  val userDatabase: Map[UserId, User] = Map(
    1 -> User(1, "@one"),
    2 -> User(2, "@two"),
    3 -> User(3, "@three"),
    4 -> User(4, "@four")
  )

  implicit object UserSource extends DataSource[UserId, User] {
    override def name = "User"

    override def fetchOne(id: UserId): Query[Option[User]] = {
      Query.sync({
        latency(userDatabase.get(id), s"One User $id")
      })
    }
    override def fetchMany(ids: NonEmptyList[UserId]): Query[Map[UserId, User]] = {
      Query.sync({
        latency(userDatabase.filterKeys(ids.toList.contains), s"Many Users $ids")
      })
    }
  }

  def getUser(id: UserId): Fetch[User] = Fetch(id)

  type PostId = Int
  case class Post(id: PostId, author: UserId, content: String)

  val postDatabase: Map[PostId, Post] = Map(
    1 -> Post(1, 2, "An article"),
    2 -> Post(2, 3, "Another article"),
    3 -> Post(3, 4, "Yet another article")
  )

  implicit object PostSource extends DataSource[PostId, Post] {
    override def name = "Post"

    override def fetchOne(id: PostId): Query[Option[Post]] = {
      Query.sync({
        latency(postDatabase.get(id), s"One Post $id")
      })
    }
    override def fetchMany(ids: NonEmptyList[PostId]): Query[Map[PostId, Post]] = {
      Query.sync({
        latency(postDatabase.filterKeys(ids.toList.contains), s"Many Posts $ids")
      })
    }

  }

  def getPost(id: PostId): Fetch[Post] = Fetch(id)

  def getAuthor(p: Post): Fetch[User] = Fetch(p.author)

  type PostTopic = String

  implicit object PostTopicSource extends DataSource[Post, PostTopic] {
    override def name = "Post topic"

    override def fetchOne(id: Post): Query[Option[PostTopic]] = {
      Query.sync({
        val topic = if (id.id % 2 == 0) "monad" else "applicative"
        latency(Option(topic), s"One Post Topic $id")
      })
    }
    override def fetchMany(ids: NonEmptyList[Post]): Query[Map[Post, PostTopic]] = {
      Query.sync({
        val result =
          ids.toList.map(id => (id, if (id.id % 2 == 0) "monad" else "applicative")).toMap
        latency(result, s"Many Post Topics $ids")
      })
    }
  }

  def getPostTopic(post: Post): Fetch[PostTopic] = Fetch(post)

  val cache = InMemoryCache(UserSource.identity(1) -> User(1, "@dialelo"))

  final case class ForgetfulCache() extends DataSourceCache {
    override def get[A](k: DataSourceIdentity): Option[A]               = None
    override def update[A](k: DataSourceIdentity, v: A): ForgetfulCache = this
  }

  import fetch.syntax._

  val fetchError: Fetch[User] = (new Exception("Oh noes")).fetch

  import cats.syntax.cartesian._
  import cats.syntax.traverse._

  val postsByAuthor: Fetch[List[Post]] = for {
    posts   <- List(1, 2).traverse(getPost)
    authors <- posts.traverse(getAuthor)
    ordered = (posts zip authors).sortBy({ case (_, author) => author.username }).map(_._1)
  } yield ordered

  val postTopics: Fetch[Map[PostTopic, Int]] = for {
    posts  <- List(2, 3).traverse(getPost)
    topics <- posts.traverse(getPostTopic)
    countByTopic = (posts zip topics).groupBy(_._2).mapValues(_.size)
  } yield countByTopic

  val homePage = (postsByAuthor |@| postTopics).tupled

  import monix.eval.Task

  import monix.execution.Cancelable
  import scala.concurrent.duration._
  import fetch.monixTask.implicits._

  def queryToTask[A](q: Query[A]): Task[A] = q match {
    case Sync(e) => evalToTask(e)
    case Async(action, timeout) => {
      val task: Task[A] = Task.create((scheduler, callback) => {
        scheduler.execute(new Runnable {
          def run() = action(callback.onSuccess, callback.onError)
        })

        Cancelable.empty
      })

      timeout match {
        case finite: FiniteDuration => task.timeout(finite)
        case _                      => task
      }
    }
    case Ap(qf, qx) => Task.zip2(queryToTask(qf), queryToTask(qx)).map({ case (f, x) => f(x) })
  }

  def totalFetched(rounds: Seq[Round]): Int =
    rounds.map((round: Round) => requestFetches(round.request)).toList.sum

  def requestFetches(r: FetchRequest): Int =
    r match {
      case FetchOne(_, _)       => 1
      case FetchMany(ids, _)    => ids.toList.size
      case Concurrent(requests) => requests.toList.map(requestFetches).sum
    }
}
