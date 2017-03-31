val scalaExerciesV = "0.4.0-SNAPSHOT"

def dep(artifactId: String) = "org.scala-exercises" %% artifactId % scalaExerciesV

lazy val fetch = (project in file("."))
  .enablePlugins(ExerciseCompilerPlugin)
  .settings(
    name := "exercises-fetch",
    libraryDependencies ++= Seq(
      dep("exercise-compiler"),
      dep("definitions"),
      %%("fetch"),
      %%("fetch-monix"),
      %%("shapeless"),
      %%("scalatest"),
      %%("scalacheck"),
      %%("scheckShapeless")
    )
  )

// Distribution

pgpPassphrase := Some(getEnvVar("PGP_PASSPHRASE").getOrElse("").toCharArray)
pgpPublicRing := file(s"$gpgFolder/pubring.gpg")
pgpSecretRing := file(s"$gpgFolder/secring.gpg")
