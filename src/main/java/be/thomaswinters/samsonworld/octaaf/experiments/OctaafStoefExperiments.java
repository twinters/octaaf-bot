package be.thomaswinters.samsonworld.octaaf.experiments;

import be.thomaswinters.twitter.tweetsfetcher.UserTweetsFetcher;
import be.thomaswinters.twitter.util.TwitterLogin;
import be.thomaswinters.twitter.util.download.TweetDownloader;
import twitter4j.TwitterException;

import java.io.File;
import java.io.IOException;

public class OctaafStoefExperiments {

    public static void main(String[] args) throws IOException, TwitterException {

//        OctaafStoefGenerator octaaf = new OctaafStoefGenerator();
//        System.out.println(octaaf.generateRelated("Aheuum. Aheuuuum. Aheum.\n" +
//                "Aan allen die luiheid overwinnen: proficiat.\n" +
//                "Aan allen die werklust overwinnen: ook proficiat."));

        downloadTweets();

    }

    private static void downloadTweets() throws IOException, TwitterException {
        new TweetDownloader(
                new UserTweetsFetcher(
                        TwitterLogin.getTwitterFromEnvironment("octaaf."),
                        "burgemeesterbot",false,true)).downloadTo(new File("burgemeesterbot.txt"));

    }
}
