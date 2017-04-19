/*
 * scala-exercises - exercises-fetch
 * Copyright (C) 2015-2016 47 Degrees, LLC. <http://www.47deg.com>
 */

package fetchlib

import org.scalacheck.Shapeless._
import org.scalaexercises.Test
import org.scalatest.prop.Checkers
import org.scalatest.refspec.RefSpec
import shapeless.HNil

class SyntaxSpec extends RefSpec with Checkers {

  import Test._
  import fetchlib.FetchTutorialHelper._
  import fetchlib.SyntaxSection._

  def `Implicit Syntax`: Unit =
    check(testSuccess(implicitSyntax _, 42 :: HNil))

  def `Error Syntax`: Unit =
    check(testSuccess(errorSyntax _, true :: HNil))

  def `runA Syntax`: Unit =
    check(testSuccess(runA _, 1 :: HNil))

  def `runE Syntax`: Unit =
    check(testSuccess(runE _, true :: HNil))

  def `runF Syntax`: Unit =
    check(testSuccess(runF _, 1 :: true :: HNil))

  def `Pure Syntax`: Unit =
    check(testSuccess(companionPure _, 42 :: HNil))

  def `Join Syntax`: Unit =
    check(testSuccess(companionJoin _, (Post(1, 2, "An article"), User(2, "@two")) :: HNil))

  def `Sequence Syntax`: Unit = {
    check(
      testSuccess(
        companionSequence _,
        List(User(1, "@one"), User(2, "@two"), User(3, "@three")) :: HNil))
  }

  def `Traverse Syntax`: Unit = {
    check(
      testSuccess(
        companionTraverse _,
        List(User(1, "@one"), User(2, "@two"), User(3, "@three")) :: HNil))
  }

}
