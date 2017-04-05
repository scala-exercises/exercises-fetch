/*
 * scala-exercises - exercises-fetch
 * Copyright (C) 2015-2016 47 Degrees, LLC. <http://www.47deg.com>
 */

package exercises

import org.scalaexercises.Test
import org.scalatest.Spec
import org.scalatest.prop.Checkers
import shapeless.HNil

class ErrorHandlingSpec extends Spec with Checkers {

  import Test._
  import fetchlib.ErrorHandlingSection._

  def `Exception` =
    check(testSuccess(catsEval _, Left(fetch.UnhandledException) :: HNil))

  def `Debug` =
    check(testSuccess(debugDescribe _, Left(fetch.UnhandledException) :: HNil))

  def `One Request` =
    check(testSuccess(oneRequest _, Left(fetch.NotFound) :: HNil))

  def `Missing` =
    check(
      testSuccess(
        missing _,
        "Missing identities Map(User -> List(5, 6))" :: "Environment FetchEnv(InMemoryCache(Map()),Queue())" :: HNil))

}
