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

import fetchlib.FetchTutorialHelper.userDatabase
import org.scalacheck.ScalacheckShapeless._
import org.scalaexercises.Test.testSuccess
import org.scalatest.refspec.RefSpec
import org.scalatestplus.scalacheck.Checkers
import shapeless.HNil

class CachingSpec extends RefSpec with Checkers {

  def `Cache Prepopulating`(): Unit =
    check(testSuccess(CachingSection.prepopulating _, 1 :: "@one" :: HNil))

  def `Cache Partial Hits`(): Unit =
    check(testSuccess(CachingSection.cachePartialHits _, "@one" :: "@dialelo" :: HNil))

  def `Cache Replay`(): Unit =
    check(testSuccess(CachingSection.replaying _, 3 :: 3 :: HNil))

  def `Cache Custom`(): Unit =
    check(testSuccess(CachingSection.customCache _, userDatabase(1) :: HNil))
}
