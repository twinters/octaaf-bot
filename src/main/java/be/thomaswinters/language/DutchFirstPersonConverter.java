package be.thomaswinters.language;

import be.thomaswinters.sentence.SentenceUtil;
import com.google.common.collect.ImmutableSet;

import java.util.stream.Collectors;

public class DutchFirstPersonConverter {
    private final ImmutableSet<Character> vowels = ImmutableSet.of('a', 'e', 'i', 'o', 'u');
    private final ImmutableSet<String> tweeklanken = ImmutableSet.of("au", "oe", "ou", "ui", "eu", "ie", "ei", "ai");
    private final ImmutableSet<String> deurtjeOpenUitzonderingen = ImmutableSet.of("komen");

    public String thirdToFirstPersonPronouns(String bitOfText) {
        return thirdToOtherPersonPronouns(bitOfText, "ik", "mijn", "mij", "mijzelf");
    }

    public String firstToSecondPersonPronouns(String bitOfText) {
        return firstToOtherPersonPronouns(bitOfText, "jij", "jouw", "jou", "jezelf");
    }

    public String firstToThirdMalePersonPronouns(String bitOfText) {
        return firstToOtherPersonPronouns(bitOfText, "hij", "zijn", "hem", "zichzelf");
    }


    public String thirdToSecondPersonPronouns(String bitOfText) {
        return thirdToOtherPersonPronouns(bitOfText, "jij", "jouw", "jou", "jezelf");
    }

    private String thirdToOtherPersonPronouns(String bitOfText, String newSubject, String newObsessive, String newObject, String newReflective) {
        // Todo: "zich" toevoegen: work with collections?
        return convertPersonPronouns(bitOfText, "zij", "hun", "hen", "zichzelf", newSubject, newObsessive, newObject, newReflective);
    }

    private String firstToOtherPersonPronouns(String bitOfText, String newSubject, String newObsessive, String newObject, String newReflective) {
        return convertPersonPronouns(bitOfText, "ik", "mijn", "mij", "mijzelf", newSubject, newObsessive, newObject, newReflective);
    }


    private String convertPersonPronouns(String bitOfText,
                                         String oldSubject, String oldObsessive, String oldObject, String oldReflective,
                                         String newSubject, String newObsessive, String newObject, String newReflective) {
        return SentenceUtil.splitOnSpaces(bitOfText)
                .map(word -> {
                    String pureWord = SentenceUtil.removeNonLetters(word);
                    if (pureWord.equals(oldSubject)) {
                        return word.replaceAll(oldSubject, newSubject);
                    }
                    if (pureWord.equals(oldObsessive)) {
                        return word.replaceAll(oldObsessive, newObsessive);
                    }
                    if (pureWord.equals(oldObject)) {
                        return word.replaceAll(oldObject, newObject);
                    }
                    if (pureWord.equals(oldReflective)) {
                        return word.replaceAll(oldReflective, newReflective);
                    } else {
                        return word;
                    }
                })
                .collect(Collectors.joining(" "));
    }

    public String toFirstPersonSingularVerb(String verb) {

        if (verb.equals("zijn")) {
            return "ben";
        }
        if (verb.endsWith("ien")) {
            return verb.substring(0, verb.length() - 1);
        }
        if (verb.endsWith("oen")) {
            return verb.substring(0, verb.length() - 1);
        }
        if (verb.endsWith("aan")) {
            return verb.substring(0, verb.length() - 2);
        }
        if (verb.contains("en")) {
            String result = verb.substring(0, verb.lastIndexOf("en"));


            // Deurtje open lettertje lopen
            if (result.length() >= 2
                    && !vowels.contains(result.charAt(result.length() - 1))
                    && vowels.contains(result.charAt(result.length() - 2))
                    && (result.length() >= 3 && !tweeklanken.contains(result.substring(result.length() - 3, result.length() - 1)))
                    // Verdubbel geen i
                    && result.charAt(result.length() - 2) != 'i'
                    // Uitzondering voor 'el'
                    && !(result.substring(result.length() - 2,result.length() - 1).equals("el"))
                    ) {
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
//        throw new RuntimeException("HELP " + verb);
//        return "XX_" + verb + "_XX";
        return verb;
    }
}
