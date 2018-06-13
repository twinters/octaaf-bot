package be.thomaswinters.samsonworld.octaaf.experiments;

import be.thomaswinters.samsonworld.octaaf.OctaafStoefGenerator;

import java.io.IOException;

public class OctaafStoefExperiments {

    public static void main(String[] args) throws IOException {

        OctaafStoefGenerator octaaf = new OctaafStoefGenerator();
        System.out.println(octaaf.generateRelated("Aheuum. Aheuuuum. Aheum.\n" +
                "Aan allen die luiheid overwinnen: proficiat.\n" +
                "Aan allen die werklust overwinnen: ook proficiat."));


    }
}
