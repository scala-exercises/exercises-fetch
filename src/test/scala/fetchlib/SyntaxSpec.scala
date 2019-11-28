/*
 *  scala-exercises - exercises-fetch
 *  Copyright (C) 2015-2019 47 Degrees, LLC. <http://www.47deg.com>
 *
 */

package fetchlib

import org.scalacheck.ScalacheckShapeless._
import org.scalaexercises.Test
import org.scalatestplus.scalacheck.Checkers
import org.scalatest.refspec.RefSpec
import shapeless.HNil

class SyntaxSpec extends RefSpec with Checkers {

  import Test._
  import fetchlib.FetchTutorialHelper._
  import fetchlib.SyntaxSection._

  def `Pure Syntax`(): Unit =
    check(testSuccess(pureSyntax _, 42 :: HNil))

  def `Error Syntax`(): Unit =
    check(testSuccess(errorSyntax _, true :: HNil))

  def `Applicative Syntax`(): Unit =
    check(testSuccess(applicativeSyntax _, userDatabase(2) :: HNil))

  def `Apply Syntax`(): Unit =
    check(testSuccess(applySyntax _, "@one is friends with @two" :: HNil))

}
