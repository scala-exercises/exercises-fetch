package exercises

import fetchlib._
import shapeless.HNil

import org.scalaexercises.Test
import org.scalatest.Spec
import org.scalatest.prop.Checkers

import org.scalacheck.Shapeless._

class CachingSpec extends Spec with Checkers {

  import FetchTutorialHelper._
  import CachingSection._
  import Test._

  def `Cache Prepopulating` =
    check(testSuccess(prepopulating _, 0 :: HNil))

  def `Cache Partial Hits` =
    check(testSuccess(cachePartialHits _, 2 :: HNil))

  def `Cache Replay` =
    check(testSuccess(replaying _, 1 :: 0 :: HNil))

  def `Cache Custom` =
    check(testSuccess(customCache _, 2 :: HNil))
}
