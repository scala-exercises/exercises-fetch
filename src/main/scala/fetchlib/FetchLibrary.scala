package fetchlib

import org.scalaexercises.definitions._

/** Fetch is a library for making access to data both simple & efficient. 
  *
  * @param name fetch
  */
object FetchLibrary extends Library {
  override def owner = "scala-exercises"
  override def repository = "exercises-fetch"

  override def color = Some("#2F2859")

  override def sections = List(
    UsageSection,
    CachingSection,
    ErrorHandlingSection,
    SyntaxSection,
    CatsSection,
    ConcurrencyMonadsSection
  )
}
