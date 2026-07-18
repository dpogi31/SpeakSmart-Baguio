package com.example.speaksmartbaguio.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.FirebaseFirestore;

public class VersionChecker {

    private static final String PREF_NAME = "app_versions";

    public interface VersionCallback {
        void onResult(
                boolean dictionaryNeedsUpdate,
                boolean phrasebookNeedsUpdate,
                long dictionaryVersion,
                long phrasebookVersion
        );
    }

    public static void checkVersions(Context context,
                                     @NonNull VersionCallback callback) {

        FirebaseFirestore.getInstance()
                .collection("app_metadata")
                .document("versions")
                .get()
                .addOnSuccessListener(document -> {

                    SharedPreferences prefs =
                            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

                    long localDictionary =
                            prefs.getLong("dictionaryVersion", 0);

                    long localPhrasebook =
                            prefs.getLong("phrasebookVersion", 0);

                    long serverDictionary =
                            document.getLong("dictionaryVersion") == null
                                    ? 0
                                    : document.getLong("dictionaryVersion");

                    long serverPhrasebook =
                            document.getLong("phrasebookVersion") == null
                                    ? 0
                                    : document.getLong("phrasebookVersion");

                    callback.onResult(
                            serverDictionary > localDictionary,
                            serverPhrasebook > localPhrasebook,
                            serverDictionary,
                            serverPhrasebook
                    );

                })
                .addOnFailureListener(e ->
                        callback.onResult(
                                false,
                                false,
                                0,
                                0
                        ));
    }

    public static void saveDictionaryVersion(Context context,
                                             long version) {

        SharedPreferences prefs =
                context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        prefs.edit()
                .putLong("dictionaryVersion", version)
                .apply();
    }

    public static void savePhrasebookVersion(Context context,
                                             long version) {

        SharedPreferences prefs =
                context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        prefs.edit()
                .putLong("phrasebookVersion", version)
                .apply();
    }
}