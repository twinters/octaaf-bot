package be.thomaswinters.pos;

import org.languagetool.AnalyzedSentence;
import org.languagetool.AnalyzedToken;
import org.languagetool.AnalyzedTokenReadings;
import org.languagetool.JLanguageTool;
import org.languagetool.language.Dutch;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LanguageToolTagger {


    private final JLanguageTool langTool;

    public LanguageToolTagger(JLanguageTool langTool) {
        this.langTool = langTool;
    }

    public LanguageToolTagger() {
        this(new JLanguageTool(new Dutch()));
    }

    public static List<String> getTags(AnalyzedTokenReadings token) {
        return token.getReadings().stream().filter(e -> !e.hasNoTag()).map(AnalyzedToken::getPOSTag)
                .collect(Collectors.toList());
    }

    public static void main(String[] args) {
        try {
            new LanguageToolTagger().tag("Maar neen Samson, \"masseren\" ! Dat betekent het toepassen van uitwendige druk op de zachte weefsels.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<AnalyzedTokenReadings> tag(String sentence) throws IOException {
        List<AnalyzedSentence> answers = langTool.analyzeText(sentence);

        return answers.stream().flatMap(analyzedSentence -> Stream.of(analyzedSentence.getTokens())).collect(Collectors.toList());
    }
}
