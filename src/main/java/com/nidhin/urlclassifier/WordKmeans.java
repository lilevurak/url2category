package com.nidhin.urlclassifier;

import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;

public class WordKmeans implements Serializable{

	private HashMap<String, double[]> wordMap = null;

	private int iter;

	private Classes[] cArray = null;

	public WordKmeans(HashMap<String, double[]> wordMap, int clcn, int iter) {
		this.wordMap = wordMap;
		this.iter = iter;
		if (wordMap.size() < clcn) {
			cArray = new Classes[wordMap.size()];
		} else {
			cArray = new Classes[clcn];
		}
	}

	public Classes[] explain() {
		Iterator<Entry<String, double[]>> iterator = wordMap.entrySet().iterator();
		for (int i = 0; i < cArray.length && iterator.hasNext(); i++) {
			Entry<String, double[]> next = iterator.next();
			cArray[i] = new Classes(i, next.getValue());
		}


		for (int i = 0; i < iter; i++) {
			for (Classes classes : cArray) {
				classes.clean();
			}

			iterator = wordMap.entrySet().iterator();
			while (iterator.hasNext()) {
				Entry<String, double[]> next = iterator.next();
				double miniScore = Double.MAX_VALUE;
				double tempScore;
				int classesId = 0;
				for (Classes classes : cArray) {
					tempScore = classes.distance(next.getValue());
					if (miniScore > tempScore) {
						miniScore = tempScore;
						classesId = classes.id;
					}
				}
				cArray[classesId].putValue(next.getKey(), miniScore);
			}

			for (Classes classes : cArray) {
				classes.updateCenter(wordMap);
			}
		}

		return cArray;
	}

	public static class Classes implements Serializable{
		private int id;

		private double[] center;

		public Classes(int id, double[] center) {
			this.id = id;
			this.center = center.clone();
		}

		Map<String, Double> values = new HashMap<String, Double>();

		public double[] getCenter() {
			return center;
		}

		public double distance(double[] value) {
			double sum = 0;
			for (int i = 0; i < value.length; i++) {
				sum += (center[i] - value[i]) * (center[i] - value[i]);
			}
			return sum;
		}

		public void putValue(String word, double score) {
			values.put(word, score);
		}


		public void updateCenter(HashMap<String, double[]> wordMap) {
			for (int i = 0; i < center.length; i++) {
				center[i] = 0;
			}
			double[] value = null;
			for (String keyWord : values.keySet()) {
				value = wordMap.get(keyWord);
				for (int i = 0; i < value.length; i++) {
					center[i] += value[i];
				}
			}
			for (int i = 0; i < center.length; i++) {
				center[i] = center[i] / values.size();
			}
		}

		public void clean() {
			values.clear();
		}

		public List<Entry<String, Double>> getTop(int n) {
			List<Entry<String, Double>> arrayList = new ArrayList<Entry<String, Double>>(
				values.entrySet());
			Collections.sort(arrayList, new Comparator<Entry<String, Double>>() {
				@Override
				public int compare(Entry<String, Double> o1, Entry<String, Double> o2) {
					if (o1.getValue() > o2.getValue())
						return 1;
					else if (o1.getValue() < o2.getValue()) {
						return -1;
					} else {
						return 0;
					}
				}
			});
			int min = Math.min(n, arrayList.size() - 1);
			if (min <= 1) return Collections.emptyList();
			return arrayList.subList(0, min);
		}

		public List<Entry<String, Double>> getAll() {
			List<Entry<String, Double>> arrayList = new ArrayList<>(
				values.entrySet());
			if (arrayList.size() <= 1) {
				return arrayList;
			}

			Collections.sort(arrayList, new Comparator<Entry<String, Double>>() {
				@Override
				public int compare(Entry<String, Double> o1, Entry<String, Double> o2) {
					if (o1.getValue() > o2.getValue()) {
						return 1;
					} else if (o1.getValue() < o2.getValue()) {
						return -1;
					} else {
						return 0;
					}
				}
			});
			return arrayList;
		}
	}

}
