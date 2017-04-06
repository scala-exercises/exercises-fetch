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

class BatchingSpec extends RefSpec with Checkers {

  def `Batching Maximum batch size`: Unit =
    check(testSuccess(BatchingSection.maximumSize _, 1 :: HNil))

  def `Batching Batch execution strategy`: Unit =
    check(testSuccess(BatchingSection.executionStrategy _, 2 :: HNil))

}
