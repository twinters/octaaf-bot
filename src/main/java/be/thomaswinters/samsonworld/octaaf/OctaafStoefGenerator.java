package be.thomaswinters.samsonworld.octaaf;

import be.thomaswinters.action.ActionDescription;
import be.thomaswinters.action.ActionExtractor;
import be.thomaswinters.chatbot.IChatBot;
import be.thomaswinters.chatbot.data.IChatMessage;
import be.thomaswinters.generator.selection.ISelector;
import be.thomaswinters.generator.selection.RouletteWheelSelection;
import be.thomaswinters.generator.streamgenerator.reacting.IReactingStreamGenerator;
import be.thomaswinters.random.Picker;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OctaafStoefGenerator implements IChatBot, IReactingStreamGenerator<String, String> {

    private final Set<String> prohibitedActions = Set.of("betekenen", "gaan", "zullen");
    private final Set<String> prohibitedSubjects = Set.of("en", "jij");
    private final Set<ActionDescription> prohibitedFullActions =
            Set.of(
                    new ActionDescription("zijn", ""),
                    new ActionDescription("zijn", "naar"),
                    new ActionDescription("worden", ""),
                    new ActionDescription("hebben", ""),
                    new ActionDescription("gaan", ""),
                    new ActionDescription("houden", ""),
                    new ActionDescription("hebben", "honger"));

    private final ActionExtractor actionExtractor;

    public OctaafStoefGenerator() throws IOException {
        this.actionExtractor = new ActionExtractor();
    }

    @Override
    public Optional<String> generateReply(IChatMessage message) {
        if (message.getUser().getScreenName().toLowerCase().equals("samsonrobot")) {
            String[] betweenQuotes =StringUtils.substringsBetween(message.getText(), "\"", "\"");
            if (betweenQuotes != null) {
                System.out.println(Arrays.asList(betweenQuotes));
                return Picker.pickOptional(Stream.of(
                        betweenQuotes)
                        .flatMap(this::generateStream).collect(Collectors.toList()));
            } else {
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
                    String firstPersonAction = toFirstPerson(chosen.getVerb());
                    return ("Ah, " + chosen.getRestOfSentence() + " " + chosen.getVerb() + "! " +
                            "Dat is toevallig een van mijn specialiteiten! Mijn Miranda zegt dat ook altijd: 'Pa,' zegt ze, " +
                            "'zoals jij " + chosen.getRestOfSentence() + " kan " + chosen.getVerb() + "...' ja zo "
                            + firstPersonAction + " ik " + chosen.getRestOfSentence() + " hé!").trim().replaceAll("\\s{2,}", " ");
                });
    }

    private String toFirstPerson(String verb) {
        if (verb.contains("en")) {
            String result = verb.substring(0, verb.lastIndexOf("en"));
            if (result.length() >= 2 && result.charAt(result.length() - 1) == result.charAt(result.length() - 2)) {
                return result.substring(0, result.length() - 1);
            }
        }
        if (verb.equals("zijn")) {
            return "ben";
        }
        return "XX_" + verb + "_XX";
    }


}
