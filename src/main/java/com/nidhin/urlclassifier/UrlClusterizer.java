package com.nidhin.urlclassifier;

import de.l3s.boilerpipe.BoilerpipeProcessingException;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by nidhin on 27/7/17.
 */
public class UrlClusterizer {
    private KeywordExtractor keywordExtractor;
    private HashMap<String, double[]> textKeyVectorsMap = new HashMap<>();
    private HashMap<String, double[]> metaKeyVectorsMap = new HashMap<>();
    private HashSet<double[]> metaVectors = new HashSet<>();
    private Word2Vecdl4j word2Vecdl4j = new Word2Vecdl4j();

    public UrlClusterizer(){
        this.keywordExtractor = new KeywordExtractor();
    }

    public void loadW2v(String filePath){
        word2Vecdl4j.loadModel("en", new File(filePath));
    }
    public void generateVectorsForUrl(String url) throws IOException, BoilerpipeProcessingException {
        keywordExtractor.generateHtmlDoc(url);
        textKeyVectorsMap.clear();
        metaKeyVectorsMap.clear();
        metaVectors.clear();
        HashSet<String> textKeywords = keywordExtractor.extractKeyWords();
        HashSet<String> metaKeyWords = keywordExtractor.getMetaKeywords();

        for (String key : textKeywords){
            double[] vec = word2Vecdl4j.getVectorForNGram("en", key);
            if (vec != null){
                textKeyVectorsMap.put(key, vec);
            }
        }

        for (String key : metaKeyWords){
            double[] vec = word2Vecdl4j.getVectorForNGram("en", key);
            if (vec != null){
                metaKeyVectorsMap.put(key, vec);
                metaVectors.add(vec);
            }
        }
    }

    public WordKmeans.Classes[] generateClusters(){
        WordKmeans wordKmeans = new WordKmeans(textKeyVectorsMap, 50, 50);
        WordKmeans.Classes[] clusters = wordKmeans.explain();
        return clusters;
    }

    public List<HashMap<String, Object>> getScoredAndSortedClusters(WordKmeans.Classes[] clusters){

        List<HashMap<String, Object>> sortedClusterMaps = Arrays.asList(clusters)
                .stream()
                .map(classes -> {
                    double score = getClusterScore(classes, metaVectors);
                    HashMap<String, Object> tempMap = new HashMap<>();
                    tempMap.put("score", score);
                    tempMap.put("cluster", classes);
                    return tempMap;
                })
                .sorted((o1, o2) -> {
                    double score1 = (double) o1.get("score");
                    double score2 = (double) o2.get("score");
                    if (score1 < score2)
                        return -1;
                    else if (score1 > score2)
                        return 1;
                    else return 0;
                })
                .collect(Collectors.toList());
        return sortedClusterMaps;
    }

    public double getClusterScore(WordKmeans.Classes cluster, HashSet<double[]> metaVectors){
        double scoreDist = Double.MAX_VALUE;
        String mKey = "";
        for (double[] metaVector : metaVectors) {
            double dist = word2Vecdl4j.distance(cluster.getCenter(), metaVector);
            if (dist< scoreDist){
                scoreDist = dist;
//                mKey = word2Vec.
            }
        }
        return scoreDist;
    }

    public static void main(String[] args) throws IOException, BoilerpipeProcessingException {
        String url = "https://www.albert.io/blog/how-do-you-calculate-your-unweighted-gpa/";
        UrlClusterizer urlClusterizer= new UrlClusterizer();
        urlClusterizer.loadW2v("/home/nidhin/Jump2/jump-classifier/GoogleNews-vectors-negative300.bin");
        Scanner sc = new Scanner(System.in);
        while (true){
            System.out.println("Enter url : ");
            url = sc.nextLine();
            urlClusterizer.generateVectorsForUrl(url);
            WordKmeans.Classes [] classes = urlClusterizer.generateClusters();
            List<HashMap<String, Object>> sortedClusters = urlClusterizer.getScoredAndSortedClusters(classes);
//            System.out.println(sortedClusters);
            for (HashMap<String, Object> cluster: sortedClusters){
                System.out.println("Score - " + cluster.get("score"));
                System.out.println("Classes - ");
                WordKmeans.Classes classes1 = (WordKmeans.Classes) cluster.get("cluster");
                System.out.println(classes1.values);
            }
            System.out.println();
        }

    }
}
