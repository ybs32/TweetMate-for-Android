package com.ybsystem.tweetmate.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.fragment.app.FragmentTransaction;

import com.ybsystem.tweetmate.R;
import com.ybsystem.tweetmate.application.TweetMateApp;
import com.ybsystem.tweetmate.fragments.fragment.PostFragment;
import com.ybsystem.tweetmate.libs.eventbus.PostEvent;
import com.ybsystem.tweetmate.models.entities.EntityArray;
import com.ybsystem.tweetmate.utils.DialogUtils;
import com.ybsystem.tweetmate.utils.GlideUtils;
import com.ybsystem.tweetmate.utils.KeyboardUtils;
import com.ybsystem.tweetmate.utils.ToastUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;

import static com.ybsystem.tweetmate.models.enums.ImageOption.NONE;
import static com.ybsystem.tweetmate.resources.ResString.*;

public class PostActivity extends ActivityBase {

    public List<Uri> mImageUris;
    public static final int REQUEST_GALLERY = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Init
        mImageUris = new ArrayList<>();

        // Set
        setPostFragment(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe
    public void onEvent(PostEvent event) {
        // Close keyboard
        View focus = getCurrentFocus();
        if (focus != null) {
            KeyboardUtils.closeKeyboard(focus);
            focus.clearFocus();
        }
        // Finish
        finish();
        overridePendingTransition(R.anim.none, R.anim.fade_out_to_bottom);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.post, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Save draft
            case R.id.item_draft:
                // Check text
                EditText edit = findViewById(R.id.edit_post);
                if (edit.length() == 0) {
                    ToastUtils.showShortToast(STR_FAIL_NO_TEXT);
                    return true;
                }
                // Save draft
                EntityArray<String> drafts = TweetMateApp.getMyAccount().getDrafts();
                drafts.add(edit.getText().toString());
                ToastUtils.showShortToast(STR_SUCCESS_SAVE_DRAFT);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Check result
        if (resultCode != RESULT_OK) {
            return;
        }

        // Check request
        if (requestCode == REQUEST_GALLERY) {
            // Store uri
            Uri uri = data.getData();
            mImageUris.add(uri);

            // Get layout
            LinearLayout ll = findViewById(R.id.linear_photo);
            ImageView image = (ImageView) ll.getChildAt(mImageUris.size() - 1);
            image.setOnClickListener(v ->
                    DialogUtils.showConfirm(
                            STR_CONFIRM_CANCEL_IMAGE,
                            (dialog, which) -> cancelImages()
                    )
            );
            // Show image
            GlideUtils.load(uri.toString(), image, NONE);
            ll.setVisibility(View.VISIBLE);
            image.setVisibility(View.VISIBLE);
        }
    }

    private void setPostFragment(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            PostFragment fragment = new PostFragment();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(android.R.id.content, fragment);
            transaction.commit();
        }
    }

    private void cancelImages() {
        // Clear array
        mImageUris.clear();

        // Hide images
        findViewById(R.id.image_photo_1).setVisibility(View.GONE);
        findViewById(R.id.image_photo_2).setVisibility(View.GONE);
        findViewById(R.id.image_photo_3).setVisibility(View.GONE);
        findViewById(R.id.image_photo_4).setVisibility(View.GONE);
        findViewById(R.id.linear_photo).setVisibility(View.GONE);

        // Show message
        ToastUtils.showShortToast(STR_SUCCESS_CANCEL_IMAGE);
    }

}
