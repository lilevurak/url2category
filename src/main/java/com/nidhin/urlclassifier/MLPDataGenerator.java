package com.nidhin.urlclassifier;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.spark.mllib.linalg.Vector;

import java.io.*;
import java.util.*;

/**
 * Created by nidhin on 25/7/17.
 */
public class MLPDataGenerator {
    HashMap<String, ArrayList<String>> urlKeywordsMap = new HashMap<>();
    HashMap<String, ArrayList<String>> urlMetaKeywordsMap = new HashMap<>();
    HashMap<String, ArrayList<String>> aspirationUrlsMap = new HashMap<>();


    public void loadMaps() throws IOException, ClassNotFoundException {
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream("urlKeywordsMap.ser"));
        urlKeywordsMap = (HashMap<String, ArrayList<String>>) ois.readObject();
        ois.close();
        ois = new ObjectInputStream(new FileInputStream("urlMetaKeywordsMap.ser"));
        urlMetaKeywordsMap = (HashMap<String, ArrayList<String>>) ois.readObject();
        ois.close();
        ois = new ObjectInputStream(new FileInputStream("aspirationUrlsMap.ser"));
        aspirationUrlsMap = (HashMap<String, ArrayList<String>>) ois.readObject();
        ois.close();
    }

    public void generateAspirationClusters() throws IOException {
        Word2Vec word2Vec = new Word2Vec();
        word2Vec.loadModel("en", new File("/home/nidhin/Confidential/playground/word2vec/en.ser"));

        HashMap<String, HashSet<double[]>> aspirationClustersMap = new HashMap<>();
        int totalCluster = 0;
        for (Map.Entry<String, ArrayList<String>> aspirationUrlsEntry : aspirationUrlsMap.entrySet()){
            String aspiration = aspirationUrlsEntry.getKey();
            aspirationClustersMap.put(aspiration, new HashSet<>());
            ArrayList<String> urls = aspirationUrlsEntry.getValue();

            for (String url : urls){
                if (url.endsWith("pdf") || url.endsWith("PDF"))
                    continue;
                HashSet<String> allKeyWords = new HashSet<>();
                if (urlKeywordsMap.get(url) != null)
                allKeyWords.addAll(urlKeywordsMap.get(url));
                if (urlMetaKeywordsMap.get(url) != null)
                allKeyWords.addAll(urlMetaKeywordsMap.get(url));
                HashMap<String, double[]> wordVecMap = new HashMap<>();
                for (String keyword : allKeyWords){
                    Vector keyVec = word2Vec.getVectorForNGram("en", keyword);
                    if (keyVec != null){
                        wordVecMap.put(keyword, keyVec.toArray());
                    }
                }
                if (wordVecMap.isEmpty()){
                    continue;
                }
                WordKmeans wordKmeans = new WordKmeans(wordVecMap, 50, 100);
                WordKmeans.Classes[] clusters = wordKmeans.explain();
                totalCluster += clusters.length;
                for (int k = 0; k < clusters.length; k++){
                    aspirationClustersMap.get(aspiration).add(clusters[k].getCenter());
                }
            }
        }
        System.out.println("total clusters - "+ totalCluster);
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("aspirationClustersMap-small.ser"));
        oos.writeObject(aspirationClustersMap);
        oos.flush();
        oos.close();
    }

    public void convertAspirationClusterToLibSVMFormat() throws IOException, ClassNotFoundException {
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream("aspirationClustersMap-top15perpage-google.ser"));
        HashMap<String, HashSet<double[]>> aspirationClustersMap = (HashMap<String, HashSet<double[]>>) ois.readObject();
        List<String> sortedAspirations = new ArrayList<>(aspirationClustersMap.keySet());
        Collections.sort(sortedAspirations);
        CSVPrinter csvPrinter= new CSVPrinter(new FileWriter("mlp-libsvm-google.csv"), CSVFormat.DEFAULT.withDelimiter(' '));
        List<List<String>> records= new ArrayList<>();
        int totalCluster = 0, skip =0;
        for (String aspiration : sortedAspirations){
            HashSet<double[]> clusters = aspirationClustersMap.get(aspiration);
            String labelIndex = String.valueOf(sortedAspirations.indexOf(aspiration));
            totalCluster += clusters.size();
            System.out.println("label - " + labelIndex + ": clusters - " + clusters.size());
            for (double[] cluster : clusters){
                boolean skipCluster = false;
                List<String> record = new ArrayList<>();
                record.add(labelIndex);
                for (int i =0; i< cluster.length; i++){
                    if (String.valueOf(cluster[i]).equals("NaN")){
                        skip++;
                        skipCluster = true;
                        break;
                    }
                    String clusterDataPoint = String.format("%d:%f", i+1, cluster[i]);
                    record.add(clusterDataPoint);
                }
                if (skipCluster)
                    continue;
                records.add(record);
            }
        }
        System.out.println("total -" + totalCluster + " skip - " + skip);
        Collections.shuffle(records);
        for (List<String> record : records){
            csvPrinter.printRecord(record);
//            csvPrinter.println();
        }
        csvPrinter.flush();
        csvPrinter.close();

    }

    public void convertAspirationClusterToCSV() throws IOException, ClassNotFoundException {
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream("aspirationClustersMap-top10perpage-google.ser"));
        HashMap<String, HashSet<double[]>> aspirationClustersMap = (HashMap<String, HashSet<double[]>>) ois.readObject();
        List<String> sortedAspirations = new ArrayList<>(aspirationClustersMap.keySet());
        Collections.sort(sortedAspirations);
        CSVPrinter csvTrainPrinter= new CSVPrinter(new FileWriter("mlp-large-top10-google-train.csv"), CSVFormat.DEFAULT.withDelimiter(','));
        CSVPrinter csvEvalPrinter= new CSVPrinter(new FileWriter("mlp-large-top10-google-eval.csv"), CSVFormat.DEFAULT.withDelimiter(','));

        List<List<String>> records= new ArrayList<>();
        List<List<String>> trainRecords= new ArrayList<>();
        List<List<String>> evalRecords= new ArrayList<>();
        int totalCluster = 0, skip = 0;
        for (String aspiration : sortedAspirations){
            HashSet<double[]> clusters = aspirationClustersMap.get(aspiration);
            String labelIndex = String.valueOf(sortedAspirations.indexOf(aspiration));
            totalCluster += clusters.size();
            System.out.println("label - " + labelIndex + ": clusters - " + clusters.size());
            for (double[] cluster : clusters){
                boolean skipCluster = false;

                List<String> record = new ArrayList<>();
                record.add(labelIndex);
                for (int i =0; i< cluster.length; i++){
                    String clusterDataPoint = String.valueOf(cluster[i]);
                    if (clusterDataPoint.equals("NaN")){
                        skipCluster = true;
                        skip ++;
                        break;
                    }
                    record.add(clusterDataPoint);
                }
                if (skipCluster){
                    continue;
                }
                records.add(record);
            }
        }
        System.out.println("total -" + totalCluster + " skip - " + skip);
        Collections.shuffle(records);
        int splitIndex = (int) Math.abs(records.size() * 0.8);
        trainRecords = records.subList(0, splitIndex);
        evalRecords = records.subList(splitIndex, records.size());
        for (List<String> record : trainRecords){
            csvTrainPrinter.printRecord(record);
//            csvPrinter.println();
        }
        csvTrainPrinter.flush();
        csvTrainPrinter.close();

        for (List<String> record : evalRecords){
            csvEvalPrinter.printRecord(record);
//            csvPrinter.println();
        }
        csvEvalPrinter.flush();
        csvEvalPrinter.close();

    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        MLPDataGenerator mlpDataGenerator = new MLPDataGenerator();
//        mlpDataGenerator.loadMaps();
//        mlpDataGenerator.generateAspirationClusters();
//        mlpDataGenerator.convertAspirationClusterToLibSVMFormat();
        mlpDataGenerator.convertAspirationClusterToCSV();
    }
}
