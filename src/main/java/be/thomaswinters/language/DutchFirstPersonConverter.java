package be.thomaswinters.language;

import be.thomaswinters.sentence.SentenceUtil;
import com.google.common.collect.ImmutableSet;

import java.util.stream.Collectors;

public class DutchFirstPersonConverter {
    private final ImmutableSet<Character> vowels = ImmutableSet.of('a', 'e', 'i', 'o', 'u');
    private final ImmutableSet<String> tweeklanken = ImmutableSet.of("au", "oe", "ou", "ui", "eu", "ie");
    private final ImmutableSet<String> deurtjeOpenUitzonderingen = ImmutableSet.of("komen");

    public String toFirstPersonPronouns(String bitOfText) {
        return SentenceUtil.splitOnSpaces(bitOfText)
                .map(word -> {
                    String pureWord = SentenceUtil.removeNonLetters(word);
                    switch (pureWord) {
                        case "zij":
                            return word.replaceAll("zij", "ik");
                        case "hun":
                            return word.replaceAll("hun", "mijn");
                        case "hen":
                            return word.replaceAll("hen", "mij");
                        case "zichzelf":
                            return word.replaceAll("zichzelf", "mijzelf");
                        default:
                            return word;
                    }
                })
                .collect(Collectors.joining(" "));
    }

    public String toFirstPersonSingularVerb(String verb) {
        if (verb.contains("en")) {
            String result = verb.substring(0, verb.lastIndexOf("en"));


            // Deurtje open lettertje lopen
            if (result.length() >= 2
                    && !vowels.contains(result.charAt(result.length() - 1))
                    && vowels.contains(result.charAt(result.length() - 2))
                    && !tweeklanken.contains(result.substring(result.length() - 3, result.length() - 1))) {
                if (deurtjeOpenUitzonderingen.stream().noneMatch(verb::endsWith)) {
                    result = result.substring(0, result.length() - 1)
                            // Repeat last vowel
                            + result.charAt(result.length() - 2)
                            // put real last letter at end.
                            + result.charAt(result.length() - 1);
                }
            }
            // Dubbele letters vermijden
            else if (result.length() >= 2 && result.charAt(result.length() - 1) == result.charAt(result.length() - 2)) {
                result = result.substring(0, result.length() - 1);
            }


            if (result.charAt(result.length() - 1) == 'v') {
                result = result.substring(0, result.length() - 1) + 'f';
            }
            if (result.charAt(result.length() - 1) == 'z') {
                result = result.substring(0, result.length() - 1) + 's';
            }
            return result;
        }
        if (verb.equals("zijn")) {
            return "ben";
        }
        return "XX_" + verb + "_XX";
    }
}
