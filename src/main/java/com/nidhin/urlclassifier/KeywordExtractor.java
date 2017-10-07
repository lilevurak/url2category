package com.nidhin.urlclassifier;

import de.l3s.boilerpipe.BoilerpipeProcessingException;
import de.l3s.boilerpipe.extractors.ArticleExtractor;
import de.l3s.boilerpipe.extractors.DefaultExtractor;
import de.l3s.boilerpipe.extractors.ExtractorBase;
import edu.ehu.galan.rake.RakeAlgorithm;
import edu.ehu.galan.rake.model.Document;
import edu.ehu.galan.rake.model.Term;
import edu.ehu.galan.rake.model.Token;
import edu.illinois.cs.cogcomp.chunker.main.lbjava.Chunker;
import edu.illinois.cs.cogcomp.lbjava.nlp.SentenceSplitter;
import edu.illinois.cs.cogcomp.lbjava.nlp.WordSplitter;
import edu.illinois.cs.cogcomp.lbjava.nlp.seg.PlainToTokenParser;
import edu.illinois.cs.cogcomp.lbjava.parse.Parser;
import edu.illinois.cs.cogcomp.pos.lbjava.POSTagger;
import org.apache.commons.codec.digest.DigestUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by nidhin on 20/7/17.
 */
public class KeywordExtractor {
    ExtractorBase articleExtractor = ArticleExtractor.getInstance();
    ExtractorBase defaultExtractor = DefaultExtractor.getInstance();
    ArticleTextExtractor extractor = new ArticleTextExtractor();
    POSTagger tagger;
    Chunker chunker;
    String htmlDoc = null;
    List<String> stopWords = new ArrayList<>();
    String urlStr;

    public KeywordExtractor(){
        this.tagger = new POSTagger();
        this.chunker = new Chunker();
    }

    public void generateHtmlDoc(String urlStr){
        try {
            if (!urlStr.startsWith("http")){
                urlStr = "http://" + urlStr;
            }
            this.urlStr =urlStr;
            org.jsoup.nodes.Document doc = Jsoup.connect(urlStr).referrer("google.com")
                    .userAgent("Mozilla/5.0 (Windows NT 6.3; rv:36.0) Gecko/20100101 Firefox/36.0")
                    .timeout(5000)
                    .get();
//            connection.addRequestProperty("Accept-Language", "en-US,en;q=0.8");
//            connection.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.3; rv:36.0) Gecko/20100101 Firefox/36.0");
//            connection.addRequestProperty("Referer", "google.com");
//            connection.setConnectTimeout(10000);
//            connection.setReadTimeout(10000);
//            String encoding = null;
//            try {
//                encoding = connection.getContentEncoding();
//            }catch (Exception e){
//
//            }
//            encoding = encoding == null ? "UTF-8" : encoding;
//            htmlDoc = IOUtils.toString(connection.getInputStream(), encoding);
            htmlDoc = doc.html();
        }catch (Exception e){
            e.printStackTrace();
            htmlDoc = null;
        }
    }

    public boolean loadHtml(String urlStr, String asp){
        try {
            if (!urlStr.startsWith("http")){
                urlStr = "http://" + urlStr;
            }

            String fileName = DigestUtils.md5Hex(urlStr);

            byte[] enc = Files.readAllBytes(Paths.get("htmls", asp, fileName+".html"));
            htmlDoc = new String(enc);
            return true;
        }catch (Exception e){
//            e.printStackTrace();
            htmlDoc = null;
            return false;
        }
    }


    public HashSet<String> extractKeyWords() throws IOException, BoilerpipeProcessingException {
        if (htmlDoc == null)
            return new HashSet<>();

        try{
//            String articleText = articleExtractor.getText(htmlDoc);
            String articleText = extractor.getRelevantText(htmlDoc);
            String boilerArticleText = articleExtractor.getText(htmlDoc);
            String boilerDefaultText = defaultExtractor.getText(htmlDoc);
            String textToAnalyse = "";
            int customLength = articleText.length(), boilerArticlelength=boilerArticleText.length(),
                    boilerDefaultLength = boilerDefaultText.length();
            if (boilerArticlelength >= customLength && boilerArticlelength >= boilerDefaultLength )
                textToAnalyse = boilerArticleText;
            else if (boilerDefaultLength >= customLength && boilerDefaultLength >= boilerArticlelength )
                textToAnalyse = boilerDefaultText;
            else
                textToAnalyse = articleText;


//            HashSet<String> terms = RAKE(new String[]{articleText});
            HashSet<String> terms = BiTrigramKeys(new String[]{textToAnalyse.trim()});
            return terms;

        }catch (Exception e){
            e.printStackTrace();
            return new HashSet<>();
        }

    }

    public HashSet<String> ngrams(String s, int len) {
        String[] parts = s.split(" ");
        HashSet<String> result = new HashSet<>();
        for(int i = 0; i < parts.length - len + 1; i++) {
            StringBuilder sb = new StringBuilder();
            boolean skipNgram = false;
            for(int k = 0; k < len; k++) {
                if(k > 0) sb.append(' ');
                if (stopWords.contains(parts[i+k].toLowerCase().trim())){
                    skipNgram = true;
                    break;
                }
                sb.append(parts[i+k]);
            }
            if (!skipNgram)
            result.add(sb.toString());
        }
        return result;
    }
    public void saveHtmlAsFile(String parentDir, String aspDir) throws IOException {
        if (htmlDoc == null)
            return;
        File aspDirFile = Paths.get(parentDir, aspDir).toFile();
        if (!aspDirFile.exists()) {
            aspDirFile.mkdirs();
        }
        String fileName = String.format("%s.html", DigestUtils.md5Hex(urlStr));
        FileWriter fileWriter = new FileWriter(Paths.get(parentDir, aspDir, fileName).toFile());
        fileWriter.write(htmlDoc);
        fileWriter.flush();
        fileWriter.close();
    }

    public HashSet<String> BiTrigramKeys(String[] input){
        List<LinkedList<Token>> tokenizedSentenceList = new ArrayList<LinkedList<Token>>();
        List<String> sentenceList = new ArrayList<>();
        boolean first = true;
        Parser parser = new PlainToTokenParser(new WordSplitter(new SentenceSplitter(input)));
        String sentence = "";
        LinkedList<Token> tokenList = null;
        for (edu.illinois.cs.cogcomp.lbjava.nlp.seg.Token word = (edu.illinois.cs.cogcomp.lbjava.nlp.seg.Token) parser.next(); word != null;
             word = (edu.illinois.cs.cogcomp.lbjava.nlp.seg.Token) parser.next()) {
            String chunked = chunker.discreteValue(word);
            tagger.discreteValue(word);
            if (first) {
                tokenList = new LinkedList<>();
                tokenizedSentenceList.add(tokenList);
                first = false;
            }
            tokenList.add(new Token(word.form, word.partOfSpeech, null, chunked));
            sentence = sentence + " " + (word.form);
            if (word.next == null) {
                sentenceList.add(sentence);
                first = true;
                sentence = "";
            }
        }
//        parser.reset();
        HashSet<String> grams = new HashSet<>();
        for (String sent : sentenceList){
            grams.addAll(ngrams(sent, 2));
            grams.addAll(ngrams(sent, 3));
        }


        return grams;
    }

    public void loadStopWords(String file) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;
        while ((line = br.readLine()) != null){
            stopWords.add(line.trim().toLowerCase());
        }
    }

    public HashSet<String> RAKE(String[] input){
        List<LinkedList<Token>> tokenizedSentenceList = new ArrayList<LinkedList<Token>>();
        List<String> sentenceList = new ArrayList<>();
        boolean first = true;
        Parser parser = new PlainToTokenParser(new WordSplitter(new SentenceSplitter(input)));
        String sentence = "";
        LinkedList<Token> tokenList = null;
        for (edu.illinois.cs.cogcomp.lbjava.nlp.seg.Token word = (edu.illinois.cs.cogcomp.lbjava.nlp.seg.Token) parser.next(); word != null;
             word = (edu.illinois.cs.cogcomp.lbjava.nlp.seg.Token) parser.next()) {
            String chunked = chunker.discreteValue(word);
            tagger.discreteValue(word);
            if (first) {
                tokenList = new LinkedList<>();
                tokenizedSentenceList.add(tokenList);
                first = false;
            }
            tokenList.add(new Token(word.form, word.partOfSpeech, null, chunked));
            sentence = sentence + " " + (word.form);
            if (word.next == null) {
                sentenceList.add(sentence);
                first = true;
                sentence = "";
            }
        }
//        parser.reset();


        RakeAlgorithm ex = new RakeAlgorithm();
        ex.loadStopWordsList("/home/nidhin/Jump2/jump-classifier/src/main/resources/SmartStopListEn");
        ex.loadPunctStopWord("/home/nidhin/Jump2/jump-classifier/src/main/resources/RakePunctDefaultStopList");
        Document doc = new Document(".","test.txt");
        doc.setSentenceList(sentenceList);
        ex.init(doc, null);
        ex.runAlgorithm();
        List<Term> fullterms = doc.getTermList();
        List<String> terms = doc.getTermList().stream().map(term -> {
            return term.getTerm().trim().toLowerCase();
        }).collect(Collectors.toList());
        return new HashSet<>(terms);
    }

    public static void main(String[] args) throws IOException, BoilerpipeProcessingException {
//        String[] arr = new String[2];
//        arr[0] = "When I was twenty-four my father accidentally gave me some life changing advice. It came at the end of one of our frequent phone calls.";
//        arr[1] = "Then came the Shadow King in search of furious vengeance. Turning all before him to dust.";
////        List<String> keys = new KeywordExtractor().extractKeyWords("https://medium.com/the-mission/choose-who-you-want-to-become-ab1f41fe5377");
////        System.out.println(keys);
        KeywordExtractor keywordExtractor = new KeywordExtractor();
        keywordExtractor.generateHtmlDoc("http://www.nea.org/assets/docs/Indicators_of_Success-BGH_ac5-final.pdf");
        keywordExtractor.extractKeyWords();
        keywordExtractor.getMetaKeywords();
    }

    public HashSet<String> getMetaKeywords(){
        HashSet<String> metaKeywords = new HashSet<>();
        if (htmlDoc == null)
            return metaKeywords;

        org.jsoup.nodes.Document doc = Jsoup.parse(htmlDoc);
        Element head = doc.getElementsByTag("head").first();
       Elements elements = head.getElementsByTag("meta");
        final String[] ogDesc = {null};
        final String[] keyWords = {null};
        elements.forEach(element -> {
            String attrName;
            if (element.hasAttr("name"))
                attrName = "name";
            else
                attrName = "property";
            String attr = element.attr(attrName);
            if (attr== null){

            }
            else if (attr.equalsIgnoreCase("keywords")){
                System.out.println(element);
                keyWords[0] = element.attr("content");
            }
            else if (attr.equalsIgnoreCase("og:description")){
                System.out.println(element);
                ogDesc[0] = element.attr("content");
            }


        });
        Element h1Elem = doc.getElementsByTag("h1").first();
        String title = "";
        if (h1Elem != null){
            title = h1Elem.text();
        }
        if (keyWords[0] != null){
            metaKeywords.addAll(Arrays.asList(keyWords[0].split(",\\s*")));
        }
        if (ogDesc[0] == null){
            ogDesc[0] = "";
        }
        if (title == null){
            title = "";
        }
        System.out.println("title - " + title);
//        metaKeywords.addAll(RAKE(new String[]{ogDesc[0], title}));
        metaKeywords.addAll(BiTrigramKeys(new String[]{ogDesc[0].trim(), title.trim()}));
        return metaKeywords;
    }

    public HashSet<String> getStrictMetaKeywords(){
        HashSet<String> metaKeywords = new HashSet<>();
        if (htmlDoc == null)
            return metaKeywords;

        org.jsoup.nodes.Document doc = Jsoup.parse(htmlDoc);
        Element head = doc.getElementsByTag("head").first();
        Elements elements = head.getElementsByTag("meta");
        final String[] ogDesc = {null};
        final String[] keyWords = {null};
        elements.forEach(element -> {
            String attrName;
            if (element.hasAttr("name"))
                attrName = "name";
            else
                attrName = "property";
            String attr = element.attr(attrName);
            if (attr== null){

            }
            else if (attr.equalsIgnoreCase("keywords")){
//                System.out.println(element);
                keyWords[0] = element.attr("content");
            }
            else if (attr.equalsIgnoreCase("og:description")){
//                System.out.println(element);
                ogDesc[0] = element.attr("content");
            }

        });
        Element h1Elem = doc.getElementsByTag("h1").first();
        String title = "";
        if (h1Elem != null){
            title = h1Elem.text();
        }
        if (keyWords[0] != null){
            List<String> lowercasekeywords = Arrays.asList(keyWords[0].split(",\\s+"))
                    .stream()
                    .map(s -> {
                        return s.trim().toLowerCase();
                    })
                    .collect(Collectors.toList());
            metaKeywords.addAll(lowercasekeywords);
        }
        if (ogDesc[0] == null){
            ogDesc[0] = "";
        }
        if (title == null){
            title = "";
        }
//        System.out.println("title - " + title);
        metaKeywords.addAll(RAKE(new String[]{ogDesc[0], title}));
        return metaKeywords;
    }



}
