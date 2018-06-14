package be.thomaswinters.action;

import be.thomaswinters.action.data.ActionDescription;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class ActionExtractorTest {

    private ActionExtractor actionExtractor;

    @Before
    public void setup() throws IOException {
        actionExtractor = new ActionExtractor();
    }

    @Test
    public void simple() throws IOException {
        assertEquals(Collections.singletonList(
                new ActionDescription("zijn", "blij")
                ),
                actionExtractor.extractAction("Ik ben blij."));
    }

    @Test
    public void simple_lowercase() throws IOException {
        assertEquals(Collections.singletonList(
                new ActionDescription("zijn", "blij")
                ),
                actionExtractor.extractAction("ik ben blij"));


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

        assertEquals(Arrays.asList(
                new ActionDescription("gebruiken", "een kurkentrekker"),
                new ActionDescription("gebruiken", "geen kurkentrekker")
                ),
                actionExtractor.extractAction("Aheuuum. Aheuuuuum. Aheum.\n" +
                        "        Aan allen die een kurkentrekker gebruiken: proficiat.\n" +
                        "        Aan allen die geen kurkentrekker gebruiken: ook proficiat."));


    }

//    @Test
    public void burgemeester_hard_tests() throws IOException {

        assertEquals(Arrays.asList(
                new ActionDescription("trainen", "om een ninja te worden"),
                new ActionDescription("niet trainen", "om een ninja te worden")
                ),
                actionExtractor.extractAction("Aheuum. Aheuuuum. Aheum.\n" +
                        "Aan allen die trainen om een ninja te worden: proficiat.\n" +
                        "Aan allen die niet trainen om een ninja te worden: ook proficiat."));
        assertEquals(Arrays.asList(
                new ActionDescription("verwijderen", "slijm uit hun keel"),
                new ActionDescription("verwijderen", "slijm aan hun keel")
                ),
                actionExtractor.extractAction("Aheuuum. Aheuuuum. Aheum.\n" +
                        "Aan allen die slijm verwijderen uit hun keel: proficiat.\n" +
                        "Aan allen die slijm verwijderen aan hun keel: ook proficiat."));
    }

}