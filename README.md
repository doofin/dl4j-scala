# dl4j-scala
Deep learning in scala with dl4j and cuda. Currenly contains a CNN example for mnist data set

# run
Assume you have installed sbt,and have mnist data set under `./mnist_png/training/` ,which contains 10 sub folder with 10 classes of handwriting number,which contains individual image file like png.This can be downloaded from https://github.com/myleott/mnist_png

then

    sbt 
    jvm/run

# why build.sbt is so complex?
    This project is a js cross project, I plan to do some visualization via scala.js in the future
