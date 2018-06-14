package be.thomaswinters.samsonworld.octaaf.experiments;

import be.thomaswinters.samsonworld.octaaf.OctaafStoefGenerator;
import be.thomaswinters.twitter.tweetsfetcher.UserTweetsFetcher;
import be.thomaswinters.twitter.util.TwitterLogin;
import be.thomaswinters.twitter.util.download.TweetDownloader;
import be.thomaswinters.util.DataLoader;
import twitter4j.TwitterException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;

public class OctaafStoefExperiments {

    public static void main(String[] args) throws IOException, TwitterException {

        OctaafStoefGenerator octaaf = new OctaafStoefGenerator();


        DataLoader.readLines( "experiments\\gert_bot.txt")
                .subList(0, 50)
                .stream()
                .map(octaaf::generateRelated)
                .forEach(System.out::println);

        System.out.println(octaaf.generateRelated("Aheuum. Aheuuuum. Aheum. Aan allen die luiheid overwinnen: proficiat. Aan allen die werklust overwinnen: ook proficiat."));

//        downloadTweets();

    }

    private static void downloadTweets() throws IOException, TwitterException {
        new TweetDownloader(
                new UserTweetsFetcher(
                        TwitterLogin.getTwitterFromEnvironment("octaaf."),
                        "gert_bot", false, true)).downloadTo(new File("gert_bot.txt"));

    }
}
