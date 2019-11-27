import ProjectPlugin.autoImport._

val scalaExercisesV = "0.5.0-SNAPSHOT"

def dep(artifactId: String) = "org.scala-exercises" %% artifactId % scalaExercisesV

lazy val fetch = (project in file("."))
  .enablePlugins(ExerciseCompilerPlugin)
  .settings(
    name := "exercises-fetch",
    libraryDependencies ++= Seq(
      dep("exercise-compiler"),
      dep("definitions"),
      %%("cats-core", V.cats),
      %%("cats-effect", V.cats),
      %%("fetch", V.fetch),
      %%("fetch-debug", V.fetch),
      %%("shapeless", V.shapeless),
      %%("scalatest", V.scalatest),
      %%("scalacheck", V.scalacheck),
      "com.github.alexarchambault" %% "scalacheck-shapeless_1.14" % V.scalacheckShapeless
    )
  )

// Distribution

pgpPassphrase := Some(getEnvVar("PGP_PASSPHRASE").getOrElse("").toCharArray)
pgpPublicRing := file(s"$gpgFolder/pubring.gpg")
pgpSecretRing := file(s"$gpgFolder/secring.gpg")
