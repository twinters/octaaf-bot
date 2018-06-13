package be.thomaswinters.pos.data;

import java.util.List;
import java.util.stream.Collectors;

public class WordLemmaPOS extends WordPOS {
    private final List<LemmaPOS> lemmas;

    public WordLemmaPOS(WordPOS wordPos, List<LemmaPOS> lemmas) {
        super(wordPos.getWord(), wordPos.getTag());
        this.lemmas = lemmas;
    }

    public List<LemmaPOS> getLemmas() {
        return lemmas;
    }

    @Override
    public String toString() {
        return super.toString() + '<' + getLemmas().stream()
                .map(LemmaPOS::toString)
                .collect(Collectors.joining("|")) + '>';
    }
}
