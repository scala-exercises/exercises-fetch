/*
 * Copyright 2016-2020 47 Degrees Open Source <https://www.47deg.com>
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

class ErrorHandlingSpec extends RefSpec with Checkers {

  def `Exception`(): Unit =
    check(testSuccess(ErrorHandlingSection.catsEval _, true :: HNil))

  def `Debug`(): Unit =
    check(testSuccess(ErrorHandlingSection.debugDescribe _, true :: HNil))

  def `Missing`(): Unit =
    check(testSuccess(ErrorHandlingSection.missing _, true :: HNil))

  def `Missing identity`(): Unit =
    check(testSuccess(ErrorHandlingSection.missingIdentity _, "Users" :: 5 :: HNil))

}
