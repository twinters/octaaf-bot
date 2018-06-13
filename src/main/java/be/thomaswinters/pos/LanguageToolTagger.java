package be.thomaswinters.pos;

import be.thomaswinters.replacement.Replacer;
import org.languagetool.AnalyzedSentence;
import org.languagetool.AnalyzedTokenReadings;
import org.languagetool.JLanguageTool;
import org.languagetool.language.Dutch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class LanguageToolTagger {


    private final JLanguageTool langTool;

    public LanguageToolTagger(JLanguageTool langTool) {
        this.langTool = langTool;
    }

    public LanguageToolTagger() {
        this(new JLanguageTool(new Dutch()));
    }

    public static List<String> getTags(AnalyzedTokenReadings token) {
        return token.getReadings().stream().filter(e -> !e.hasNoTag()).map(e -> e.getPOSTag())
                .collect(Collectors.toList());
    }
    public void tag(String sentence) throws IOException {
        List<AnalyzedSentence> answers = langTool.analyzeText(sentence);

        List<Replacer> replacerList = new ArrayList<>();

        for (AnalyzedSentence analyzedSentence : answers) {
            List<AnalyzedTokenReadings> tokens = Arrays.asList(analyzedSentence.getTokens());
            for (AnalyzedTokenReadings token : tokens) {

                List<String> tags = getTags(token);
                System.out.println(token);
            }
        }
    }
    public static void main(String[] args) {
        try {
            new LanguageToolTagger().tag("Maar neen Samson, \"masseren\" ! Dat betekent het toepassen van uitwendige druk op de zachte weefsels.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
