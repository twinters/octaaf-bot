package be.thomaswinters.samsonworld.octaaf.experiments;

import be.thomaswinters.chatbot.data.ChatMessage;
import be.thomaswinters.chatbot.data.ChatUser;
import be.thomaswinters.chatbot.data.IChatUser;
import be.thomaswinters.samsonworld.octaaf.OctaafStoefGenerator;
import be.thomaswinters.twitter.tweetsfetcher.UserTweetsFetcher;
import be.thomaswinters.twitter.util.TwitterLogin;
import be.thomaswinters.twitter.util.download.TweetDownloader;
import be.thomaswinters.util.DataLoader;
import twitter4j.TwitterException;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

public class OctaafStoefExperiments {

    public static void main(String[] args) throws IOException, TwitterException {

        reactTo("experiments\\samson.txt", "SamsonRobot");
//        downloadTweets();

    }

    private static void reactTo(String file, String botName) throws IOException {
        OctaafStoefGenerator octaaf = new OctaafStoefGenerator();

        IChatUser chatUser = new ChatUser(botName);

        DataLoader.readLines(file)
//                .subList(0, 50)
                .stream()
                .peek(System.out::println)
                .map(e->new ChatMessage(Optional.empty(),e,chatUser))
                .map(octaaf::generateRelated)
                .forEach(out -> {
                    if (out.isPresent()) {
                        System.out.println("OCTAAF: " + out.get());
                    } else {
                        System.out.println("NIKS");
                    }
                    System.out.println("\n");
                });

    }

    private static void downloadTweets() throws IOException, TwitterException {
        new TweetDownloader(
                new UserTweetsFetcher(
                        TwitterLogin.getTwitterFromEnvironment("octaaf."),
                        "albertbot", false, true)).downloadTo(new File("alberto.txt"));

    }
}
