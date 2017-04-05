/*
 * scala-exercises - exercises-fetch
 * Copyright (C) 2015-2016 47 Degrees, LLC. <http://www.47deg.com>
 */

package exercises

import org.scalaexercises.Test
import org.scalatest.Spec
import org.scalatest.prop.Checkers
import shapeless.HNil

class CachingSpec extends Spec with Checkers {

  import Test._
  import fetchlib.CachingSection._
  import fetchlib.FetchTutorialHelper._

  def `Cache Prepopulating` =
    check(testSuccess(prepopulating _, User(1, "@dialelo") :: HNil))

  def `Cache Partial Hits` =
    check(testSuccess(cachePartialHits _, 3 :: HNil))

  def `Cache Replay` =
    check(testSuccess(replaying _, 1 :: 0 :: HNil))

  def `Cache Custom` =
    check(testSuccess(customCache _, (User(1, "@one"), User(1, "@one")) :: HNil))
}
