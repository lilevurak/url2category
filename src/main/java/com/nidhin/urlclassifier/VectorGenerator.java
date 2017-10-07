package com.nidhin.urlclassifier;

import de.l3s.boilerpipe.BoilerpipeProcessingException;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Created by nidhin on 20/7/17.
 */
public class VectorGenerator {
    HashMap<String, ArrayList<String>> aspirationUrlsMap = new HashMap<>();
    HashMap<String, ArrayList<String>> urlKeywordsMap = new HashMap<>();
    HashMap<String, ArrayList<String>> urlMetaKeywordsMap = new HashMap<>();
//    HashMap<String, double[]> keyVectorMap = new HashMap<>();
    HashMap<String, HashSet<String>> aspirationKeywordsMap = new HashMap<>();
    HashMap<String, HashSet<double[]>> aspirationKeyVectorsMap = new HashMap<>();
    HashMap<String, WordKmeans.Classes[]> urlClustersMap = new HashMap<>();


    public void generate(String [] files) throws IOException, BoilerpipeProcessingException {

        for (int i=0; i< files.length; i++){
            Path filePath = Paths.get("aspirations_latest_with_bing",files[i]);
            BufferedReader br = new BufferedReader(new FileReader(filePath.toString()));
            String line;
            String asp = files[i];
            if (!aspirationUrlsMap.containsKey(asp)){
                aspirationUrlsMap.put(asp, new ArrayList<>());
            }
            while ((line = br.readLine()) != null){
                aspirationUrlsMap.get(asp).add(line);
            }
        }

        KeywordExtractor keywordExtractor = new KeywordExtractor();
        keywordExtractor.loadStopWords("/home/ubuntu/aspirations-oct2nd/SmartStopListEn");
        Word2Vecdl4j word2Vec = new Word2Vecdl4j();
        word2Vec.loadModel("en", new File("/home/ubuntu/aspirations-oct2nd/GoogleNews-vectors-negative300.bin"));

        AtomicInteger aspCount = new AtomicInteger(0);
        for (String key : aspirationUrlsMap.keySet()) {
            aspirationKeywordsMap.put(key, new HashSet<>());
            System.out.println("Fetching keys for - " + key);
            aspCount.incrementAndGet();
            AtomicInteger count = new AtomicInteger(0);
            int totUrls = aspirationUrlsMap.get(key).size();
//            for (String url : aspirationUrlsMap.get(key)){
            List<HashMap<String, WordKmeans.Classes[]>> urlmaps = aspirationUrlsMap.get(key).stream()
                    .map(url -> {
                        try {
                            if (url.contains("pdf") || url.contains("PDF")) {
                                System.out.println("SKIPPING PDF");
                                return null;
                            }
                            count.incrementAndGet();
                            if (!keywordExtractor.loadHtml(url, key)) {
                                keywordExtractor.generateHtmlDoc(url);
                                keywordExtractor.saveHtmlAsFile("htmls", key);
                            }
//                    keywordExtractor.loadHtml(url, key);
                            HashSet<String> keywords = keywordExtractor.extractKeyWords();
                            HashSet<String> metaKeywords = keywordExtractor.getStrictMetaKeywords();
                            if (count.get() % 50 == 0) {
                                System.out.println("fetched url count - " + count + "/" + totUrls + " asp count - " + aspCount.get());
                            }
                            urlKeywordsMap.put(url, new ArrayList<>(keywords));
                            urlMetaKeywordsMap.put(url, new ArrayList<>(metaKeywords));
                            aspirationKeywordsMap.get(key).addAll(keywords);

                            HashSet<String> keySet = new HashSet<>(metaKeywords);
                            keySet.addAll(keywords);
                            HashMap<String, double[]> keyWordsDoubleVecMap = new HashMap<>();
                            int totalKwords = 0;
                            for (String keyword : keySet) {
                                double[] vec = word2Vec.getVectorForNGram("en", keyword.trim().toLowerCase());
                                if (vec != null) {
                                    keyWordsDoubleVecMap.put(keyword, vec);
                                    if (totalKwords > 5000) {
                                        break;
                                    }
                                    totalKwords++;
                                }

                            }
//                            System.out.println("clustering...  :: Keywords count - " + keyWordsDoubleVecMap.size());
                            WordKmeans wordKmeans = new WordKmeans(keyWordsDoubleVecMap, 50, 75);
                            WordKmeans.Classes[] classes = wordKmeans.explain();
//                    urlClustersMap.put(url, classes);
//                            System.out.println("classes - " + classes.length + " url - " + url);
                            HashMap<String, WordKmeans.Classes[]> map = new HashMap<>();
                            map.put(url, classes);
                            return map;
                        } catch (StackOverflowError e) {
                            e.printStackTrace();
                        } catch (BoilerpipeProcessingException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        return null;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            for (HashMap<String, WordKmeans.Classes[]> map : urlmaps){
                urlClustersMap.putAll(map);
            }

        }


        System.out.println("serializing...");
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("aspirationUrlsMap.ser"));
        oos.writeObject(aspirationUrlsMap);
        oos.flush();
        oos.close();

        oos = new ObjectOutputStream(new FileOutputStream("urlKeywordsMap.ser"));
        oos.writeObject(urlKeywordsMap);
        oos.flush();
        oos.close();
        oos = new ObjectOutputStream(new FileOutputStream("urlMetaKeywordsMap.ser"));
        oos.writeObject(urlMetaKeywordsMap);
        oos.flush();
        oos.close();
//        oos = new ObjectOutputStream(new FileOutputStream("keyVectorMap.ser"));
//        oos.writeObject(keyVectorMap);
//        oos.flush();
//        oos.close();
        oos = new ObjectOutputStream(new FileOutputStream("aspirationKeywordsMap.ser"));
        oos.writeObject(aspirationKeywordsMap);
        oos.flush();
        oos.close();



        oos = new ObjectOutputStream(new FileOutputStream("urlClustersMap-google.ser"));
        oos.writeObject(urlClustersMap);
        oos.flush();
        oos.close();

    }

    public void genAspKeyVecs(String[] files) throws IOException {
        for (int i=0; i< files.length; i++){
            Path filePath = Paths.get("aspirations_latest",files[i]);
            BufferedReader br = new BufferedReader(new FileReader(filePath.toString()));
            String line;
            String asp = files[i];
            if (!aspirationUrlsMap.containsKey(asp)){
                aspirationUrlsMap.put(asp, new ArrayList<>());
            }
            while ((line = br.readLine()) != null){
                aspirationUrlsMap.get(asp).add(line);
            }
        }

        KeywordExtractor keywordExtractor = new KeywordExtractor();
        keywordExtractor.loadStopWords("/home/nidhin/Jump2/jump-classifier/src/main/resources/SmartStopListEn");
        Word2Vecdl4j word2Vec = new Word2Vecdl4j();
        word2Vec.loadModel("en", new File("/home/nidhin/Jump2/jump-classifier/GoogleNews-vectors-negative300.bin"));

        int aspCount =0;
        for (String key : aspirationUrlsMap.keySet()){
            aspirationKeywordsMap.put(key, new HashSet<>());
            aspirationKeyVectorsMap.put(key, new HashSet<>());
            System.out.println("Fetching keys for - " + key);
            aspCount++;
            int count = 0;
            for (String url : aspirationUrlsMap.get(key)){
                try{
                    if (url.contains("pdf") || url.contains("PDF")){
                        System.out.println("SKIPPING PDF");
                        continue;
                    }
                    count++;
//                    keywordExtractor.generateHtmlDoc(url);
//                    keywordExtractor.saveHtmlAsFile("/home/nidhin/Jump2/jump-classifier/htmls", key);
                    keywordExtractor.loadHtml(url, key);
                    HashSet<String> metaKeywords = keywordExtractor.getStrictMetaKeywords();
                    System.out.println("fetched url count - " + count + " asp count - " + aspCount);
//                    aspirationKeywordsMap.get(key).addAll(metaKeywords);

                    for (String keyword : metaKeywords){
                        double[] keyVector = word2Vec.getVectorForNGram("en", keyword);
                        if (keyVector != null){
                            aspirationKeywordsMap.get(key).add(keyword);
                            aspirationKeyVectorsMap.get(key).add(keyVector);
                        }
                    }
                }catch (StackOverflowError e){
                    e.printStackTrace();
                }

            }
        }


        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("aspirationMetaKeywordsMap-fordistscoring.ser"));
        oos.writeObject(aspirationKeywordsMap);
        oos.flush();
        oos.close();
        oos = new ObjectOutputStream(new FileOutputStream("aspirationMetaKeyVectorsMap-fordistscoring.ser"));
        oos.writeObject(aspirationKeyVectorsMap);
        oos.flush();
        oos.close();
    }

    public static void main(String[] args) throws IOException, BoilerpipeProcessingException {
        VectorGenerator vg= new VectorGenerator();
//        String [] files = new String[]{"achieve a high gpa", "discover my career path", "find an internship or job", "find balance in my life", "find my purpose",
//        "find my tribe", "get out of debt", "give back to my community", "graduate on time", "live a healthier lifestyle", "networking",
//                "obtain skills for a chosen career", "personal achievements", "post grad preparation", "reduce stress", "seek continuing education programs"};
//        String [] files = new String[]{"achieve high gpa", "build community", "discover purpose",
//                "find balance", "graduate on time", "maximize money", "network effectively", "pursue career"};
        String [] files = new String[]{"Achieve High GPA", "Build Community", "Discover Purpose",
                "Find Balance", "Graduate On Time", "Maximize Money", "Network Effectively", "Pursue Career"};
        vg.generate(files);


    }
}
