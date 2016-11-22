package exercises

import fetchlib._
import shapeless.HNil

import org.scalaexercises.Test
import org.scalatest.Spec
import org.scalatest.prop.Checkers

import org.scalacheck.Shapeless._

class SyntaxSpec extends Spec with Checkers {

  import FetchTutorialHelper._
  import SyntaxSection._
  import Test._

  def `Implicit Syntax` =
    check(testSuccess(implicitSyntax _, 42 :: HNil))

  def `Error Syntax` =
    check(testSuccess(errorSyntax _, true :: HNil))

  def `runA Syntax` =
    check(testSuccess(runA _, 1 :: HNil))

  def `runE Syntax` =
    check(testSuccess(runE _, true :: HNil))

  def `runF Syntax` =
    check(testSuccess(runF _, 1 :: true :: HNil))

}
