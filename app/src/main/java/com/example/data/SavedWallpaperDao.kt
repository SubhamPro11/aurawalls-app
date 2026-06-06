package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedWallpaperDao {
    @Query("SELECT * FROM saved_wallpapers ORDER BY savedAt DESC")
    fun getAllSavedWallpapers(): Flow<List<SavedWallpaper>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveWallpaper(wallpaper: SavedWallpaper)

    @Query("DELETE FROM saved_wallpapers WHERE id = :id")
    suspend fun unsaveWallpaperById(id: String)

    @Query("SELECT EXISTS(SELECT 1 FROM saved_wallpapers WHERE id = :id)")
    fun isWallpaperSaved(id: String): Flow<Boolean>
}
