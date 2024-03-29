package com.ybsystem.tweetmate.models.entities.twitter;

import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;

import androidx.core.text.HtmlCompat;

import com.ybsystem.tweetmate.application.TweetMateApp;
import com.ybsystem.tweetmate.models.entities.Entity;
import com.ybsystem.tweetmate.databases.PrefAppearance;
import com.ybsystem.tweetmate.databases.PrefTheme;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.Data;
import lombok.EqualsAndHashCode;
import twitter4j.HashtagEntity;
import twitter4j.MediaEntity;
import twitter4j.Status;
import twitter4j.URLEntity;
import twitter4j.User;
import twitter4j.UserMentionEntity;

import static androidx.core.text.HtmlCompat.TO_HTML_PARAGRAPH_LINES_INDIVIDUAL;
import static com.ybsystem.tweetmate.resources.ResColor.*;

@Data
@EqualsAndHashCode(callSuper = false)
public class TwitterStatus extends Entity {

    private TwitterUser user;
    private TwitterStatus rtStatus;
    private TwitterStatus qtStatus;
    private TwitterURLEntity[] urlEntities;
    private TwitterMediaEntity[] mediaEntities;
    private TwitterHashtagEntity[] hashtagEntities;
    private TwitterUserMentionEntity[] userMentionEntities;

    private Date createdAt;
    private long id;
    private long quotedStatusId;
    private long currentUserRetweetId;

    private String lang;
    private String source;
    private String text;
    private int displayTextRangeStart;
    private int displayTextRangeEnd;

    // In reply to
    private long inReplyToStatusId;
    private long inReplyToUserId;
    private String inReplyToScreenName;

    // Count
    private int favoriteCount;
    private int retweetCount;

    // State
    private boolean isFavorited;
    private boolean isRetweeted;
    private boolean isRetweet;
    private boolean isRetweetedByMe;

    // Custom
    private String convertedText;
    private String convertedRelativeTime;
    private String convertedAbsoluteTime;
    private String convertedVia;
    private ArrayList<String> imageUrls;
    private boolean isMyTweet;
    private boolean isPublic;
    private boolean isTalk;
    private boolean isDetail;
    private boolean isTwiMedia;
    private boolean isThirdMedia;
    private boolean isVideo;

    // Media
    private static final Pattern PATTERN_YOUTUBE =
            Pattern.compile("^https?://(?:www\\.youtube\\.com/watch\\?.*v=|youtu\\.be/)([\\w-]+)");

    // Not use
    // private Place place;
    // private Scopes scopes;
    // private GeoLocation geoLocation;
    // private TwitterURLEntity quotedStatusPermalink;
    // private TwitterSymbolEntity[] symbolEntities;
    // private long[] contributorsIDs;
    // private boolean isTruncated;
    // private boolean isPossiblySensitive;

    public TwitterStatus(Status status) {
        User user = status.getUser();
        Status rtStatus = status.getRetweetedStatus();
        Status qtStatus = status.getQuotedStatus();
        this.user = user != null ? new TwitterUser(user) : null;
        this.rtStatus = rtStatus != null ? new TwitterStatus(rtStatus) : null;
        this.qtStatus = qtStatus != null ? new TwitterStatus(qtStatus) : null;

        URLEntity[] urls = status.getURLEntities();
        MediaEntity[] medias = status.getMediaEntities();
        HashtagEntity[] hashtags = status.getHashtagEntities();
        UserMentionEntity[] userMentions = status.getUserMentionEntities();
        this.urlEntities = new TwitterURLEntity[urls.length];
        this.mediaEntities = new TwitterMediaEntity[medias.length];
        this.hashtagEntities = new TwitterHashtagEntity[hashtags.length];
        this.userMentionEntities = new TwitterUserMentionEntity[userMentions.length];
        for (int i = 0; i < urls.length; i++) {
            this.urlEntities[i] = new TwitterURLEntity(urls[i]);
        }
        for (int i = 0; i < medias.length; i++) {
            this.mediaEntities[i] = new TwitterMediaEntity(medias[i]);
        }
        for (int i = 0; i < hashtags.length; i++) {
            this.hashtagEntities[i] = new TwitterHashtagEntity(hashtags[i]);
        }
        for (int i = 0; i < userMentions.length; i++) {
            this.userMentionEntities[i] = new TwitterUserMentionEntity(userMentions[i]);
        }

        this.createdAt = status.getCreatedAt();
        this.id = status.getId();
        this.quotedStatusId = status.getQuotedStatusId();
        this.currentUserRetweetId = status.getCurrentUserRetweetId();

        this.lang = status.getLang();
        this.source = status.getSource();
        this.text = status.getText();
        this.displayTextRangeStart = status.getDisplayTextRangeStart();
        this.displayTextRangeEnd = status.getDisplayTextRangeEnd();

        // In reply to
        this.inReplyToStatusId = status.getInReplyToStatusId();
        this.inReplyToUserId = status.getInReplyToUserId();
        this.inReplyToScreenName = status.getInReplyToScreenName();

        // Count
        this.favoriteCount = status.getFavoriteCount();
        this.retweetCount = status.getRetweetCount();

        // State
        this.isFavorited = status.isFavorited();
        this.isRetweeted = status.isRetweeted();
        this.isRetweet = rtStatus != null;
        this.isRetweetedByMe = currentUserRetweetId != -1L;

        // Custom
        this.convertedText = createTweetText(status);
        this.convertedRelativeTime = createRelativeTime(status);
        this.convertedAbsoluteTime = createAbsoluteTime(status);
        this.convertedVia = createVia(status);
        this.imageUrls = createImageUrls(status);
        this.isMyTweet = status.getUser().getId() == TweetMateApp.getMyUser().getId();
        this.isPublic = isMyTweet || !status.getUser().isProtected();
        this.isTalk = status.getInReplyToStatusId() != -1;
        this.isDetail = false;
        this.isTwiMedia = (!imageUrls.isEmpty() && mediaEntities.length > 0);
        this.isThirdMedia = (!imageUrls.isEmpty()  && mediaEntities.length == 0);
        this.isVideo = (isTwiMedia && !mediaEntities[0].getType().equals("photo"));
    }

    private static String createTweetText(Status status) {
        // Init
        SpannableStringBuilder ssb = new SpannableStringBuilder(status.getText());
        int color = PrefTheme.isCustomThemeEnabled() ? PrefTheme.getLinkColor() : COLOR_LINK;

        // @Mentions
        UserMentionEntity[] mentions = status.getUserMentionEntities();
        if (mentions != null && mentions.length != 0) {
            for (UserMentionEntity mention : mentions) {
                ssb.setSpan(new ForegroundColorSpan(color), mention.getStart(), mention.getEnd(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

        // #Hashtag
        HashtagEntity[] hashtags = status.getHashtagEntities();
        if (hashtags != null && hashtags.length != 0) {
            for (HashtagEntity hashtag : hashtags) {
                ssb.setSpan(new ForegroundColorSpan(color), hashtag.getStart(), hashtag.getEnd(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

        // URL
        int offset = 0;
        URLEntity[] urls = status.getURLEntities();
        if (urls != null && urls.length != 0) {
            for (URLEntity url : urls) {
                int start = url.getStart();
                int end = url.getEnd();
                start += offset;
                end += offset;
                if (ssb.length() > start && ssb.length() >= end) {
                    String displayURL = url.getDisplayURL();
                    ssb.replace(start, end, displayURL);
                    ssb.setSpan(new ForegroundColorSpan(color), start, start + displayURL.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    offset += displayURL.length() - url.getURL().length();
                }
            }
        }

        // Media
        MediaEntity[] medias = status.getMediaEntities();
        if (medias != null && medias.length != 0) {
            int start = medias[0].getStart();
            int end = medias[0].getEnd();
            if (end == status.getText().length()) {
                start += offset;
                end += offset;
            }
            if (ssb.length() > start && ssb.length() >= end) {
                if (PrefAppearance.isShowThumbnail()) {
                    ssb.replace(start, end, "");
                } else {
                    String displayURL = medias[0].getDisplayURL();
                    ssb.replace(start, end, displayURL);
                    ssb.setSpan(new ForegroundColorSpan(color), start, start + displayURL.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }

        // Convert to string
        String htmlText = HtmlCompat.toHtml(ssb, TO_HTML_PARAGRAPH_LINES_INDIVIDUAL);
        String match = "<p dir=\"ltr\" style=\"margin-top:0; margin-bottom:0;\">";
        htmlText = htmlText.replaceFirst("(?s)" + match + "(?!.*?" + match + ")", "");
        htmlText = htmlText.replaceFirst("(?s)" + "</p>" + "(?!.*?" + "</p>" + ")", "");
        htmlText = htmlText.replaceAll("\n", "");

        return htmlText;
    }

    private static String createRelativeTime(Status status) {
        // Convert
        Date date = status.getCreatedAt();
        return new TwitterTimeSpanConverter().toTimeSpanString(date);
    }

    private static String createAbsoluteTime(Status status) {
        // Convert
        Date date = status.getCreatedAt();
        return new SimpleDateFormat("y/M/d HH:mm", Locale.getDefault()).format(date);
    }

    private static String createVia(Status status) {
        // Convert
        String[] texts = status.getSource().split("[<>]");
        if (texts.length > 2) {
            return "via " + texts[2];
        } else {
            return "via Unknown Client";
        }
    }

    private static ArrayList<String> createImageUrls(Status status) {
        // Init
        ArrayList<String> imageUrls = new ArrayList<>();
        Status source = status.isRetweet() ? status.getRetweetedStatus() : status;

        // Check media
        if (source.getMediaEntities().length > 0) {
            // Twitter media
            MediaEntity[] medias = source.getMediaEntities();
            for (int i = 0; i < medias.length; i++) {
                imageUrls.add(medias[i].getMediaURLHttps());
            }
        } else {
            // Third party media
            for (URLEntity url : source.getURLEntities()) {
                // Limit is 4
                if (imageUrls.size() >= 4) {
                    break;
                }
                // Youtube
                Matcher matcher = PATTERN_YOUTUBE.matcher(url.getExpandedURL());
                if (matcher.find()) {
                    imageUrls.add("http://i.ytimg.com/vi/" + matcher.group(1) + "/0.jpg");
                    continue;
                }
                // Others (Pending...)
            }
        }
        return imageUrls;
    }

}
