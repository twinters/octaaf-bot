package be.thomaswinters.samsonworld.jeanine;

import be.thomaswinters.chatbot.IChatBot;
import be.thomaswinters.chatbot.data.IChatMessage;
import be.thomaswinters.generator.fitness.IFitnessFunction;
import be.thomaswinters.generator.selection.ISelector;
import be.thomaswinters.generator.selection.TournamentSelection;
import be.thomaswinters.language.DutchFirstPersonConverter;
import be.thomaswinters.sentence.SentenceUtil;
import be.thomaswinters.wikihow.WikiHowPageScraper;
import be.thomaswinters.wikihow.WikihowSearcher;
import be.thomaswinters.wikihow.data.Page;
import be.thomaswinters.wikihow.data.PageCard;
import org.jsoup.HttpStatusException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class JeanineTipsGenerator implements IChatBot {
    private final WikihowSearcher searcher = new WikihowSearcher("nl");
    private final WikiHowPageScraper wikiHowPageScraper = new WikiHowPageScraper("nl");
    private final IFitnessFunction<String> tipFitnessFunction = e -> 1 / e.length();
    private final ISelector<String> tipSelector = new TournamentSelection<>(tipFitnessFunction, 5);


    private List<Page> getPages(String search) throws IOException {
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
                        return Optional.of(wikiHowPageScraper.scrape(e));
                    } catch (HttpStatusException httpEx) {
                        if (httpEx.getStatusCode() == 404) {
                            return Optional.<Page>empty();
                        }
                        throw new RuntimeException(httpEx);
                    } catch (IOException e1) {
                        throw new RuntimeException(e1);
                    }
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    private List<String> getFirstTipsIn(List<Page> pages) {
        return pages
                .stream()
                .filter(e -> !e.getTips().isEmpty())
                .map(Page::getTips)
                .findFirst()
                .orElse(new ArrayList<>());
    }

    private String decapitalise(String input) {
        return Character.toLowerCase(input.charAt(0)) + input.substring(1);
    }

    private boolean isValidTip(String tip) {
        return !tip.matches(".*\\d+\\..*")
                && !tip.contains("http")
                && !tip.contains("deze methode");
    }

    private String cleanTip(String tip) {

        return tip
                .replaceAll("\\(.*\\)", "")
                .replaceAll("\\[.*\\]", "")
                .replaceAll("hij/zij", "hij")
                .replaceAll("hem/haar", "hem");


    }

    private final DutchFirstPersonConverter firstPersonConverter = new DutchFirstPersonConverter();
    @Override
    public Optional<String> generateReply(IChatMessage message) {
        if (message.getUser().getScreenName().toLowerCase().contains("octaaf")) {

            String messageText = message.getText();

            // Check if it contains an action
            if (messageText.contains("!")) {
                String actionText = messageText.substring(0, messageText.indexOf('!')).replaceAll("[Aa]h,? ?", "");

                List<String> tips = new ArrayList<>();
                try {
                    tips = getFirstTipsIn(getPages(actionText));
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (!tips.isEmpty()) {
                    Optional<String> selectedTip = tipSelector.select(
                            tips.stream()
                                    .map(SentenceUtil::getFirstSentence)
                                    .map(String::trim)
                                    .filter(e -> e.length() > 0)
                                    .filter(this::isValidTip)
                                    .map(this::decapitalise)
                                    .map(this::cleanTip)
                                    .peek(e -> System.out.println("TIP: " + e)));

                    if (selectedTip.isPresent()) {
                        return Optional.of("Octaaf, ik heb nog een goede tip van mijn hobbyclub over "
                                + firstPersonConverter.firstToSecondPersonPronouns(actionText) + ": " + selectedTip.get());
                    }
                }
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

        return Optional.empty();
    }
}
