package be.thomaswinters.samsonworld.jeanine;

import be.thomaswinters.action.data.ActionDescription;
import be.thomaswinters.chatbot.IChatBot;
import be.thomaswinters.chatbot.data.IChatMessage;
import be.thomaswinters.random.Picker;
import be.thomaswinters.sentence.SentenceUtil;
import be.thomaswinters.wikihow.WikiHowPageScraper;
import be.thomaswinters.wikihow.WikihowSearcher;
import be.thomaswinters.wikihow.data.Page;
import be.thomaswinters.wikihow.data.PageCard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
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


    public Optional<String> replyToOctaaf(ActionDescription action) throws IOException {
        List<String> fullActionWords = SentenceUtil
                .splitOnSpaces(action.getVerb() + " " + action.getRestOfSentence())
                .map(String::toLowerCase)
                .collect(Collectors.toList());
        List<PageCard> relatedPages = searcher.search(fullActionWords);
        Optional<List<String>> optionalTips = relatedPages
                .stream()
                // Sort by decreasing amount of matchine words
                .sorted(Comparator.comparingInt((PageCard e) -> {
                    List<String> words = SentenceUtil.splitOnSpaces(e.getTitle()).collect(Collectors.toList());
                    words.retainAll(fullActionWords);
                    return words.size();
                }).reversed())
                .map(e-> {
                    try {
                        return wikiHowPageScraper.scrape(e).getTips();
                    } catch (IOException e1) {
                        throw new RuntimeException(e1);
                    }
                })
                .filter(e->!e.isEmpty())
                .findFirst();

        if (optionalTips.isPresent()) {
            return Optional.of("Ah, Octaaf, ik heb nog een goede tip over "
                    + action.getRestOfSentence() + " " + action.getVerb()
                    + ": " + Picker.pick(
                    optionalTips.get()
                            .stream()
                            .flatMap(e -> SentenceUtil.splitInSentences(e).stream())
                            .collect(Collectors.toList())));
        } else {
            if (Math.random() > 0.5) {
                return Optional.of("Ah maar goed zijn in "
                        + action.getRestOfSentence() + " " + action.getVerb()
                        + ", dat heeft hij van zijn moeder!.");
            } else {
                return Optional.of("Dat is weer typisch, het "
                        + action.getVerb() + " van " + action.getRestOfSentence()
                        + "... Dat heeft hij van zijn vader hé!.");
            }
        }

    }

    @Override
    public Optional<String> generateReply(IChatMessage message) {
        try {
            Page page = wikiHowPageScraper.scrape("https://nl.wikihow.com/Vieze-geurtjes-uit-tapijt-verwijderen");
            return Picker.pickOptional(page.getTips());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }
}
