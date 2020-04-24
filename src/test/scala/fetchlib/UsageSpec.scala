/*
 * Copyright 2016-2020 47 Degrees <https://47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fetchlib

import fetchlib.FetchTutorialHelper.{postDatabase, userDatabase}
import org.scalacheck.ScalacheckShapeless._
import org.scalaexercises.Test.testSuccess
import org.scalatestplus.scalacheck.Checkers
import org.scalatest.refspec.RefSpec
import shapeless.HNil

class UsageSpec extends RefSpec with Checkers {

  def `Creating And Running`(): Unit =
    check(testSuccess(UsageSection.creatingAndRunning _, userDatabase(1) :: HNil))

  def `Sequencing Strategy`(): Unit =
    check(testSuccess(UsageSection.sequencing _, (userDatabase(1), userDatabase(2)) :: HNil))

  def `Batching Strategy`(): Unit =
    check(testSuccess(UsageSection.batching _, (userDatabase(1), userDatabase(2)) :: HNil))

  def `Deduplication Strategy`(): Unit =
    check(testSuccess(UsageSection.deduplication _, (userDatabase(1), userDatabase(1)) :: HNil))

  def `Caching Strategy`(): Unit =
    check(testSuccess(UsageSection.caching _, (userDatabase(1), userDatabase(1)) :: HNil))

  def `Combining Data`(): Unit =
    check(testSuccess(UsageSection.combiningData _, (postDatabase(1), "applicative") :: HNil))

  def `Combinators sequence`(): Unit = {
    check(
      testSuccess(
        UsageSection.sequence _,
        List(userDatabase(1), userDatabase(2), userDatabase(3)) :: HNil
      )
    )
  }

  def `Combinators traverse`(): Unit = {
    check(
      testSuccess(
        UsageSection.traverse _,
        List(userDatabase(1), userDatabase(2), userDatabase(3)) :: HNil
      )
    )
  }

}
