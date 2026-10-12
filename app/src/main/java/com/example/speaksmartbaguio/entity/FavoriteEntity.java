
package com.example.speaksmartbaguio.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "favorites")
public class FavoriteEntity {

    @PrimaryKey(autoGenerate = true)
    private int id;

    @NonNull
    private String type = "";

    @NonNull
    private String ilokanoText = "";

    @NonNull
    private String englishText = "";

    @NonNull
    private String tagalogText = "";

    private long savedAt;

    public FavoriteEntity(
            @NonNull String type,
            @NonNull String ilokanoText,
            @NonNull String englishText,
            @NonNull String tagalogText,
            long savedAt
    ) {
        this.type = type;
        this.ilokanoText = ilokanoText;
        this.englishText = englishText;
        this.tagalogText = tagalogText;
        this.savedAt = savedAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @NonNull
    public String getType() {
        return type;
    }

    public void setType(@NonNull String type) {
        this.type = type;
    }

    @NonNull
    public String getIlokanoText() {
        return ilokanoText;
    }

    public void setIlokanoText(@NonNull String ilokanoText) {
        this.ilokanoText = ilokanoText;
    }

    @NonNull
    public String getEnglishText() {
        return englishText;
    }

    public void setEnglishText(@NonNull String englishText) {
        this.englishText = englishText;
    }

    @NonNull
    public String getTagalogText() {
        return tagalogText;
    }

    public void setTagalogText(@NonNull String tagalogText) {
        this.tagalogText = tagalogText;
    }

    public long getSavedAt() {
        return savedAt;
    }

    public void setSavedAt(long savedAt) {
        this.savedAt = savedAt;
    }
}
