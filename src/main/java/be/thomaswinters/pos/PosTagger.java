package be.thomaswinters.pos;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;

import java.io.IOException;
import java.net.URL;
import java.util.stream.IntStream;

public class PosTagger {

    private final POSModel model;

    public PosTagger(URL modelLocation) throws IOException {
        this.model = new POSModel(modelLocation);
    }

    public PosTagger() throws IOException {
        this(ClassLoader.getSystemResource("nl-pos-perceptron.bin"));
    }

    public void tag(String sentence) {
        String sent[] = sentence.split(" ");
        POSTaggerME tagger = new POSTaggerME(model);
        String tags[] = tagger.tag(sent);

        IntStream.range(0, sent.length)
                .mapToObj(i -> sent[i] + '_' + tags[i])
                .forEach(System.out::println);

    }


    public static void main(String[] args) {
        try {
            new PosTagger().tag("Maar neen Samson , \"masseren\" ! Dat betekent het toepassen van uitwendige druk op de zachte weefsels .");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
