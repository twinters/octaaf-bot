package be.thomaswinters.samsonworld.octaaf;

import be.thomaswinters.samsonworld.jeanine.JeanineTipsGenerator;
import be.thomaswinters.twitter.bot.GeneratorTwitterBot;
import be.thomaswinters.twitter.bot.TwitterBot;
import be.thomaswinters.twitter.bot.TwitterBotExecutor;
import be.thomaswinters.twitter.tweetsfetcher.AdvancedListTweetsFetcher;
import be.thomaswinters.twitter.tweetsfetcher.ITweetsFetcher;
import be.thomaswinters.twitter.tweetsfetcher.SearchTweetsFetcher;
import be.thomaswinters.twitter.tweetsfetcher.TimelineTweetsFetcher;
import be.thomaswinters.twitter.tweetsfetcher.filter.AlreadyParticipatedFilter;
import be.thomaswinters.twitter.userfetcher.ListUserFetcher;
import be.thomaswinters.twitter.util.TwitterLogin;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;

import java.io.IOException;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

import static be.thomaswinters.twitter.exception.TwitterUnchecker.uncheck;

public class OctaafTwitterBot {

    public static void main(String[] args) throws TwitterException, IOException {
        new TwitterBotExecutor(new OctaafTwitterBot().build()).run(args);

    }

    public TwitterBot build() throws IOException, TwitterException {
        long samsonBotsList = 1006565134796500992L;

        Twitter octaafTwitter = TwitterLogin.getTwitterFromEnvironment("octaaf.");
        Twitter jeanineTwitter = TwitterLogin.getTwitterFromEnvironment("jeanine.");

        OctaafStoefGenerator octaafStoefGenerator = new OctaafStoefGenerator();
        JeanineTipsGenerator jeanineTipsGenerator = new JeanineTipsGenerator();


        // Bot friends
        Collection<User> botFriends = ListUserFetcher.getUsers(octaafTwitter, samsonBotsList);
        ITweetsFetcher botFriendsTweetsFetcher =
                new AdvancedListTweetsFetcher(octaafTwitter, samsonBotsList, false, true);

        ITweetsFetcher tweetsToAnswer =
                TwitterBot.MENTIONS_RETRIEVER.apply(octaafTwitter)
                        .combineWith(
                                new TimelineTweetsFetcher(octaafTwitter),
                                botFriendsTweetsFetcher
                                        .combineWith(
                                                new TimelineTweetsFetcher(octaafTwitter))
                                        .filter(uncheck(AlreadyParticipatedFilter::new, octaafTwitter, 3)),
                                new SearchTweetsFetcher(octaafTwitter, "octaaf de bolle")
                                        .filterRandomly(octaafTwitter, 1, 4),
                                new SearchTweetsFetcher(octaafTwitter, "octaaf", "samson")
                                        .filterRandomly(octaafTwitter, 1, 4))
                        // Filter out botfriends tweets randomly
                        .filterRandomlyIf(octaafTwitter, e -> botFriends.contains(e.getUser()), 1, 5)
                        // Filter out own tweets & retweets
                        .filterOutRetweets()
                        .filterOutOwnTweets(octaafTwitter);

        TwitterBot octaafBot =
                new GeneratorTwitterBot(octaafTwitter,
                        Optional::empty,
                        octaafStoefGenerator,
                        tweetsToAnswer);

        TwitterBot jeanineBot =
                new GeneratorTwitterBot(jeanineTwitter,
                        Optional::empty,
                        jeanineTipsGenerator,
                        x -> Stream.of());
        octaafBot.addPostListener(jeanineBot::replyToStatus);
        octaafBot.addReplyListener((message, toMessage) -> jeanineBot.replyToStatus(message));

        return octaafBot;
    }
}
