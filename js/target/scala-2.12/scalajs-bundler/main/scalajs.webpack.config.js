module.exports = {
  "entry": {
    "js-fastopt": ["/home/d/syncthing/code/scala/scalaCross/js/target/scala-2.12/scalajs-bundler/main/js-fastopt-entrypoint.js"]
  },
  "output": {
    "path": "/home/d/syncthing/code/scala/scalaCross/js/target/scala-2.12/scalajs-bundler/main",
    "filename": "[name]-library.js",
    "library": "ScalaJSBundlerLibrary",
    "libraryTarget": "var"
  },
  "devtool": "source-map",
  "module": {
    "rules": [{
      "test": new RegExp("\\.js$"),
      "enforce": "pre",
      "use": ["source-map-loader"]
    }]
  }
}