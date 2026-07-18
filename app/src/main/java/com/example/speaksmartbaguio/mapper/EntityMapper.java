package com.example.speaksmartbaguio.mapper;

import com.example.speaksmartbaguio.Word;
import com.example.speaksmartbaguio.entity.DictionaryEntity;
import java.util.ArrayList;
import java.util.List;
import com.example.speaksmartbaguio.Phrase;
import com.example.speaksmartbaguio.entity.PhraseEntity;
public class EntityMapper {

    public static DictionaryEntity toEntity(Word word) {

        DictionaryEntity entity = new DictionaryEntity();

        entity.setId(word.getId());
        entity.setIlokanoWord(word.getIlokanoWord());
        entity.setEnglishTranslation(word.getEnglishTranslation());
        entity.setTagalogTranslation(word.getTagalogTranslation());
        entity.setPartOfSpeech(word.getPartOfSpeech());
        entity.setCategory(word.getCategory());
        entity.setTtsUrl(word.getTtsUrl());
        entity.setUpdatedAt(word.getUpdatedAt());

        return entity;
    }
    public static List<DictionaryEntity> toEntityList(List<Word> words) {

        List<DictionaryEntity> entities = new ArrayList<>();

        for (Word word : words) {
            entities.add(toEntity(word));
        }

        return entities;
    }

    public static List<Word> toWordList(List<DictionaryEntity> entities) {

        List<Word> words = new ArrayList<>();

        for (DictionaryEntity entity : entities) {
            words.add(toWord(entity));
        }

        return words;
    }
    public static Word toWord(DictionaryEntity entity) {

        Word word = new Word();

        word.setId(entity.getId());
        word.setIlokanoWord(entity.getIlokanoWord());
        word.setEnglishTranslation(entity.getEnglishTranslation());
        word.setTagalogTranslation(entity.getTagalogTranslation());
        word.setPartOfSpeech(entity.getPartOfSpeech());
        word.setCategory(entity.getCategory());
        word.setTtsUrl(entity.getTtsUrl());
        word.setUpdatedAt(entity.getUpdatedAt());

        return word;
    }
    public static PhraseEntity toEntity(Phrase phrase) {

        PhraseEntity entity = new PhraseEntity();

        entity.setId(phrase.getId());
        entity.setIlokanoWord(phrase.getIlokanoWord());
        entity.setEnglishTranslation(phrase.getEnglishTranslation());
        entity.setTagalogTranslation(phrase.getTagalogTranslation());
        entity.setPartOfSpeech(phrase.getPartOfSpeech());
        entity.setCategory(phrase.getCategory());
        entity.setTtsUrl(phrase.getTtsUrl());
        entity.setUpdatedAt(phrase.getUpdatedAt());

        return entity;
    }

    public static List<PhraseEntity> toPhraseEntityList(List<Phrase> phrases) {

        List<PhraseEntity> entities = new ArrayList<>();

        for (Phrase phrase : phrases) {
            entities.add(toEntity(phrase));
        }

        return entities;
    }

    public static Phrase toPhrase(PhraseEntity entity) {

        Phrase phrase = new Phrase();

        phrase.setId(entity.getId());
        phrase.setIlokanoWord(entity.getIlokanoWord());
        phrase.setEnglishTranslation(entity.getEnglishTranslation());
        phrase.setTagalogTranslation(entity.getTagalogTranslation());
        phrase.setPartOfSpeech(entity.getPartOfSpeech());
        phrase.setCategory(entity.getCategory());
        phrase.setTtsUrl(entity.getTtsUrl());
        phrase.setUpdatedAt(entity.getUpdatedAt());

        return phrase;
    }

    public static List<Phrase> toPhraseList(List<PhraseEntity> entities) {

        List<Phrase> phrases = new ArrayList<>();

        for (PhraseEntity entity : entities) {
            phrases.add(toPhrase(entity));
        }

        return phrases;
    }
}