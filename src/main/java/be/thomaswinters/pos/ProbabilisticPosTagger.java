package be.thomaswinters.pos;

import be.thomaswinters.pos.data.LemmaPOS;
import be.thomaswinters.pos.data.POStag;
import be.thomaswinters.pos.data.WordLemmaPOS;
import be.thomaswinters.pos.data.WordPOS;
import be.thomaswinters.util.DataLoader;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import org.languagetool.AnalyzedToken;
import org.languagetool.AnalyzedTokenReadings;
import org.languagetool.tokenizers.WordTokenizer;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ProbabilisticPosTagger {

    // OpenNLP tool
    private final POSModel model;
    private final WordTokenizer tokenizer = new WordTokenizer();
    private final LanguageToolTagger languageToolTagger = new LanguageToolTagger();

    public ProbabilisticPosTagger(URL modelLocation) throws IOException {
        this.model = new POSModel(modelLocation);
    }

    public ProbabilisticPosTagger() throws IOException {
        this(ClassLoader.getSystemResource("nl-pos-perceptron.bin"));
    }

    public static void main(String[] args) {
        try {
            ProbabilisticPosTagger tagger = new ProbabilisticPosTagger();

            DataLoader.readLines("tweets.txt").stream().map(e -> {
                try {
                    return tagger.tag(e);
                } catch (IOException e1) {
                    throw new RuntimeException(e);
                }
            }).forEach(System.out::println);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Adds the possible lemmas and best word pos to the sentences
     *
     * @param sentence
     * @return
     */
    public List<WordLemmaPOS> tag(String sentence) throws IOException {
        List<WordPOS> wordPOStags = tagWordPOS(sentence);
        List<AnalyzedTokenReadings> tokensLeft = languageToolTagger.tag(sentence);

        List<WordLemmaPOS> result = new ArrayList<>();
        for (WordPOS wordPos : wordPOStags) {

            // Filter tokens until this word occurs
            int idx = getIndexOf(wordPos.getWord(), tokensLeft);
            AnalyzedTokenReadings reading = tokensLeft.get(idx);
            tokensLeft = tokensLeft.subList(idx, tokensLeft.size());

            // Convert to lemmas & filter
            List<LemmaPOS> lemmas = toLemmas(reading.getReadings());

            System.out.println(wordPos + " :: " + lemmas);
            lemmas = filterLemmas(wordPos.getTag(), lemmas);
            System.out.println(wordPos + " => " + lemmas);
            result.add(new WordLemmaPOS(wordPos, lemmas));

        }


        return result;

    }

    private List<LemmaPOS> filterLemmas(POStag tag, List<LemmaPOS> lemmas) {
        return lemmas.stream().filter(lemma -> isLemmaFromTag(tag, lemma)).collect(Collectors.toList());
    }

    private boolean isLemmaFromTag(POStag tag, LemmaPOS lemma) {
        if (lemma.getPOS() == null) {
            return false;
        }
        return getTagFrom(lemma.getPOS()).equals(tag);
    }

    private POStag getTagFrom(String languageToolTag) {
        switch (languageToolTag.split(":")[0]) {
            case "ZNW":
                return POStag.NOUN;
            case "WKW":
                return POStag.VERB;
//            case "Punc":
//                return POStag.PUNCTUATION;
//            case "Art":
//                return POStag.ARTICLE;
            case "VRZ":
                return POStag.PREPOSITION;
            case "BNW":
                return POStag.ADJECTIVE;
            case "PVW":
                return POStag.PRONOUN;
            case "ENM":
            case "CNJ":
                return POStag.CONJUNCTION;
            case "BYW":
                return POStag.ADVERB;
            case "GET":
                return POStag.NUMBER;
//            case "Misc":
//                return POStag.MISCELLANEOUS;
//            case "GET":
//                return POStag.INTERJECTION;
            case "SPC": // Speciaal, bv maand
            case "TSW":
            case "MTE":
            case "SENT_END":
            case "PARA_END":
                return POStag.MISCELLANEOUS;
            default:
                throw new IllegalStateException("Unknown POS " + languageToolTag);
        }
    }

    private List<LemmaPOS> toLemmas(List<AnalyzedToken> readings) {
        return readings.stream()
                .map(reading -> new LemmaPOS(reading.getLemma(), reading.getPOSTag()))
                .collect(Collectors.toList());
    }

    private int getIndexOf(String word, List<AnalyzedTokenReadings> readings) {
        OptionalInt result = IntStream.range(0, readings.size())
                .filter(i -> readings.get(i).getToken().contains(word))
                .findFirst();
        if (result.isPresent()) {
            return result.getAsInt();
        }
        throw new IllegalStateException(readings + " does not contain word: " + word);
    }


    /**
     * Tags the sentence with their most probable tag
     *
     * @param sentence
     * @return
     */
    public List<WordPOS> tagWordPOS(String sentence) {
        List<String> tokenized = tokenizer.tokenize(sentence);
        tokenized.removeAll(Collections.singleton(" "));
        String[] tokenizedPrimitive = tokenized.toArray(new String[0]);

        POSTaggerME tagger = new POSTaggerME(model);
        String tags[] = tagger.tag(tokenizedPrimitive);

        return IntStream.range(0, tokenizedPrimitive.length)
                .mapToObj(i -> new WordPOS(tokenizedPrimitive[i], toTag(tags[i])))
                .collect(Collectors.toList());

    }

    private POStag toTag(String tag) {
        switch (tag) {
            case "N":
                return POStag.NOUN;
            case "V":
                return POStag.VERB;
            case "Punc":
                return POStag.PUNCTUATION;
            case "Art":
                return POStag.ARTICLE;
            case "Prep":
                return POStag.PREPOSITION;
            case "Adj":
                return POStag.ADJECTIVE;
            case "Pron":
                return POStag.PRONOUN;
            case "Conj":
                return POStag.CONJUNCTION;
            case "Adv":
                return POStag.ADVERB;
            case "Num":
                return POStag.NUMBER;
            case "Misc":
                return POStag.MISCELLANEOUS;
            case "Int":
                return POStag.INTERJECTION;
            default:
                throw new IllegalStateException("Unknown POS " + tag);

        }
    }
}
