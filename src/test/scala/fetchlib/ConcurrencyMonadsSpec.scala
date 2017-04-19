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

class ConcurrencyMonadsSpec extends RefSpec with Checkers {

  def `Std Future`: Unit =
    check(testSuccess(ConcurrencyMonadsSection.stdFutures _, (2, 2) :: HNil))

  def `Monix Task`: Unit =
    check(testSuccess(ConcurrencyMonadsSection.monixTask _, (2, 2) :: HNil))

  def `Custom Types via Monad Error`: Unit =
    check(testSuccess(ConcurrencyMonadsSection.customTypes _, (2, 2) :: HNil))

}
