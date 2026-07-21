package com.example.speaksmartbaguio.repository;

import android.content.Context;

import com.example.speaksmartbaguio.dao.DictionaryDao;
import com.example.speaksmartbaguio.database.DatabaseClient;
import com.example.speaksmartbaguio.entity.DictionaryEntity;
import android.util.Log;

import com.example.speaksmartbaguio.ApiService;
import com.example.speaksmartbaguio.Word;
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

    private static final int PAGE_SIZE = 100;

    private int currentPage = 1;

    private final List<DictionaryEntity> buffer = new ArrayList<>();
    public DictionaryRepository(Context context) {

        this.context = context.getApplicationContext();

        dictionaryDao = DatabaseClient
                .getInstance(this.context)
                .dictionaryDao();

        executorService = Executors.newSingleThreadExecutor();

        apiService = ApiService.getInstance();
    }
    public boolean isEmpty() {
        return dictionaryDao.getCount() == 0;
    }
    public interface LoadCallback {
        void onLoaded(List<DictionaryEntity> result);
    }
    public interface SyncCallback {

        void onFinished();

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
        executorService.execute(() -> {
            List<DictionaryEntity> result = dictionaryDao.getAllWords();
            callback.onLoaded(result);
        });
    }

    public void searchWords(String query, LoadCallback callback) {
        executorService.execute(() -> {
            List<DictionaryEntity> result = dictionaryDao.searchWords(query);
            callback.onLoaded(result);
        });
    }
    public void syncFromServer(
            Context context,
            long version,
            Runnable finished) {

        ApiService api = ApiService.getInstance();

        List<DictionaryEntity> allWords = new ArrayList<>();

        downloadPage(
                api,
                context,
                version,
                1,
                allWords,
                finished
        );
    }
    private void downloadPage(
            ApiService api,
            Context context,
            long version,
            int page,
            List<DictionaryEntity> allWords,
            Runnable finished) {

        api.getDictionary(
                page,
                100,
                "",
                new ApiService.ApiCallback<Word>() {

                    @Override
                    public void onSuccess(List<Word> items,
                                          boolean hasMore) {

                        allWords.addAll(
                                EntityMapper.toEntityList(items)
                        );

                        if (hasMore) {

                            downloadPage(
                                    api,
                                    context,
                                    version,
                                    page + 1,
                                    allWords,
                                    finished
                            );

                        } else {

                            replaceAll(allWords);

                            VersionChecker.saveDictionaryVersion(
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
    private void downloadPage(long newVersion,
                              SyncCallback callback) {

        apiService.getDictionary(
                currentPage,
                PAGE_SIZE,
                "",
                new ApiService.ApiCallback<Word>() {

                    @Override
                    public void onSuccess(List<Word> items,
                                          boolean hasMore) {

                        buffer.addAll(
                                EntityMapper.toEntityList(items)
                        );

                        if (hasMore) {

                            currentPage++;

                            downloadPage(
                                    newVersion,
                                    callback
                            );

                        } else {

                            replaceAll(buffer);

                            VersionChecker.saveDictionaryVersion(
                                    context,
                                    newVersion
                            );

                            if (callback != null) {

                                callback.onFinished();

                            }

                        }

                    }

                    @Override
                    public void onError(String error) {

                        Log.e(
                                "SYNC",
                                error
                        );

                        if (callback != null) {

                            callback.onFinished();

                        }

                    }

                });

    }
    public void deleteAll() {
        executorService.execute(dictionaryDao::deleteAll);
    }

    public void close() {
        executorService.shutdown();
    }
}