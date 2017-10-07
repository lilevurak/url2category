package com.nidhin.urlclassifier;


import org.apache.spark.mllib.linalg.Vector;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.StringTokenizer;

/**
 * Created by nidhin on 26/7/17.
 */
public class Word2Vecdl4j {
    private final Logger logger = LoggerFactory.getLogger(Word2Vec.class);

    private final static String WORD2VEC_MODEL_DIR = "resources-word2vec";


    private HashMap<String, org.deeplearning4j.models.word2vec.Word2Vec> modelMap = new HashMap<>();

    public Word2Vecdl4j() {
    }


    public boolean loadModel(String language, File file) {
        org.deeplearning4j.models.word2vec.Word2Vec word2Vec = WordVectorSerializer.readWord2VecModel(file);
        modelMap.put(language, word2Vec);
        return true;
    }

//    public Tuple2<String, Object>[] getNearbyWords(String lang, String word, int count) {
//        try {
//            Word2VecModel model = modelMap.get(lang);
//            if (model == null) {
//                return null;
//            }
//            Tuple2<String, Object>[] nearWords = model.findSynonyms(word, count);
//            return nearWords;
//        } catch (Exception e) {
//            return null;
//        }
//    }

//    public Tuple2<String, Object>[] getNearbyWords(String lang, Vector vector, int count) {
//        try {
//            Word2VecModel model = modelMap.get(lang);
//            if (model == null) {
//                return null;
//            }
//            Tuple2<String, Object>[] nearWords = model.findSynonyms(vector, count);
//            return nearWords;
//        } catch (Exception e) {
//            return null;
//        }
//    }

    public double[] getGlobalVector(String lang, String word) {
        org.deeplearning4j.models.word2vec.Word2Vec model = modelMap.get(lang);
        if (model == null) {
            return null;
        }

        try {
            double[] vector = model.getWordVector(word);
            return vector;
        } catch (Exception e) {
            return null;
        }
    }

//    public void loadAllModels(String resourcesDirectory) {
//        File word2VecDir = new File(resourcesDirectory + WORD2VEC_MODEL_DIR);
//        File modelFiles[] = word2VecDir.listFiles();
//        for (File modelFile : modelFiles) {
//            String fileName = modelFile.getAbsoluteFile().getName();
//            loadModel(fileName.split("\\.")[0], modelFile);
//        }
//    }

//    public double distance(String lang, String word1, String word2) {
//        Vector v1 = getVectorForNGram(lang, word1);
//        Vector v2 = getVectorForNGram(lang, word2);
//        return Vectors.sqdist(v1, v2);
//    }

//    public double distance(Vector vector1, Vector vector2) {
//        return Vectors.sqdist(vector1, vector2);
//    }
    public double distance(double[] vector1, double[] vector2) {

        double sumOfSquares = 0;
        for (int i = 0; i< vector1.length; i++){
            sumOfSquares += Math.pow(vector1[i] - vector2[i], 2);
        }
        return Math.sqrt(sumOfSquares);
    }


//    public double distance(String lang, String word1, Vector v2) {
//        Vector v1 = getVectorForNGram(lang, word1);
//        if (v1 == null) {
//            return -1.0d;
//        } else {
//            return Vectors.sqdist(v1, v2);
//        }
//    }


    public double cosineSimilarity(double[] vectorA, double[] vectorB) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += Math.pow(vectorA[i], 2);
            normB += Math.pow(vectorB[i], 2);
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }


    public double[] getVectorForNGram(String lang, String word) {
        org.deeplearning4j.models.word2vec.Word2Vec model = modelMap.get(lang);
        if (model == null) {
            return null;
        }
        StringTokenizer tokenizer = new StringTokenizer(word);
        double[] fullVector = null;
        while (tokenizer.hasMoreTokens()) {
            double[] vector = getGlobalVector(lang, tokenizer.nextToken());
            if (vector == null) {
                continue;
            }
            if (fullVector == null) {
                fullVector = vector;
            } else {
                fullVector = addVectors(vector, fullVector);
            }
        }
        if (fullVector == null || fullVector.length < 1) {
            return null;
        } else {
            return fullVector;
        }
    }


    public double[] addVectors(double[] a, double[] b) {
        double[] c = new double[a.length];
        for (int i = 0; i < a.length; i++) {
            c[i] = a[i] + b[i];
        }
        return c;
    }

    public double[] subVectors(double[] a, double[] b) {
        double[] c = new double[a.length];
        for (int i = 0; i < a.length; i++) {
            c[i] = a[i] - b[i];
        }
        return c;
    }


//    public double euclidean(Vector v1, Vector v2) {
//        return Vectors.sqdist(v1, v2);
//    }

    public static void main(String args[]) throws IOException {
        Word2Vec word2Vec = new Word2Vec();
        word2Vec.loadAllModels("/Users/deepak/Desktop/w2v/");
        String line;
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Enter first keyword : ");
        while ((line = bufferedReader.readLine()) != null) {
            System.out.println("Enter second keyword : ");

            String line2 = bufferedReader.readLine();

            Vector vector = word2Vec.getVectorForNGram("en", line);
            Vector vector1 = word2Vec.getVectorForNGram("en", line2);


            System.out.println("Euclidean DIST ( Smaller the better ) : " + word2Vec.distance("en", line, line2));
            System.out.println("COSINE SIM (Larger the better ) : " + word2Vec.cosineSimilarity(vector.toArray(), vector1.toArray()));

            System.out.println("\nEnter first keyword : ");


        }
    }
}
