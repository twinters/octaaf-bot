package be.thomaswinters.action;

import java.util.Objects;

public class ActionDescription {
    private final String verb;
    private final String restOfSentence;

    public ActionDescription(String verb, String restOfSentence) {
        this.verb = verb;
        this.restOfSentence = restOfSentence;
    }

    public String getVerb() {
        return verb;
    }

    public String getRestOfSentence() {
        return restOfSentence;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ActionDescription that = (ActionDescription) o;
        return Objects.equals(verb, that.verb) &&
                Objects.equals(restOfSentence, that.restOfSentence);
    }

    @Override
    public int hashCode() {

        return Objects.hash(verb, restOfSentence);
    }

    @Override
    public String toString() {
        return verb + " : " + restOfSentence;
    }
}
