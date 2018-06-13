package be.thomaswinters.pos;

import be.thomaswinters.pos.data.POStag;
import be.thomaswinters.pos.data.WordPOS;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import org.languagetool.tokenizers.WordTokenizer;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ProbabilisticPosTagger {

    // OpenNLP tool
    private final POSModel model;
    private final WordTokenizer tokenizer = new WordTokenizer();

    public ProbabilisticPosTagger(URL modelLocation) throws IOException {
        this.model = new POSModel(modelLocation);
    }

    public ProbabilisticPosTagger() throws IOException {
        this(ClassLoader.getSystemResource("nl-pos-perceptron.bin"));
    }

    public static void main(String[] args) {
        try {
            System.out.println(
                    new ProbabilisticPosTagger()
                            .tag("Maar neen Samson, \"masseren\"! Dat betekent het toepassen van uitwendige druk op de zachte weefsels."));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<WordPOS> tag(String sentence) {
        List<String> tokenized = tokenizer.tokenize(sentence);
        tokenized.removeAll(Collections.singleton(" "));
        String[] tokenizedPrimitive = tokenized.toArray(new String[tokenized.size()]);

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
            default:
                throw new IllegalStateException("Unknown POS " + tag);

        }
    }
}
