package com.nidhin.urlclassifier;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.DataSetPreProcessor;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.factory.Nd4j;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.*;

/**
 * Created by nidhin on 25/7/17.
 */
public class MLPDataSetIterator implements DataSetIterator {
    private int batchSize;
    private int vectorSize;
    private int totalExamples;
    private int cursor = 0;
    private List<String> aspirationLabels;
    private HashMap<String, HashSet<double[]>> aspirationClusters = new HashMap<>();
    private ArrayList<double[]> completeClusterDS = new ArrayList<>();
    private ArrayList<Integer> completeLabelIndex = new ArrayList<>();
    private boolean train;

    public MLPDataSetIterator(int batchSize, int vectorSize, String aspirationClustersMapSerFile, boolean train) throws IOException, ClassNotFoundException {
        this.batchSize = batchSize;
        this.vectorSize = vectorSize;
        ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(aspirationClustersMapSerFile));
        aspirationClusters = (HashMap<String, HashSet<double[]>>) objectInputStream.readObject();
        this.aspirationLabels = new ArrayList<>(aspirationClusters.keySet());
        for (String label: this.aspirationLabels){
            for (double[] cluster : aspirationClusters.get(label)){
                boolean skipCluster = false;
                for (int j =0; j< cluster.length; j++){
                    if (String.valueOf(cluster[j]).equals("NaN")){
                        skipCluster = true;
                        break;
                    }
                }
                if (!skipCluster) {
                    this.completeClusterDS.add(cluster);
                    this.completeLabelIndex.add(this.aspirationLabels.indexOf(label));
                }
            }
        }
        this.train = train;
        if (train) {
//            Random random = new Random(123);
//            Collections.shuffle(this.completeClusterDS, random);
//            Collections.shuffle(this.completeLabelIndex, random);
        }
        else {
            Random random = new Random(123);
            Collections.shuffle(this.completeClusterDS, random);
            Collections.shuffle(this.completeLabelIndex, random);
            this.completeLabelIndex = new ArrayList<>(this.completeLabelIndex.subList(0, (int) Math.abs(this.completeLabelIndex.size() * 0.3)));
            this.completeClusterDS = new ArrayList<>(this.completeClusterDS.subList(0, (int) Math.abs(this.completeClusterDS.size() * 0.3)));
        }
        this.totalExamples = this.completeClusterDS.size();

    }


    @Override
    public DataSet next(int i) {
        if (cursor >= 260 && cursor <= 270){
            System.out.println(cursor);
        }
        if (cursor > totalExamples) throw new NoSuchElementException();
        return nextDS(i);
    }

    private DataSet nextDS(int i){
        int startIndex = cursor;
        int endIndex = Math.min(cursor + i, totalExamples);
        List<double []> clusters = completeClusterDS.subList(startIndex, endIndex);
        List<Integer> labelIndexes = completeLabelIndex.subList(startIndex, endIndex);
        cursor += i;

        INDArray features = Nd4j.create(clusters.size(), vectorSize);
        INDArray labels = Nd4j.zeros(clusters.size(), aspirationLabels.size());

        for (int j = 0; j< clusters.size(); j++){

            features.putRow(j, Nd4j.create(clusters.get(j)));
            labels.putScalar(j, labelIndexes.get(j), 1.0);
        }
        return new DataSet(features, labels);
    }

    public DataSet getPrevDS(){
        int startIndex = cursor - batchSize;
        int endIndex = Math.min(cursor, totalExamples);
        List<double []> clusters = completeClusterDS.subList(startIndex, endIndex);
        List<Integer> labelIndexes = completeLabelIndex.subList(startIndex, endIndex);
//        cursor += i;

        INDArray features = Nd4j.create(clusters.size(), vectorSize);
        INDArray labels = Nd4j.zeros(clusters.size(), aspirationLabels.size());

        for (int j = 0; j< clusters.size(); j++){

            features.putRow(j, Nd4j.create(clusters.get(j)));
            labels.putScalar(j, labelIndexes.get(j), 1.0);
        }
        return new DataSet(features, labels);
    }

    @Override
    public int totalExamples() {
        return totalExamples;
    }

    @Override
    public int inputColumns() {
        return vectorSize;
    }

    @Override
    public int totalOutcomes() {
        return aspirationLabels.size();
    }

    @Override
    public boolean resetSupported() {
        return false;
    }

    @Override
    public boolean asyncSupported() {
        return false;
    }

    @Override
    public void reset() {

        cursor = 0;
    }

    @Override
    public int batch() {
        return batchSize;
    }

    @Override
    public int cursor() {
//        System.out.println("cursor - " + cursor);
        return cursor;
    }

    @Override
    public int numExamples() {
        return totalExamples();
    }

    @Override
    public void setPreProcessor(DataSetPreProcessor dataSetPreProcessor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DataSetPreProcessor getPreProcessor() {
        return null;
    }


    @Override
    public List<String> getLabels() {
        return aspirationLabels;
    }

    @Override
    public boolean hasNext() {
        boolean hasNext = cursor < totalExamples;
//        System.out.println("has next - " + hasNext);
        return hasNext;
    }

    @Override
    public DataSet next() {
        return next(batchSize);
    }
}
