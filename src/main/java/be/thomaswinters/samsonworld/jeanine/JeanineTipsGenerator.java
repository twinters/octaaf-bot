package be.thomaswinters.samsonworld.jeanine;

import be.thomaswinters.action.data.ActionDescription;
import be.thomaswinters.chatbot.IChatBot;
import be.thomaswinters.chatbot.data.IChatMessage;
import be.thomaswinters.random.Picker;
import be.thomaswinters.sentence.SentenceUtil;
import be.thomaswinters.wikihow.WikiHowPageScraper;
import be.thomaswinters.wikihow.WikihowSearcher;
import be.thomaswinters.wikihow.data.PageCard;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class JeanineTipsGenerator implements IChatBot {
    private final WikihowSearcher searcher = new WikihowSearcher("nl");
    private final WikiHowPageScraper wikiHowPageScraper = new WikiHowPageScraper("nl");


    public static void main(String[] args) {
        JeanineTipsGenerator jeanine = new JeanineTipsGenerator();
        for (int i = 0; i < 1; i++) {
            System.out.println(
                    jeanine.generateReply(null));
        }

    }

    private List<String> getTipsFor(String search) throws IOException {
        List<String> searchWords = SentenceUtil.splitOnSpaces(search)
                .map(String::toLowerCase)
                .collect(Collectors.toList());
        List<PageCard> relatedPages = searcher.search(searchWords);
        return relatedPages
                .stream()
                // Sort by decreasing amount of matchine words
                .sorted(Comparator.comparingInt((PageCard e) -> {
                    List<String> words = SentenceUtil.splitOnSpaces(e.getTitle()).collect(Collectors.toList());
                    words.retainAll(searchWords);
                    return words.size();
                }).reversed())
                .map(e -> {
                    try {
                        return wikiHowPageScraper.scrape(e).getTips();
                    } catch (IOException e1) {
                        throw new RuntimeException(e1);
                    }
                })
                .filter(e -> !e.isEmpty())
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }


    @Override
    public Optional<String> generateReply(IChatMessage message) {
        if (message.getUser().getScreenName().toLowerCase().contains("octaaf")) {

            String messageText = message.getText();

            // Check if it contains an action
            if (messageText.contains("!")) {
                String actionText = messageText.substring(0, messageText.indexOf('!')).replaceAll("[Aa]h,? ?", "");

                List<String> tips = new ArrayList<>();
                try {
                    tips = getTipsFor(actionText);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (!tips.isEmpty()) {
                    return Optional.of("Ah, Octaaf, ik heb nog een goede tip over "
                            + actionText
                            + ": "
                            + Picker.pick(
                            tips
                                    .stream()
                                    .flatMap(e -> SentenceUtil.splitInSentences(e).stream())
                                    .collect(Collectors.toList()))
                    );
                } else {
                    if (Math.random() > 0.5) {
                        return Optional.of("Ah maar goed zijn in "
                                + actionText
                                + ", dat heeft hij van zijn moeder!.");
                    } else {
                        return Optional.of("Dat is weer typisch, het "
                                + actionText
                                + "... Dat heeft hij van zijn vader hé!.");
                    }
                }
            }

        }

        return Optional.empty();
    }
}
