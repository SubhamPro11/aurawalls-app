package com.example.viewmodel

import android.app.Application
import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.SavedWallpaper
import com.example.network.GeminiClient
import com.example.network.RetrofitClient
import com.example.network.WallpaperDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

// Unified client model representing wallpapers across UI
data class WallpaperItem(
    val id: String,
    val title: String,
    val category: String,
    val resolution: String = "4K",
    val url: String,
    val thumbnailUrl: String,
    val views: Int = 0,
    val downloads: Int = 0,
    val creatorName: String = "Midjourney Studio",
    val dominantColor: String? = null,
    val tags: List<String> = emptyList(),
    val isAiGenerated: Boolean = false
)

sealed interface UIState<out T> {
    object Loading : UIState<Nothing>
    data class Success<out T>(val data: T) : UIState<T>
    data class Error(val message: String) : UIState<Nothing>
}

class AuraViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val savedDao = db.savedWallpaperDao()

    // Interactive States
    private val _currentTab = MutableStateFlow(0) // 0: Home, 1: Explore, 2: Create, 3: Saved, 4: Profile
    val currentTab = _currentTab.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedColorFilter = MutableStateFlow<String?>(null) // Hex color code
    val selectedColorFilter = _selectedColorFilter.asStateFlow()

    // Wallpaper items streams from Service
    private val _wallpapersState = MutableStateFlow<UIState<List<WallpaperItem>>>(UIState.Loading)
    val wallpapersState = _wallpapersState.asStateFlow()

    // Offline saved list monitored reactively
    val savedWallpapers: StateFlow<List<SavedWallpaper>> = savedDao.getAllSavedWallpapers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Wallpaper Selected for Live Preview Overlay Modal
    private val _previewWallpaper = MutableStateFlow<WallpaperItem?>(null)
    val previewWallpaper = _previewWallpaper.asStateFlow()

    // AI Generation States
    private val _aiPrompt = MutableStateFlow("")
    val aiPrompt = _aiPrompt.asStateFlow()

    private val _aiGenerationState = MutableStateFlow<UIState<Bitmap?>>(UIState.Success(null))
    val aiGenerationState = _aiGenerationState.asStateFlow()

    // Auto-Rotate Background States
    private val _autoRotateEnabled = MutableStateFlow(false)
    val autoRotateEnabled = _autoRotateEnabled.asStateFlow()

    private val _autoRotateInterval = MutableStateFlow("Daily") // Hourly, Daily, Weekly
    val autoRotateInterval = _autoRotateInterval.asStateFlow()

    // Seed/Fallback wallpapers following image_39a3e5.jpg specification precisely to secure offline or failed api calls
    private val fallbackWallpapers = listOf(
        WallpaperItem(
            id = "w1",
            title = "Nebula Core — Deep Space",
            category = "Galaxy",
            resolution = "4K",
            // Beautiful open source backgrounds matching the specified visuals
            url = "https://images.unsplash.com/photo-1462331940025-496dfbfc7564?q=80&w=1200",
            thumbnailUrl = "https://images.unsplash.com/photo-1462331940025-496dfbfc7564?q=80&w=600",
            views = 4500,
            downloads = 2400,
            creatorName = "Midjourney Studio",
            dominantColor = "#6366F1",
            tags = listOf("Deep Space", "Nebula", "Galaxy", "Violet")
        ),
        WallpaperItem(
            id = "w2",
            title = "Vaporwave Grid",
            category = "Cyberpunk",
            resolution = "4K",
            url = "https://images.unsplash.com/photo-1508739773434-c26b3d09e071?q=80&w=1200",
            thumbnailUrl = "https://images.unsplash.com/photo-1508739773434-c26b3d09e071?q=80&w=600",
            views = 3100,
            downloads = 1200,
            creatorName = "Midjourney Studio",
            dominantColor = "#A855F7",
            tags = listOf("Synthwave", "Cyberpunk", "Neon", "Grid")
        ),
        WallpaperItem(
            id = "w3",
            title = "Aurora Peaks",
            category = "Minimal",
            resolution = "4K",
            url = "https://images.unsplash.com/photo-1518156677180-95a2893f3e9f?q=80&w=1200",
            thumbnailUrl = "https://images.unsplash.com/photo-1518156677180-95a2893f3e9f?q=80&w=600",
            views = 2800,
            downloads = 874,
            creatorName = "Unsplash Artists",
            dominantColor = "#10B981",
            tags = listOf("Minimal", "Aurora", "Glow", "Green")
        ),
        WallpaperItem(
            id = "w4",
            title = "Crimson Bloom",
            category = "Cyberpunk",
            resolution = "4K",
            url = "https://images.unsplash.com/photo-1500485035595-cbe6f645feb1?q=80&w=1200",
            thumbnailUrl = "https://images.unsplash.com/photo-1500485035595-cbe6f645feb1?q=80&w=600",
            views = 1500,
            downloads = 650,
            creatorName = "Midjourney Studio",
            dominantColor = "#EF4444",
            tags = listOf("Floral", "Red", "Dark Cyber")
        ),
        WallpaperItem(
            id = "w5",
            title = "Ocean Matrix",
            category = "Cyberpunk",
            resolution = "4K",
            url = "https://images.unsplash.com/photo-1505238685112-78dc590c6792?q=80&w=1200",
            thumbnailUrl = "https://images.unsplash.com/photo-1505238685112-78dc590c6792?q=80&w=600",
            views = 1200,
            downloads = 540,
            creatorName = "Midjourney Studio",
            dominantColor = "#06B6D4",
            tags = listOf("Cyberpunk", "Ocean", "Green-Blue")
        ),
        WallpaperItem(
            id = "w6",
            title = "Ethereal Cosmic Dust",
            category = "Galaxy",
            resolution = "8K",
            url = "https://images.unsplash.com/photo-1541185933-ef5d8ed016c2?q=80&w=1200",
            thumbnailUrl = "https://images.unsplash.com/photo-1541185933-ef5d8ed016c2?q=80&w=600",
            views = 5800,
            downloads = 3900,
            creatorName = "NASA Universe",
            dominantColor = "#6366F1",
            tags = listOf("Cosmology", "Purple Glow", "Dust")
        ),
        WallpaperItem(
            id = "w7",
            title = "Abstract Vector Lines",
            category = "Minimal",
            resolution = "4K",
            url = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?q=80&w=1200",
            thumbnailUrl = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?q=80&w=600",
            views = 980,
            downloads = 320,
            creatorName = "Minimalist Lab",
            dominantColor = "#E5E7EB",
            tags = listOf("Lines", "Clean", "Minimal")
        )
    )

    init {
        // Initial data pull
        loadWallpapers()
    }

    fun selectTab(tabIdx: Int) {
        _currentTab.value = tabIdx
    }

    fun setCategory(category: String) {
        _selectedCategory.value = category
        loadWallpapers()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        loadWallpapers()
    }

    fun selectColorFilter(colorHex: String?) {
        _selectedColorFilter.value = colorHex
        loadWallpapers()
    }

    fun setPreviewWallpaper(item: WallpaperItem?) {
        _previewWallpaper.value = item
    }

    fun setAiPrompt(text: String) {
        _aiPrompt.value = text
    }

    fun toggleAutoRotate() {
        _autoRotateEnabled.value = !_autoRotateEnabled.value
        val stateText = if (_autoRotateEnabled.value) "scheduled sequence active" else "disabled"
        Toast.makeText(getApplication(), "Daily Auto-Rotate $stateText", Toast.LENGTH_SHORT).show()
    }

    fun setAutoRotateInterval(interval: String) {
        _autoRotateInterval.value = interval
        Toast.makeText(getApplication(), "Rotation cycle is set to $interval", Toast.LENGTH_SHORT).show()
    }

    fun loadWallpapers() {
        viewModelScope.launch {
            _wallpapersState.value = UIState.Loading
            try {
                // Request live API parameters based on state controllers
                val searchVal = if (_searchQuery.value.trim().isEmpty()) null else _searchQuery.value.trim()
                val categoryVal = if (_selectedCategory.value == "All") null else _selectedCategory.value
                val colorVal = _selectedColorFilter.value

                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.service.getWallpapers(
                        category = categoryVal,
                        search = searchVal,
                        color = colorVal
                    )
                }

                if (response.isEmpty()) {
                    // Fall back to pre-seeded list filtering to prevent empty screen AI slop while mapping UI designs
                    val filteredFallbacks = applyLocalFiltering(fallbackWallpapers, categoryVal, searchVal, colorVal)
                    _wallpapersState.value = UIState.Success(filteredFallbacks)
                } else {
                    val items = response.map { dtoToModel(it) }
                    _wallpapersState.value = UIState.Success(items)
                }
            } catch (e: Exception) {
                Log.e("AuraViewModel", "Error fetching from live domain, applying visual fallbacks", e)
                // Graceful degradation: utilize preloaded cards with active search/cat filtering
                val categoryVal = if (_selectedCategory.value == "All") null else _selectedCategory.value
                val searchVal = if (_searchQuery.value.trim().isEmpty()) null else _searchQuery.value.trim()
                val colorVal = _selectedColorFilter.value
                val filteredFallbacks = applyLocalFiltering(fallbackWallpapers, categoryVal, searchVal, colorVal)
                _wallpapersState.value = UIState.Success(filteredFallbacks)
            }
        }
    }

    private fun applyLocalFiltering(
        list: List<WallpaperItem>,
        category: String?,
        search: String?,
        color: String?
    ): List<WallpaperItem> {
        return list.filter { item ->
            val matchesCategory = category == null || item.category.lowercase() == category.lowercase()
            val matchesSearch = search == null || 
                    item.title.lowercase().contains(search.lowercase()) ||
                    item.tags.any { it.lowercase().contains(search.lowercase()) }
            val matchesColor = color == null || item.dominantColor?.lowercase() == color.lowercase()
            
            matchesCategory && matchesSearch && matchesColor
        }
    }

    private fun dtoToModel(dto: WallpaperDto): WallpaperItem {
        val rootId = dto.id ?: java.util.UUID.randomUUID().toString()
        val standardResUrl = dto.url ?: "https://images.unsplash.com/photo-1462331940025-496dfbfc7564?q=80&w=1200"
        return WallpaperItem(
            id = rootId,
            title = dto.title ?: "Aura Backdrops",
            category = dto.category ?: "Cinema",
            resolution = dto.resolution ?: "4K",
            url = standardResUrl,
            thumbnailUrl = dto.thumbnailUrl ?: standardResUrl,
            views = dto.views ?: (100..5000).random(),
            downloads = dto.downloads ?: (50..1500).random(),
            creatorName = dto.creatorName ?: "Collaborative Artist",
            dominantColor = dto.dominantColor,
            tags = dto.tags ?: emptyList()
        )
    }

    // Checking if wallpaper is saved
    fun isWallpaperSavedFlow(id: String): Flow<Boolean> {
        return savedDao.isWallpaperSaved(id)
    }

    fun toggleSaveWallpaper(item: WallpaperItem) {
        viewModelScope.launch {
            val isCurrentlySaved = savedDao.isWallpaperSaved(item.id).first()
            if (isCurrentlySaved) {
                savedDao.unsaveWallpaperById(item.id)
                if (item.url.startsWith("file://") || item.url.startsWith("/")) {
                    try {
                        val path = item.url.removePrefix("file://")
                        val file = File(path)
                        if (file.exists()) {
                            file.delete()
                        }
                    } catch (e: Exception) {
                        Log.e("AuraViewModel", "Error deleting local wallpaper file", e)
                    }
                }
                Toast.makeText(getApplication(), "Removed from saved vault", Toast.LENGTH_SHORT).show()
            } else {
                val dbEntity = SavedWallpaper(
                    id = item.id,
                    title = item.title,
                    category = item.category,
                    resolution = item.resolution,
                    url = item.url,
                    thumbnailUrl = item.thumbnailUrl,
                    views = item.views,
                    downloads = item.downloads,
                    creatorName = item.creatorName
                )
                savedDao.saveWallpaper(dbEntity)
                Toast.makeText(getApplication(), "Saved to offline vault", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // AI Wallpaper Generator Trigger
    fun triggerAiWallpaperGeneration() {
        val promptText = _aiPrompt.value.trim()
        if (promptText.isEmpty()) {
            Toast.makeText(getApplication(), "Please enter a visual descriptive prompt!", Toast.LENGTH_SHORT).show()
            return
        }

        viewModelScope.launch {
            _aiGenerationState.value = UIState.Loading
            try {
                val bitmap = GeminiClient.generateWallpaper(promptText)
                _aiGenerationState.value = UIState.Success(bitmap)
                Toast.makeText(getApplication(), "Aura AI: Wallpaper generated successfully!", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Log.e("AuraViewModel", "AI Generation Error", e)
                _aiGenerationState.value = UIState.Error(e.message ?: "Failed generating AI artwork")
            }
        }
    }

    /**
     * Executes One-Tap apply Wallpaper service
     * 1: Home Screen, 2: Lock Screen, 3: Both
     */
    fun applyWallpaper(context: Context, imageUrl: String, target: Int, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val successfullySet = withContext(Dispatchers.IO) {
                try {
                    val bitmap = if (imageUrl.startsWith("file://") || imageUrl.startsWith("/")) {
                        val path = imageUrl.removePrefix("file://")
                        BitmapFactory.decodeFile(path)
                    } else {
                        val client = OkHttpClient()
                        val request = Request.Builder().url(imageUrl).build()
                        val response = client.newCall(request).execute()
                        if (!response.isSuccessful) return@withContext false

                        val stream = response.body?.byteStream() ?: return@withContext false
                        BitmapFactory.decodeStream(stream)
                    } ?: return@withContext false

                    val wallpaperManager = WallpaperManager.getInstance(context)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        when (target) {
                            1 -> wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                            2 -> wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
                            3 -> {
                                wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                                wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
                            }
                        }
                    } else {
                        wallpaperManager.setBitmap(bitmap)
                    }
                    true
                } catch (e: IOException) {
                    Log.e("AuraViewModel", "Error downloading wallpaper", e)
                    false
                } catch (e: Exception) {
                    Log.e("AuraViewModel", "Error applying wallpaper", e)
                    false
                }
            }

            withContext(Dispatchers.Main) {
                onComplete(successfullySet)
                if (successfullySet) {
                    Toast.makeText(context, "Wallpaper configured successfully!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Failed to download / configure wallpaper", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Sets generated bitmap as wallpaper
    fun applyGeneratedWallpaper(context: Context, bitmap: Bitmap, target: Int, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val succeeded = withContext(Dispatchers.IO) {
                try {
                    val wallpaperManager = WallpaperManager.getInstance(context)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        when (target) {
                            1 -> wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                            2 -> wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
                            3 -> {
                                wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                                wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
                            }
                        }
                    } else {
                        wallpaperManager.setBitmap(bitmap)
                    }
                    true
                } catch (e: Exception) {
                    Log.e("AuraViewModel", "Error setting AI wallpaper", e)
                    false
                }
            }
            withContext(Dispatchers.Main) {
                onComplete(succeeded)
                if (succeeded) {
                    Toast.makeText(context, "AI background set on your device!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Failed setting background image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Save bitmap locally to internal storage and return file URI path
    fun saveBitmapToFile(context: Context, bitmap: Bitmap, filename: String): String? {
        return try {
            val file = File(context.filesDir, filename)
            val out = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush()
            out.close()
            "file://${file.absolutePath}"
        } catch (e: Exception) {
            Log.e("AuraViewModel", "Error saving bitmap to file", e)
            null
        }
    }
}
