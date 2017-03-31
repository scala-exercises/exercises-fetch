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

class ConcurrencyMonadsSpec extends Spec with Checkers {

  import FetchTutorialHelper._
  import ConcurrencyMonadsSection._
  import Test._

  def `Std Future` =
    check(testSuccess(stdFutures _, (2, 2) :: HNil))

  def `Monix Task` =
    check(testSuccess(monixTask _, (2, 2) :: HNil))

  def `Custom Types via Monad Error` =
    check(testSuccess(customTypes _, (2, 2) :: HNil))

}
