/*
 *  scala-exercises - exercises-fetch
 *  Copyright (C) 2015-2019 47 Degrees, LLC. <http://www.47deg.com>
 *
 */

package fetchlib

import fetchlib.FetchTutorialHelper.{postDatabase, userDatabase}
import org.scalacheck.ScalacheckShapeless._
import org.scalaexercises.Test.testSuccess
import org.scalatestplus.scalacheck.Checkers
import org.scalatest.refspec.RefSpec
import shapeless.HNil

class UsageSpec extends RefSpec with Checkers {

  def `Creating And Running`(): Unit =
    check(testSuccess(UsageSection.creatingAndRunning _, userDatabase(1) :: HNil))

  def `Sequencing Strategy`(): Unit =
    check(testSuccess(UsageSection.sequencing _, (userDatabase(1), userDatabase(2)) :: HNil))

  def `Batching Strategy`(): Unit =
    check(testSuccess(UsageSection.batching _, (userDatabase(1), userDatabase(2)) :: HNil))

  def `Deduplication Strategy`(): Unit =
    check(testSuccess(UsageSection.deduplication _, (userDatabase(1), userDatabase(1)) :: HNil))

  def `Caching Strategy`(): Unit =
    check(testSuccess(UsageSection.caching _, (userDatabase(1), userDatabase(1)) :: HNil))

  def `Combining Data`(): Unit =
    check(testSuccess(UsageSection.combiningData _, (postDatabase(1), "applicative") :: HNil))

  def `Combinators sequence`(): Unit = {
    check(
      testSuccess(
        UsageSection.sequence _,
        List(userDatabase(1), userDatabase(2), userDatabase(3)) :: HNil))
  }

  def `Combinators traverse`(): Unit = {
    check(
      testSuccess(
        UsageSection.traverse _,
        List(userDatabase(1), userDatabase(2), userDatabase(3)) :: HNil))
  }

}
