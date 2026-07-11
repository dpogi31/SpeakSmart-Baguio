package com.example.speaksmartbaguio;

public class Phrase {
    private String category;
    private String englishTranslation;
    private String ilokanoWord;
    private String tagalogTranslation;
    private String partOfSpeech;
    private String ttsUrl;
    private String updatedAt;

    public Phrase() {}

    public Phrase(String category, String englishTranslation, String ilokanoWord, String tagalogTranslation, String partOfSpeech) {
        this.category = category;
        this.englishTranslation = englishTranslation;
        this.ilokanoWord = ilokanoWord;
        this.tagalogTranslation = tagalogTranslation;
        this.partOfSpeech = partOfSpeech;
    }

    public String getCategory() { return category; }
    public String getEnglishTranslation() { return englishTranslation; }
    public String getIlokanoWord() { return ilokanoWord; }
    public String getTagalogTranslation() { return tagalogTranslation; }
    public String getPartOfSpeech() { return partOfSpeech; }
    public String getTtsUrl() { return ttsUrl; }
    public String getUpdatedAt() { return updatedAt; }

    public void setCategory(String category) { this.category = category; }
    public void setEnglishTranslation(String englishTranslation) { this.englishTranslation = englishTranslation; }
    public void setIlokanoWord(String ilokanoWord) { this.ilokanoWord = ilokanoWord; }
    public void setTagalogTranslation(String tagalogTranslation) { this.tagalogTranslation = tagalogTranslation; }
    public void setPartOfSpeech(String partOfSpeech) { this.partOfSpeech = partOfSpeech; }
    public void setTtsUrl(String ttsUrl) { this.ttsUrl = ttsUrl; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
