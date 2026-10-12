
package com.example.speaksmartbaguio.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.FirebaseFirestore;

public final class VersionChecker {

    private static final String PREF_NAME = "app_versions";

    public interface VersionCallback {
        void onResult(
                boolean dictionaryNeedsUpdate,
                boolean phrasebookNeedsUpdate,
                long dictionaryVersion,
                long phrasebookVersion
        );
    }

    public interface SyncVersionCallback {
        void onResult(
                boolean checkSucceeded,
                boolean dictionaryNeedsUpdate,
                boolean phrasebookNeedsUpdate,
                long dictionaryVersion,
                long phrasebookVersion,
                String error
        );
    }

    private VersionChecker() {
        // Utility class.
    }

    public static void checkVersions(
            Context context,
            @NonNull VersionCallback callback
    ) {
        checkVersionsForSync(context, (
                checkSucceeded,
                dictionaryNeedsUpdate,
                phrasebookNeedsUpdate,
                dictionaryVersion,
                phrasebookVersion,
                error
        ) -> {
            if (checkSucceeded) {
                callback.onResult(
                        dictionaryNeedsUpdate,
                        phrasebookNeedsUpdate,
                        dictionaryVersion,
                        phrasebookVersion
                );
            } else {
                SharedPreferences prefs = getPreferences(context);

                long localDictionary =
                        prefs.getLong("dictionaryVersion", 0);

                long localPhrasebook =
                        prefs.getLong("phrasebookVersion", 0);

                callback.onResult(
                        localDictionary == 0,
                        localPhrasebook == 0,
                        0,
                        0
                );
            }
        });
    }

    public static void checkVersionsForSync(
            Context context,
            @NonNull SyncVersionCallback callback
    ) {
        Context appContext = context.getApplicationContext();

        FirebaseFirestore.getInstance()
                .collection("app_metadata")
                .document("versions")
                .get()
                .addOnSuccessListener(document -> {
                    SharedPreferences prefs =
                            getPreferences(appContext);

                    long localDictionary =
                            prefs.getLong("dictionaryVersion", 0);

                    long localPhrasebook =
                            prefs.getLong("phrasebookVersion", 0);

                    Long dictionaryValue =
                            document.getLong("dictionaryVersion");

                    Long phrasebookValue =
                            document.getLong("phrasebookVersion");

                    long serverDictionary =
                            dictionaryValue == null ? 0 : dictionaryValue;

                    long serverPhrasebook =
                            phrasebookValue == null ? 0 : phrasebookValue;

                    callback.onResult(
                            true,
                            serverDictionary > localDictionary,
                            serverPhrasebook > localPhrasebook,
                            serverDictionary,
                            serverPhrasebook,
                            null
                    );
                })
                .addOnFailureListener(error -> {
                    callback.onResult(
                            false,
                            false,
                            false,
                            0,
                            0,
                            error.getMessage() == null
                                    ? "Could not check server versions."
                                    : error.getMessage()
                    );
                });
    }

    public static void saveDictionaryVersion(
            Context context,
            long version
    ) {
        getPreferences(context).edit()
                .putLong("dictionaryVersion", version)
                .apply();
    }

    public static void savePhrasebookVersion(
            Context context,
            long version
    ) {
        getPreferences(context).edit()
                .putLong("phrasebookVersion", version)
                .apply();
    }

    private static SharedPreferences getPreferences(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(
                        PREF_NAME,
                        Context.MODE_PRIVATE
                );
    }
}
