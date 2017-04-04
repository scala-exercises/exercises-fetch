/*
 * scala-exercises - exercises-fetch
 * Copyright (C) 2015-2016 47 Degrees, LLC. <http://www.47deg.com>
 */

package exercises

import fetchlib._
import shapeless.HNil

import org.scalaexercises.Test
import org.scalatest.Spec
import org.scalatest.prop.Checkers

import org.scalacheck.Shapeless._

class BatchingSpec extends Spec with Checkers {

  import FetchTutorialHelper._
  import BatchingSection._
  import Test._

  def `Batching Maximum batch size` =
    check(testSuccess(maxSize _, 4 :: HNil))

  def `Batching Batch execution strategy` =
    check(testSuccess(executionStrategy _, 4 :: HNil))

}
