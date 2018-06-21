package be.thomaswinters.samsonworld.octaaf;

import be.thomaswinters.samsonworld.jeanine.JeannineTipsGenerator;
import be.thomaswinters.twitter.bot.GeneratorTwitterBot;
import be.thomaswinters.twitter.bot.TwitterBot;
import be.thomaswinters.twitter.bot.TwitterBotExecutor;
import be.thomaswinters.twitter.exception.TwitterUnchecker;
import be.thomaswinters.twitter.tweetsfetcher.*;
import be.thomaswinters.twitter.tweetsfetcher.filter.AlreadyParticipatedFilter;
import be.thomaswinters.twitter.tweetsfetcher.filter.AlreadyRepliedToByOthersFilter;
import be.thomaswinters.twitter.userfetcher.ListUserFetcher;
import be.thomaswinters.twitter.util.TwitterLogin;
import be.thomaswinters.util.DataLoader;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;

import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OctaafTwitterBot {

    private static final List<String> prohibitedWordsToAnswer = Stream.concat(
            DataLoader.readLinesUnchecked("explicit/bad-words.txt").stream(),
            DataLoader.readLinesUnchecked("explicit/sensitive-topics.txt").stream())
            .collect(Collectors.toList());

    public static void main(String[] args) throws TwitterException, IOException {
        DeBolleBots deBolleBots = new OctaafTwitterBot().buildDeBolleBots();
        TwitterBot octaafBot = deBolleBots.octaafBot;
        TwitterBot jeanineBot = deBolleBots.jeannineBot;

        // First reply to all unreplied, as this will be influenced by Octaaf.
        jeanineBot.replyToAllUnrepliedMentions();

        // Run octaafbot
        new TwitterBotExecutor(octaafBot).run(args);

    }

    public DeBolleBots buildDeBolleBots() throws IOException {


        long samsonBotsList = 1006565134796500992L;

        Twitter octaafTwitter = TwitterLogin.getTwitterFromEnvironment("octaaf");
        Twitter jeannineTwitter = TwitterLogin.getTwitterFromEnvironment("jeannine");

        OctaafStoefGenerator octaafStoefGenerator = new OctaafStoefGenerator();
        JeannineTipsGenerator jeannineTipsGenerator = new JeannineTipsGenerator();


        // Bot friends
        Collection<User> botFriends = ListUserFetcher.getUsers(octaafTwitter, samsonBotsList);
        TweetsFetcherCache botFriendsTweetsFetcher =
                new AdvancedListTweetsFetcher(octaafTwitter, samsonBotsList, false, true)
                        .cache(Duration.ofMinutes(5));
        AlreadyRepliedToByOthersFilter alreadyRepliedToByOthersFilter =
                new AlreadyRepliedToByOthersFilter(botFriendsTweetsFetcher);

        ITweetsFetcher tweetsToAnswerOctaaf =
                TwitterBot.MENTIONS_RETRIEVER.apply(octaafTwitter)
                        .combineWith(
                                new TimelineTweetsFetcher(octaafTwitter)
                                        .combineWith(
                                                botFriendsTweetsFetcher)
                                        .filter(TwitterUnchecker.uncheck(AlreadyParticipatedFilter::new, octaafTwitter, 3)),
                                new SearchTweetsFetcher(octaafTwitter, "octaaf de bolle")
                                        .filterRandomly(octaafTwitter, 1, 3),
                                new SearchTweetsFetcher(octaafTwitter, "octaaf", "samson")
                                        .filterRandomly(octaafTwitter, 1, 3))
                        // Filter out botfriends tweets randomly
                        .filterRandomlyIf(octaafTwitter, e -> botFriends.contains(e.getUser()), 1, 18)
                        // Filter out own tweets & retweets
                        .filterOutRetweets()
                        // Filter out already replied to messages
                        .filterRandomlyIf(octaafTwitter, status ->
                                !status.getUser().getScreenName().toLowerCase().contains("samson") &&
                                        alreadyRepliedToByOthersFilter.test(status), 1, 5)
                        .filterOutOwnTweets(octaafTwitter)
                        .filterOutMessagesWithWords(prohibitedWordsToAnswer);


        ITweetsFetcher tweetsToAnswerJeanine =
                TwitterBot.MENTIONS_RETRIEVER.apply(jeannineTwitter)
                        .combineWith(
                                new TimelineTweetsFetcher(jeannineTwitter)
                                        .combineWith(
                                                botFriendsTweetsFetcher)
                                        .filter(TwitterUnchecker.uncheck(AlreadyParticipatedFilter::new, jeannineTwitter, 3)),
                                new SearchTweetsFetcher(jeannineTwitter, "jeannine de bolle")
                                        .combineWith(
                                                new SearchTweetsFetcher(jeannineTwitter, "jeanine de bolle"),
                                                new SearchTweetsFetcher(jeannineTwitter, "mevrouw praline")
                                        )
                                        .filterRandomly(jeannineTwitter, 1, 4))
                        // Filter out botfriends tweets randomly
                        .filterRandomlyIf(jeannineTwitter, e -> botFriends.contains(e.getUser()), 1, 20)
                        // Filter out own tweets & retweets
                        .filterOutRetweets()
                        // Filter out already replied to messages
                        .filterRandomlyIf(jeannineTwitter, alreadyRepliedToByOthersFilter, 1, 3)
                        .filterOutOwnTweets(jeannineTwitter)
                        .filterOutMessagesWithWords(prohibitedWordsToAnswer);

        TwitterBot octaafBot =
                new GeneratorTwitterBot(octaafTwitter,
                        Optional::empty,
                        octaafStoefGenerator,
                        tweetsToAnswerOctaaf);

        TwitterBot jeannineBot =
                new GeneratorTwitterBot(jeannineTwitter,
                        Optional::empty,
                        jeannineTipsGenerator,
                        tweetsToAnswerJeanine);
        octaafBot.addPostListener(jeannineBot::replyToStatus);
        octaafBot.addReplyListener((message, toMessage) -> jeannineBot.replyToStatus(message));

        return new DeBolleBots(octaafBot, jeannineBot);
    }

    public TwitterBot build() throws IOException, TwitterException {
        return buildDeBolleBots().octaafBot;
    }

    private static class DeBolleBots {
        private final TwitterBot octaafBot;
        private final TwitterBot jeannineBot;

        public DeBolleBots(TwitterBot octaafBot, TwitterBot jeanineBot) {
            this.octaafBot = octaafBot;
            this.jeannineBot = jeanineBot;
        }
    }
}
