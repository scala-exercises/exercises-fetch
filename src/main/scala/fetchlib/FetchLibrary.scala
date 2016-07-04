package fetchlib

/** Fetch is a library for making access to data both simple & efficient. 
  *
  * @param name fetch
  */
object FetchLibrary extends exercise.Library {
  override def owner = "scala-exercises"
  override def repository = "exercises-fetch"

  override def color = Some("#2F2859")

  override def sections = List(
    UsageSection,
    CachingSection
  )
}
