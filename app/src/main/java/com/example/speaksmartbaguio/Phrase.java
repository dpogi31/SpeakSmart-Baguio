package com.example.speaksmartbaguio;

import androidx.annotation.NonNull;

public class Phrase {

    private String id;
    private String category;
    private String englishTranslation;
    private String ilokanoWord;
    private String tagalogTranslation;
    private String partOfSpeech;
    private String ilokanoSentence;
    private String englishSentence;
    private String ttsUrl;
    private String updatedAt;

    public Phrase() {
    }

    public Phrase(String category,
                  String englishTranslation,
                  String ilokanoWord,
                  String tagalogTranslation,
                  String partOfSpeech) {

        this.category = category;
        this.englishTranslation = englishTranslation;
        this.ilokanoWord = ilokanoWord;
        this.tagalogTranslation = tagalogTranslation;
        this.partOfSpeech = partOfSpeech;
    }

    public String getId() {
        return id;
    }

    public String getCategory() {
        return category != null ? category : "";
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
    public String getIlokanoSentence() {
        return ilokanoSentence != null ? ilokanoSentence : "";
    }
    public String getEnglishSentence() {
        return englishSentence != null ? englishSentence : "";
    }
    public String getTtsUrl() {
        return ttsUrl;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setCategory(String category) {
        this.category = category;
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
    public void setIlokanoSentence(String ilokanoSentence) {
        this.ilokanoSentence = ilokanoSentence;
    }
    public void setEnglishSentence(String englishSentence) {
        this.englishSentence = englishSentence;
    }

    public void setTtsUrl(String ttsUrl) {
        this.ttsUrl = ttsUrl;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    @NonNull
    @Override
    public String toString() {
        return "Phrase{" +
                "id='" + id + '\'' +
                ", category='" + getCategory() + '\'' +
                ", englishTranslation='" + getEnglishTranslation() + '\'' +
                ", ilokanoWord='" + getIlokanoWord() + '\'' +
                ", tagalogTranslation='" + getTagalogTranslation() + '\'' +
                ", partOfSpeech='" + getPartOfSpeech() + '\'' +
                ", ilokanoSentence='" + getIlokanoSentence() + '\'' +
                ", englishSentence='" + getEnglishSentence() + '\'' +
                ", ttsUrl='" + ttsUrl + '\'' +
                ", updatedAt='" + updatedAt + '\'' +
                '}';
    }
}