/*
 * scala-exercises - exercises-fetch
 * Copyright (C) 2015-2016 47 Degrees, LLC. <http://www.47deg.com>
 */

package fetchlib

import fetchlib.FetchTutorialHelper.userDatabase
import org.scalacheck.Shapeless._
import org.scalaexercises.Test.testSuccess
import org.scalatest.prop.Checkers
import org.scalatest.refspec.RefSpec
import shapeless.HNil

class CachingSpec extends RefSpec with Checkers {

  def `Cache Prepopulating`: Unit =
    check(testSuccess(CachingSection.prepopulating _, userDatabase(1) :: HNil))

  def `Cache Partial Hits`: Unit =
    check(testSuccess(CachingSection.cachePartialHits _, 3 :: HNil))

  def `Cache Replay`: Unit =
    check(testSuccess(CachingSection.replaying _, 1 :: 3 :: HNil))

  def `Cache Custom`: Unit =
    check(testSuccess(CachingSection.customCache _, (userDatabase(1), userDatabase(1)) :: HNil))
}
