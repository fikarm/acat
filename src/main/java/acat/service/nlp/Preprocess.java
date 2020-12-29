package acat.service.nlp;

import acat.model.Entity;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.comments.Comment;
import javafx.concurrent.Task;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class Preprocess extends Task<List<String>> {

    private Path output;

    private List<Entity> entities;
    private List<String> stopwords;

    private boolean commentOnly, cleanSymbol, cleanStopwords, stemCorpus;
    private int     cleanShortwords, cleanRarewords;


    public Preprocess(List<Entity> entities,
                      String outputdir,
                      boolean commentOnly,
                      boolean cleanSymbol,
                      boolean cleanStopwords,
                      boolean stemCorpus,
                      int cleanShortwords,
                      int cleanRarewords)
    {

        setStopwords();
        this.entities = entities;
        this.output = Paths.get(outputdir);

        this.commentOnly = commentOnly;
        this.cleanSymbol = cleanSymbol;
        this.cleanStopwords = cleanStopwords;
        this.cleanShortwords = cleanShortwords;
        this.cleanRarewords = cleanRarewords;
        this.stemCorpus = stemCorpus;

    }

    @Override
    protected List<String> call() throws IOException {

        List<String> corpus = makeCorpus()
                .stream()
                .map(line -> {

                    String clean = line;

                    if (cleanSymbol)

                        clean = cleanSymbol(clean);

                    if (cleanStopwords)

                        clean = cleanStopwords(clean);

                    if (cleanShortwords > 0)

                        clean = cleanShortwords(clean, cleanShortwords);

                    if (cleanRarewords > 0)

                        clean = cleanRarewords(clean, cleanRarewords);

                    return clean;

                })
                .collect(Collectors.toList());

        Files.write(output, corpus);

        if (stemCorpus) {

            PortStemmer.stemCorpus(output.toString());
            updateMessage("stemming done.");

        }

        updateMessage("preprocess done.");

        return corpus;
    }


    private List<String> makeCorpus() {

        List<String> docs = new ArrayList<>();

        if (commentOnly)

            entities.forEach(e -> docs.add(getComment(e)));

        else

            entities.forEach(e -> docs.add(toSingleLine(e)));

        return docs;
    }

    private String cleanSymbol(String text) {

        updateMessage("clean symbol ");

        return text.toLowerCase()
                .replaceAll("\\s\\w+://[^\\s]+", "some_url")
                .replaceAll("@([^\\s]+)", "$1")
                .replaceAll("<[^>]+>", "")
                .replaceAll("\\d+", "")
                .replaceAll("\"", " ")
                .replaceAll("'", " ")
                .replaceAll("[=+\\-*/%!><&|?:~^\\]\\[)(}{;,#`$\\\\]", " ")
                .replaceAll("\\.", " ")
                .replaceAll("\\s+", " ")
                .trim();

    }

    private String cleanStopwords(String text) {

        updateMessage("clean stopwords ");

        return Arrays.stream(text.split(" "))
                .filter(token -> !stopwords.contains(token))
                .collect(Collectors.joining(" "));
    }

    private String cleanShortwords(String text, int n) {

        updateMessage("clean words length are less than or equal " + n);

        return Arrays
                .stream(text.split("\\s+"))
                .filter(s -> s.length() > n)
                .collect(Collectors.joining(" "));

    }

    private String cleanRarewords(String text, int n) {

        updateMessage("clean words occurences are less than or equal " + n);

        Map<String, Integer> freq = wordFrequencies(text);

        return Arrays
                .stream(text.split("\\s+"))
                .filter(s -> freq.get(s) > n)
                .collect(Collectors.joining(" "));

    }


    private void setStopwords() {

        stopwords = Arrays.asList(
                "i", "me", "my", "myself", "we", "our", "ours", "ourselves",
                "you", "you're", "you've", "you'll", "you'd", "your",
                "yours", "yourself", "yourselves", "he", "him", "his",
                "himself", "she", "she's", "her", "hers", "herself", "it",
                "it's", "its", "itself", "they", "them", "their", "theirs",
                "themselves", "what", "which", "who", "whom", "this", "that",
                "that'll", "these", "those", "am", "is", "are", "was", "were",
                "be", "been", "being", "have", "has", "had", "having", "do",
                "does", "did", "doing", "a", "an", "the", "and", "but", "if",
                "or", "because", "as", "until", "while", "of", "at", "by",
                "for", "with", "about", "against", "between", "into", "through",
                "during", "before", "after", "above", "below", "to", "from",
                "up", "down", "in", "out", "on", "off", "over", "under",
                "again", "further", "then", "once", "here", "there", "when",
                "where", "why", "how", "all", "any", "both", "each", "few",
                "more", "most", "other", "some", "such", "no", "nor", "not",
                "only", "own", "same", "so", "than", "too", "very", "s", "t",
                "can", "will", "just", "don", "don't", "should", "should've",
                "now", "d", "ll", "m", "o", "re", "ve", "y", "ain", "aren",
                "aren't", "couldn", "couldn't", "didn", "didn't", "doesn",
                "doesn't", "hadn", "hadn't", "hasn", "hasn't", "haven",
                "haven't", "isn", "isn't", "ma", "mightn", "mightn't", "mustn",
                "mustn't", "needn", "needn't", "shan", "shan't", "shouldn",
                "shouldn't", "wasn", "wasn't", "weren", "weren't", "won",
                "won't", "wouldn", "wouldn't");

    }

    private String toSingleLine(Entity e) {

        try {

            updateMessage(e.name + " to single line");

            return String.join(" ", Files.readAllLines(e.path));

        } catch (IOException ex) {

            ex.printStackTrace();
            return null;

        }

    }

    private String getComment(Entity e) {

        try {

            updateMessage(e.name + " get the comment only");

            StaticJavaParser.getConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.RAW);

            return StaticJavaParser.parse(e.getFile())
                    .getComments()
                    .stream()
                    .map(Comment::getContent)
                    .collect(Collectors.joining())
                    .replaceAll("\\s+", " ");

        } catch (FileNotFoundException ex) {

            ex.printStackTrace();
            return null;

        }

    }

    private Map<String, Integer> wordFrequencies(String text) {

        updateMessage("calculating words frequency");

        Map<String, Integer> freq = new HashMap<>();
        String[] tokens = text.split("\\s+");

        for (String token : tokens)

            if (freq.containsKey(token))

                freq.put(token, freq.get(token) + 1);

            else

                freq.put(token, 1);

        return freq;

    }

}
