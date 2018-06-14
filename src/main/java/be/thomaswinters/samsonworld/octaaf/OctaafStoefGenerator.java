package be.thomaswinters.samsonworld.octaaf;

import be.thomaswinters.action.ActionExtractor;
import be.thomaswinters.generator.generators.reacting.IReactingGenerator;
import be.thomaswinters.random.Picker;
import be.thomaswinters.action.ActionDescription;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class OctaafStoefGenerator implements IReactingGenerator<String,String> {

    private final Set<String> prohibitedActions = Set.of("betekenen");

    private final ActionExtractor actionExtractor;

    public OctaafStoefGenerator() throws IOException {
        this.actionExtractor = new ActionExtractor();
    }

    @Override
    public Optional<String> generateRelated(String input) {
        List<ActionDescription> actionDescriptions;
        try {
            actionDescriptions = actionExtractor.extractAction(input);
        } catch (IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }

        // Filter out
        actionDescriptions = actionDescriptions.stream()
                .filter(e->!prohibitedActions.contains(e.getVerb()))
                .collect(Collectors.toList());


        if (!actionDescriptions.isEmpty()) {
            ActionDescription chosen = Picker.pick(actionDescriptions);
            String firstPersonAction = toFirstPerson(chosen.getVerb());
            String result =("Ah, " + chosen.getRestOfSentence() + " " + chosen.getVerb() + "! " +
                    "Dat is toevallig een van mijn specialiteiten! Mijn Miranda zegt dat ook altijd: 'Pa,' zegt ze, " +
                    "'zoals jij "+chosen.getRestOfSentence()+" kan "+chosen.getVerb()+"...', ja zo "
                    +firstPersonAction+" ik "+chosen.getRestOfSentence()+" hé!").trim().replaceAll("\\s{2,}", " ");
            return Optional.of(result);
        }

        System.out.println("Nothing found for " + input);
        return Optional.empty();
    }

    private String toFirstPerson(String verb) {
        if (verb.contains("en")) {
            return verb.substring(0, verb.lastIndexOf("en"));
        }
        if (verb.equals("zijn")) {
            return "ben";
        }
        return "XX_"+verb+"_XX";
    }


}
