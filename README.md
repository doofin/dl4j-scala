# dl4j-scala
Deep learning in scala with dl4j and cuda. Currenly contains a CNN example for mnist data set

# run
Assume you have installed sbt,and have mnist data set under `./mnist_png/training/` ,which contains 10 sub folder with 10 classes of handwriting number,which contains individual image file like png.This can be downloaded from https://github.com/myleott/mnist_png

then

    sbt 
    jvm/run
# Abstractions for deep learning:computational graph
why computational graph : many framework emphasis on layer based abstraction to provide a high level api ,which is enough for forward nn ,but that's not general enough to allow recurrent nn to be defined.

introduction : http://www.cs.cornell.edu/courses/cs5740/2017sp/lectures/04-nn-compgraph.pdf

dl4j api for that : https://deeplearning4j.org/compgraph
    
# why build.sbt is so complex?
    This project is a js cross project, I plan to do some visualization via scala.js in the future

# multi dimension lstm (mdlstm)

papers

Multi-Dimensional Recurrent Neural Networks : https://arxiv.org/pdf/0705.2011.pdf

handwriting : http://papers.nips.cc/paper/3449-offline-handwriting-recognition-with-multidimensional-recurrent-neural-networks.pdf

handwriting ,improved http://ieeexplore.ieee.org/stamp/stamp.jsp?tp=&arnumber=7814068

benchmarks : https://github.com/jpuigcerver/rnn2d/wiki/LSTM-2D

intro : 
CTC : https://gab41.lab41.org/speech-recognition-you-down-with-ctc-8d3b558943f0
