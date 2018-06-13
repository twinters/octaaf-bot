package be.thomaswinters.pos;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import org.apache.commons.lang3.ArrayUtils;
import org.languagetool.tokenizers.WordTokenizer;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

public class PosTagger {

    // OpenNLP tool
    private final POSModel model;

    public PosTagger(URL modelLocation) throws IOException {
        this.model = new POSModel(modelLocation);
    }

    public PosTagger() throws IOException {
        this(ClassLoader.getSystemResource("nl-pos-perceptron.bin"));
    }

    private final WordTokenizer tokenizer = new WordTokenizer();
    public void tag(String sentence) {
        List<String> tokenized = tokenizer.tokenize(sentence);
        tokenized.removeAll(Collections.singleton(" "));
        String[] tokenizedPrimitive = tokenized.toArray(new String[tokenized.size()]);

        POSTaggerME tagger = new POSTaggerME(model);
        String tags[] = tagger.tag(tokenizedPrimitive);

        IntStream.range(0, tokenizedPrimitive.length)
                .mapToObj(i -> tokenizedPrimitive[i] + '_' + tags[i])
                .forEach(System.out::println);

    }


    public static void main(String[] args) {
        try {
            new PosTagger().tag("Maar neen Samson, \"masseren\"! Dat betekent het toepassen van uitwendige druk op de zachte weefsels.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
