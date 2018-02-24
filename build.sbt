name := "dl"
resolvers ++= Seq(
  Resolver.jcenterRepo,
  "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/",
  "scala-integration" at "https://scala-ci.typesafe.com/artifactory/scala-integration/")

//scalaVersion := "2.12.5-bin-3995c7e"
scalaVersion := "2.12.4"
cancelable in Global := true

lazy val shared = (crossProject.crossType(CrossType.Pure) in file("shared"))
  .settings(
    cancelable := true,
    libraryDependencies ++= Seq(
      "com.lihaoyi" %%% "upickle" % "0.4.4",
      "com.lihaoyi" %%% "autowire" % "0.2.6",
      "io.suzaku" %%% "boopickle" % "1.2.6",
      "com.lihaoyi" %%% "scalatags" % "0.6.7"
    )
  )
//ugly thing,the val name must equal the string,and can not be put inline!!


lazy val sharedJS = shared.js.settings(name := "sharedJS")
lazy val js: Project = (project in file("js"))
  .dependsOn(sharedJS)
  .settings(
    npmDependencies in Compile ++= Seq(
    ),
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "0.9.1",
      "com.raquo" %%% "laminar" % "0.2",
      "com.raquo.xstream" %%% "xstream" % "0.3.1",
      "com.raquo" %%% "dombuilder" % "0.5",
    ),
    webpackBundlingMode := BundlingMode.LibraryAndApplication(),
    scalaJSUseMainModuleInitializer := true,
    emitSourceMaps in fastOptJS := true,
    emitSourceMaps in fullOptJS := false
  )
  .enablePlugins(ScalaJSPlugin, ScalaJSBundlerPlugin, ScalaJSWeb) //ScalaJSWeb is for sourcemap


lazy val sharedJVM = shared.jvm.settings(name := "sharedJVM")
lazy val jvm = (project in file("jvm"))
  .settings(
    scalaJSProjects := Seq(js), //for sourcemap
    pipelineStages in Assets := Seq(scalaJSPipeline), //for sourcemap
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http" % "10.0.11",
      "com.typesafe.slick" %% "slick" % "3.2.1",
      "org.slf4j" % "slf4j-simple" % "1.8.0-beta0",
      //"org.slf4j" % "slf4j-nop" % "1.6.4",
      "com.typesafe.slick" %% "slick-hikaricp" % "3.2.1",
      "org.xerial" % "sqlite-jdbc" % "3.7.2",
      "org.deeplearning4j" % "deeplearning4j-core" % "0.9.1",
      "org.nd4j" % "nd4j-cuda-8.0-platform" % "0.9.1",
      "org.nd4j" % "nd4j-cuda-8.0" % "0.9.1"
      /*"io.github.nafg" %% "slick-migration-api" % "0.4.2",*/
      /*"org.scorexfoundation" %% "scrypto" % "2.0.0"*/
    )
  )
  .dependsOn(sharedJVM)
  .enablePlugins(SbtWeb) //for sourcemap JavaAppPackaging

val startDev = Command.command("startDev") { st: State =>
  st
}


