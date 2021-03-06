package org.deeplearning4j.VideoFrameClassifier;

import org.apache.commons.io.FileUtils;
import org.datavec.api.conf.Configuration;
import org.datavec.api.records.reader.SequenceRecordReader;
import org.datavec.api.records.reader.impl.csv.CSVSequenceRecordReader;
import org.datavec.api.split.InputSplit;
import org.datavec.api.split.NumberedFileInputSplit;
import org.datavec.codec.reader.NativeCodecRecordReader;        //You need to modify pom.xml to include dependency (groupID "org.bytedeco", artifactID "javacv-platform") and (groupID "org.datavec", artifactID "datavec-data-codec")
import org.deeplearning4j.core.storage.StatsStorage;
import org.deeplearning4j.datasets.datavec.SequenceRecordReaderDataSetIterator;
import org.deeplearning4j.examples.utils.DownloaderUtility;     //You need the file inside /src/main/java/org/deeplearning4j/examples/utils/DownloaderUtility
//Notice the org.deeplearning4j is equal to org/deeplearning4j. examples.utils is equal to examples/utils
import org.deeplearning4j.nn.conf.BackpropType;
import org.deeplearning4j.nn.conf.GradientNormalization;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.*;
import org.deeplearning4j.nn.conf.preprocessor.CnnToFeedForwardPreProcessor;
import org.deeplearning4j.nn.conf.preprocessor.FeedForwardToRnnPreProcessor;
import org.deeplearning4j.nn.conf.preprocessor.RnnToCnnPreProcessor;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.deeplearning4j.ui.api.UIServer;
import org.deeplearning4j.ui.model.stats.StatsListener;
import org.deeplearning4j.ui.model.storage.InMemoryStatsStorage;
import org.nd4j.evaluation.classification.Evaluation;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.AsyncDataSetIterator;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.DataSetPreProcessor;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.AdaGrad;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 * Example: Combine convolutional, max pooling, dense (feed forward) and recurrent (LSTM) layers to classify each
 * frame of a video (using a generated/synthetic video data set)
 * Specifically, each video contains a shape (randomly selected: circles, squares, lines, arcs) which persist for
 * multiple frames (though move between frames) and may leave the frame. Each video contains multiple shapes which
 * are shown for some random number of frames.
 * The network needs to classify these shapes, even when the shape has left the frame.
 *
 * This example is somewhat contrived, but shows data import and network configuration for classifying video frames.
 * The data for this example is automatically downloaded to:
 * "~/dl4j-examples-data/dl4j-examples/video/videoshapesexample"
 * *******************************************************
 * @author Alex Black
 */
public class VideoFrameClassifierModified2 {

    public static final int N_VIDEOS = 20;     //155  TODO - this is just for test
    public static final int V_WIDTH = 130;
    public static final int V_HEIGHT = 130;
    public static final int V_NFRAMES = 361;    //TODO - this only works for the first video
    public static final int numLabels = 2;      //Only Accident or No Accident

    public static void main(String[] args) throws Exception {

        // Note that you will need to run with at least 7G off heap memory
        // if you want to keep this batchsize and train the nn config specified
        int miniBatchSize = 2;

        String dataDirectory = "src/main/resources/CarAccidentData/zProperData/";
        //String dataDirectory = DownloaderUtility.VIDEOEXAMPLE.Download() + "/videoshapesexample/";

        //Set up network architecture:
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(12345)
                .l2(0.001) //l2 regularization on all layers
                .updater(new AdaGrad(0.04))
                .list()
                .layer(new ConvolutionLayer.Builder(10, 10)
                        .nIn(3) //3 channels: RGB
                        .nOut(30)
                        .stride(4, 4)
                        .activation(Activation.RELU)
                        .weightInit(WeightInit.RELU)
                        .build())   //Output: (130-10+0)/4+1 = 31 -> 31*31*30
                .layer(new SubsamplingLayer.Builder(SubsamplingLayer.PoolingType.MAX)
                        .kernelSize(3, 3)
                        .stride(2, 2).build())   //(31-3+0)/2+1 = 15
                .layer(new ConvolutionLayer.Builder(3, 3)
                        .nIn(30)
                        .nOut(10)
                        .stride(2, 2)
                        .activation(Activation.RELU)
                        .weightInit(WeightInit.RELU)
                        .build())   //Output: (15-3+0)/2+1 = 7 -> 7*7*10 = 490
                .layer(new DenseLayer.Builder()
                        .activation(Activation.RELU)
                        .nIn(490)
                        .nOut(50)
                        .weightInit(WeightInit.RELU)
                        .gradientNormalization(GradientNormalization.ClipElementWiseAbsoluteValue)
                        .gradientNormalizationThreshold(10)
                        .updater(new AdaGrad(0.01))
                        .build())
                .layer(new LSTM.Builder()
                        .activation(Activation.TANH)
                        .nIn(50)
                        .nOut(50)
                        .weightInit(WeightInit.XAVIER)
                        .updater(new AdaGrad(0.008))
                        .gradientNormalization(GradientNormalization.ClipElementWiseAbsoluteValue)
                        .gradientNormalizationThreshold(10)
                        .build())
                .layer(new RnnOutputLayer.Builder(LossFunctions.LossFunction.MCXENT)
                        .activation(Activation.SOFTMAX)
                        .nIn(50)
                        .nOut(numLabels)    //2 possible condition: accident or not
                        .weightInit(WeightInit.XAVIER)
                        .gradientNormalization(GradientNormalization.ClipElementWiseAbsoluteValue)
                        .gradientNormalizationThreshold(10)
                        .build())
                .inputPreProcessor(0, new RnnToCnnPreProcessor(V_HEIGHT, V_WIDTH, 3))
                .inputPreProcessor(3, new CnnToFeedForwardPreProcessor(7, 7, 10))
                .inputPreProcessor(4, new FeedForwardToRnnPreProcessor())
                .backpropType(BackpropType.TruncatedBPTT)
                .tBPTTForwardLength(V_NFRAMES / 5)
                .tBPTTBackwardLength(V_NFRAMES / 5)
                .build();

        MultiLayerNetwork net = new MultiLayerNetwork(conf);
        net.init();

        // summary of layer and parameters
        System.out.println(net.summary());


        StatsStorage storage = new InMemoryStatsStorage();
        UIServer server = UIServer.getInstance();
        server.attach(storage);
        net.setListeners(new StatsListener(storage, 10));

        int testStartIdx = (int) (0.9 * N_VIDEOS);  //90% in train, 10% in test
        int nTest = N_VIDEOS - testStartIdx;

        //Conduct learning
        System.out.println("Starting training...");
        net.setListeners(new ScoreIterationListener(10));

        int nTrainEpochs = 15;
        for (int i = 0; i < nTrainEpochs; i++) {
            DataSetIterator trainData = getDataSetIterator(dataDirectory, 0, testStartIdx - 1, miniBatchSize);
            while(trainData.hasNext())
                net.fit(trainData.next());
            Nd4j.saveBinary(net.params(),new File("videomodel.bin"));
            FileUtils.writeStringToFile(new File("videoconf.json"), conf.toJson(), (Charset) null);
            System.out.println("Epoch " + i + " complete");

            //Evaluate classification performance:
            evaluatePerformance(net,testStartIdx,nTest,dataDirectory);
        }
    }


    private static void evaluatePerformance(MultiLayerNetwork net, int testStartIdx, int nExamples, String outputDirectory) throws Exception {
        //Assuming here that the full test data set doesn't fit in memory -> load 10 examples at a time
        Map<Integer, String> labelMap = new HashMap<>();
        labelMap.put(0, "NoAccident");
        labelMap.put(1, "Accident");
        Evaluation evaluation = new Evaluation(labelMap);

        DataSetIterator testData = getDataSetIterator(outputDirectory, testStartIdx, nExamples, 2);
        while(testData.hasNext()) {
            DataSet dsTest = testData.next();
            INDArray predicted = net.output(dsTest.getFeatures(), false);
            INDArray actual = dsTest.getLabels();
            evaluation.evalTimeSeries(actual, predicted);
        }

        System.out.println(evaluation.stats());
    }

    private static DataSetIterator getDataSetIterator(String dataDirectory, int startIdx, int nExamples, int miniBatchSize) throws Exception {
        //Here, our data and labels are in separate files
        //videos: shapes_0.mp4, shapes_1.mp4, etc
        //labels: shapes_0.txt, shapes_1.txt, etc. One time step per line

        SequenceRecordReader featuresTrain = getFeaturesReader(dataDirectory, startIdx, nExamples);
        SequenceRecordReader labelsTrain = getLabelsReader(dataDirectory, startIdx, nExamples);

        SequenceRecordReaderDataSetIterator sequenceIter =
                new SequenceRecordReaderDataSetIterator(featuresTrain, labelsTrain, miniBatchSize, numLabels, false);
        sequenceIter.setPreProcessor(new VideoPreProcessor());

        //AsyncDataSetIterator: Used to (pre-load) load data in a separate thread
        return new AsyncDataSetIterator(sequenceIter,1);
    }

    private static SequenceRecordReader getFeaturesReader(String path, int startIdx, int num) throws IOException, InterruptedException {
        //InputSplit is used here to define what the file paths look like
        InputSplit is = new NumberedFileInputSplit(path + "%d.mp4", startIdx, startIdx + num - 1);

        Configuration conf = new Configuration();
        conf.set(NativeCodecRecordReader.RAVEL, "true");
        conf.set(NativeCodecRecordReader.START_FRAME, "0");
        conf.set(NativeCodecRecordReader.TOTAL_FRAMES, String.valueOf(V_NFRAMES));
        conf.set(NativeCodecRecordReader.ROWS, String.valueOf(V_WIDTH));
        conf.set(NativeCodecRecordReader.COLUMNS, String.valueOf(V_HEIGHT));
        NativeCodecRecordReader crr = new NativeCodecRecordReader();
        crr.initialize(conf, is);
        return crr;

    }

    private static SequenceRecordReader getLabelsReader(String path, int startIdx, int num) throws Exception {
        InputSplit isLabels = new NumberedFileInputSplit(path + "%d.csv", startIdx, startIdx + num - 1);
        CSVSequenceRecordReader csvSeq = new CSVSequenceRecordReader();
        csvSeq.initialize(isLabels);
        return csvSeq;
    }

    private static class VideoPreProcessor implements DataSetPreProcessor {
        @Override
        public void preProcess(org.nd4j.linalg.dataset.api.DataSet toPreProcess) {
            toPreProcess.getFeatures().divi(255);  //[0,255] -> [0,1] for input pixel values
        }
    }
}
