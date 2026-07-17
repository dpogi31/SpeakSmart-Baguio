package com.example.speaksmartbaguio;

import androidx.annotation.NonNull;

public class Word {

    private String id;
    private String englishTranslation;
    private String ilokanoWord;
    private String tagalogTranslation;
    private String partOfSpeech;
    private String ttsUrl;
    private String category;
    private String updatedAt;

    public Word() {
    }

    public Word(String englishTranslation,
                String ilokanoWord,
                String tagalogTranslation,
                String partOfSpeech) {

        this.englishTranslation = englishTranslation;
        this.ilokanoWord = ilokanoWord;
        this.tagalogTranslation = tagalogTranslation;
        this.partOfSpeech = partOfSpeech;
    }

    // Optional constructor with ID
    public Word(String id,
                String englishTranslation,
                String ilokanoWord,
                String tagalogTranslation,
                String partOfSpeech) {

        this.id = id;
        this.englishTranslation = englishTranslation;
        this.ilokanoWord = ilokanoWord;
        this.tagalogTranslation = tagalogTranslation;
        this.partOfSpeech = partOfSpeech;
    }

    public String getId() {
        return id;
    }

    public String getEnglishTranslation() {
        return englishTranslation != null ? englishTranslation : "";
    }

    public String getIlokanoWord() {
        return ilokanoWord != null ? ilokanoWord : "";
    }

    public String getTagalogTranslation() {
        return tagalogTranslation != null ? tagalogTranslation : "";
    }

    public String getPartOfSpeech() {
        return partOfSpeech != null ? partOfSpeech : "";
    }

    public String getTtsUrl() {
        return ttsUrl;
    }

    public String getCategory() {
        return category;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setEnglishTranslation(String englishTranslation) {
        this.englishTranslation = englishTranslation;
    }

    public void setIlokanoWord(String ilokanoWord) {
        this.ilokanoWord = ilokanoWord;
    }

    public void setTagalogTranslation(String tagalogTranslation) {
        this.tagalogTranslation = tagalogTranslation;
    }

    public void setPartOfSpeech(String partOfSpeech) {
        this.partOfSpeech = partOfSpeech;
    }

    public void setTtsUrl(String ttsUrl) {
        this.ttsUrl = ttsUrl;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    @NonNull
    @Override
    public String toString() {
        return "Word{" +
                "id='" + id + '\'' +
                ", englishTranslation='" + getEnglishTranslation() + '\'' +
                ", ilokanoWord='" + getIlokanoWord() + '\'' +
                ", tagalogTranslation='" + getTagalogTranslation() + '\'' +
                ", partOfSpeech='" + getPartOfSpeech() + '\'' +
                ", ttsUrl='" + ttsUrl + '\'' +
                ", category='" + category + '\'' +
                ", updatedAt='" + updatedAt + '\'' +
                '}';
    }
}