package exercises

import fetchlib._
import shapeless.HNil

import org.scalatest.Spec
import org.scalatest.prop.Checkers

import org.scalacheck.Shapeless._

class QuickStartSpec extends Spec with Checkers {

  import FetchTutorialHelper._
  import UsageSection._
  import Test._

  def `Creating And Running` =
    check(testSuccess(creatingAndRunning _, userDatabase(1) :: HNil))

  def `Sequencing Strategy` =
    check(testSuccess(sequencing _, (userDatabase(1), userDatabase(2)) :: 2 :: HNil))

  def `Batching Strategy` =
    check(testSuccess(batching _, (userDatabase(1), userDatabase(2)) :: 1 :: HNil))

  def `Deduplication Strategy` = 
    check(testSuccess(deduplication _, (userDatabase(1), userDatabase(1)) :: 1 :: HNil))

  def `Caching Strategy` = 
    check(testSuccess(caching _, (userDatabase(1), userDatabase(1)) :: 1 :: HNil))


}
