package com.ybsystem.tweetmate.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.ybsystem.tweetmate.R;
import com.ybsystem.tweetmate.adapters.pager.SearchPagerAdapter;
import com.ybsystem.tweetmate.adapters.pager.TrendTopicPagerAdapter;
import com.ybsystem.tweetmate.application.TweetMateApp;
import com.ybsystem.tweetmate.fragments.fragment.EasyTweetFragment;
import com.ybsystem.tweetmate.fragments.timeline.TimelineBase;
import com.ybsystem.tweetmate.libs.eventbus.ColumnEvent;
import com.ybsystem.tweetmate.models.entities.Column;
import com.ybsystem.tweetmate.models.entities.ColumnArray;
import com.ybsystem.tweetmate.databases.PrefSystem;
import com.ybsystem.tweetmate.usecases.ClickUseCase;
import com.ybsystem.tweetmate.usecases.SearchUseCase;
import com.ybsystem.tweetmate.utils.DialogUtils;
import com.ybsystem.tweetmate.utils.KeyboardUtils;
import com.ybsystem.tweetmate.utils.ToastUtils;

import org.greenrobot.eventbus.EventBus;

import static com.ybsystem.tweetmate.activities.preference.SettingActivity.*;
import static com.ybsystem.tweetmate.models.enums.ColumnType.*;
import static com.ybsystem.tweetmate.models.enums.TweetStyle.*;
import static com.ybsystem.tweetmate.resources.ResString.*;

public class SearchActivity extends ActivityBase {

    private Menu mMenu;

    // Trend/Topic
    private EditText mSearchEdit;
    private ViewPager mTrendTopicPager;
    private TrendTopicPagerAdapter mTrendTopicPagerAdapter;

    // Search
    private String mSearchWord;
    private ViewPager mSearchPager;
    private SearchPagerAdapter mSearchPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Init
        mSearchWord = getIntent().getStringExtra("SEARCH_WORD");

        // Set
        setContentView(R.layout.activity_search);
        setTweetAction(savedInstanceState);

        // Check if search word exists
        if (mSearchWord == null) {
            // Trend/Topic
            setTrendTopicPager();
            setSearchEditActionBar();
        } else {
            // Search result
            setSearchPager();
            getSupportActionBar().setTitle(mSearchWord);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.mMenu = menu;
        getMenuInflater().inflate(R.menu.search, menu);
        updateMenu();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Saved searches
            case R.id.item_saved_search:
                SearchUseCase.showSavedSearch();
                return true;
            // Save search
            case R.id.item_save_search:
                SearchUseCase.saveSearch(mSearchWord);
                return true;
            // Add column
            case R.id.item_add_column:
                showAddDialog();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (resultCode) {
            case REBOOT_PREPARATION:
                setResult(REBOOT_PREPARATION);
                break;
        }
    }

    private void updateMenu() {
        MenuItem savedSearch = mMenu.findItem(R.id.item_saved_search);
        MenuItem saveSearch = mMenu.findItem(R.id.item_save_search);
        MenuItem addColumn = mMenu.findItem(R.id.item_add_column);

        if (mSearchWord == null) {
            saveSearch.setVisible(false);
            addColumn.setVisible(false);
        } else {
            savedSearch.setVisible(false);
        }
    }

    private void setTrendTopicPager() {
        // Set trend/topic pager
        mTrendTopicPager = findViewById(R.id.pager_common);
        mTrendTopicPager.setOffscreenPageLimit(2);
        mTrendTopicPagerAdapter = new TrendTopicPagerAdapter(getSupportFragmentManager());
        mTrendTopicPager.setAdapter(mTrendTopicPagerAdapter);

        // Set tab
        TabLayout tabLayout = findViewById(R.id.tab_common);
        tabLayout.setupWithViewPager(mTrendTopicPager);
        ViewGroup vg = (ViewGroup) tabLayout.getChildAt(0);

        // When tab clicked
        for (int i = 0; i < 2; i++) {
            final int TAB_NUM = i;
            vg.getChildAt(i).setOnClickListener(v -> {
                // If current tab clicked, move top of the timeline
                int currentPage = mTrendTopicPager.getCurrentItem();
                if (currentPage == TAB_NUM) {
                    TimelineBase timeline = (TimelineBase)
                            mTrendTopicPagerAdapter.instantiateItem(mTrendTopicPager, currentPage);
                    timeline.getRecyclerView().scrollToPosition(0);
                }
            });
        }
    }

    private void setSearchPager() {
        // Set search pager
        mSearchPager = findViewById(R.id.pager_common);
        mSearchPager.setOffscreenPageLimit(3);
        mSearchPagerAdapter = new SearchPagerAdapter(getSupportFragmentManager(), mSearchWord);
        mSearchPager.setAdapter(mSearchPagerAdapter);

        // Set tab
        TabLayout tabLayout = findViewById(R.id.tab_common);
        tabLayout.setupWithViewPager(mSearchPager);
        ViewGroup vg = (ViewGroup) tabLayout.getChildAt(0);

        // When tab clicked
        for (int i = 0; i < 3; i++) {
            final int TAB_NUM = i;
            vg.getChildAt(i).setOnClickListener(v -> {
                // If current tab clicked, move top of the timeline
                int currentPage = mSearchPager.getCurrentItem();
                if (currentPage == TAB_NUM) {
                    TimelineBase timeline = (TimelineBase)
                            mSearchPagerAdapter.instantiateItem(mSearchPager, currentPage);
                    timeline.getRecyclerView().scrollToPosition(0);
                }
            });
        }
    }

    private void setTweetAction(Bundle savedInstanceState) {
        if (PrefSystem.getTweetStyle() == ON_THE_TIMELINE) {
            // EasyTweet
            if (savedInstanceState == null) {
                Fragment fragment = new EasyTweetFragment();
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.frame_easy_tweet, fragment);
                transaction.commit();
            }
            // Hide tweet button
            findViewById(R.id.fab).setVisibility(View.GONE);
        } else {
            // TweetButton
            findViewById(R.id.fab).setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), PostActivity.class);
                if (hasHashTag()) intent.putExtra("TWEET_SUFFIX", mSearchWord);
                startActivityForResult(intent, 0);
                overridePendingTransition(R.anim.fade_in_from_bottom, R.anim.none);
            });
        }
    }

    private void setSearchEditActionBar() {
        // Set custom actionbar
        ViewGroup root = findViewById(android.R.id.content);
        View view = getLayoutInflater().inflate(R.layout.edit_search_bar, root, false);
        getSupportActionBar().setCustomView(view);
        getSupportActionBar().setDisplayShowCustomEnabled(true);

        // Set edit text
        mSearchEdit = view.findViewById(R.id.edit_search);
        mSearchEdit.setOnKeyListener((v, keyCode, event) -> {
            // When pressed enter
            if (mSearchEdit.length() != 0 && keyCode == KeyEvent.KEYCODE_ENTER
                    && event.getAction() == KeyEvent.ACTION_DOWN) {
                // Close keyboard
                KeyboardUtils.closeKeyboard(v);

                // Intent to SearchActivity
                ClickUseCase.searchWord(mSearchEdit.getText().toString());
                return true;
            }
            return false;
        });
    }

    private void showAddDialog() {
        if (TweetMateApp.getMyAccount().getColumns().size() >= 8) {
            ToastUtils.showShortToast(STR_FAIL_NO_MORE_ADD);
            return;
        }
        DialogUtils.showConfirm(
                STR_CONFIRM_ADD_COLUMN,
                (dialog, which) -> {
                    // Add column
                    ColumnArray<Column> columns = TweetMateApp.getMyAccount().getColumns();
                    boolean isSucceeded = columns.add(
                            new Column(mSearchWord.hashCode(), mSearchWord, SEARCH_SINGLE, false));
                    // Check
                    if (!isSucceeded) {
                        return;
                    }
                    // Success
                    ToastUtils.showShortToast(STR_SUCCESS_ADD_COLUMN);
                    EventBus.getDefault().postSticky(new ColumnEvent());
                    setResult(REBOOT_PREPARATION);
                }
        );
    }

    private boolean hasHashTag() {
        // Check word
        if (mSearchWord == null) {
            return false;
        }
        if (!mSearchWord.startsWith("#") && !mSearchWord.startsWith("＃")) {
            return false;
        }
        return true;
    }

}
