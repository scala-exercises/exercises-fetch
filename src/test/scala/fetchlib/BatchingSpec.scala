/*
 * scala-exercises - exercises-fetch
 * Copyright (C) 2015-2016 47 Degrees, LLC. <http://www.47deg.com>
 */

package exercises

import org.scalaexercises.Test
import org.scalatest.Spec
import org.scalatest.prop.Checkers
import shapeless.HNil

class BatchingSpec extends Spec with Checkers {

  import Test._
  import fetchlib.BatchingSection._
  def `Batching Maximum batch size` =
    check(testSuccess(maximumSize _, 4 :: HNil))

  def `Batching Batch execution strategy` =
    check(testSuccess(executionStrategy _, 4 :: HNil))

}
