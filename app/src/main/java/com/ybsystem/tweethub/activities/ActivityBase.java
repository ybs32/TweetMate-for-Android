package com.ybsystem.tweethub.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;

import com.ybsystem.tweethub.R;
import com.ybsystem.tweethub.application.TweetHubApp;
import com.ybsystem.tweethub.storages.PrefTheme;

public abstract class ActivityBase extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TweetHubApp.setActivity(this);
        setTheme();
        setBackButton();
    }

    @Override
    protected void onResume(){
        super.onResume();
        TweetHubApp.setActivity(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        TweetHubApp.setActivity(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        switch (getLocalClassName()) {
            case "activities.MainActivity":
                break;
            case "activities.preference.SettingActivity":
            case "activities.preference.ThemeActivity":
            case "activities.preference.AppearanceActivity":
            case "activities.preference.ClickActionActivity":
            case "activities.preference.SystemActivity":
            case "activities.preference.WallpaperActivity":
                overridePendingTransition(R.anim.zoom_in, R.anim.slide_out_to_right);
                break;
        }
    }

    private void setTheme() {
        switch (PrefTheme.getTheme()) {
            case "LIGHT":
                setTheme(R.style.LightTheme);
                break;
            case "DARK":
                setTheme(R.style.DarkTheme);
        }
    }

    private void setBackButton() {
        switch (getLocalClassName()) {
            case "activities.MainActivity":
                break;
            case "activities.preference.SettingActivity":
            case "activities.preference.ThemeActivity":
            case "activities.preference.AppearanceActivity":
            case "activities.preference.ClickActionActivity":
            case "activities.preference.SystemActivity":
            case "activities.preference.WallpaperActivity":
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                break;
        }
    }

}
