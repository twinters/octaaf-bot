package be.thomaswinters.samsonworld.octaaf;

import be.thomaswinters.generator.generators.IGenerator;
import be.thomaswinters.generator.selection.RouletteWheelSelection;
import be.thomaswinters.samsonworld.jeanine.JeannineTipsGenerator;
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
        DeBolleBots deBolleBots = new OctaafTwitterBot().buildDeBolleBots();
        TwitterBot octaafBot = deBolleBots.octaafBot;
        TwitterBot jeannineBot = deBolleBots.jeannineBot;

        // First reply to all unreplied, as this will be influenced by Octaaf.
        jeannineBot.replyToAllUnrepliedMentions();

        // Run Jeannine
        new Thread(() -> {
            try {
                new TwitterBotExecutor(jeannineBot).run(args);
            } catch (TwitterException e) {
                e.printStackTrace();
            }
        }).start();
        // Run octaafbot
        new Thread(() -> {
            try {
                new TwitterBotExecutor(octaafBot).run(args);
            } catch (TwitterException e) {
                e.printStackTrace();
            }
        }).start();

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
                        new SearchTweetsFetcher(jeannineTwitter, "jeannine de bolle"),
                        new SearchTweetsFetcher(jeannineTwitter, "jeanine de bolle"),
                        new SearchTweetsFetcher(jeannineTwitter, "mevrouw praline"),
                        new SearchTweetsFetcher(jeannineTwitter, "miranda","de bolle")
                )
                        .orElse(new TweetsFetcherCombiner(
                                TwitterBot.MENTIONS_RETRIEVER.apply(octaafTwitter),
                                new TimelineTweetsFetcher(octaafTwitter),
                                botFriendsTweetsFetcher))
                        // Filter out own tweets & retweets
                        .filterOutRetweets()
                        .filterOutOwnTweets(octaafTwitter)
                        .filterOutMessagesWithWords(prohibitedWordsToAnswer)
                        .cache(Duration.ofMinutes(5))
                        .seed(() -> TwitterUnchecker.uncheck(TwitterUtil::getLastRealTweet, octaafTwitter))
                        .distinct()
                        .
                                reduceToGenerator(new RouletteWheelSelection<>(this::calculateQuoteRetweetFitnessFunction));


        ITweetsFetcher tweetsToAnswerJeanine =
                TwitterBot.MENTIONS_RETRIEVER.apply(jeannineTwitter)
                        .combineWith(
                                new TimelineTweetsFetcher(jeannineTwitter)
                                        .combineWith(
                                                botFriendsTweetsFetcher)
                                        .filter(TwitterUnchecker.uncheck(AlreadyParticipatedFilter::new, jeannineTwitter, 4)),
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

//        IFitnessFunction<String> octaafQuoteRetweetSelector = tweetText -> {
//          if (tweetText.)
//        };


        TwitterBot octaafBot =
                new TwitterBot(octaafTwitter,
                        BehaviourCreator.createQuoterFromMessageReactor(
                                octaafStoefGenerator,
                                tweetsToQuoteRetweetOctaaf)
                                .retry(5),
                        BehaviourCreator.fromMessageReactor(octaafStoefGenerator)
                                .retry(5),
                        tweetsToAnswerOctaaf);

        TwitterBot jeannineBot =
                new TwitterBot(jeannineTwitter,
                        BehaviourCreator.empty(),
                        BehaviourCreator.fromMessageReactor(jeannineTipsGenerator)
                                .retry(5),
                        tweetsToAnswerJeanine);

        // Make Jeanine react to Octaaf tweets
        octaafBot.getTweeter().addPostListener(jeannineBot::replyToStatus);
        octaafBot.getTweeter().addReplyListener((message, toMessage) -> jeannineBot.replyToStatus(message));

        return new DeBolleBots(octaafBot, jeannineBot);
    }

    private double calculateQuoteRetweetFitnessFunction(Status status) {
        if (status.getUser().getScreenName().equals("JeannineBot")) {
            return 0.2d;
        } else if (status.getText().toLowerCase().contains("octaaf")) {
            return 20d;
        }
        return 5d;
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
