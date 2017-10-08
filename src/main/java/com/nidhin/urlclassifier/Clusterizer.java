package com.nidhin.urlclassifier;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by nidhin on 21/7/17.
 */
public class Clusterizer {
    HashMap<String, ArrayList<String>> urlKeywordsMap = new HashMap<>();
    HashMap<String, ArrayList<String>> urlMetaKeywordsMap = new HashMap<>();
    HashMap<String, double[]> keyVectorMap = new HashMap<>();
    Word2Vecdl4j word2Vecdl4j = new Word2Vecdl4j();

    public void generateClusters() throws IOException {
        HashMap<String, WordKmeans.Classes[]> urlClustersMap = new HashMap<>();
//        for (Map.Entry<String, ArrayList<String>> urlKeywords : urlKeywordsMap.entrySet()){
//
//            String url = urlKeywords.getKey();
//            ArrayList<String> keywords = urlKeywords.getValue();
//            ArrayList<String> metaKeywords = urlMetaKeywordsMap.get(url);
//            HashSet<String> keySet = new HashSet<>(metaKeywords);
//            keySet.addAll(keywords);
//            HashMap<String, double[]> keyWordsDoubleVecMap = new HashMap<>();
//            int totalKwords = 0;
//            for (String keyword : keySet){
//                if (keyVectorMap.get(keyword) != null)
//                keyWordsDoubleVecMap.put(keyword, keyVectorMap.get(keyword));
////                double[] vec = word2Vecdl4j.getVectorForNGram("en", keyword);
////                if (vec != null){
////                    keyWordsDoubleVecMap.put(keyword, vec);
////                }
//                if (totalKwords > 5000){
//                    break;
//                }
//                totalKwords++;
//            }
//            System.out.println("clustering...  :: Keywords count - " + keyWordsDoubleVecMap.size());
//            WordKmeans wordKmeans = new WordKmeans(keyWordsDoubleVecMap, 50, 100);
//            WordKmeans.Classes[] classes = wordKmeans.explain();
//            urlClustersMap.put(url, classes);
//            System.out.println("classes - " + classes.length + " url - " + url);
//        }


        List<HashMap<String, WordKmeans.Classes[]>> urlmaps = urlKeywordsMap.entrySet().stream()
                .parallel()
                .map(urlKeywords -> {

                    String url = urlKeywords.getKey();
                    ArrayList<String> keywords = urlKeywords.getValue();
                    ArrayList<String> metaKeywords = urlMetaKeywordsMap.get(url);
                    HashSet<String> keySet = new HashSet<>(metaKeywords);
                    keySet.addAll(keywords);
                    HashMap<String, double[]> keyWordsDoubleVecMap = new HashMap<>();
                    int totalKwords = 0;
                    for (String keyword : keySet){
                        if (keyVectorMap.get(keyword) != null)
                            keyWordsDoubleVecMap.put(keyword, keyVectorMap.get(keyword));
//                double[] vec = word2Vecdl4j.getVectorForNGram("en", keyword);
//                if (vec != null){
//                    keyWordsDoubleVecMap.put(keyword, vec);
//                }
                        if (totalKwords > 5000){
                            break;
                        }
                        totalKwords++;
                    }
                    System.out.println("clustering...  :: Keywords count - " + keyWordsDoubleVecMap.size());
                    WordKmeans wordKmeans = new WordKmeans(keyWordsDoubleVecMap, 50, 100);
                    WordKmeans.Classes[] classes = wordKmeans.explain();
//                    urlClustersMap.put(url, classes);
                    System.out.println("classes - " + classes.length + " url - " + url);
                    HashMap<String, WordKmeans.Classes[]> map = new HashMap<>();
                    map.put(url, classes);
                    return map;
                })
                .collect(Collectors.toList());
        for (HashMap<String, WordKmeans.Classes[]> map : urlmaps){
            urlClustersMap.putAll(map);
        }
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("urlClustersMap-google.ser"));
        oos.writeObject(urlClustersMap);
        oos.flush();
        oos.close();

    }
    public void loadW2v(String path){
//        long t1 = Calendar.getInstance().getTimeInMillis();
//        word2Vecdl4j.loadModel("en", new File(path));
//        long t2 = Calendar.getInstance().getTimeInMillis();
//        System.out.println("Google model load time - " + (t2-t1)/1000.0 +" secs");
    }

    public void loadMaps() throws IOException, ClassNotFoundException {

        ObjectInputStream ois = new ObjectInputStream(new FileInputStream("urlKeywordsMap.ser"));
        urlKeywordsMap = (HashMap<String, ArrayList<String>>) ois.readObject();
        ois.close();
        ois = new ObjectInputStream(new FileInputStream("urlMetaKeywordsMap.ser"));
        urlMetaKeywordsMap = (HashMap<String, ArrayList<String>>) ois.readObject();
        ois.close();
        ois = new ObjectInputStream(new FileInputStream("keyVectorMap.ser"));
        keyVectorMap = (HashMap<String, double[]>) ois.readObject();
        ois.close();
    }



    public static void main(String[] args) throws IOException, ClassNotFoundException {
        Clusterizer clusterizer = new Clusterizer();
        clusterizer.loadMaps();
        clusterizer.loadW2v("/home/nidhin/Jump2/jump-classifier/GoogleNews-vectors-negative300.bin");
        clusterizer.generateClusters();

//        ObjectInputStream ois = new ObjectInputStream(new FileInputStream("urlClustersMap.ser"));
//        HashMap<String, WordKmeans.Classes[]> urlClustersMap = (HashMap<String, WordKmeans.Classes[]>) ois.readObject();
//        ois.close();

//        System.out.println(urlClustersMap);
    }
}
