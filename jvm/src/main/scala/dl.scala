import akka.actor.ActorSystem
import org.deeplearning4j.datasets.iterator.impl.MnistDataSetIterator
import org.deeplearning4j.eval.Evaluation
import org.deeplearning4j.nn.api.Model
import org.deeplearning4j.nn.conf.inputs.InputType
import org.deeplearning4j.nn.conf.layers.ConvolutionLayer
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork
import org.deeplearning4j.optimize.api.IterationListener
import org.nd4j.linalg.activations.Activation
import org.nd4j.linalg.dataset.DataSet

import scala.collection.JavaConverters._

object dl {

  import org.deeplearning4j.nn.api.OptimizationAlgorithm
  import org.deeplearning4j.nn.conf.layers.{DenseLayer, OutputLayer, SubsamplingLayer}
  import org.deeplearning4j.nn.conf.{NeuralNetConfiguration, Updater}
  import org.deeplearning4j.nn.weights.WeightInit
  import org.nd4j.linalg.lossfunctions.LossFunctions.LossFunction
  val numberOfClasses=10
  implicit val sys=ActorSystem("stm")
  def lenet()={
    val nChannels = 1
    //val outputNum = 2 //

    // for GPU you usually want to have higher batchSize
    val batchSize = 128
    val nEpochs = 10
    val iterations = 1
    val seed = 123
    new NeuralNetConfiguration.Builder()
      .seed(seed)
      .iterations(iterations) // Training iterations as above
      .regularization(true).l2(0.0005)
      .learningRate(.01)//.biasLearningRate(0.02)
      .weightInit(WeightInit.XAVIER)
      .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
      .updater(Updater.NESTEROVS)//NESTEROVS
      .list()
      .layer(0, new ConvolutionLayer.Builder(5, 5)
        //nIn and nOut specify depth. nIn here is the nChannels and nOut is the number of filters to be applied
        .nIn(nChannels)
        .stride(1, 1)
        .nOut(20)
        .activation(Activation.IDENTITY) //id
        .build())
      .layer(1, new SubsamplingLayer.Builder(SubsamplingLayer.PoolingType.MAX) //max pool
        .kernelSize(2,2)
        .stride(2,2)
        .build())
      .layer(2, new ConvolutionLayer.Builder(5, 5)
        //Note that nIn need not be specified in later layers
        .stride(1, 1)
        .nOut(50)
        .activation(Activation.IDENTITY)//id
        .build())
      .layer(3, new SubsamplingLayer.Builder(SubsamplingLayer.PoolingType.MAX)
        .kernelSize(2,2)
        .stride(2,2)
        .build())
      .layer(4, new DenseLayer.Builder().activation(Activation.RELU)
        .nOut(500).build())
      .layer(5, new OutputLayer.Builder(LossFunction.NEGATIVELOGLIKELIHOOD)
        .nOut(numberOfClasses)
        .activation(Activation.SOFTMAX) //sf
        .build())
      .setInputType(InputType.convolutionalFlat(28,28,1)) //See note below
      .backprop(true).pretrain(false).build()
  }
  def lg(x: Any) = println(x)

  def dl_main(): Unit = {
    val mnistset=new MnistDataSetIterator(40,true,12345)// working!
    val mydataset=ImagePipelineExample.getfilesIter()
    val dtset: Iterator[DataSet] =mnistset.asScala
/*    mydataset.asScala.take(1).foreach{ds: DataSet =>
      //ds.normalize()
      val fm=ds.getFeatureMatrix.reshape(40,784).divi(256)
      lg(s"shape:${stringOf(fm.shape())},${fm.rank()}") //(40, 1, 28, 28),4 . 40 is batch size!
      lg(stringOf(fm))
      //lg(s"shape:${stringOf(fm.getRow(0).getRow(0).shape())},${fm.rank()}")//(28, 28),4
      //lg("means:"+ds.exampleMeans()+",")
    }
    mnistset.asScala.take(1).foreach{ds: DataSet =>
      val fm=ds.getFeatureMatrix
      lg(s"shape:${stringOf(fm.shape())},${fm.rank()}")
      lg(stringOf(fm))
      //lg("means:"+ds.exampleMeans()+",")
    }*/

    val mnn = new MultiLayerNetwork(lenet()) {
      init()
      setListeners(new IterationListener {
        override def invoke(): Unit = {
          lg("invoke")
        }
        override def iterationDone(model: Model, iteration: Int): Unit = {
          lg(s"$iteration th iteration,score:${model.score()}")
        }
        override def invoked(): Boolean = {
          lg("invoked")
          true
        }
      })

      1 to 1 foreach { i =>
        lg(s"start training...$i th epoches")
        dtset.foreach{d=>
          //d.normalize()
          //d.getLabelNames()
          //fit(d.getFeatureMatrix,d.getLabels)
          //fit(d.getFeatureMatrix,d.getLabels)
        }
        //fit(dtset)
      }

      lg("finished")
      //dtset.reset()
      lg(dtset.size+",dtset size")
      val ev=new Evaluation(numberOfClasses)
      ImagePipelineExample.getfilesIter().take(20).foreach{x=>
        val real=x.getLabels
        val pred=output(x.getFeatureMatrix)
        ev.eval(real,pred)
      }
      lg("eval:"+ev.stats())

    }
/*
    StdIn.readLine() // let it run until user presses return
    sys.terminate()
*/
  }


}

//adapted from dl4j
/**
  * Created by susaneraly on 6/9/16.
  */
object ImagePipelineExample { //Images are of format given by allowedExtension -
  import java.io.File
  import java.util.Random

  import org.datavec.api.io.filters.BalancedPathFilter
  import org.datavec.api.io.labels.ParentPathLabelGenerator
  import org.datavec.api.split.FileSplit
  import org.datavec.image.loader.BaseImageLoader
  import org.datavec.image.recordreader.ImageRecordReader
  import org.datavec.image.transform.MultiImageTransform
  import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator
  import org.nd4j.linalg.dataset.DataSet

  private val allowedExtensions = BaseImageLoader.ALLOWED_FORMATS
  private val seed = 12345
  private val randNumGen = new Random(seed)
  private val width = 28
  private val height = 28
  private val channels = 1

  //@throws[Exception]
  def getfilesIter(): Iterator[DataSet] = { //DIRECTORY STRUCTURE:
    //Images in the dataset have to be organized in directories by class/label.
    //In this example there are ten images in three classes
    //Here is the directory structure
    //                                    parentDir
    //                                  /    |     \
    //                                 /     |      \
    //                            labelA  labelB   labelC
    //
    //Set your data up like this so that labels from each label/class live in their own directory
    //And these label/class directories live together in the parent directory
    //
    //
    val parentDir = new File("./mnist_png/training/")
    //new ClassPathResource("DataExamples/ImagePipeline/").getFile
    //Files in directories under the parent dir that have "allowed extensions" split needs a random number generator for reproducibility when splitting the files into train and test
    val filesInDir = new FileSplit(parentDir, allowedExtensions, randNumGen)
    //You do not have to manually specify labels. This class (instantiated as below) will
    //parse the parent dir and use the name of the subdirectories as label/class names
    val labelMaker = new ParentPathLabelGenerator
    //The balanced path filter gives you fine tune control of the min/max cases to load for each class
    //Below is a bare bones version. Refer to javadoc for details
    val pathFilter = new BalancedPathFilter(randNumGen, allowedExtensions, labelMaker)
    //Split the image files into train and test. Specify the train test split as 80%,20%
    val filesInDirSplit = filesInDir.sample(pathFilter, 70, 30)
    //InputSplit testData = filesInDirSplit[1];  //The testData is never used in the example, commenting out.
    //Specifying a new record reader with the height and width you want the images to be resized to.
    //Note that the images in this example are all of different size
    //They will all be resized to the height and width specified below
    val recordReader = new ImageRecordReader(height, width, channels, labelMaker)
    //Often there is a need to transforming images to artificially increase the size of the dataset
    //DataVec has built in powerful features from OpenCV
    //You can chain transformations as shown below, write your own classes that will say detect a face and crop to size
    /*ImageTransform transform = new MultiImageTransform(randNumGen,
                new CropImageTransform(10), new FlipImageTransform(),
                new ScaleImageTransform(10), new WarpImageTransform(10));
                */
    //You can use the ShowImageTransform to view your images
    //Code below gives you a look before and after, for a side by side comparison
    val transform = new MultiImageTransform(randNumGen) //, new ShowImageTransform("Display - before "))
    //Initialize the record reader with the train data and the transform chain
    recordReader.initialize(filesInDirSplit(0), transform)
    val outputNum = recordReader.numLabels
    //convert the record reader to an iterator for training - Refer to other examples for how to use an iterator
    val batchSize = 40 // Minibatch size. Here: The number of images to fetch for each call to dataIter.next().
    val labelIndex = 1
    // Index of the label Writable (usually an IntWritable), as obtained by recordReader.next()
    // List<Writable> lw = recordReader.next();
    // then lw[0] =  NDArray shaped [1,3,50,50] (1, heightm width, channels)
    //      lw[0] =  label as integer.
    val dataIter: RecordReaderDataSetIterator = new RecordReaderDataSetIterator(recordReader, batchSize, labelIndex, outputNum)
    dataIter.asScala.take(900).map{d: DataSet =>
      new DataSet(d.getFeatureMatrix.reshape(40,784).divi(256),d.getLabels)
      //d
    }

  }

}

