package com.ybsystem.tweethub.usecases;

import com.ybsystem.tweethub.application.TweetHubApp;;
import com.ybsystem.tweethub.libs.eventbus.StatusEvent;
import com.ybsystem.tweethub.models.entities.twitter.TwitterStatus;
import com.ybsystem.tweethub.storages.PrefAppearance;
import com.ybsystem.tweethub.storages.PrefSystem;
import com.ybsystem.tweethub.utils.DialogUtils;
import com.ybsystem.tweethub.utils.ExceptionUtils;
import com.ybsystem.tweethub.utils.ToastUtils;

import org.greenrobot.eventbus.EventBus;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.observers.DisposableObserver;
import io.reactivex.rxjava3.schedulers.Schedulers;
import twitter4j.TwitterException;

import static com.ybsystem.tweethub.models.enums.ConfirmAction.*;

public class TwitterUseCase {

    private TwitterUseCase() {
    }

    public static void retweet(TwitterStatus status) {
        // Check
        if (!status.isRetweeted() && !status.isPublic()) {
            return;
        }

        // Create text
        boolean isRetweeted = status.isRetweeted();
        String confirm = isRetweeted ? "リツイートを解除しますか？" : "リツイートしますか？";
        String success = isRetweeted ? "リツイートを解除しました。" : "リツイートしました。";
        String fail = isRetweeted ? "リツイート解除に失敗しました..." : "リツイートに失敗しました...";

        Observable<Object> observable = Observable.create(e -> {
            try {
                if (isRetweeted) {
                    TweetHubApp.getTwitter().unRetweetStatus(status.getId());
                } else {
                    TweetHubApp.getTwitter().retweetStatus(status.getId());
                }
                e.onComplete();
            } catch (TwitterException ex) {
                e.onError(ex);
            }
        });

        DisposableObserver<Object> disposable = new DisposableObserver<Object>() {
            @Override
            public void onNext(Object obj) {
            }

            @Override
            public void onError(Throwable t) {
                // Failed...
                TwitterException e = (TwitterException) t;
                ToastUtils.showShortToast(fail);
                ToastUtils.showShortToast(ExceptionUtils.getErrorMessage(e));
            }

            @Override
            public void onComplete() {
                // Success
                if (isRetweeted) {
                    status.setRetweeted(false);
                    status.setRetweetCount(status.getRetweetCount() - 1);
                    boolean isDeleteTarget = status.isRetweet() && status.isMyTweet();
                    EventBus.getDefault().post(isDeleteTarget ? new StatusEvent(status) : new StatusEvent());
                } else {
                    status.setRetweeted(true);
                    status.setRetweetCount(status.getRetweetCount() + 1);
                    EventBus.getDefault().post(new StatusEvent());
                }
                ToastUtils.showShortToast(success);
            }
        };

        // Show confirm dialog
        if (PrefSystem.getConfirmSettings().contains(RETWEET)) {
            DialogUtils.showConfirmDialog(
                    confirm,
                    (dialog, which) -> observable
                            .subscribeOn(Schedulers.newThread())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(disposable)
            );
        } else {
            observable
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(disposable);
        }
    }

    public static void favorite(TwitterStatus status) {
        // Create text
        boolean isFavorited = status.isFavorited();
        String str = PrefAppearance.getLikeFavText();
        String confirm = isFavorited ? str + "を解除しますか？" : str + "しますか？";
        String success = isFavorited ? str + "を解除しました。" : str + "しました。";
        String fail = isFavorited ? str + "解除に失敗しました..." : str + "に失敗しました...";

        Observable<Object> observable = Observable.create(e -> {
            try {
                if (isFavorited) {
                    TweetHubApp.getTwitter().destroyFavorite(status.getId());
                } else {
                    TweetHubApp.getTwitter().createFavorite(status.getId());
                }
                e.onComplete();
            } catch (TwitterException ex) {
                e.onError(ex);
            }
        });

        DisposableObserver<Object> disposable = new DisposableObserver<Object>() {
            @Override
            public void onNext(Object obj) {
            }

            @Override
            public void onError(Throwable t) {
                // Failed...
                TwitterException e = (TwitterException) t;
                ToastUtils.showShortToast(fail);
                ToastUtils.showShortToast(ExceptionUtils.getErrorMessage(e));
            }

            @Override
            public void onComplete() {
                // Success
                if (isFavorited) {
                    status.setFavorited(false);
                    status.setFavoriteCount(status.getFavoriteCount() - 1);
                } else {
                    status.setFavorited(true);
                    status.setFavoriteCount(status.getFavoriteCount() + 1);
                }
                // Notify event
                EventBus.getDefault().post(new StatusEvent());
                ToastUtils.showShortToast(success);
            }
        };

        // Show confirm dialog
        if (PrefSystem.getConfirmSettings().contains(LIKE)) {
            DialogUtils.showConfirmDialog(
                    confirm,
                    (dialog, which) -> observable
                            .subscribeOn(Schedulers.newThread())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(disposable)
            );
        } else {
            observable
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(disposable);
        }
    }

    public static void delete(TwitterStatus status) {
        Observable<Object> observable = Observable.create(e -> {
            try {
                TweetHubApp.getTwitter().destroyStatus(status.getId());
                e.onComplete();
            } catch (TwitterException ex) {
                e.onError(ex);
            }
        });

        DisposableObserver<Object> disposable = new DisposableObserver<Object>() {
            @Override
            public void onNext(Object obj) {
            }

            @Override
            public void onError(Throwable t) {
                // Failed...
                TwitterException e = (TwitterException) t;
                ToastUtils.showShortToast("ツイートの削除に失敗しました...");
                ToastUtils.showShortToast(ExceptionUtils.getErrorMessage(e));
            }

            @Override
            public void onComplete() {
                // Success
                EventBus.getDefault().post(new StatusEvent(status));
                ToastUtils.showShortToast("ツイートを削除しました。");
            }
        };

        // Show confirm dialog
        if (PrefSystem.getConfirmSettings().contains(DELETE)) {
            DialogUtils.showConfirmDialog(
                    "ツイートを削除しますか？",
                    (dialog, which) -> observable
                            .subscribeOn(Schedulers.newThread())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(disposable)
            );
        } else {
            observable
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(disposable);
        }
    }

}
