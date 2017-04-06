/*
 * scala-exercises - exercises-fetch
 * Copyright (C) 2015-2016 47 Degrees, LLC. <http://www.47deg.com>
 */

package fetchlib

import org.scalacheck.Shapeless._
import org.scalaexercises.Test.testSuccess
import org.scalatest.prop.Checkers
import org.scalatest.refspec.RefSpec
import shapeless.HNil

class ErrorHandlingSpec extends RefSpec with Checkers {

  def `Exception`: Unit =
    check(testSuccess(ErrorHandlingSection.catsEval _, "Left(fetch.UnhandledException)" :: HNil))

  def `Debug`: Unit =
    check(
      testSuccess(ErrorHandlingSection.debugDescribe _, "Left(fetch.UnhandledException)" :: HNil))

  def `One Request`: Unit =
    check(testSuccess(ErrorHandlingSection.oneRequest _, "Left(fetch.NotFound)" :: HNil))

  def `Missing`: Unit = {
    check(
      testSuccess(
        ErrorHandlingSection.missing _,
        "Missing identities Map(User -> List(5, 6))" ::
          "Environment FetchEnv(InMemoryCache(Map()),Queue())" :: HNil))
  }

}
