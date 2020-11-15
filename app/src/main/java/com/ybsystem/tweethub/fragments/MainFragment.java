package com.ybsystem.tweethub.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.ybsystem.tweethub.R;
import com.ybsystem.tweethub.adapters.pager.MainPagerAdapter;
import com.ybsystem.tweethub.application.TweetHubApp;
import com.ybsystem.tweethub.fragments.timeline.TimelineBase;
import com.ybsystem.tweethub.models.entities.twitter.TwitterUser;
import com.ybsystem.tweethub.storages.PrefTheme;

import java.util.ArrayList;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.observers.DisposableObserver;
import io.reactivex.rxjava3.schedulers.Schedulers;
import twitter4j.IDs;
import twitter4j.Twitter;
import twitter4j.User;

public class MainFragment extends Fragment {

    private ViewPager mMainTlPager;
    private MainPagerAdapter mMainPagerAdapter;

    private final int FRIEND = 0;
    private final int FOLLOWER = 1;
    private final int MUTE = 2;
    private final int BLOCK = 3;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        // Set contents
        setAppIcon(view);
        setMainTlPager(view);

        // Fetch twitter user
        fetchMyTwitterUser();

        return view;
    }

    private void setAppIcon(View view) {
        // Find view
        ImageView icon = view.findViewById(R.id.image_app_icon);
        // Set drawable resource
        switch (PrefTheme.getTheme()) {
            case "LIGHT":
                icon.setImageResource(R.drawable.ic_launcher);
                break;
            case "DARK":
                icon.setImageResource(R.drawable.ic_launcher_dark);
                break;
        }
    }

    private void setMainTlPager(View view) {
        // Set main pager
        mMainTlPager = view.findViewById(R.id.pager_main);
        mMainTlPager.setOffscreenPageLimit(10);
        mMainPagerAdapter = new MainPagerAdapter(getActivity().getSupportFragmentManager());
        mMainTlPager.setAdapter(mMainPagerAdapter);
        mMainTlPager.setCurrentItem(TweetHubApp.getMyAccount().getColumns().getBootColumnNum());

        // Set main tab
        TabLayout tabLayout = view.findViewById(R.id.tab_main);
        tabLayout.setupWithViewPager(mMainTlPager);
        ViewGroup vg = (ViewGroup) tabLayout.getChildAt(0);

        // Process for each tabs
        for (int i = 0; i < TweetHubApp.getMyAccount().getColumns().size(); i++) {
            // When tab clicked
            TextView tv = (TextView) ((LinearLayout) vg.getChildAt(i)).getChildAt(1);
            tv.setMaxEms(5);
            tv.setSingleLine();
            tv.setEllipsize(TextUtils.TruncateAt.END);

            final int TAB_NUM = i;
            vg.getChildAt(i).setOnClickListener(v -> {
                // If current tab clicked, move top of the timeline
                int currentPage = mMainTlPager.getCurrentItem();
                if (currentPage == TAB_NUM) {
                    TimelineBase timeline = (TimelineBase) mMainPagerAdapter.instantiateItem(mMainTlPager, currentPage);
                    timeline.getRecyclerView().scrollToPosition(0);
                }
            });
        }
    }

    private void fetchMyTwitterUser() {
        Observable<User> observable = Observable.create(e -> {
            // Fetch
            User user4j;
            Twitter twitter = TweetHubApp.getTwitter();
            user4j = twitter.showUser(twitter.getId());

            // Check
            if (user4j != null) {
                e.onNext(user4j);
            }
            e.onComplete();
        });

        DisposableObserver<User> disposable = new DisposableObserver<User>() {
            @Override
            public void onNext(User user4j) {
                // Save
                TwitterUser user = new TwitterUser(user4j);
                TweetHubApp.getMyAccount().setUser(user);

                // Fetch
                fetchUserIDs(FRIEND, user.getFriendIds(), -1);
                fetchUserIDs(FOLLOWER, user.getFollowerIds(), -1);
                fetchUserIDs(MUTE, user.getMuteIds(), -1);
                fetchUserIDs(BLOCK, user.getBlockIds(), -1);
            }

            @Override
            public void onError(Throwable t) {
            }

            @Override
            public void onComplete() {
            }
        };

        observable
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(disposable);
    }

    private void fetchUserIDs(int type, ArrayList<Long> list, long cursor) {
        Observable<IDs> observable = Observable.create(e -> {
            // Fetch
            IDs ids = null;
            Twitter twitter = TweetHubApp.getTwitter();
            switch (type) {
                case FRIEND:
                    ids = twitter.getFriendsIDs(cursor);
                    break;
                case FOLLOWER:
                    ids = twitter.getFollowersIDs(cursor);
                    break;
                case MUTE:
                    ids = twitter.getMutesIDs(cursor);
                    break;
                case BLOCK:
                    ids = twitter.getBlocksIDs(cursor);
                    break;
            }
            // Check
            if (ids != null && ids.getIDs().length != 0) {
                e.onNext(ids);
            }
            e.onComplete();
        });

        DisposableObserver<IDs> disposable = new DisposableObserver<IDs>() {
            @Override
            public void onNext(IDs ids) {
                // Append
                for (long id : ids.getIDs()) {
                    list.add(id);
                }
                // Save
                TwitterUser user = TweetHubApp.getMyUser();
                switch (type) {
                    case FRIEND:
                        user.setFriendIds(list);
                        break;
                    case FOLLOWER:
                        user.setFollowerIds(list);
                        break;
                    case MUTE:
                        user.setMuteIds(list);
                        break;
                    case BLOCK:
                        user.setBlockIds(list);
                        break;
                }
                TweetHubApp.getMyAccount().setUser(user);

                // Check next
                if (ids.hasNext()) {
                    fetchUserIDs(type, list, ids.getNextCursor());
                }
            }

            @Override
            public void onError(Throwable t) {
            }

            @Override
            public void onComplete() {
            }
        };

        observable
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(disposable);
    }

}