package exercises

import fetchlib._
import shapeless.HNil

import org.scalaexercises.Test
import org.scalatest.Spec
import org.scalatest.prop.Checkers

import org.scalacheck.Shapeless._

class ErrorHandlingSpec extends Spec with Checkers {

  import FetchTutorialHelper._
  import ErrorHandlingSection._
  import Test._

  def `Failed Fetch` =
    check(testSuccess(failedFetch _, true :: HNil))

  def `Attempt Failed Fetch` =
    check(testSuccess(attemptFailedFetch _, true :: HNil))

  def `Attempt Failed Fetch Syntax` =
    check(testSuccess(attemptFailedFetchSyntax _, true :: HNil))

}
