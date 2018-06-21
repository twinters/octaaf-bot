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

    private final Set<ActionDescription> prohibitedFullActions =
            Set.of(
                    // Prohibited verbs
                    new ActionDescription("betekenen", ".*"),
                    new ActionDescription("gaan", ".*"),
                    new ActionDescription("zullen", ".*"),
                    new ActionDescription("kunnen", ".*"),

                    // Prohibited Subjects
                    new ActionDescription(".*", "en"),
                    new ActionDescription(".*", "en .*"),
                    new ActionDescription(".*", "of"),
                    new ActionDescription(".*", "of .*"),
                    new ActionDescription(".*", "jij"),
                    new ActionDescription(".*", "jullie"),
                    new ActionDescription(".*", "wij"),
                    new ActionDescription(".*", ".*kan.*"),
                    new ActionDescription(".*", ".*voltooid deelwoord.*"),
                    new ActionDescription(".*", ".*genitief van de.*"),

                    // Prohibited full actions
                    new ActionDescription("zijn", ""),
                    new ActionDescription("zijn", "naar"),
                    new ActionDescription("zijn", ".* te .*"),
                    new ActionDescription("worden", ""),
                    new ActionDescription("hebben", ""),
                    new ActionDescription("gaan", ""),
                    new ActionDescription("houden", ""),
                    new ActionDescription("stellen", ""),
                    new ActionDescription("geven", ""),
                    new ActionDescription("geven", "te"),
                    new ActionDescription("hebben", "honger"),

                    // Albertobot counter

                    new ActionDescription("zijn", ".*sterven van de honger.*")
            );

    private final ActionExtractor actionExtractor;
    private final Set<String> voorzetsels = Set.of("af", "toe", "weg", "op", "binnen", "door", "in", "langs",
            "om", "over", "rond", "uit", "voorbij", "aan");
    private final Set<String> voorzetselsUitzonderingPrefixen = Set.of("inter", "intimideer", "installeer", "investeer",
            "innoveer", "overtref", "overkomen", "overwegen", "overlasten", "overschrijd", "overtuig");

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
        if (message.getUser().getScreenName().toLowerCase().equals("AlbertBot")) {
            if (message.getText().contains("AL-BER-TO")) {
                return Optional.empty();
            }
        }
        if (message.getUser().getScreenName().toLowerCase().equals("JeannineBot")) {
            if ((message.getText().contains("tip")
                    || message.getText().contains("advies"))
                    || message.getText().contains("onthoud")) {
                if (Math.random() < .8d) {
                    return Optional.of("Ja moeke, ja!");
                }
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
//                .peek(e -> {
//                    if (prohibitedFullActions.stream()
//                            .noneMatch(action -> e.getVerb().matches(action.getVerb())
//                                    && e.getRestOfSentence().matches(action.getRestOfSentence()))) {
//                        System.out.println("°ALLOWED: " + e);
//                    } else {
//                        System.out.println("_BLOCKED: " + e);
//                    }
//                })
                .filter(e -> prohibitedFullActions.stream()
                        .noneMatch(action -> e.getVerb().matches(action.getVerb())
                                && e.getRestOfSentence().matches(action.getRestOfSentence())))
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
