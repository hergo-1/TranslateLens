package com.translatelens.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "translation_history")
data class TranslationHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val originalImagePath: String,
    val translatedImagePath: String,
    val sourceLanguage: String,
    val targetLanguage: String,
    val originalText: String,
    val translatedText: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)
