package be.thomaswinters.samsonworld.octaaf;

import be.thomaswinters.samsonworld.octaaf.data.ActionDescription;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class ActionExtractorTest {

    private ActionExtractor actionExtractor;

    @Before
    public void setup() throws IOException {
        actionExtractor = new ActionExtractor();
    }

    @Test
    public void burgemeester_tests() throws IOException {
        assertEquals(Arrays.asList(
                new ActionDescription("overwinnen", "luiheid"),
                new ActionDescription("overwinnen", "werklust")
                ),
                actionExtractor.extractAction("Aheuum. Aheuuuum. Aheum.\n" +
                        "Aan allen die luiheid overwinnen: proficiat.\n" +
                        "Aan allen die werklust overwinnen: ook proficiat."));
    }

}