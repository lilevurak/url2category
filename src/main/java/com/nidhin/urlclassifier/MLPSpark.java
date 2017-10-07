package com.nidhin.urlclassifier;

import org.apache.spark.ml.classification.MultilayerPerceptronClassificationModel;
import org.apache.spark.ml.classification.MultilayerPerceptronClassifier;
import org.apache.spark.ml.evaluation.MulticlassClassificationEvaluator;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

/**
 * Created by nidhin on 26/7/17.
 */
public class MLPSpark {
    public void runMLP() throws IOException {
        // Load training data
        String path = "/home/nidhin/Confidential/blueprints/RnD/jump-url-classifier/mlp-libsvm-small.csv";
        SparkSession spark = SparkSession
                .builder()
                .master("local[4]")
                .appName("JavaMultilayerPerceptronClassifierExample")
                .getOrCreate();

        Dataset<Row> dataFrame = spark.read().format("libsvm").load(path);

// Split the data into train and test
        Dataset<Row>[] splits = dataFrame.randomSplit(new double[]{0.8, 0.2}, 1234L);
        Dataset<Row> train = splits[0];
//        Dataset<Row> train = dataFrame;
        Dataset<Row> test = splits[1];

// specify layers for the neural network:
// input layer of size 4 (features), two intermediate of size 5 and 4
// and output of size 3 (classes)
//        int[] layers = new int[] {100, 150, 120, 100, 40, 16};//0.107 10clust
//        int[] layers = new int[] {100, 150, 120, 100, 60, 16};//0.104 "
//        int[] layers = new int[] {100, 200, 180, 120, 80, 40, 16};//0.12611 "
//        int[] layers = new int[] {100, 300, 240, 200, 160, 120, 90, 60, 40, 16};//0.075
//        int[] layers = new int[] {100, 200, 180, 120, 80, 40, 16};//0.1138 50
//        int[] layers = new int[] {100, 200, 180, 120, 100, 80, 55, 40, 16};//0.1138 50
            int[] layers = new int[]{100, 150, 120, 80 ,40, 16};


// create the trainer and set its parameters
        MultilayerPerceptronClassifier trainer = new MultilayerPerceptronClassifier()
                .setLayers(layers)
                .setBlockSize(64)
                .setSeed(1234L)
                .setMaxIter(100);

// train the model
        MultilayerPerceptronClassificationModel model = trainer.fit(train);

// compute accuracy on the test set
        Dataset<Row> result = model.transform(test);
        Dataset<Row> predictionAndLabels = result.select("prediction", "label");
        MulticlassClassificationEvaluator evaluator = new MulticlassClassificationEvaluator()
                .setMetricName("accuracy");

        System.out.println("\n\n\nTest set accuracy = " + evaluator.evaluate(predictionAndLabels) + "\n\n\n");

        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("spark-mlp.ser"));
        oos.writeObject(model);
        oos.flush();
        oos.close();
    }

    public static void main(String[] args) throws IOException {
        new MLPSpark().runMLP();
    }
}
