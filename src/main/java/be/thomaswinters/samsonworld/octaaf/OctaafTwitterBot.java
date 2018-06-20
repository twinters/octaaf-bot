package be.thomaswinters.samsonworld.octaaf;

import be.thomaswinters.generator.streamgenerator.IStreamGenerator;
import be.thomaswinters.samsonworld.jeanine.JeanineTipsGenerator;
import be.thomaswinters.twitter.bot.GeneratorTwitterBot;
import be.thomaswinters.twitter.bot.TwitterBot;
import be.thomaswinters.twitter.bot.TwitterBotExecutor;
import be.thomaswinters.twitter.exception.TwitterUnchecker;
import be.thomaswinters.twitter.tweetsfetcher.*;
import be.thomaswinters.twitter.tweetsfetcher.filter.AlreadyParticipatedFilter;
import be.thomaswinters.twitter.userfetcher.ListUserFetcher;
import be.thomaswinters.twitter.util.TwitterLogin;
import be.thomaswinters.twitter.util.analysis.TwitterAnalysisUtil;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;

import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.Optional;

public class OctaafTwitterBot {

    public static void main(String[] args) throws TwitterException, IOException {
        DeBolleBots deBolleBots = new OctaafTwitterBot().buildDeBolleBots();
        TwitterBot octaafBot = deBolleBots.octaafBot;
        TwitterBot jeanineBot = deBolleBots.jeanineBot;

        // First reply to all unreplied, as this will be influenced by Octaaf.
        jeanineBot.replyToAllUnrepliedMentions();

        // Run octaafbot
        new TwitterBotExecutor(octaafBot).run(args);

    }

    public DeBolleBots buildDeBolleBots() throws IOException {

        long samsonBotsList = 1006565134796500992L;

        Twitter octaafTwitter = TwitterLogin.getTwitterFromEnvironment("octaaf");
        Twitter jeanineTwitter = TwitterLogin.getTwitterFromEnvironment("jeanine");

        OctaafStoefGenerator octaafStoefGenerator = new OctaafStoefGenerator();
        JeanineTipsGenerator jeanineTipsGenerator = new JeanineTipsGenerator();


        // Bot friends
        Collection<User> botFriends = ListUserFetcher.getUsers(octaafTwitter, samsonBotsList);
        TweetsFetcherCache botFriendsTweetsFetcher =
                new AdvancedListTweetsFetcher(octaafTwitter, samsonBotsList, false, true)
                        .cache(Duration.ofMinutes(5));
        IStreamGenerator<Long> alreadyRepliedToByFriends =
                botFriendsTweetsFetcher
                        .mapToRepliedToIds()
                        .seed(() -> TwitterUnchecker.uncheck(
                                TwitterAnalysisUtil::getLastReplyStatus, octaafTwitter)
                                .map(Status::getId).orElse(1L));

        ITweetsFetcher tweetsToAnswerOctaaf =
                TwitterBot.MENTIONS_RETRIEVER.apply(octaafTwitter)
                        .combineWith(
                                new TimelineTweetsFetcher(octaafTwitter)
                                        .combineWith(
                                                botFriendsTweetsFetcher)
                                        .filter(TwitterUnchecker.uncheck(AlreadyParticipatedFilter::new, octaafTwitter, 3)),
                                new SearchTweetsFetcher(octaafTwitter, "octaaf de bolle")
                                        .filterRandomly(octaafTwitter, 1, 4),
                                new SearchTweetsFetcher(octaafTwitter, "octaaf", "samson")
                                        .filterRandomly(octaafTwitter, 1, 4))
                        // Filter out botfriends tweets randomly
                        .filterRandomlyIf(octaafTwitter, e -> botFriends.contains(e.getUser()), 1, 16)
                        // Filter out own tweets & retweets
                        .filterOutRetweets()
                        // Filter out already replied to messages
                        .filterRandomlyIf(octaafTwitter, status ->
                                !status.getUser().getScreenName().toLowerCase().contains("samson") &&
                                        alreadyRepliedToByFriends
                                                .generateStream()
                                                .noneMatch(id -> id.equals(status.getId())), 1, 3)
                        .filterOutOwnTweets(octaafTwitter);


        ITweetsFetcher tweetsToAnswerJeanine =
                TwitterBot.MENTIONS_RETRIEVER.apply(jeanineTwitter)
                        .combineWith(
                                new TimelineTweetsFetcher(jeanineTwitter)
                                        .combineWith(
                                                botFriendsTweetsFetcher)
                                        .filter(TwitterUnchecker.uncheck(AlreadyParticipatedFilter::new, jeanineTwitter, 3)),
                                new SearchTweetsFetcher(jeanineTwitter, "jeannine de bolle")
                                        .combineWith(
                                                new SearchTweetsFetcher(jeanineTwitter, "jeanine de bolle"),
                                                new SearchTweetsFetcher(jeanineTwitter, "mevrouw praline")
                                        )
                                        .filterRandomly(jeanineTwitter, 1, 4))
                        // Filter out botfriends tweets randomly
                        .filterRandomlyIf(jeanineTwitter, e -> botFriends.contains(e.getUser()), 1, 22)
                        // Filter out own tweets & retweets
                        .filterOutRetweets()
                        // Filter out already replied to messages
                        .filterRandomlyIf(jeanineTwitter, status ->
                                alreadyRepliedToByFriends
                                        .generateStream()
                                        .noneMatch(id -> id.equals(status.getId())), 1, 3)
                        .filterOutOwnTweets(jeanineTwitter);

        TwitterBot octaafBot =
                new GeneratorTwitterBot(octaafTwitter,
                        Optional::empty,
                        octaafStoefGenerator,
                        tweetsToAnswerOctaaf);

        TwitterBot jeanineBot =
                new GeneratorTwitterBot(jeanineTwitter,
                        Optional::empty,
                        jeanineTipsGenerator,
                        tweetsToAnswerJeanine);
        octaafBot.addPostListener(jeanineBot::replyToStatus);
        octaafBot.addReplyListener((message, toMessage) -> jeanineBot.replyToStatus(message));

        return new DeBolleBots(octaafBot, jeanineBot);
    }

    public TwitterBot build() throws IOException, TwitterException {
        return buildDeBolleBots().octaafBot;
    }

    private static class DeBolleBots {
        private final TwitterBot octaafBot;
        private final TwitterBot jeanineBot;

        public DeBolleBots(TwitterBot octaafBot, TwitterBot jeanineBot) {
            this.octaafBot = octaafBot;
            this.jeanineBot = jeanineBot;
        }
    }
}
