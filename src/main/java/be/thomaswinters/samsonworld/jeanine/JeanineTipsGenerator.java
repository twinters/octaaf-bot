package be.thomaswinters.samsonworld.jeanine;

import be.thomaswinters.chatbot.IChatBot;
import be.thomaswinters.chatbot.data.IChatMessage;
import be.thomaswinters.generator.fitness.IFitnessFunction;
import be.thomaswinters.generator.selection.ISelector;
import be.thomaswinters.generator.selection.TournamentSelection;
import be.thomaswinters.language.DutchFirstPersonConverter;
import be.thomaswinters.replacement.Replacer;
import be.thomaswinters.replacement.Replacers;
import be.thomaswinters.sentence.SentenceUtil;
import be.thomaswinters.textgeneration.domain.context.TextGeneratorContext;
import be.thomaswinters.textgeneration.domain.factories.command.CommandFactory;
import be.thomaswinters.textgeneration.domain.factories.command.SingleTextGeneratorArgumentCommandFactory;
import be.thomaswinters.textgeneration.domain.generators.ITextGenerator;
import be.thomaswinters.textgeneration.domain.generators.commands.LambdaSingleGeneratorArgumentCommand;
import be.thomaswinters.textgeneration.domain.generators.named.NamedGeneratorRegister;
import be.thomaswinters.textgeneration.domain.parsers.DeclarationsFileParser;
import be.thomaswinters.wikihow.WikiHowPageScraper;
import be.thomaswinters.wikihow.WikihowSearcher;
import be.thomaswinters.wikihow.data.Page;
import be.thomaswinters.wikihow.data.PageCard;
import org.jsoup.HttpStatusException;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class JeanineTipsGenerator implements IChatBot {
    private final WikihowSearcher searcher = new WikihowSearcher("nl");
    private final WikiHowPageScraper wikiHowPageScraper = new WikiHowPageScraper("nl");
    private final IFitnessFunction<String> tipFitnessFunction = e -> 1 / e.length();
    private final DutchFirstPersonConverter firstPersonConverter = new DutchFirstPersonConverter();
    private final ISelector<String> tipSelector = new TournamentSelection<>(tipFitnessFunction, 5);
    private final ITextGenerator templatedGenerator;
    private Replacers tipNegators = new Replacers(Arrays.asList(
            new Replacer("een", "geen", false, true),
            new Replacer("goed", "slecht", false, true),
            new Replacer("de meeste", "zeer weinig", false, true),
            new Replacer("niet meer", "nog steeds", false, true),
            new Replacer("niet", "zeker wel", false, true),
            new Replacer("ook", "zeker niet", false, true)
    ));

    public JeanineTipsGenerator() throws IOException {
        List<CommandFactory> customCommands = Arrays.asList(
                new SingleTextGeneratorArgumentCommandFactory(
                        "firstToThirdMalePersonPronouns",
                        e -> new LambdaSingleGeneratorArgumentCommand(e,
                                firstPersonConverter::firstToThirdMalePersonPronouns,
                                "firstToThirdMalePersonPronouns")),
                new SingleTextGeneratorArgumentCommandFactory(
                        "firstToSecondPersonPronouns",
                        e -> new LambdaSingleGeneratorArgumentCommand(e,
                                firstPersonConverter::firstToSecondPersonPronouns,
                                "firstToSecondPersonPronouns"))
        );
        this.templatedGenerator = DeclarationsFileParser.createTemplatedGenerator(
                ClassLoader.getSystemResource("templates/jeanine.decl"),
                customCommands
        );
    }

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
                && !tip.contains("deze methode")
                && !tip.contains("de methode")
                && !tip.contains("deze oefening");
    }

    private String cleanTip(String tip) {

        return tip
                .replaceAll("\\(.*\\)", "")
                .replaceAll("\\[.*\\]", "")
                .replaceAll("hij/zij", "hij")
                .replaceAll("hem/haar", "hem");


    }

    private String negateTip(String text) {
        String result = tipNegators.replace(text);
        System.out.println(
                "NEGATED TEXT:\t" + text + "\n" +
                        "TO RESULTING:\t" + result);
        return result;
    }

    @Override
    public Optional<String> generateReply(IChatMessage message) {
        if (message.getUser().getScreenName().toLowerCase().contains("octaaf")) {

            String messageText = message.getText();
            NamedGeneratorRegister register = new NamedGeneratorRegister();

            // Check if it contains an action
            if (messageText.contains("!")) {
                String actionText = messageText.substring(0, messageText.indexOf('!')).replaceAll("[Aa]h,? ?", "");

                List<String> actionWords = SentenceUtil.splitOnSpaces(actionText).collect(Collectors.toList());
                String actionVerb = actionWords.get(actionWords.size() - 1);
                Optional<String> actionDescription = actionWords.size() > 1 ?
                        Optional.of(SentenceUtil.joinWithSpaces(actionWords.subList(0, actionWords.size() - 1))) :
                        Optional.empty();

                register.createGenerator("actionVerb", actionVerb);
                actionDescription.ifPresent(e ->
                        register.createGenerator("actionDescription", e));


                List<String> tips = new ArrayList<>();
                try {
                    tips = getFirstTipsIn(getPages(actionText));
                } catch (HttpStatusException e) {
                    if (e.getStatusCode()==404) {
                        System.out.println("404 for " + actionText);
                    }
                    e.printStackTrace();
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
                                    .map(this::cleanTip));

                    if (selectedTip.isPresent()) {
                        String tip = selectedTip.get();
                        // Check if action is something inverted (burgemeester)
                        if (actionText.contains("niet") || actionText.contains("geen")) {
                            tip = negateTip(tip);
                        }
                        register.createGenerator("tip", tip);
                    }
                }
            }

            String result =
                    templatedGenerator.generate(
                            new TextGeneratorContext(register, true)
                    );
            if (result.trim().length() > 0) {
                return Optional.of(result);
            }
        }
        return Optional.empty();
    }
}
