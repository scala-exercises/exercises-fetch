/*
 *  scala-exercises - exercises-fetch
 *  Copyright (C) 2015-2019 47 Degrees, LLC. <http://www.47deg.com>
 *
 */

package fetchlib

import org.scalacheck.ScalacheckShapeless._
import org.scalaexercises.Test.testSuccess
import org.scalatestplus.scalacheck.Checkers
import org.scalatest.refspec.RefSpec
import shapeless.HNil

class ErrorHandlingSpec extends RefSpec with Checkers {

  def `Exception`(): Unit =
    check(testSuccess(ErrorHandlingSection.catsEval _, true :: HNil))

  def `Debug`(): Unit =
    check(testSuccess(ErrorHandlingSection.debugDescribe _, true :: HNil))

  def `Missing`(): Unit =
    check(testSuccess(ErrorHandlingSection.missing _, true :: HNil))

  def `Missing identity`(): Unit =
    check(testSuccess(ErrorHandlingSection.missingIdentity _, "Users" :: 5 :: HNil))

}
