package com.ybsystem.tweetmate.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;

import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.wangjie.rapidfloatingactionbutton.*;
import com.ybsystem.tweetmate.R;
import com.ybsystem.tweetmate.activities.preference.SettingActivity;
import com.ybsystem.tweetmate.application.TweetMateApp;
import com.ybsystem.tweetmate.fragments.dialog.*;
import com.ybsystem.tweetmate.fragments.fragment.DrawerFragment;
import com.ybsystem.tweetmate.fragments.fragment.EasyTweetFragment;
import com.ybsystem.tweetmate.fragments.fragment.MainFragment;
import com.ybsystem.tweetmate.libs.glide.GlideApp;
import com.ybsystem.tweetmate.libs.rfab.CardItem;
import com.ybsystem.tweetmate.libs.rfab.RapidFloatingActionListView;
import com.ybsystem.tweetmate.databases.*;
import com.ybsystem.tweetmate.utils.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.ybsystem.tweetmate.activities.preference.SettingActivity.*;
import static com.ybsystem.tweetmate.models.enums.ConfirmAction.*;
import static com.ybsystem.tweetmate.models.enums.TweetStyle.*;
import static com.ybsystem.tweetmate.resources.ResColor.*;
import static com.ybsystem.tweetmate.resources.ResString.*;

public class MainActivity extends ActivityBase
        implements RapidFloatingActionListView.OnRfaListViewListener {

    // FloatingActionButton
    private RapidFloatingActionHelper mRfaHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if account exists
        if (TweetMateApp.getData().isAccountEmpty()) {
            Intent intent = new Intent(this, OAuthActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        // Main process
        getSupportActionBar().hide();
        setContentView(R.layout.activity_main);
        setMainFragment(savedInstanceState);
        setDrawerFragment(savedInstanceState);
        setTweetAction(savedInstanceState);
        setWallpaper();
        showUpdateInfo();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout d = findViewById(R.id.drawer_layout);
        int s = GravityCompat.START;

        // Check drawer state
        if (d.isDrawerOpen(s)) {
            d.closeDrawer(s);
        } else if (PrefSystem.getConfirmSettings().contains(FINISH)) {
            DialogUtils.showConfirm(STR_CONFIRM_FINISH_APP, (di, wh) -> finish());
        } else {
            finish();
        }
    }

    @Override
    public void onItemClick(int position) {
        Intent intent;
        switch (position) {
            case 0: // Search
                intent = new Intent(this, SearchActivity.class);
                startActivityForResult(intent, 0);
                overridePendingTransition(R.anim.slide_in_from_right, R.anim.zoom_out);
                break;
            case 1: // Account
                DialogUtils.showAccountChoice();
                break;
            case 2: // Profile
                intent = new Intent(this, ProfileActivity.class);
                intent.putExtra("USER_ID", TweetMateApp.getMyUser().getId());
                startActivityForResult(intent, 0);
                overridePendingTransition(R.anim.slide_in_from_right, R.anim.zoom_out);
                break;
            case 3: // Settings
                intent = new Intent(this, SettingActivity.class);
                startActivityForResult(intent, 0);
                overridePendingTransition(R.anim.slide_in_from_right, R.anim.zoom_out);
                break;
        }
        mRfaHelper.toggleContent();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (resultCode) {
            case REBOOT_IMMEDIATE:
                ActivityUtils.rebootActivity(this);
                break;
            case REBOOT_PREPARATION:
                DialogUtils.showPersistentProgress(STR_APPLYING);
                new Handler().postDelayed(() -> {
                    DialogUtils.dismissProgress();
                    ToastUtils.showShortToast(STR_SUCCESS_APPLY);
                    ActivityUtils.rebootActivity(this);
                }, 1500);
                break;
        }
    }

    private void setMainFragment(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            Fragment fragment = new MainFragment();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.frame_main, fragment);
            transaction.commit();
        }
    }

    private void setDrawerFragment(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            Fragment fragment = new DrawerFragment();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.frame_drawer, fragment);
            transaction.commit();
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
            // Hide buttons
            findViewById(R.id.fab).setVisibility(View.GONE);
            findViewById(R.id.rfab).setVisibility(View.GONE);
        } else {
            // TweetButton
            findViewById(R.id.fab).setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), PostActivity.class);
                startActivityForResult(intent, 0);
                overridePendingTransition(R.anim.fade_in_from_bottom, R.anim.none);
            });
            // MoreButton
            setMoreActionButton();
        }
    }

    private void setMoreActionButton() {
        // Find
        RapidFloatingActionLayout rfaLayout = findViewById(R.id.rfal);
        RapidFloatingActionButton rfaButton = findViewById(R.id.rfab);

        // Create ListView
        RapidFloatingActionListView rfaListView = new RapidFloatingActionListView(this);
        rfaListView.setOnRfaListViewListener(this);

        // Set list items
        List<CardItem> cardItems = new ArrayList<>();
        cardItems.add(new CardItem().setName(STR_SEARCH).setResId(R.drawable.ic_search));
        cardItems.add(new CardItem().setName(STR_ACCOUNT).setResId(R.drawable.ic_user));
        cardItems.add(new CardItem().setName(STR_PROFILE).setResId(R.drawable.ic_doc));
        cardItems.add(new CardItem().setName(STR_SETTINGS).setResId(R.drawable.ic_setting));
        rfaListView.setList(cardItems);

        // Build action button
        mRfaHelper = new RapidFloatingActionHelper(
                this,
                rfaLayout,
                rfaButton,
                rfaListView
        ).build();
    }

    private void setWallpaper() {
        // Check setting
        String path = PrefWallpaper.getWallpaperPath();
        if (path.equals(STR_NOT_SET)) {
            return;
        }
        // Get color
        int color = COLOR_BACKGROUND;
        color = PrefWallpaper.applyTransparency(color);

        // Visible wallpaper
        ImageView wallpaper = findViewById(R.id.image_wallpaper);
        wallpaper.setColorFilter(color);
        wallpaper.setVisibility(View.VISIBLE);

        // Load image
        Uri uri = Uri.fromFile(new File(path));
        GlideApp.with(this).load(uri).into(wallpaper);
    }

    private void showUpdateInfo() {
        // Get version
        String version = TweetMateApp.getData().getVersion();
        String newVersion = getString(R.string.app_version);
        String updateInfo = getString(R.string.app_update_info);

        // Check version
        if (version.equals(newVersion)) {
            return;
        }
        // Show dialog
        NoticeDialog dialog = new NoticeDialog().newInstance(STR_UPDATE_INFO, updateInfo);
        FragmentManager fm = getSupportFragmentManager();
        if (fm.findFragmentByTag("NoticeDialog") == null) {
            dialog.show(fm, "NoticeDialog");
        }
        // Save new version
        TweetMateApp.getData().setVersion(newVersion);
    }

}
