/*
 * scala-exercises - exercises-fetch
 * Copyright (C) 2015-2016 47 Degrees, LLC. <http://www.47deg.com>
 */

package fetchlib

import org.scalaexercises.definitions.Library

/** Fetch is a library for making access to data both simple & efficient.
 *
 * @param name fetch
 */
object FetchLibrary extends Library {
  override def logoPath = "fetch"

  override def owner = "scala-exercises"

  override def repository = "exercises-fetch"

  override def color = Some("#2F2859")

  override def sections =
    List(
      UsageSection,
      CachingSection,
      BatchingSection,
      ErrorHandlingSection,
      SyntaxSection,
      CatsSection,
      ConcurrencyMonadsSection
    )

}
