package com.nidhin.urlclassifier;

import org.deeplearning4j.eval.Evaluation;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

/**
 * Created by nidhin on 25/7/17.
 */
public class MLPController {
    int seed = 1234;
    double learningRate = 0.001;
    int batchSize = 32;
    int printIterations = 100;
    int nEpochs = 100;

    int numInputs = 100;
    int numOutputs = 16;
    int numHiddenNodes1 = 75;
    int numHiddenNodes2 = 50;
    int numHiddenNodes3 = 32;

    MultiLayerNetwork mlp;


    public void createMLPNetwork(){
//        Nd4j.ENFORCE_NUMERICAL_STABILITY = true;


        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
//                .seed(seed) //include a random seed for reproducibility
                // use stochastic gradient descent as an optimization algorithm
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .iterations(1)
                .learningRate(learningRate) //specify the learning rate
//                .updater(Updater.NESTEROVS).momentum(0.9) //specify the rate of change of the learning rate.
                .regularization(true).l2(1e-4)
                .updater(Updater.NESTEROVS).momentum(0.9)
                .list()
                .layer(0, new DenseLayer.Builder() //create the first, input layer with xavier initialization
                        .nIn(numInputs)
                        .nOut(150)
                        .activation(Activation.RELU)
                        .weightInit(WeightInit.XAVIER)
                        .build())
                .layer(1, new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD) //create hidden layer
                        .nIn(150)
                        .nOut(numOutputs)
                        .activation(Activation.SOFTMAX)
                        .weightInit(WeightInit.XAVIER)
                        .build())
                .pretrain(false).backprop(true) //use backpropagation to adjust weights
                .build();


        mlp = new MultiLayerNetwork(conf);
    }

    public void trainMLP(String aspirationClustersMapSerFile) throws IOException, ClassNotFoundException {



        MLPDataSetIterator mlpDataSetIterator = new MLPDataSetIterator(batchSize, 100, aspirationClustersMapSerFile, true);
        MLPDataSetIterator mlpTestDataSetIterator = new MLPDataSetIterator(batchSize, 100, aspirationClustersMapSerFile, false);

        mlp.init();
//        UIServer uiServer = UIServer.getInstance();
//
//        //Configure where the network information (gradients, score vs. time etc) is to be stored. Here: store in memory.
//        StatsStorage statsStorage = new FileStatsStorage(new File("stats-store"));         //Alternative: new FileStatsStorage(File), for saving and loading later
//
//        //Attach the StatsStorage instance to the UI: this allows the contents of the StatsStorage to be visualized
//        uiServer.attach(statsStorage);

        //Then add the StatsListener to collect this information from the network, as it trains
        mlp.setListeners(new ScoreIterationListener(printIterations));
        for (int i=0; i< nEpochs; i++){
            mlp.fit(mlpDataSetIterator);
            mlpDataSetIterator.reset();
            System.out.println("Epoch #" + i +" over.");
//            serializeNetwork("aspirationsMLP.ser");

            Evaluation evaluation = new Evaluation();
            while (mlpTestDataSetIterator.hasNext()){
                DataSet t = mlpTestDataSetIterator.next();
                  INDArray features = t.getFeatureMatrix();
                INDArray lables = t.getLabels();
                INDArray inMask = t.getFeaturesMaskArray();
                INDArray outMask = t.getLabelsMaskArray();
                INDArray predicted = mlp.output(features,false,inMask,outMask);

                evaluation.eval(lables,predicted);
            }
            mlpTestDataSetIterator.reset();
            System.out.println(evaluation.stats());



        }
    }

    public void serializeNetwork(String filePath) throws IOException {
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath));
        oos.writeObject(mlp);
        oos.flush();
        oos.close();
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        MLPController mlpController = new MLPController();
        mlpController.createMLPNetwork();
        mlpController.trainMLP("aspirationClustersMap.ser");
        mlpController.serializeNetwork("aspirationsMLP.ser");
    }


}
