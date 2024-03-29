package com.ybsystem.tweetmate.fragments.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.fragment.app.DialogFragment;

import com.ybsystem.tweetmate.R;
import com.ybsystem.tweetmate.adapters.holder.TweetRow;
import com.ybsystem.tweetmate.application.TweetMateApp;
import com.ybsystem.tweetmate.models.entities.twitter.TwitterStatus;
import com.ybsystem.tweetmate.utils.AnimationUtils;
import com.ybsystem.tweetmate.utils.ExceptionUtils;
import com.ybsystem.tweetmate.utils.ToastUtils;

import java.util.List;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.observers.DisposableObserver;
import io.reactivex.rxjava3.schedulers.Schedulers;
import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Twitter;
import twitter4j.TwitterException;

import static com.ybsystem.tweetmate.resources.ResColor.*;
import static com.ybsystem.tweetmate.resources.ResString.*;

public class PreviewThemeDialog extends DialogFragment {

    private QueryResult mTweetResult;
    private QueryResult mRetweetResult;
    private List<twitter4j.Status> mReplyStatuses;
    private List<twitter4j.Status> mMyTweetStatuses;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Show full screen
        Window window = getDialog().getWindow();
        WindowManager.LayoutParams params = window.getAttributes();
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        params.height = ViewGroup.LayoutParams.MATCH_PARENT;
        window.setAttributes(params);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Inflate
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View view = inflater.inflate(R.layout.dialog_preview_theme, null);

        // Set background color
        int color = COLOR_BACKGROUND;
        view.findViewById(R.id.linear_preview).setBackgroundColor(color);

        // Load
        loadTweet(view);

        // Build dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view);

        return builder.create();
    }

    private void loadTweet(View view) {
        // Async
        Observable<Object> observable = Observable.create(e -> {
            try {
                Twitter twitter = TweetMateApp.getTwitter();
                Query tweetQuery = new Query("from:GooglePlay exclude:retweets exclude:nativeretweets");
                Query retweetQuery = new Query("from:GooglePlay filter:nativeretweets");

                // Fetch data
                mTweetResult = twitter.search(tweetQuery);
                mRetweetResult = twitter.search(retweetQuery);
                mReplyStatuses = twitter.getMentionsTimeline();
                mMyTweetStatuses = twitter.getUserTimeline();

                // Notify
                e.onComplete();
            } catch (TwitterException ex) {
                e.onError(ex);
                dismiss();
            }
        });

        DisposableObserver<Object> disposable = new DisposableObserver<Object>() {
            @Override
            public void onNext(Object o) {
            }

            @Override
            public void onError(Throwable t) {
                // Failed...
                ToastUtils.showShortToast(STR_FAIL_LOAD);
                ToastUtils.showShortToast(ExceptionUtils.getErrorMessage(t));
            }

            @Override
            public void onComplete() {
                // Tweet
                List<twitter4j.Status> tweets = mTweetResult.getTweets();
                if (!tweets.isEmpty()) {
                    View v = view.findViewById(R.id.include_tweet);
                    renderTweet(v, new TwitterStatus(tweets.get(0)));
                    v.setVisibility(View.VISIBLE);
                }
                // Retweet
                List<twitter4j.Status> retweets = mRetweetResult.getTweets();
                if (!retweets.isEmpty()) {
                    View v = view.findViewById(R.id.include_retweet);
                    renderTweet(v, new TwitterStatus(retweets.get(0)));
                    v.setVisibility(View.VISIBLE);
                }
                // Reply
                if (!mReplyStatuses.isEmpty()) {
                    View v = view.findViewById(R.id.include_reply);
                    renderTweet(v, new TwitterStatus(mReplyStatuses.get(0)));
                    v.setVisibility(View.VISIBLE);
                }
                // My Tweet
                if (!mMyTweetStatuses.isEmpty()) {
                    // Exclude retweet
                    mMyTweetStatuses = mMyTweetStatuses.stream()
                            .filter(status -> !status.isRetweet()).collect(Collectors.toList());

                    if (!mMyTweetStatuses.isEmpty()) {
                        View v = view.findViewById(R.id.include_mytweet);
                        renderTweet(v, new TwitterStatus(mMyTweetStatuses.get(0)));
                        v.setVisibility(View.VISIBLE);
                    }
                }
                // Show
                AnimationUtils.fadeOut(view.findViewById(R.id.linear_loading), 200);
                AnimationUtils.fadeIn(view.findViewById(R.id.linear_preview), 400);
            }
        };

        observable
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(disposable);
    }

    private void renderTweet(View view, TwitterStatus status) {
        TweetRow tweetRow = new TweetRow(view);
        tweetRow.initVisibilities();
        tweetRow.setStatus(status);
        tweetRow.setUserName();
        tweetRow.setScreenName();
        tweetRow.setUserIcon();
        tweetRow.setTweetText();
        tweetRow.setRelativeTime();
        tweetRow.setAbsoluteTime();
        tweetRow.setVia();
        tweetRow.setRtFavCount();
        tweetRow.setRetweetedBy();
        tweetRow.setMarks();
        tweetRow.setBackgroundColor();
    }

}
