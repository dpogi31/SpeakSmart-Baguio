package com.example.speaksmartbaguio.repository;

import android.content.Context;

import com.example.speaksmartbaguio.dao.DictionaryDao;
import com.example.speaksmartbaguio.database.DatabaseClient;
import com.example.speaksmartbaguio.entity.DictionaryEntity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DictionaryRepository {

    private final DictionaryDao dictionaryDao;
    private final ExecutorService executorService;

    public DictionaryRepository(Context context) {
        dictionaryDao = DatabaseClient.getInstance(context).dictionaryDao();
        executorService = Executors.newSingleThreadExecutor();
    }

    public interface LoadCallback {
        void onLoaded(List<DictionaryEntity> result);
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

    public void deleteAll() {
        executorService.execute(dictionaryDao::deleteAll);
    }

    public void close() {
        executorService.shutdown();
    }
}