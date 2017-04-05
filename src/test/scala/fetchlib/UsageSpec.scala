/*
 * scala-exercises - exercises-fetch
 * Copyright (C) 2015-2016 47 Degrees, LLC. <http://www.47deg.com>
 */

package exercises

import org.scalaexercises.Test
import org.scalatest.Spec
import org.scalatest.prop.Checkers
import shapeless.HNil

class UsageSpec extends Spec with Checkers {

  import Test._
  import fetchlib.UsageSection._
  import fetchlib.FetchTutorialHelper._

  def `Creating And Running` =
    check(testSuccess(creatingAndRunning _, userDatabase(1) :: HNil))

  def `Sequencing Strategy` =
    check(testSuccess(sequencing _, (userDatabase(1), userDatabase(2)) :: HNil))

  def `Batching Strategy` =
    check(testSuccess(batching _, (userDatabase(1), userDatabase(2)) :: HNil))

  def `Deduplication Strategy` =
    check(testSuccess(deduplication _, (userDatabase(1), userDatabase(1)) :: HNil))

  def `Caching Strategy` =
    check(testSuccess(caching _, (userDatabase(1), userDatabase(1)) :: HNil))

  def `Sync Queries` =
    check(testSuccess(synchronous _, "Computing 42" :: HNil))

  def `Cats Sync Queries` =
    check(testSuccess(catsSynchronous _, "Computing 42" :: HNil))

  def `Combining Data` =
    check(testSuccess(combiningData _, (postDatabase(1), "applicative") :: HNil))

  def `Combining Concurrency` =
    check(testSuccess(concurrency _, (postDatabase(1), userDatabase(2)) :: HNil))

  def `Combinators sequence` =
    check(testSuccess(sequence _, List(userDatabase(1), userDatabase(2), userDatabase(3)) :: HNil))

  def `Combinators traverse` =
    check(testSuccess(traverse _, List(userDatabase(1), userDatabase(2), userDatabase(3)) :: HNil))

}
