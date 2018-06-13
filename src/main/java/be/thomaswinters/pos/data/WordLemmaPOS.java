package be.thomaswinters.pos.data;

import java.util.List;

public class WordLemmaPOS extends WordPOS {
    private final List<LemmaPOS> lemmas;

    public WordLemmaPOS(WordPOS wordPos, List<LemmaPOS> lemmas) {
        super(wordPos.getWord(), wordPos.getTag());
        this.lemmas = lemmas;
    }

    public List<LemmaPOS> getLemmas() {
        return lemmas;
    }
}
