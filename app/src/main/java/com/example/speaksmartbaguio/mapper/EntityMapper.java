package com.example.speaksmartbaguio.mapper;

import com.example.speaksmartbaguio.Word;
import com.example.speaksmartbaguio.entity.DictionaryEntity;
import java.util.ArrayList;
import java.util.List;
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

}