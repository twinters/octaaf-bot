package be.thomaswinters.samsonworld.octaaf.experiments;

import be.thomaswinters.chatbot.data.ChatMessage;
import be.thomaswinters.chatbot.data.ChatUser;
import be.thomaswinters.chatbot.data.IChatUser;
import be.thomaswinters.samsonworld.octaaf.OctaafStoefGenerator;
import be.thomaswinters.twitter.tweetsfetcher.SearchTweetsFetcher;
import be.thomaswinters.twitter.util.TwitterLogin;
import be.thomaswinters.twitter.util.download.TweetDownloader;
import be.thomaswinters.util.DataLoader;
import twitter4j.Twitter;
import twitter4j.TwitterException;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

public class OctaafStoefExperiments {

    public static void main(String[] args) throws IOException, TwitterException {
//        singleTest("is zingen ook een van jouw specialiteiten?");
//        reactTo("experiments\\burgemeesterbot.txt", "BurgemeesterBot");
        allReactingsTo("experiments\\search-samson-gert.txt");
//                downloadTweets();

    }

    private static void singleTest(String s) throws IOException {
        OctaafStoefGenerator octaaf = new OctaafStoefGenerator();
        System.out.println(octaaf.generateRelated(s));
    }

    private static void reactTo(String file, String botName) throws IOException {
        OctaafStoefGenerator octaaf = new OctaafStoefGenerator();

        IChatUser chatUser = new ChatUser(botName);

        DataLoader.readLines(file)
//                .subList(0, 50)
                .stream()
                .peek(System.out::println)
                .map(e -> new ChatMessage(Optional.empty(), e, chatUser))
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

    private static void allReactingsTo(String file) throws IOException {
        OctaafStoefGenerator octaaf = new OctaafStoefGenerator();

        DataLoader.readLines(file)
//                .subList(0, 50)
                .stream()
                .peek(System.out::println)
                .map(octaaf::generateStream)
                .forEach(out -> {
                    System.out.println("OCTAAF:\n" + out.collect(Collectors.joining("\n")) + "\n\n");
                });

    }

    private static void downloadTweets() throws IOException, TwitterException {
        Twitter twitter = TwitterLogin.getTwitterFromEnvironment("octaaf.");
        new TweetDownloader(
//                new UserTweetsFetcher(
//                        twitter,
//                        "albertbot", false, true)
                new SearchTweetsFetcher(twitter, Arrays.asList("samson", "gert"))
                        .combineWith(
                                new SearchTweetsFetcher(twitter, Arrays.asList("samson", "octaaf")))
                        .combineWith(
                                new SearchTweetsFetcher(twitter, Arrays.asList("samson", "alberto")))
                        .combineWith(
                                new SearchTweetsFetcher(twitter, Arrays.asList("samson", "burgemeester")))

        ).downloadTo(new File("search-samson-gert.txt"));

    }
}
