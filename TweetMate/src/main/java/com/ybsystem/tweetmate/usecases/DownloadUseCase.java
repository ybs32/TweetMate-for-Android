package com.ybsystem.tweetmate.usecases;

import android.app.Activity;
import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;

import com.ybsystem.tweetmate.application.TweetMateApp;
import com.ybsystem.tweetmate.utils.DialogUtils;
import com.ybsystem.tweetmate.utils.StorageUtils;
import com.ybsystem.tweetmate.utils.ToastUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.observers.DisposableObserver;
import io.reactivex.rxjava3.schedulers.Schedulers;

import static com.ybsystem.tweetmate.resources.ResString.*;

public class DownloadUseCase {

    private DownloadUseCase() {
    }

    public static void downloadImage(String url) {
        // Async
        Observable<String> observable = createSaveImageObs(url);

        // Result
        DisposableObserver<String> disp = new DisposableObserver<String>() {
            @Override
            public void onNext(String path) {
                // Success
                ToastUtils.showLongToast(STR_SUCCESS_SAVE_IMAGE + "\n" + path);
            }

            @Override
            public void onError(Throwable t) {
                // Failed...
                ToastUtils.showShortToast(t.getMessage());
            }

            @Override
            public void onComplete() {
            }
        };

        // Show progress
        DialogUtils.showProgress(STR_CONNECTING);

        // Subscribe
        observable.subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally(DialogUtils::dismissProgress)
                .subscribe(disp);
    }

    private static Observable<String> createSaveImageObs(String url) {
        // Create
        return Observable.create(e -> {
            String path = "";
            FileOutputStream out = null;

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Over Android 10
                    Activity act = TweetMateApp.getActivity();
                    ContentResolver resolver = act.getContentResolver();
                    Uri uri = StorageUtils.createMediaUri(Environment.DIRECTORY_PICTURES, resolver);
                    out = new FileOutputStream(resolver.openFileDescriptor(uri, "w").getFileDescriptor());
                    path = StorageUtils.pathFromUri(act, uri);
                } else {
                    // Under Android 9
                    File file = StorageUtils.createMediaFile(Environment.DIRECTORY_PICTURES);
                    out = new FileOutputStream(file);
                    path = file.getAbsolutePath();
                }

                // Write
                InputStream input = new java.net.URL(url).openStream();
                BitmapFactory.decodeStream(input)
                        .compress(Bitmap.CompressFormat.JPEG, 100, out);
                out.flush();

                // Notify
                e.onNext(path);
                e.onComplete();

            } catch (Exception ex) {
                e.onError(ex);

            } finally {
                // Close
                try { if (out != null) out.close(); }
                catch (Exception ignored) { /* Ignored */ }
            }
        });
    }

}
