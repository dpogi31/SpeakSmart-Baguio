
package com.example.speaksmartbaguio.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.speaksmartbaguio.repository.DictionaryRepository;
import com.example.speaksmartbaguio.repository.PhraseRepository;

import java.util.concurrent.atomic.AtomicBoolean;

public final class SyncManager {

    private static final String TAG = "SyncManager";
    private static final String PREFS = "sync_status";
    private static final String KEY_LAST_SUCCESS = "last_successful_sync";

    private static final AtomicBoolean syncing = new AtomicBoolean(false);
    private static final Handler mainHandler =
            new Handler(Looper.getMainLooper());

    public interface SyncListener {
        void onSyncStarted();

        void onSyncFinished(boolean success, String message);
    }

    private SyncManager() {
        // Utility class.
    }

    public static boolean isSyncing() {
        return syncing.get();
    }

    public static long getLastSuccessfulSync(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getLong(KEY_LAST_SUCCESS, 0L);
    }

    public static void sync(Context context) {
        sync(context, null);
    }

    public static void sync(Context context, SyncListener listener) {
        Context appContext = context.getApplicationContext();

        if (!NetworkUtil.isOnline(appContext)) {
            notifyFinished(
                    listener,
                    false,
                    "Offline. Your saved data is still available."
            );
            return;
        }

        if (!syncing.compareAndSet(false, true)) {
            notifyStarted(listener);
            return;
        }

        notifyStarted(listener);

        VersionChecker.checkVersionsForSync(
                appContext,
                (
                        checkSucceeded,
                        dictionaryNeedsUpdate,
                        phrasebookNeedsUpdate,
                        dictionaryVersion,
                        phrasebookVersion,
                        error
                ) -> {
                    if (!checkSucceeded) {
                        finish(
                                appContext,
                                listener,
                                false,
                                "Could not check for updates. "
                                        + safeMessage(error)
                        );
                        return;
                    }

                    int tasks = 0;

                    if (dictionaryNeedsUpdate) {
                        tasks++;
                    }

                    if (phrasebookNeedsUpdate) {
                        tasks++;
                    }

                    if (tasks == 0) {
                        finish(
                                appContext,
                                listener,
                                true,
                                "Your saved data is up to date."
                        );
                        return;
                    }

                    SyncBatch batch = new SyncBatch(
                            appContext,
                            listener,
                            tasks
                    );

                    if (dictionaryNeedsUpdate) {
                        DictionaryRepository repository =
                                new DictionaryRepository(appContext);

                        repository.syncFromServer(
                                appContext,
                                dictionaryVersion,
                                (success, syncError) -> {
                                    repository.close();

                                    Log.d(
                                            TAG,
                                            "Dictionary sync success: " + success
                                    );

                                    batch.taskFinished(
                                            success,
                                            "Dictionary: " + safeMessage(syncError)
                                    );
                                }
                        );
                    }

                    if (phrasebookNeedsUpdate) {
                        PhraseRepository repository =
                                new PhraseRepository(appContext);

                        repository.syncFromServer(
                                appContext,
                                phrasebookVersion,
                                (success, syncError) -> {
                                    repository.close();

                                    Log.d(
                                            TAG,
                                            "Phrasebook sync success: " + success
                                    );

                                    batch.taskFinished(
                                            success,
                                            "Phrasebook: " + safeMessage(syncError)
                                    );
                                }
                        );
                    }
                }
        );
    }

    private static final class SyncBatch {

        private final Context context;
        private final SyncListener listener;
        private int remaining;
        private boolean allSucceeded = true;
        private String failureMessage = "Synchronization failed.";

        SyncBatch(
                Context context,
                SyncListener listener,
                int taskCount
        ) {
            this.context = context;
            this.listener = listener;
            this.remaining = taskCount;
        }

        synchronized void taskFinished(
                boolean success,
                String error
        ) {
            if (!success) {
                allSucceeded = false;
                failureMessage = error;
            }

            remaining--;

            if (remaining == 0) {
                finish(
                        context,
                        listener,
                        allSucceeded,
                        allSucceeded
                                ? "Your data has been synchronized."
                                : failureMessage
                );
            }
        }
    }

    private static void finish(
            Context context,
            SyncListener listener,
            boolean success,
            String message
    ) {
        if (success) {
            context.getApplicationContext()
                    .getSharedPreferences(
                            PREFS,
                            Context.MODE_PRIVATE
                    )
                    .edit()
                    .putLong(
                            KEY_LAST_SUCCESS,
                            System.currentTimeMillis()
                    )
                    .apply();
        }

        syncing.set(false);
        notifyFinished(listener, success, message);
    }

    private static void notifyStarted(SyncListener listener) {
        if (listener != null) {
            mainHandler.post(listener::onSyncStarted);
        }
    }

    private static void notifyFinished(
            SyncListener listener,
            boolean success,
            String message
    ) {
        if (listener != null) {
            mainHandler.post(
                    () -> listener.onSyncFinished(success, message)
            );
        }
    }

    private static String safeMessage(String message) {
        return message == null || message.trim().isEmpty()
                ? "Please try again."
                : message;
    }
}
