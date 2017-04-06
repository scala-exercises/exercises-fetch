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

class CatsSpec extends RefSpec with Checkers {

  def `Cartesian Builder`: Unit =
    check(testSuccess(CatsSection.applicative _, "@one is friends with @two" :: HNil))

  def `Similar to Join`: Unit =
    check(testSuccess(CatsSection.similarToJoin _, "@one loves @two" :: HNil))

}
