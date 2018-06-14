package be.thomaswinters.action;

import be.thomaswinters.pos.ProbabilisticPosTagger;
import be.thomaswinters.pos.data.LemmaPOS;
import be.thomaswinters.pos.data.POStag;
import be.thomaswinters.pos.data.WordLemmaPOS;
import be.thomaswinters.pos.data.WordPOS;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ActionExtractor {
    private final ProbabilisticPosTagger tagger;

    public ActionExtractor() throws IOException {
        this.tagger = new ProbabilisticPosTagger();
    }

    public List<ActionDescription> extractAction(String sentence) throws IOException {
        List<WordLemmaPOS> wordLemmas = tagger.tag(sentence);

        System.out.println(wordLemmas);
        return IntStream
                .range(0, wordLemmas.size())
                .filter(i -> wordLemmas.get(i).getTag().equals(POStag.VERB))
                // Double check if real verb
                .filter(i -> !wordLemmas.get(i).getLemmas().isEmpty())
                .mapToObj(i -> findFullAction(wordLemmas, i))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

    }

    private Optional<ActionDescription> findFullAction(List<WordLemmaPOS> wordLemmas, int i) {

        // If "te" before: not a real interesting verb
//        if ()

        // If next word is punctuation: look back!
        if (wordLemmas.size() <= i+1 || wordLemmas.get(i + 1).getTag().equals(POStag.PUNCTUATION)) {
            return findBackwardsFullAction(wordLemmas, i);
        }
        return findForwardsFullAction(wordLemmas, i);

    }

    private Optional<ActionDescription> findBackwardsFullAction(List<WordLemmaPOS> wordLemmas, int i) {
        String action = toStemVerb(wordLemmas.get(i));

        int startOfAction = i;
        while (startOfAction > 0
                && canBePartOfActionDescriptor(wordLemmas.get(startOfAction - 1))) {
            startOfAction -= 1;
        }


        return Optional.of(new ActionDescription(action,
                wordLemmas
                        .subList(startOfAction, Math.max(0, i))
                        .stream()
                        .map(WordPOS::getWord)
                        .collect(Collectors.joining(" ")))
        );

    }

    private final static Set<String> meaninglessVerbs = Set.of("zijn","hebben","worden","gaan","zullen");

    private boolean canBePartOfActionDescriptor(WordLemmaPOS wordLemmaPOS) {

        // Stop when hitting a punctuation
        if (wordLemmaPOS.getTag().equals(POStag.PUNCTUATION)) {
            return false;
        }
        // Adverbs are usually also a bad sign
        if (wordLemmaPOS.getTag().equals(POStag.ADVERB)) {
            return false;
        }
        // Stop when hitting an obvious onderwerp
        if (wordLemmaPOS.getWord().equals("ik") || wordLemmaPOS.getWord().equals("jij")) {
            return false;
        }
        // Stop when hitting a bijzin
        if (wordLemmaPOS.getWord().equals("die") || wordLemmaPOS.getWord().equals("dat")) {
            return false;
        }
        if (wordLemmaPOS.getTag().equals(POStag.VERB)) {
            if (!wordLemmaPOS.getLemmas().isEmpty()) {
                return !meaninglessVerbs.contains(wordLemmaPOS.getLemmas().get(0).getLemma());
            } else {
                System.out.println("EDGY VERB: " + wordLemmaPOS);
                return true;
            }
        }
        return true;

    }

    private String toStemVerb(WordLemmaPOS wordLemmaPOS) {
        List<LemmaPOS> lemmas = wordLemmaPOS.getLemmas();
        if (!lemmas.isEmpty()) {
            return lemmas.get(0).getLemma();
        }
        if (wordLemmaPOS.getWord().endsWith("en")) {
            return wordLemmaPOS.getWord();
        } else {
            throw new IllegalStateException("No lemma of verb left: TODO: form yourself / look up on wiktionary! " + wordLemmaPOS);
        }
    }

    private Optional<ActionDescription> findForwardsFullAction(List<WordLemmaPOS> wordLemmas, int i) {
        String action = toStemVerb(wordLemmas.get(i));

        int endOfAction = i;
        while (endOfAction < wordLemmas.size()-1
                && canBePartOfActionDescriptor(wordLemmas.get(endOfAction + 1))) {
            endOfAction += 1;
        }


        return Optional.of(new ActionDescription(action,
                wordLemmas
                        .subList(i+1,Math.min(wordLemmas.size(),endOfAction)+1)
                        .stream()
                        .map(WordPOS::getWord)
                        .collect(Collectors.joining(" ")))
        );
    }
}
