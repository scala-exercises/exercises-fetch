/*
 *  scala-exercises - exercises-fetch
 *  Copyright (C) 2015-2019 47 Degrees, LLC. <http://www.47deg.com>
 *
 */

package fetchlib

import fetchlib.FetchTutorialHelper.userDatabase
import org.scalacheck.ScalacheckShapeless._
import org.scalaexercises.Test.testSuccess
import org.scalatest.refspec.RefSpec
import org.scalatestplus.scalacheck.Checkers
import shapeless.HNil

class CachingSpec extends RefSpec with Checkers {

  def `Cache Prepopulating`(): Unit =
    check(testSuccess(CachingSection.prepopulating _, 1 :: "@one" :: HNil))

  def `Cache Partial Hits`(): Unit =
    check(testSuccess(CachingSection.cachePartialHits _, "@one" :: "@dialelo" :: HNil))

  def `Cache Replay`(): Unit =
    check(testSuccess(CachingSection.replaying _, 3 :: 3 :: HNil))

  def `Cache Custom`(): Unit =
    check(testSuccess(CachingSection.customCache _, userDatabase(1) :: HNil))
}
