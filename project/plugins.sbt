resolvers ++= Seq(
  "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/")

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.22")
addSbtPlugin("ch.epfl.scala" % "sbt-scalajs-bundler" % "0.9.0")
addSbtPlugin("com.vmunier" % "sbt-web-scalajs" % "1.0.6")
/*addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.6")*/
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.3.2")