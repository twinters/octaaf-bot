package be.thomaswinters.samsonworld.octaaf;

import be.thomaswinters.generator.generators.IGenerator;
import be.thomaswinters.generator.selection.RouletteWheelSelection;
import be.thomaswinters.generator.streamgenerator.reacting.IReactingStreamGenerator;
import be.thomaswinters.twitter.bot.BehaviourCreator;
import be.thomaswinters.twitter.bot.TwitterBot;
import be.thomaswinters.twitter.bot.executor.TwitterBotExecutor;
import be.thomaswinters.twitter.exception.TwitterUnchecker;
import be.thomaswinters.twitter.tweetsfetcher.*;
import be.thomaswinters.twitter.tweetsfetcher.filter.AlreadyParticipatedFilter;
import be.thomaswinters.twitter.tweetsfetcher.filter.AlreadyRepliedToByOthersFilter;
import be.thomaswinters.twitter.userfetcher.ListUserFetcher;
import be.thomaswinters.twitter.util.TwitterLogin;
import be.thomaswinters.twitter.util.TwitterUtil;
import be.thomaswinters.util.DataLoader;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;

import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OctaafTwitterBot {

    private static final List<String> prohibitedWordsToAnswer = Stream.concat(
            DataLoader.readLinesUnchecked("explicit/bad-words.txt").stream(),
            DataLoader.readLinesUnchecked("explicit/sensitive-topics.txt").stream())
            .collect(Collectors.toList());

    public static void main(String[] args) throws TwitterException, IOException {
        TwitterBot octaafBot = new OctaafTwitterBot().build();
        new TwitterBotExecutor(octaafBot).run(args);

    }

    public TwitterBot build() throws IOException {


        long samsonBotsList = 1006565134796500992L;

        Twitter octaafTwitter = TwitterLogin.getTwitterFromEnvironment("octaaf");

        OctaafStoefGenerator octaafStoefGenerator = new OctaafStoefGenerator();

        // Bot friends
        Collection<User> botFriends = ListUserFetcher.getUsers(octaafTwitter, samsonBotsList);
        TweetsFetcherCache botFriendsTweetsFetcher =
                new AdvancedListTweetsFetcher(octaafTwitter, samsonBotsList, false, true)
//                new ListTweetsFetcher(octaafTwitter, samsonBotsList)
                        .cache(Duration.ofMinutes(5));
        AlreadyRepliedToByOthersFilter alreadyRepliedToByOthersFilter =
                new AlreadyRepliedToByOthersFilter(botFriendsTweetsFetcher);

        ITweetsFetcher tweetsToAnswerOctaaf =
                TwitterBot.MENTIONS_RETRIEVER.apply(octaafTwitter)
                        .combineWith(
                                new TimelineTweetsFetcher(octaafTwitter)
                                        .combineWith(
                                                botFriendsTweetsFetcher)
                                        .filter(TwitterUnchecker.uncheck(AlreadyParticipatedFilter::new, octaafTwitter, 3))
                        )
                        // Filter out botfriends tweets randomly
                        .filterRandomlyIf(octaafTwitter, e -> botFriends.contains(e.getUser()), 1, 15)
                        // Filter out own tweets & retweets
                        .filterOutRetweets()
                        // Filter out already replied to messages
                        .filterRandomlyIf(octaafTwitter, status ->
                                !status.getUser().getScreenName().toLowerCase().contains("samson") &&
                                        alreadyRepliedToByOthersFilter.test(status), 1, 5)
                        .filterOutOwnTweets(octaafTwitter)
                        .filterOutMessagesWithWords(prohibitedWordsToAnswer);


        IGenerator<Status> tweetsToQuoteRetweetOctaaf =
                new TweetsFetcherCombiner(
                        new SearchTweetsFetcher(octaafTwitter, "octaaf de bolle"),
                        new SearchTweetsFetcher(octaafTwitter, "octaaf", "samson"),
                        new SearchTweetsFetcher(octaafTwitter, "jeannine de bolle"),
                        new SearchTweetsFetcher(octaafTwitter, "jeanine de bolle"),
                        new SearchTweetsFetcher(octaafTwitter, "mevrouw praline"),
                        new SearchTweetsFetcher(octaafTwitter, "miranda", "de bolle"),
                        TwitterBot.MENTIONS_RETRIEVER.apply(octaafTwitter),
                        new TimelineTweetsFetcher(octaafTwitter),
                        botFriendsTweetsFetcher)
                        // Filter out own tweets & retweets
                        .filterOutRetweets()
                        .filterOutOwnTweets(octaafTwitter)
                        .filterOutMessagesWithWords(prohibitedWordsToAnswer)
                        .cache(Duration.ofMinutes(5))
                        .seed(() -> TwitterUnchecker.uncheck(TwitterUtil::getLastRealTweet, octaafTwitter))
                        .distinct()
                        .
                                reduceToGenerator(new RouletteWheelSelection<>(this::calculateQuoteRetweetFitnessFunction));


        return new TwitterBot(octaafTwitter,
                BehaviourCreator.createQuoterFromMessageReactor(
                        octaafStoefGenerator,
                        tweetsToQuoteRetweetOctaaf)
                        .retry(5),
                BehaviourCreator.fromMessageReactor(octaafStoefGenerator)
                        .retry(5),
                tweetsToAnswerOctaaf);
    }

    private double calculateQuoteRetweetFitnessFunction(Status status) {
        if (status.getUser().getScreenName().equals("JeannineBot")) {
            return 0.2d;
        } else if (status.getText().toLowerCase().contains("octaaf")) {
            return 20d;
        }
        return 5d;
    }
}
