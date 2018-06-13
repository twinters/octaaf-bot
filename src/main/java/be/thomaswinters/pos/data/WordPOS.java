package be.thomaswinters.pos.data;

import java.util.Objects;

public class WordPOS {
    private final String word;
    private final POStag tag;
    
    public WordPOS(String word, POStag tag) {
        this.word = word;
        this.tag = tag;
    }

    public POStag getTag() {
        return tag;
    }

    public String getWord() {
        return word;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WordPOS wordPOS = (WordPOS) o;
        return Objects.equals(word, wordPOS.word) &&
                tag == wordPOS.tag;
    }

    @Override
    public int hashCode() {

        return Objects.hash(word, tag);
    }
}
