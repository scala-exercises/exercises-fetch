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

import org.scalacheck.ScalacheckShapeless._
import org.scalaexercises.Test.testSuccess
import org.scalatestplus.scalacheck.Checkers
import org.scalatest.refspec.RefSpec
import shapeless.HNil

class BatchingSpec extends RefSpec with Checkers {

  def `Batching Maximum batch size`(): Unit =
    check(testSuccess(BatchingSection.maximumSize _, 4 :: HNil))

  def `Batching Batch execution strategy`(): Unit =
    check(testSuccess(BatchingSection.executionStrategy _, 4 :: HNil))

}
