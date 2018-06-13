package be.thomaswinters.pos.data;

import java.util.Objects;

public class LemmaPOS {
    private final String lemma;
    private final String POS;

    public LemmaPOS(String lemma, String POS) {

        this.lemma = lemma;
        this.POS = POS;
    }

    public String getLemma() {
        return lemma;
    }

    public String getPOS() {
        return POS;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LemmaPOS lemmaPOS = (LemmaPOS) o;
        return Objects.equals(lemma, lemmaPOS.lemma) &&
                Objects.equals(POS, lemmaPOS.POS);
    }

    @Override
    public int hashCode() {

        return Objects.hash(lemma, POS);
    }
}
