package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_wallpapers")
data class SavedWallpaper(
    @PrimaryKey val id: String,
    val title: String,
    val category: String,
    val resolution: String,
    val url: String,
    val thumbnailUrl: String,
    val views: Int,
    val downloads: Int,
    val creatorName: String,
    val savedAt: Long = System.currentTimeMillis()
)
