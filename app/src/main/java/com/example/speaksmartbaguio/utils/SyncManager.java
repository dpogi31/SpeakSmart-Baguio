package com.example.speaksmartbaguio.utils;
import com.example.speaksmartbaguio.utils.VersionChecker;

import android.content.Context;
import android.util.Log;

import com.example.speaksmartbaguio.repository.DictionaryRepository;
import com.example.speaksmartbaguio.repository.PhraseRepository;

public class SyncManager {

    private static final String TAG = "SyncManager";

    public static void sync(Context context) {

        VersionChecker.checkVersions(
                context,
                (
                        dictionaryNeedsUpdate,
                        phrasebookNeedsUpdate,
                        dictionaryVersion,
                        phrasebookVersion
                ) -> {

                    Log.d(TAG, "Dictionary update: " + dictionaryNeedsUpdate);
                    Log.d(TAG, "Phrasebook update: " + phrasebookNeedsUpdate);

                    DictionaryRepository dictionaryRepository =
                            new DictionaryRepository(context);

                    new Thread(() -> {

                        boolean roomEmpty = dictionaryRepository.isEmpty();

                        if (dictionaryNeedsUpdate || roomEmpty) {

                            Log.d(TAG, "Updating Dictionary...");
                            Log.d(TAG, "Room empty = " + roomEmpty);

                            dictionaryRepository.syncFromServer(
                                    context,
                                    dictionaryVersion,
                                    () -> Log.d(TAG, "Dictionary synced.")
                            );

                        } else {

                            Log.d(TAG, "Dictionary already up to date.");

                        }

                    }).start();

                    PhraseRepository phraseRepository =
                            new PhraseRepository(context);

                    new Thread(() -> {

                        boolean roomEmpty = phraseRepository.isEmpty();

                        if (phrasebookNeedsUpdate || roomEmpty) {

                            Log.d(TAG, "Updating Phrasebook...");
                            Log.d(TAG, "Phrase Room empty = " + roomEmpty);

                            phraseRepository.syncFromServer(
                                    context,
                                    phrasebookVersion,
                                    () -> Log.d(TAG, "Phrasebook synced.")
                            );

                        } else {

                            Log.d(TAG, "Phrasebook already up to date.");

                        }

                    }).start();
                    if (!dictionaryNeedsUpdate && !phrasebookNeedsUpdate) {
                        Log.d(TAG, "Local database is already up to date.");
                    }
                });

    }

}