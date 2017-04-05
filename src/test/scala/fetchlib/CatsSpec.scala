/*
 * scala-exercises - exercises-fetch
 * Copyright (C) 2015-2016 47 Degrees, LLC. <http://www.47deg.com>
 */

package exercises

import org.scalaexercises.Test
import org.scalatest.Spec
import org.scalatest.prop.Checkers
import shapeless.HNil

class CatsSpec extends Spec with Checkers {

  import Test._
  import fetchlib.CatsSection._

  def `Cartesian Builder` =
    check(testSuccess(applicative _, "@one is friends with @two" :: HNil))

  def `Similar to Join` =
    check(testSuccess(similarToJoin _, "@one loves @two" :: HNil))

}
