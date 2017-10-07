package com.nidhin.urlclassifier;

import org.apache.spark.mllib.feature.Word2VecModel;
import org.apache.spark.mllib.linalg.DenseVector;
import org.apache.spark.mllib.linalg.Vector;
import org.apache.spark.mllib.linalg.Vectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Tuple2;

import java.io.*;
import java.util.HashMap;
import java.util.StringTokenizer;

/**
 * Spark MLLib based Word2Vec
 */
public class Word2Vec {

	private final Logger logger = LoggerFactory.getLogger(Word2Vec.class);

	private final static String WORD2VEC_MODEL_DIR = "resources-word2vec";


	private HashMap<String, Word2VecModel> modelMap = new HashMap<>();

	public Word2Vec() {
	}


	public boolean loadModel(String language, File file) {
		try {
			FileInputStream inputFileStream = new FileInputStream(file);
			ObjectInputStream objectInputStream = new ObjectInputStream(inputFileStream);
			modelMap.put(language, (Word2VecModel) objectInputStream.readObject());
			objectInputStream.close();
			inputFileStream.close();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return false;
	}

	public Tuple2<String, Object>[] getNearbyWords(String lang, String word, int count) {
		try {
			Word2VecModel model = modelMap.get(lang);
			if (model == null) {
				return null;
			}
			Tuple2<String, Object>[] nearWords = model.findSynonyms(word, count);
			return nearWords;
		} catch (Exception e) {
			return null;
		}
	}

	public Tuple2<String, Object>[] getNearbyWords(String lang, Vector vector, int count) {
		try {
			Word2VecModel model = modelMap.get(lang);
			if (model == null) {
				return null;
			}
			Tuple2<String, Object>[] nearWords = model.findSynonyms(vector, count);
			return nearWords;
		} catch (Exception e) {
			return null;
		}
	}

	public Vector getGlobalVector(String lang, String word) {
		Word2VecModel model = modelMap.get(lang);
		if (model == null) {
			return null;
		}

		try {
			Vector nearVector = model.transform(word);
			return nearVector;
		} catch (Exception e) {
			return null;
		}
	}

	public void loadAllModels(String resourcesDirectory) {
		File word2VecDir = new File(resourcesDirectory + WORD2VEC_MODEL_DIR);
		File modelFiles[] = word2VecDir.listFiles();
		for (File modelFile : modelFiles) {
			String fileName = modelFile.getAbsoluteFile().getName();
			loadModel(fileName.split("\\.")[0], modelFile);
		}
	}

	public double distance(String lang, String word1, String word2) {
		Vector v1 = getVectorForNGram(lang, word1);
		Vector v2 = getVectorForNGram(lang, word2);
		return Vectors.sqdist(v1, v2);
	}

	public double distance(Vector vector1, Vector vector2) {
		return Vectors.sqdist(vector1, vector2);
	}


	public double distance(String lang, String word1, Vector v2) {
		Vector v1 = getVectorForNGram(lang, word1);
		if (v1 == null) {
			return -1.0d;
		} else {
			return Vectors.sqdist(v1, v2);
		}
	}


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


	public Vector getVectorForNGram(String lang, String word) {
		Word2VecModel model = modelMap.get(lang);
		if (model == null) {
			return null;
		}
		StringTokenizer tokenizer = new StringTokenizer(word);
		Vector fullVector = null;
		while (tokenizer.hasMoreTokens()) {
			Vector vector = getGlobalVector(lang, tokenizer.nextToken());
			if (vector == null) {
				continue;
			}
			if (fullVector == null) {
				fullVector = new DenseVector(vector.toArray());
			} else {
				fullVector = Vectors.dense(addVectors(vector.toArray(), fullVector.toArray()));
			}
		}
		if (fullVector == null || fullVector.size() < 1) {
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


	public double euclidean(Vector v1, Vector v2) {
		return Vectors.sqdist(v1, v2);
	}

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
