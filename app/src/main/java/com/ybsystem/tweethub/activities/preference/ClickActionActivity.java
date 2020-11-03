package com.ybsystem.tweethub.activities.preference;

import android.os.Bundle;

import androidx.fragment.app.FragmentTransaction;

import com.ybsystem.tweethub.R;
import com.ybsystem.tweethub.activities.ActivityBase;
import com.ybsystem.tweethub.fragments.preference.ClickActionFragment;

public class ClickActionActivity extends ActivityBase {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_preference);
        setClickActionFragment();
    }

    private void setClickActionFragment() {
        ClickActionFragment fragment = new ClickActionFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.frame_container, fragment);
        transaction.commit();
    }

}