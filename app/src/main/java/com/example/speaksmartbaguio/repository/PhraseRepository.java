package com.example.speaksmartbaguio.repository;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import com.example.speaksmartbaguio.dao.PhraseDao;
import com.example.speaksmartbaguio.database.DatabaseClient;
import com.example.speaksmartbaguio.entity.PhraseEntity;
import com.example.speaksmartbaguio.ApiService;
import com.example.speaksmartbaguio.Phrase;
import com.example.speaksmartbaguio.mapper.EntityMapper;
import com.example.speaksmartbaguio.utils.VersionChecker;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PhraseRepository {

    private final PhraseDao phraseDao;
    private final ExecutorService executorService;

    public interface DatabaseCallback<T> {
        void onComplete(T result);
    }
    public boolean isEmpty() {
        return phraseDao.getCount() == 0;
    }
    public PhraseRepository(Context context) {
        phraseDao = DatabaseClient.getInstance(context).phraseDao();
        executorService = Executors.newSingleThreadExecutor();
    }

    public void insert(PhraseEntity entity) {
        executorService.execute(() -> phraseDao.insert(entity));
    }

    public void insertAll(List<PhraseEntity> entities) {
        executorService.execute(() -> phraseDao.insertAll(entities));
    }

    public void replaceAll(List<PhraseEntity> entities) {

        executorService.execute(() -> {

            try {

                Log.d("PHRASE_SYNC", "Deleting old phrases...");
                phraseDao.deleteAll();

                Log.d("PHRASE_SYNC", "Inserting " + entities.size() + " phrases...");

                phraseDao.insertAll(entities);

                Log.d("PHRASE_SYNC", "Insert finished.");

                Log.d("PHRASE_SYNC",
                        "Rows after insert = " + phraseDao.getCount());

            } catch (Exception e) {

                Log.e("PHRASE_SYNC",
                        "ROOM INSERT FAILED",
                        e);

            }

        });

    }

    public void getAllPhrases(DatabaseCallback<List<PhraseEntity>> callback) {
        executorService.execute(() -> {
            List<PhraseEntity> result = phraseDao.getAllPhrases();
            callback.onComplete(result);
        });
    }
    public void getCount(DatabaseCallback<Integer> callback) {
        executorService.execute(() -> callback.onComplete(phraseDao.getCount()));
    }
    public void searchPhrases(String query,
                              DatabaseCallback<List<PhraseEntity>> callback) {
        executorService.execute(() -> {
            List<PhraseEntity> result = phraseDao.searchPhrases(query);
            callback.onComplete(result);
        });
    }
    public void syncFromServer(
            Context context,
            long version,
            Runnable finished) {

        ApiService api = ApiService.getInstance();

        List<PhraseEntity> allPhrases = new ArrayList<>();

        downloadPage(
                api,
                context,
                version,
                1,
                allPhrases,
                finished
        );
    }
    private void downloadPage(
            ApiService api,
            Context context,
            long version,
            int page,
            List<PhraseEntity> allPhrases,
            Runnable finished) {

        api.getPhrasebook(
                page,
                100,
                "",
                new ApiService.ApiCallback<Phrase>() {

                    @Override
                    public void onSuccess(List<Phrase> items,
                                          boolean hasMore) {
                        Log.d("PHRASE_SYNC", "Received = " + items.size());
                        allPhrases.addAll(
                                EntityMapper.toPhraseEntityList(items)
                        );
                        Log.d("PHRASE_SYNC", "Buffer size = " + allPhrases.size());

                        if (hasMore) {

                            downloadPage(
                                    api,
                                    context,
                                    version,
                                    page + 1,
                                    allPhrases,
                                    finished
                            );

                        } else {

                            replaceAll(allPhrases);
                            executorService.execute(() -> {
                            });
                            VersionChecker.savePhrasebookVersion(
                                    context,
                                    version
                            );

                            if (finished != null)
                                finished.run();

                        }

                    }

                    @Override
                    public void onError(String error) {

                        if (finished != null)
                            finished.run();

                    }

                });

    }

    public void deleteAll() {
        executorService.execute(phraseDao::deleteAll);
    }

    public void close() {
        executorService.shutdown();
    }
}