
package com.example.speaksmartbaguio.repository;

import android.content.Context;

import com.example.speaksmartbaguio.ApiService;
import com.example.speaksmartbaguio.Word;
import com.example.speaksmartbaguio.dao.DictionaryDao;
import com.example.speaksmartbaguio.database.DatabaseClient;
import com.example.speaksmartbaguio.entity.DictionaryEntity;
import com.example.speaksmartbaguio.mapper.EntityMapper;
import com.example.speaksmartbaguio.utils.VersionChecker;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DictionaryRepository {

    private final DictionaryDao dictionaryDao;
    private final ExecutorService executorService;
    private final Context context;
    private final ApiService apiService;

    public interface LoadCallback {
        void onLoaded(List<DictionaryEntity> result);
    }

    public interface SyncResultCallback {
        void onFinished(boolean success, String error);
    }

    public interface SyncCallback {
        void onFinished();
    }

    public DictionaryRepository(Context context) {
        this.context = context.getApplicationContext();

        dictionaryDao = DatabaseClient.getInstance(this.context)
                .dictionaryDao();

        executorService = Executors.newSingleThreadExecutor();
        apiService = ApiService.getInstance();
    }

    public void insert(DictionaryEntity entity) {
        executorService.execute(() -> dictionaryDao.insert(entity));
    }

    public void insertAll(List<DictionaryEntity> entities) {
        executorService.execute(() -> dictionaryDao.insertAll(entities));
    }

    public void replaceAll(List<DictionaryEntity> entities) {
        executorService.execute(() -> {
            dictionaryDao.deleteAll();
            dictionaryDao.insertAll(entities);
        });
    }

    public void getAllWords(LoadCallback callback) {
        executorService.execute(() ->
                callback.onLoaded(dictionaryDao.getAllWords())
        );
    }

    public void searchWords(String query, LoadCallback callback) {
        executorService.execute(() ->
                callback.onLoaded(dictionaryDao.searchWords(query))
        );
    }

    // Backward-compatible method for existing callers.
    public void syncFromServer(
            Context context,
            long version,
            Runnable finished
    ) {
        syncFromServer(context, version, (success, error) -> {
            if (finished != null) {
                finished.run();
            }
        });
    }

    public void syncFromServer(
            Context context,
            long version,
            SyncResultCallback callback
    ) {
        List<DictionaryEntity> allWords = new ArrayList<>();

        downloadPage(1, version, allWords, callback);
    }

    private void downloadPage(
            int page,
            long version,
            List<DictionaryEntity> allWords,
            SyncResultCallback callback
    ) {
        apiService.getDictionary(
                page,
                100,
                "",
                new ApiService.ApiCallback<Word>() {
                    @Override
                    public void onSuccess(
                            List<Word> items,
                            boolean hasMore
                    ) {
                        try {
                            if (items != null) {
                                allWords.addAll(
                                        EntityMapper.toEntityList(items)
                                );
                            }

                            if (hasMore) {
                                downloadPage(
                                        page + 1,
                                        version,
                                        allWords,
                                        callback
                                );
                            } else {
                                saveDownloadedWords(version, allWords, callback);
                            }
                        } catch (Exception error) {
                            reportFailure(callback, error.getMessage());
                        }
                    }

                    @Override
                    public void onError(String error) {
                        reportFailure(callback, error);
                    }
                }
        );
    }

    private void saveDownloadedWords(
            long version,
            List<DictionaryEntity> words,
            SyncResultCallback callback
    ) {
        executorService.execute(() -> {
            try {
                dictionaryDao.deleteAll();
                dictionaryDao.insertAll(words);

                // Save the version only after Room operations succeed.
                VersionChecker.saveDictionaryVersion(context, version);

                reportSuccess(callback);
            } catch (Exception error) {
                reportFailure(
                        callback,
                        error.getMessage() == null
                                ? "Could not save dictionary data."
                                : error.getMessage()
                );
            }
        });
    }

    private void reportSuccess(SyncResultCallback callback) {
        if (callback != null) {
            callback.onFinished(true, null);
        }
    }

    private void reportFailure(
            SyncResultCallback callback,
            String error
    ) {
        if (callback != null) {
            callback.onFinished(
                    false,
                    error == null || error.trim().isEmpty()
                            ? "Dictionary synchronization failed."
                            : error
            );
        }
    }

    public void deleteAll() {
        executorService.execute(dictionaryDao::deleteAll);
    }

    public void close() {
        executorService.shutdown();
    }
}
