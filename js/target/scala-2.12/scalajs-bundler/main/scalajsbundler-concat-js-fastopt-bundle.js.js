{
  var x66 = require("concat-with-sourcemaps");
  var x67 = require("fs");
  var x68 = new x66(true, "js-fastopt-bundle.js", ";\n");
  x68.add("", x67.readFileSync("/home/d/syncthing/code/scala/scalaCross/js/target/scala-2.12/scalajs-bundler/main/js-fastopt-library.js", "utf-8"), x67.readFileSync("/home/d/syncthing/code/scala/scalaCross/js/target/scala-2.12/scalajs-bundler/main/js-fastopt-library.js.map"));
  x68.add("js-fastopt-loader.js", x67.readFileSync("/home/d/syncthing/code/scala/scalaCross/js/target/scala-2.12/scalajs-bundler/main/js-fastopt-loader.js"));
  x68.add("", x67.readFileSync("/home/d/syncthing/code/scala/scalaCross/js/target/scala-2.12/scalajs-bundler/main/js-fastopt.js", "utf-8"), x67.readFileSync("/home/d/syncthing/code/scala/scalaCross/js/target/scala-2.12/scalajs-bundler/main/js-fastopt.js.map", "utf-8"));
  var x69 = new Buffer("\n//# sourceMappingURL=js-fastopt-bundle.js.map\n");
  var x70 = Buffer.concat([x68.content, x69]);
  x67.writeFileSync("/home/d/syncthing/code/scala/scalaCross/js/target/scala-2.12/scalajs-bundler/main/js-fastopt-bundle.js", x70);
  x67.writeFileSync("/home/d/syncthing/code/scala/scalaCross/js/target/scala-2.12/scalajs-bundler/main/js-fastopt-bundle.js.map", x68.sourceMap)
}