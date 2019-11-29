/*
 *  scala-exercises - exercises-fetch
 *  Copyright (C) 2015-2019 47 Degrees, LLC. <http://www.47deg.com>
 *
 */

package fetchlib

import org.scalacheck.ScalacheckShapeless._
import org.scalaexercises.Test
import org.scalatest.refspec.RefSpec
import org.scalatestplus.scalacheck.Checkers
import shapeless.HNil

class DebuggingSpec extends RefSpec with Checkers {

  import Test._
  import fetchlib.DebuggingSection._

  def `Fetch execution`(): Unit =
    check(testSuccess(fetchExecution _, "done" :: HNil))

}
