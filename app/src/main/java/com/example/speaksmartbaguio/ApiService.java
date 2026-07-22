package com.example.speaksmartbaguio;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ApiService {

    private static ApiService instance;
    private OkHttpClient client;
    private String baseUrl;
    private String apiKey;
    private Handler mainHandler;

    private ApiService() {
        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        baseUrl = BuildConfig.API_BASE_URL;
        if (!baseUrl.endsWith("/")) baseUrl += "/";
        apiKey = BuildConfig.API_SECRET_KEY;
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public static synchronized ApiService getInstance() {
        if (instance == null) instance = new ApiService();
        return instance;
    }

    public interface ApiCallback<T> {
        void onSuccess(List<T> items, boolean hasMore);
        void onError(String error);
    }

    public interface SingleResultCallback {
        void onSuccess(JSONObject item);
        void onError(String error);
    }

    /* ───────── Dictionary ───────── */

    public void getDictionary(int page, int limit, String query, ApiCallback<Word> callback) {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(baseUrl + "api/v1/dictionary").newBuilder();
        urlBuilder.addQueryParameter("page", String.valueOf(page));
        urlBuilder.addQueryParameter("limit", String.valueOf(limit));
        addSearchParams(urlBuilder, query);

        executeRequest(urlBuilder.build(), page, limit, callback, this::parseWordList);
    }
    public void searchDictionaryWord(String field, String value, ApiCallback<Word> callback) {

        HttpUrl.Builder urlBuilder =
                HttpUrl.parse(baseUrl + "api/v1/dictionary").newBuilder();

        urlBuilder.addQueryParameter(field, "*" + value.toLowerCase() + "*");
        urlBuilder.addQueryParameter("page", "1");
        urlBuilder.addQueryParameter("limit", "1");

        Log.d("DICT_URL", urlBuilder.build().toString());

        executeRequest(
                urlBuilder.build(),
                1,
                1,
                callback,
                this::parseWordList
        );
    }
    /* ───────── Phrasebook ───────── */

    public void getPhrasebook(int page, int limit, String query, ApiCallback<Phrase> callback) {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(baseUrl + "api/v1/phrasebook").newBuilder();
        urlBuilder.addQueryParameter("page", String.valueOf(page));
        urlBuilder.addQueryParameter("limit", String.valueOf(limit));
        addSearchParams(urlBuilder, query);

        executeRequest(urlBuilder.build(), page, limit, callback, this::parsePhraseList);
    }

    /* ───────── Translations ───────── */

    public void getTranslations(String field, String value, SingleResultCallback callback) {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(baseUrl + "api/v1/translations").newBuilder();
        urlBuilder.addQueryParameter(field, value);
        urlBuilder.addQueryParameter("limit", "1");
        Log.d("TRANSLATOR_API", urlBuilder.build().toString());
        executeSingleRequest(urlBuilder.build(), callback);
    }

    /* ───────── Request execution ───────── */

    private <T> void executeRequest(HttpUrl url, int page, int limit, ApiCallback<T> callback, JsonParser<T> parser) {
        Request request = buildRequest(url);

        Log.d("FINAL_REQUEST", request.url().toString());

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                postError("Network error: " + e.getMessage(), callback);
            }

            @Override
            public void onResponse(Call call, Response response) {
                try {
                    if (!response.isSuccessful()) {
                        postError("Internal server error", callback);
                        return;
                    }

                    String json = response.body() != null ? response.body().string() : "{}";
                    Log.d("FINAL_RESPONSE", json);
                    Log.d("DICT_RESPONSE", json);
                    JSONObject obj = new JSONObject(json);
                    JSONArray data = obj.optJSONArray("data");

                    List<T> items = parser.parse(data);
                    boolean hasMore = obj.optBoolean("hasMore", false);

                    mainHandler.post(() -> callback.onSuccess(items, hasMore));
                } catch (Exception e) {
                    postError("Parse error: " + e.getMessage(), callback);
                } finally {
                    if (response.body() != null) response.body().close();
                }
            }
        });
    }

    private void executeSingleRequest(HttpUrl url, SingleResultCallback callback) {
        Request request = buildRequest(url);

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                postError("Network error: " + e.getMessage(), callback);
            }

            @Override
            public void onResponse(Call call, Response response) {
                try {
                    if (!response.isSuccessful()) {
                        postError("Internal server error", callback);
                        return;
                    }

                    String json = response.body() != null ? response.body().string() : "{}";
                    Log.d("TRANSLATOR_API", json);
                    JSONObject obj = new JSONObject(json);
                    JSONArray data = obj.optJSONArray("data");
                    Log.d("TRANSLATOR_API", "Items = " + (data == null ? "null" : data.length()));
                    if (data != null && data.length() > 0) {
                        JSONObject item = data.getJSONObject(0);
                        mainHandler.post(() -> callback.onSuccess(item));
                    } else {
                        mainHandler.post(() -> callback.onSuccess(null));
                    }
                } catch (Exception e) {
                    postError("Parse error: " + e.getMessage(), callback);
                } finally {
                    if (response.body() != null) response.body().close();
                }

            }
        });
    }

    /* ───────── Helpers ───────── */

    private Request buildRequest(HttpUrl url) {
        return new Request.Builder()
                .url(url)
                .header("x-api-key", apiKey)
                .build();
    }

    private String getString(JSONObject obj, String primary, String fallback) {
        if (obj.has(primary)) {
            String val = obj.optString(primary, "");
            if (!val.isEmpty()) return val;
        }
        return obj.optString(fallback, "");
    }

    private String getString(JSONObject obj, String... keys) {

        for (String key : keys) {

            if (obj.has(key) && !obj.isNull(key)) {

                String value = obj.optString(key, "");

                if (!value.isEmpty()) {
                    return value;
                }
            }
        }

        return "";
    }

    private void addSearchParams(HttpUrl.Builder builder, String query) {
        if (query == null || query.trim().isEmpty()) return;
        String q = query.trim().toLowerCase();
        builder.addQueryParameter("ilokanoWord", "*" + q + "*");
    }

    private void postError(String message, Object callback) {
        mainHandler.post(() -> {
            if (callback instanceof ApiCallback) {
                ((ApiCallback<?>) callback).onError(message);
            } else if (callback instanceof SingleResultCallback) {
                ((SingleResultCallback) callback).onError(message);
            }
        });
    }

    /* ───────── JSON parsers ───────── */

    private interface JsonParser<T> {
        List<T> parse(JSONArray array) throws Exception;
    }

    private List<Word> parseWordList(JSONArray array) throws Exception {
        List<Word> list = new ArrayList<>();
        if (array == null) return list;
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.getJSONObject(i);
            Word w = new Word();

            w.setId(getString(item, "id"));
            w.setEnglishTranslation(getString(item,
                    "english",
                    "englishTranslation",
                    "english_translation"));

            w.setIlokanoWord(getString(item,
                    "ilokano",
                    "ilokanoWord",
                    "ilokano_word"));

            w.setTagalogTranslation(getString(item,
                    "tagalog",
                    "tagalogTranslation",
                    "tagalog_translation"));
            w.setPartOfSpeech(getString(item, "partOfSpeech", "part_of_speech"));
            w.setTtsUrl(getString(item, "tts_url"));
            w.setCategory(getString(item, "category"));
            w.setUpdatedAt(getString(item, "updated_at"));
            list.add(w);
        }
        return list;
    }

    private List<Phrase> parsePhraseList(JSONArray array) throws Exception {
        List<Phrase> list = new ArrayList<>();
        if (array == null) return list;
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.getJSONObject(i);
            Phrase p = new Phrase();

            p.setId(getString(item, "id"));
            p.setEnglishTranslation(getString(item, "englishTranslation", "english_translation"));
            p.setIlokanoWord(getString(item, "ilokanoWord", "ilokano_word"));
            p.setTagalogTranslation(getString(item, "tagalogTranslation", "tagalog_translation"));
            p.setPartOfSpeech(getString(item, "partOfSpeech", "part_of_speech"));
            p.setTtsUrl(getString(item, "tts_url"));
            p.setCategory(getString(item, "category"));
            p.setUpdatedAt(getString(item, "updated_at"));
            list.add(p);
        }
        return list;
    }
}
