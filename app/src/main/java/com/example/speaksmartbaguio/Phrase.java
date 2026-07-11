package com.example.speaksmartbaguio;

public class Phrase {
    private String category;
    private String englishTranslation;
    private String ilokanoWord;
    private String tagalogTranslation;
    private String partOfSpeech;
    public Phrase() {}
    public Phrase(String category, String englishTranslation, String ilokanoWord, String tagalogTranslation, String partOfSpeech) {
        this.category = category;
        this.englishTranslation = englishTranslation;
        this.ilokanoWord = ilokanoWord;
        this.tagalogTranslation = tagalogTranslation;
        this.partOfSpeech = partOfSpeech;
    }

    // Getters
    public String getCategory() { return category; }
    public String getEnglishTranslation() { return englishTranslation; }
    public String getIlokanoWord() { return ilokanoWord; }
    public String getTagalogTranslation() { return tagalogTranslation; } // Added
    public String getPartOfSpeech() { return partOfSpeech; }

    // Setters
    public void setCategory(String category) { this.category = category; }
    public void setEnglishTranslation(String englishTranslation) { this.englishTranslation = englishTranslation; }
    public void setIlokanoWord(String ilokanoWord) { this.ilokanoWord = ilokanoWord; }
    public void setTagalogTranslation(String tagalogTranslation) { this.tagalogTranslation = tagalogTranslation; } // Added
    public void setPartOfSpeech(String partOfSpeech) { this.partOfSpeech = partOfSpeech; }
}
