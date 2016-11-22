package exercises

import fetchlib._
import shapeless.HNil

import org.scalaexercises.Test
import org.scalatest.Spec
import org.scalatest.prop.Checkers

import org.scalacheck.Shapeless._

class CatsSpec extends Spec with Checkers {

  import FetchTutorialHelper._
  import CatsSection._
  import Test._

  def `Cartesian Builder` =
    check(testSuccess(applicative _, 3 :: HNil))

  def `Join ops` =
    check(testSuccess(similarToJoin _, 3 :: HNil))

}
