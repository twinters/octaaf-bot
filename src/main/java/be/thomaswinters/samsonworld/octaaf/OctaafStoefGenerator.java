package be.thomaswinters.samsonworld.octaaf;

import be.thomaswinters.action.ActionExtractor;
import be.thomaswinters.action.data.ActionDescription;
import be.thomaswinters.chatbot.IChatBot;
import be.thomaswinters.chatbot.data.IChatMessage;
import be.thomaswinters.generator.selection.ISelector;
import be.thomaswinters.generator.selection.RouletteWheelSelection;
import be.thomaswinters.generator.streamgenerator.reacting.IReactingStreamGenerator;
import be.thomaswinters.language.DutchFirstPersonConverter;
import be.thomaswinters.random.Picker;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OctaafStoefGenerator implements IChatBot, IReactingStreamGenerator<String, String> {

    private final DutchFirstPersonConverter firstPersonConverter = new DutchFirstPersonConverter();

    private final Set<String> prohibitedActions = Set.of("betekenen", "gaan", "zullen", "kunnen");
    private final Set<String> prohibitedSubjects = Set.of("en", "jij", "jullie", "wij", "kan");
    private final Set<ActionDescription> prohibitedFullActions =
            Set.of(
                    new ActionDescription("zijn", ""),
                    new ActionDescription("zijn", "naar"),
                    new ActionDescription("worden", ""),
                    new ActionDescription("hebben", ""),
                    new ActionDescription("gaan", ""),
                    new ActionDescription("houden", ""),
                    new ActionDescription("stellen", ""),
                    new ActionDescription("geven", ""),
                    new ActionDescription("geven", "te"),
                    new ActionDescription("hebben", "honger"));

    private final ActionExtractor actionExtractor;
    private final Set<String> voorzetsels = Set.of("af", "toe", "weg", "op", "binnen", "door", "in", "langs",
            "om", "over", "rond", "uit", "voorbij");
    private final Set<String> voorzetselsUitzonderingPrefixen = Set.of("inter","intimideer","installeer","investeer",
            "innoveer","overtref","overkomen","overwegen","overlasten","overschrijd","overtuig");

    public OctaafStoefGenerator() throws IOException {
        this.actionExtractor = new ActionExtractor();
    }

    @Override
    public Optional<String> generateReply(IChatMessage message) {
        if (message.getUser().getScreenName().toLowerCase().equals("samsonrobot")) {
            String[] betweenQuotes = StringUtils.substringsBetween(message.getText(), "\"", "\"");
            if (betweenQuotes != null) {
                return Picker.pickOptional(Stream.of(
                        betweenQuotes)
                        .flatMap(this::generateStream)
                        .collect(Collectors.toList()));
            } else {
                return Optional.empty();
            }
        }
        if (message.getUser().getScreenName().toLowerCase().equals("burgemeesterbot")) {
            if (message.getText().contains("iemand anders zijn")) {
                return Optional.empty();
            }
        }
        if (message.getUser().getScreenName().toLowerCase().equals("AlbertoBot")) {
            if (message.getText().contains("AL-BER-TO")) {
                return Optional.empty();
            }
        }
        return generateRelated(message.getText());
    }

    //    @Override
    public Optional<String> generateRelated(String input) {
        List<String> options = generateStream(input).collect(Collectors.toList());

        if (options.isEmpty()) {
            return Optional.empty();
        }

        // Chance is with i = index from last place of array, with last place i=1:
        // 1 / i^2
        ISelector<String> selector = new RouletteWheelSelection<>(
                option -> 1 / Math.pow(options.size() - options.lastIndexOf(option), 2));
        return selector.select(options.stream());

    }

    private Optional<String> getVoorzetselFor(String firstPersonVerb) {
        return voorzetsels.stream()
                .filter(e -> firstPersonVerb.startsWith(e)
                        && voorzetselsUitzonderingPrefixen
                        .stream()
                        .noneMatch(firstPersonVerb::startsWith))
                .filter(vz -> vz.length() < firstPersonVerb.length())
                .findFirst();
    }

    @Override
    public Stream<String> generateStream(String input) {
        List<ActionDescription> actionDescriptions;
        try {
            actionDescriptions = actionExtractor.extractAction(input);
        } catch (IOException e) {
            e.printStackTrace();
            return Stream.of();
        }

        return actionDescriptions
                .stream()
                .filter(e -> !prohibitedActions.contains(e.getVerb()))
                .filter(e -> !prohibitedSubjects.contains(e.getRestOfSentence()))
                .filter(e -> !prohibitedFullActions.contains(e))
                .map(chosen -> {
                    String firstPersonAction = firstPersonConverter.toFirstPersonSingularVerb(chosen.getVerb());
                    Optional<String> optionalVoorzetsel = getVoorzetselFor(firstPersonAction);
                    if (optionalVoorzetsel.isPresent()) {
                        firstPersonAction = firstPersonAction.substring(optionalVoorzetsel.get().length());
                    }

                    String restOfSentence = firstPersonConverter.thirdToFirstPersonPronouns(chosen.getRestOfSentence());
                    String restOfSentenceSecondPerson = firstPersonConverter.thirdToSecondPersonPronouns(chosen.getRestOfSentence());
                    return ("Ah, " + restOfSentence + " " + chosen.getVerb() + "! " +
                            "Dat is nu toevallig één van mijn specialiteiten! Mijn Miranda zegt dat ook altijd: \"Pa,\" zegt ze, " +
                            "\"zoals jij " + restOfSentenceSecondPerson + " kan " + chosen.getVerb() + "...\" ja zo "
                            + firstPersonAction + " ik "
                            + restOfSentence
                            + optionalVoorzetsel.map(s -> " " + s).orElse("")
                            + " hé!")
                            .trim().replaceAll("\\s{2,}", " ");
                });
    }


}
