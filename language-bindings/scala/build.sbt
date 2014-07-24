name := "rx-redis-scala"

libraryDependencies ++= List(
  "com.netflix.rxjava" % "rxjava-scala" % "0.19.1" exclude("org.scala-lang", "scala-library")
)
