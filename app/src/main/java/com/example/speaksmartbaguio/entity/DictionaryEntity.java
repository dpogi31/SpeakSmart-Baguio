package com.example.speaksmartbaguio.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "dictionary")
public class DictionaryEntity {

    @PrimaryKey
    @NonNull
    private String id;

    private String ilokanoWord;
    private String englishTranslation;
    private String tagalogTranslation;
    private String partOfSpeech;
    private String category;
    private String ttsUrl;
    private String updatedAt;

    public DictionaryEntity() {
    }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getIlokanoWord() {
        return ilokanoWord;
    }

    public void setIlokanoWord(String ilokanoWord) {
        this.ilokanoWord = ilokanoWord;
    }

    public String getEnglishTranslation() {
        return englishTranslation;
    }

    public void setEnglishTranslation(String englishTranslation) {
        this.englishTranslation = englishTranslation;
    }

    public String getTagalogTranslation() {
        return tagalogTranslation;
    }

    public void setTagalogTranslation(String tagalogTranslation) {
        this.tagalogTranslation = tagalogTranslation;
    }

    public String getPartOfSpeech() {
        return partOfSpeech;
    }

    public void setPartOfSpeech(String partOfSpeech) {
        this.partOfSpeech = partOfSpeech;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getTtsUrl() {
        return ttsUrl;
    }

    public void setTtsUrl(String ttsUrl) {
        this.ttsUrl = ttsUrl;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}