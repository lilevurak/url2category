package com.nidhin.urlclassifier;

/**
 * Created by nidhin on 27/7/17.
 *
 */

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashMap;
import java.util.regex.Pattern;
public class ArticleTextExtractor {
    // Unlikely candidates
    private static final Pattern unlikely =
            Pattern.compile("(combx.*)|(comment.*)|(community.*)|(disqus.*)|(extra.*)|(foot.*)|(header.*)|(menu.*)|(remark.*)|(rss.*)|(shoutbox.*)|(sidebar.*)|(sponsor.*)|(ad.*)|(agegate.*)|(pagination.*)|(pager.*)|(popup.*)|(print.*)|(archive.*)|(comment.*)|(discuss.*)|(e[-]?mail.*)|(share.*)|(reply.*)|(all.*)|(login.*)|(sign.*)|(single.*)|(attachment.*)");

    // Most likely positive candidates
    private static final Pattern positive =
            Pattern.compile("(article.*)|(body.*)|(content.*)|(entry.*)|(hentry.*)|(main.*)|(page.*)|(pagination.*)|(post.*)|(text.*)|(blog.*)|(story.*)");

    // Most likely negative candidates
    private static final Pattern negative =
            Pattern.compile("(.*navigation.*)|(.*combx.*)|(.*comment.*)|(com-.*)|(.*contact.*)|(.*foot.*)|(.*footer.*)|(.*footnote.*)|(.*masthead.*)|(.*media.*)|(.*meta.*)|(.*outbrain.*)|(.*promo.*)|(.*related.*)|(.*scroll.*)|(.*shoutbox.*)|(.*sidebar.*)|(.*sponsor.*)|(.*shopping.*)|(.*tags.*)|(.*tool.*)|(.*widget.*)|(.*hidden.*)");

    /**
     * @param html extracts article text from given html string. wasn't tested with improper HTML
     * @returns extracted article core text html
     */
    public String getArticleHtml(String html) throws Exception {
        Document doc = Jsoup.parse(html);
        prepareDocument(doc);
        Element longest = null;
        Integer longest_int = 0;

        HashMap<Element, Integer> scores = new HashMap<Element, Integer>();
        Element bd = doc.body();
        Elements hd = doc.select("head");
        for (Element el : doc.select("body").select("*")) {
            if (el.tag().getName().equals("p") || el.tag().getName().equals("td") || el.tag().getName().equals("div")) {
                scores.put(el, 0);
            }
        }

        int maxWeight = 0;
        Element bestMatch = null;
        for (Element entry : scores.keySet()) {
            int currentWeight = getWeight(entry);
            if (currentWeight > maxWeight) {
                maxWeight = currentWeight;
                bestMatch = entry;
            }
        }

        if (bestMatch != null) {
            return bestMatch.html();
        } else {
            return null;
        }
    }

    /**
     * Prepares document. Currently only stripping unlikely candidates, since from time to time they're getting more score than good ones
     * especially in cases when major text is short.
     *
     * @param doc document to prepare. Passed as reference, and changed inside of function
     */
    protected void prepareDocument(Document doc) {
        doc = stripUnlikelyCandidates(doc);
    }

    /**
     * Removes unlikely candidates from HTML. Currently takes id and class name and matches them against list of patterns
     *
     * @param doc document to strip unlikely candidates from
     */
    public Document stripUnlikelyCandidates(Document doc) {
        for (Element child : doc.body().children()) {
            if (child.tag().getName().equalsIgnoreCase("body")) {
                continue;
            }
            String className = child.className();
            String id = child.id();
            if ((unlikely.matcher(className).matches() ||
                    unlikely.matcher(id).matches() ||
                    negative.matcher(className).matches() ||
                    negative.matcher(id).matches()
            ) && !(positive.matcher(id).matches() ||
                    positive.matcher(className).matches())) {
                child.remove();
            }
        }
        return doc;
    }


    public boolean shouldPursue(String id, String className) {
        if (id == null && className == null) {
            return true;
        } else {
            if (negative.matcher(className).matches() ||
                    negative.matcher(id).matches()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Weights current element. By matching it with positive candidates and weighting child nodes. Since it's impossible to predict which
     * exactly names, ids or class names will be used in HTML, major role is played by child nodes
     *
     * @param e Element to weight, along with child nodes
     */
    protected int getWeight(Element e) {
        Integer weight = 0;
        if (positive.matcher(e.className()).matches()) {
            weight += 25;
        }

        if (positive.matcher(e.id()).matches()) {
            weight += 25;
        }

        if (weight >= 0) {
            int childNodesWeight = weightChildNodes(e);
            if (childNodesWeight == 0 && e.ownText().length() > 100) {
                weight += Math.round(e.ownText().length() / 100) * 5;
            } else {
                weight += childNodesWeight;
            }
        }
        return weight;
    }

    /**
     * Weights a child nodes of given Element. During tests some difficulties were met. For instance, not every single document has nested
     * paragraph tags inside of the major article tag. Sometimes people are adding one more nesting level. So, we're adding 4 points for every
     * 100 symbols contained in tag nested inside of the current weighted element, but only 3 points for every element that's nested 2 levels
     * deep. This way we give more chances to extract the element that has less nested levels, increasing probability of the correct extraction.
     *
     * @param e Element, who's child nodes will be weighted
     */
    protected int weightChildNodes(Element e) {
        int weight = 0;
        for (Element child : e.children()) {
            if (child.tag().getName().equals("div") || child.tag().getName().equals("p")) {
                if (child.ownText().length() > 100) {
                    if (child.parent() == e) {
                        weight += Math.round(child.ownText().length() / 100) * 4;
                    } else if (child.parent().parent() == e) {
                        weight += Math.round(child.ownText().length() / 100) * 3;
                    }
                }
            }
        }
        return weight;
    }

    public String getRelevantText(String htmlDoc){
        Document doc = Jsoup.parse(htmlDoc);
        String txt = getTextRec(doc.children());
        return txt;
    }

    public String getTextRec(Elements elements){
        String text = "";
        for (Element element: elements){
            if (shouldPursue(element.id(), element.className())){
                text += element.ownText();
                if (!(text.trim().endsWith(".") || text.trim().endsWith("?") || text.trim().endsWith("!")) && text.trim().length()>0){
                    text += ". ";
                }
                text += getTextRec(element.children());
            }
        }
        return text;
    }
}
