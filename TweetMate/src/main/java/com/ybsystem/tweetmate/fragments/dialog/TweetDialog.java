package com.ybsystem.tweetmate.fragments.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;

import com.ybsystem.tweetmate.R;
import com.ybsystem.tweetmate.application.TweetMateApp;
import com.ybsystem.tweetmate.models.entities.twitter.TwitterHashtagEntity;
import com.ybsystem.tweetmate.models.entities.twitter.TwitterStatus;
import com.ybsystem.tweetmate.models.entities.twitter.TwitterURLEntity;
import com.ybsystem.tweetmate.models.entities.twitter.TwitterUserMentionEntity;
import com.ybsystem.tweetmate.models.enums.TweetMenu;
import com.ybsystem.tweetmate.adapters.holder.TweetRow;
import com.ybsystem.tweetmate.databases.PrefAppearance;
import com.ybsystem.tweetmate.databases.PrefClickAction;
import com.ybsystem.tweetmate.databases.PrefSystem;
import com.ybsystem.tweetmate.usecases.ClickUseCase;
import com.ybsystem.tweetmate.usecases.StatusUseCase;
import com.ybsystem.tweetmate.utils.GlideUtils;

import java.util.Set;

import lombok.Data;

import static com.ybsystem.tweetmate.models.enums.ImageOption.*;
import static com.ybsystem.tweetmate.models.enums.TweetMenu.*;
import static com.ybsystem.tweetmate.resources.ResColor.*;
import static com.ybsystem.tweetmate.resources.ResString.*;

public class TweetDialog extends DialogFragment {
    // Status
    private TwitterStatus mStatus;
    private TwitterStatus mSource;

    public TweetDialog newInstance(TwitterStatus status) {
        Bundle bundle = new Bundle();
        bundle.putSerializable("STATUS", status);
        setArguments(bundle);
        return this;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Inflate
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View view = inflater.inflate(R.layout.dialog_tweet, null);

        // Init
        mStatus = (TwitterStatus) getArguments().getSerializable("STATUS");
        mSource = mStatus.isRetweet() ? mStatus.getRtStatus() : mStatus;

        // Set tweet
        TweetRow tweetRow = new TweetRow(view);
        renderTweet(tweetRow);

        // Set menu list
        ListView listView = view.findViewById(R.id.list_view);
        MenuArrayAdapter adapter = new MenuArrayAdapter(getActivity());
        listView.setAdapter(adapter);
        listView.setDivider(null);
        listView.setFocusable(false);
        setMenuItems(adapter);

        // Set click listener
        setClickListener(view, listView, adapter);

        // Create dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view);

        return builder.create();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        dismiss();
    }

    private void renderTweet(TweetRow tweetRow) {
        // Set all fields
        tweetRow.initVisibilities();
        tweetRow.setStatus(mStatus);
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
        tweetRow.setThumbnail();
        tweetRow.setQuoteTweet();
        tweetRow.setBackgroundColor();
    }

    @Data
    private static class Menu {
        private int drawable;
        private int color;
        private String label;
        private String userIconURL;
        private Runnable runnable;

        public Menu(int drawable, int color, String label,
                    String userIconURL, Runnable runnable) {
            this.drawable = drawable;
            this.color = color;
            this.label = label;
            this.userIconURL = userIconURL;
            this.runnable = runnable;
        }
    }

    private static class MenuArrayAdapter extends ArrayAdapter<Menu> {

        public MenuArrayAdapter(Context context) {
            super(context, android.R.layout.simple_list_item_1);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            // Inflate
            View cv = convertView;
            if (cv == null) {
                cv = LayoutInflater.from(getContext())
                        .inflate(R.layout.list_item_tweet_dialog, parent, false);
            }
            // Get item
            Menu menu = getItem(position);

            // Set image
            ImageView icon = cv.findViewById(R.id.image_icon);
            ImageView userIcon = cv.findViewById(R.id.image_user_icon);
            if (menu.getUserIconURL().equals("")) {
                // Icon
                icon.setImageResource(menu.getDrawable());
                icon.setColorFilter(menu.getColor());
                icon.setVisibility(View.VISIBLE);
                userIcon.setVisibility(View.GONE);
            } else {
                // UserIcon
                GlideUtils.load(menu.getUserIconURL(), userIcon, CIRCLE);
                userIcon.setVisibility(View.VISIBLE);
                icon.setVisibility(View.GONE);
            }

            // Set text
            TextView tv = cv.findViewById(R.id.text_item);
            tv.setText(menu.getLabel());

            return cv;
        }
    }

    private void setMenuItems(MenuArrayAdapter adapter) {
        // Menu setting
        Set<TweetMenu> menuSetting = PrefClickAction.getTweetMenu();

        // Reply
        if (menuSetting.contains(REPLY)) {
            adapter.add(new Menu(
                    R.drawable.ic_reply, COLOR_STRONG, STR_REPLY_VERB, "",
                    () -> ClickUseCase.reply(mSource)
            ));
        }
        // Retweet
        if (menuSetting.contains(RETWEET)) {
            if (mStatus.isRetweeted()) {
                adapter.add(new Menu(
                        R.drawable.ic_retweet, COLOR_RETWEET, STR_UNRETWEET, "",
                        () -> StatusUseCase.retweet(mStatus)
                ));
            } else if (mSource.isPublic()) {
                adapter.add(new Menu(
                        R.drawable.ic_retweet, COLOR_RETWEET, STR_RETWEET, "",
                        null
                ));
            }
        }
        // Favorite
        if (menuSetting.contains(LIKE)) {
            if (mStatus.isFavorited()) {
                adapter.add(new Menu(
                        PrefAppearance.getLikeFavDrawable(),
                        PrefAppearance.getLikeFavColor(),
                        PrefAppearance.getUnLikeFavText(), "",
                        () -> StatusUseCase.favorite(mStatus)
                ));
            } else {
                adapter.add(new Menu(
                        PrefAppearance.getLikeFavDrawable(),
                        PrefAppearance.getLikeFavColor(),
                        PrefAppearance.getLikeFavText(), "",
                        () -> StatusUseCase.favorite(mStatus)
                ));
            }
        }
        // Talk
        if (menuSetting.contains(TALK)) {
            if (mSource.isTalk()
                    && !TweetMateApp.getActivity().getSupportActionBar().getTitle().equals(STR_TALK)
                    && !TweetMateApp.getActivity().getSupportActionBar().getTitle().equals(STR_DETAIL)) {
                adapter.add(new Menu(
                        R.drawable.ic_talk, COLOR_TALK, STR_SHOW_TALK, "",
                        () -> ClickUseCase.showTalk(mSource)
                ));
            }
        }
        // Delete
        if (menuSetting.contains(DELETE)) {
            if (mSource.isMyTweet() && !mStatus.isRetweet()) {
                adapter.add(new Menu(
                        R.drawable.ic_garbage, COLOR_DELETE, STR_DELETE, "",
                        () -> StatusUseCase.delete(mStatus)
                ));
            }
        }
        // URL
        if (menuSetting.contains(URL)) {
            for (TwitterURLEntity entity : mSource.getUrlEntities()) {
                adapter.add(new Menu(
                        R.drawable.ic_link, COLOR_LINK_WEAK, entity.getDisplayURL(), "",
                        () -> ClickUseCase.openURL(entity.getUrl())
                ));
            }
        }
        // Hashtag
        if (menuSetting.contains(HASH)) {
            for (TwitterHashtagEntity entity : mSource.getHashtagEntities()) {
                adapter.add(new Menu(
                        R.drawable.ic_hashtag, COLOR_LINK_WEAK, "#" + entity.getText(), "",
                        null
                ));
            }
        }
        // User
        if (menuSetting.contains(USER)) {
            adapter.add(new Menu(
                    R.drawable.ic_user, COLOR_STRONG, "@" + mSource.getUser().getScreenName(),
                    PrefSystem.getProfileThumbByQuality(mSource.getUser()),
                    () -> ClickUseCase.showUser(mSource.getUser().getId())
            ));
            if (mStatus.isRetweet()) {
                adapter.add(new Menu(
                        R.drawable.ic_user, COLOR_STRONG, "@" + mStatus.getUser().getScreenName(),
                        PrefSystem.getProfileThumbByQuality(mStatus.getUser()),
                        () -> ClickUseCase.showUser(mStatus.getUser().getId())
                ));
            }
            for (TwitterUserMentionEntity entity : mSource.getUserMentionEntities()) {
                adapter.add(new Menu(
                        R.drawable.ic_user, COLOR_STRONG, "@" + entity.getScreenName(), "",
                        () -> ClickUseCase.showUser(entity.getId())
                ));
            }
        }
        // PrevNext
        if (menuSetting.contains(PREV_NEXT)) {
            adapter.add(new Menu(
                    R.drawable.ic_prev_next, COLOR_STRONG, STR_PREV_NEXT, "",
                    () -> ClickUseCase.showPrevNext(mSource)
            ));
        }
        // Detail
        if (menuSetting.contains(DETAIL)) {
            adapter.add(new Menu(
                    R.drawable.ic_doc, COLOR_STRONG, STR_DETAIL, "",
                    () -> ClickUseCase.showDetail(mSource)
            ));
        }
        // Copy
        if (menuSetting.contains(COPY)) {
            adapter.add(new Menu(
                    R.drawable.ic_copy, COLOR_STRONG, STR_COPY, "",
                    () -> ClickUseCase.copyText(mSource)
            ));
        }
        // Share
        if (menuSetting.contains(SHARE)) {
            adapter.add(new Menu(
                    R.drawable.ic_share, COLOR_STRONG, STR_SHARE, "",
                    () -> ClickUseCase.share(mStatus)
            ));
        }
        // OpenTwitter
        if (menuSetting.contains(TWITTER)) {
            adapter.add(new Menu(
                    R.drawable.ic_twitter, COLOR_STRONG, STR_OPEN_TWITTER, "",
                    () -> ClickUseCase.openStatus(mStatus)
            ));
        }
        // Remove duplication
        for (int i = 0; i < adapter.getCount(); i++) {
            for (int j = adapter.getCount() - 1; j > i; j--) {
                if (adapter.getItem(j).getLabel().equals(adapter.getItem(i).getLabel())) {
                    adapter.remove(adapter.getItem(j));
                }
            }
        }
    }

    private void setClickListener(View dialogView, ListView listView, MenuArrayAdapter adapter) {
        // When tweet clicked
        dialogView.findViewById(R.id.relative_tweet).setOnClickListener(
                v -> dismiss()
        );
        // When menu item clicked
        listView.setOnItemClickListener((adapterView, view, i, l) -> {
            runItem(adapter.getItem(i), "NORMAL");
        });
        // When menu item long clicked
        listView.setOnItemLongClickListener((adapterView, view, i, l) -> {
            String label = adapter.getItem(i).getLabel();
            if (label.equals(STR_RETWEET) || label.equals(STR_UNRETWEET) || label.startsWith("#")) {
                runItem(adapter.getItem(i), "REVERSE");
            }
            return true;
        });
    }

    private void runItem(Menu menu, String action) {
        Runnable runnable;
        String label = menu.getLabel();

        if (label.equals(STR_RETWEET)) {
            runnable = PrefClickAction.getClickRetweet().equals(action)
                    ? () -> StatusUseCase.retweet(mStatus)
                    : () -> ClickUseCase.quote(mSource);
        } else if (label.equals(STR_UNRETWEET)) {
            runnable = PrefClickAction.getClickRetweet().equals(action)
                    ? () -> StatusUseCase.retweet(mStatus)
                    : () -> ClickUseCase.quote(mSource);
        } else if (label.startsWith("#")) {
            runnable = PrefClickAction.getClickHashtag().equals(action)
                    ? () -> ClickUseCase.searchWord(label)
                    : () -> ClickUseCase.tweetWithSuffix(label);
        } else {
            runnable = menu.getRunnable();
        }

        runnable.run();
        dismiss();
    }

}
