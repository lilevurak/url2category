package com.nidhin.urlclassifier;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by nidhin on 24/7/17.
 */
public class ClusterScorer {
    HashMap<String, ArrayList<String>> urlMetaKeywordsMap = new HashMap<>();
    HashMap<String, WordKmeans.Classes[]> urlClustersMap = new HashMap<>();
    HashMap<String, ArrayList<double[]>> urlMetaVectorsMap = new HashMap<>();
    HashMap<String, ArrayList<String>> aspirationUrlsMap = new HashMap<>();
    Word2Vecdl4j word2Vec = new Word2Vecdl4j();


    public void loadMaps() throws IOException, ClassNotFoundException {
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream("urlMetaKeywordsMap.ser"));
        urlMetaKeywordsMap = (HashMap<String, ArrayList<String>>) ois.readObject();
        ois.close();
        ois = new ObjectInputStream(new FileInputStream("urlClustersMap-google.ser"));
        urlClustersMap = (HashMap<String, WordKmeans.Classes[]>) ois.readObject();
        ois.close();
        ois = new ObjectInputStream(new FileInputStream("aspirationUrlsMap.ser"));
        aspirationUrlsMap = (HashMap<String, ArrayList<String>>) ois.readObject();
        ois.close();
    }

    public void loadWord2vec(){
        word2Vec.loadModel("en", new File("/home/ubuntu/aspirations-oct2nd/GoogleNews-vectors-negative300.bin"));
    }

    public void genMetaVectors(){
        for (Map.Entry<String, ArrayList<String>> urlMKeys : urlMetaKeywordsMap.entrySet()){
            String url = urlMKeys.getKey();
            ArrayList<String> keys = urlMKeys.getValue();
            List<double[]> mVecs = keys.stream().map(s -> word2Vec.getVectorForNGram("en", s))
                    .filter(Objects::nonNull).collect(Collectors.toList());
            if (mVecs != null && !mVecs.isEmpty())
                urlMetaVectorsMap.put(url, new ArrayList<>(mVecs));
        }
    }

    public void scoreClusters() throws IOException {
        HashMap<Double, Integer> scoreMap = new HashMap<>();
        int noClusterCount = 0;
        HashMap<String, Integer> finalAspUrlscountMap = new HashMap<>();
        HashMap<String, HashSet<double[]>> aspirationClusters = new HashMap<>();
        HashMap<String, HashSet<double[]>> urlClusters = new HashMap<>();

        for (Map.Entry<String, ArrayList<String>> aspirationUrls : aspirationUrlsMap.entrySet()) {

            String aspiration = aspirationUrls.getKey();
            aspirationClusters.put(aspiration, new HashSet<>());
            ArrayList<String> urls = aspirationUrls.getValue();

            for (String url : urls){
                urlClusters.put(url, new HashSet<>());
                WordKmeans.Classes[] clusters = urlClustersMap.get(url);
                ArrayList<double[]> metaVectors = urlMetaVectorsMap.get(url);
                if (metaVectors == null || clusters == null) {
                    continue;
                }
                ArrayList<HashMap<String, Object>> clustersWithScores = new ArrayList<>();
                for (int i = 0; i < clusters.length; i++) {
                    double score = getClusterScore(clusters[i], metaVectors);
                    HashMap<String, Object> clusterWithScore = new HashMap<>();
                    clusterWithScore.put("score", score);
                    clusterWithScore.put("cluster", clusters[i]);
                    clustersWithScores.add(clusterWithScore);
//                    if (score <= 7){
//                        aspirationClusters.get(aspiration).add(clusters[i].getCenter());
//                    }
                }
                clustersWithScores.sort((o1, o2) -> {
                    double score1 = (double) o1.get("score");
                    double score2 = (double) o2.get("score");
                    if (score1 < score2)
                        return -1;
                    else if (score1 > score2)
                        return 1;
                    else
                        return 0;
                });
                if (clustersWithScores == null || clustersWithScores.size() == 0) {
                    noClusterCount++;
                    continue;
                }
                if (!finalAspUrlscountMap.containsKey(aspiration)){
                    finalAspUrlscountMap.put(aspiration, 0);
                }
                finalAspUrlscountMap.put(aspiration, 1 + finalAspUrlscountMap.get(aspiration));
                clustersWithScores.subList(0,Math.min(10, clustersWithScores.size())).forEach(stringObjectHashMap -> {
                    aspirationClusters.get(aspiration).add(((WordKmeans.Classes)stringObjectHashMap.get("cluster")).getCenter());
                    urlClusters.get(url).add(((WordKmeans.Classes)stringObjectHashMap.get("cluster")).getCenter());

                });

                Double intScore = Math.floor((Double) clustersWithScores.get(0).get("score"));
                if (!scoreMap.containsKey(intScore)) {
                    scoreMap.put(intScore, 1);
                } else {
                    scoreMap.put(intScore, 1 + scoreMap.get(intScore));
                }
//            System.out.println(clustersWithScores);
            }
        }
        System.out.println("no clusters - " + noClusterCount);
        System.out.println(scoreMap);
        System.out.println(finalAspUrlscountMap);

        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("aspirationClustersMap-top10perpage-google.ser"));
        oos.writeObject(aspirationClusters);
        oos.flush();
        oos.close();

        oos = new ObjectOutputStream(new FileOutputStream("urlClustersMap-top10perpage-google.ser"));
        oos.writeObject(urlClusters);
        oos.flush();
        oos.close();

    }

    public double getClusterScore(WordKmeans.Classes cluster, ArrayList<double[]> metaVectors){
        double scoreDist = Double.MAX_VALUE;
        String mKey = "";
        for (double[] metaVector : metaVectors) {
            double dist = word2Vec.distance(cluster.getCenter(), metaVector);

            if (dist< scoreDist){
                scoreDist = dist;
//                mKey = word2Vec.
            }
        }
        return scoreDist;
    }


    public static void main(String[] args) throws IOException, ClassNotFoundException {
        ClusterScorer clusterScorer = new ClusterScorer();
        clusterScorer.loadWord2vec();
        clusterScorer.loadMaps();
        clusterScorer.genMetaVectors();
        clusterScorer.scoreClusters();


    }


}
