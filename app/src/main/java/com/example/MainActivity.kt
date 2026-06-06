package com.example

import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.theme.*
import com.example.viewmodel.AuraViewModel
import com.example.viewmodel.UIState
import com.example.viewmodel.WallpaperItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val model: AuraViewModel = viewModel()
                AuraApp(model)
            }
        }
    }
}

@Composable
fun AuraApp(viewModel: AuraViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()
    val previewWallpaper by viewModel.previewWallpaper.collectAsState()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("scaffold"),
        containerColor = AuraBackground,
        bottomBar = {
            AuraStickyFooter(
                activeTab = currentTab,
                onTabSelected = { viewModel.selectTab(it) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            // Main views based on tab switching
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                },
                label = "tab_switch"
            ) { targetTab ->
                when (targetTab) {
                    0 -> HomeTab(viewModel)
                    1 -> ExploreTab(viewModel)
                    2 -> CreateTab(viewModel)
                    3 -> SavedTab(viewModel)
                    4 -> ProfileTab(viewModel)
                }
            }

            // Real-time Visual Phone Lock Screen Live Preview Overlay Modal
            previewWallpaper?.let { item ->
                LivePreviewModal(
                    item = item,
                    onClose = { viewModel.setPreviewWallpaper(null) },
                    onApply = { target ->
                        val context = viewModel.getApplication<android.app.Application>()
                        viewModel.applyWallpaper(context, item.url, target) { success ->
                            if (success) viewModel.setPreviewWallpaper(null)
                        }
                    }
                )
            }
        }
    }
}

// ==========================================
// SCREEN ONE: HOME DASHBOARD
// ==========================================
@Composable
fun HomeTab(viewModel: AuraViewModel) {
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val wallpapersState by viewModel.wallpapersState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AuraBackground)
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Sticky Header: branding title and quick input
        item {
            HomeHeader(viewModel)
        }

        // Horizontal Category pills selection
        item {
            CategoryPillsList(
                selectedCategory = selectedCategory,
                onCategorySelected = { viewModel.setCategory(it) }
            )
        }

        // Trending Section
        item {
            Text(
                text = "⚡ TRENDING TODAY",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            TrendingSpotlightCard(
                onCardClicked = { trendingItem ->
                    viewModel.setPreviewWallpaper(trendingItem)
                }
            )
        }

        // 2x2 Quick Features Block
        item {
            Text(
                text = "🛡️ QUICK FEATURES",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
            QuickFeaturesGrid(viewModel)
        }

        // Popular Wallpapers header
        item {
            Text(
                text = "✨ POPULAR WALLPAPERS",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
        }

        // Main response items lists
        when (val state = wallpapersState) {
            is UIState.Loading -> {
                item {
                    ShimmerLoadingGrid()
                }
            }
            is UIState.Success -> {
                val wallpapers = state.data
                if (wallpapers.isEmpty()) {
                    item {
                        EmptyStateHint()
                    }
                } else {
                    // Portrait Grid Items
                    items(wallpapers.chunked(2)) { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            pair.forEach { wallpaper ->
                                Box(modifier = Modifier.weight(1f)) {
                                    WallpaperCard(
                                        item = wallpaper,
                                        onCardClick = { viewModel.setPreviewWallpaper(wallpaper) },
                                        onFavClick = { viewModel.toggleSaveWallpaper(wallpaper) },
                                        isSavedFlow = viewModel.isWallpaperSavedFlow(wallpaper.id)
                                    )
                                }
                            }
                            if (pair.size < 2) {
                                Box(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
            is UIState.Error -> {
                item {
                    ErrorStateView(state.message) { viewModel.loadWallpapers() }
                }
            }
        }

        // Comfort margin spacing at bottom
        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun HomeHeader(viewModel: AuraViewModel) {
    val searchQuery by viewModel.searchQuery.collectAsState()

    Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Title branding from design specifications
            Row {
                Text(
                    text = "Aura",
                    color = TextPrimary,
                    fontSize = 24.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Walls",
                    color = NeonPurple,
                    fontSize = 24.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            IconButton(onClick = { viewModel.selectTab(4) }) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile Tab",
                    tint = TextSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Glassmorphism Styled Sticky Search Entry Box
        TextField(
            value = searchQuery,
            onValueChange = {
                viewModel.setSearchQuery(it)
            },
            placeholder = { Text("Search wallpapers...", color = TextMuted) },
            leadingIcon = { Icon(Icons.Default.Search, tint = TextMuted, contentDescription = "Search Icon") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = {
                        viewModel.setSearchQuery("")
                    }) {
                        Icon(Icons.Default.Close, tint = TextSecondary, contentDescription = "Clear input")
                    }
                }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = AuraSurfaceVariant.copy(alpha = 0.6f),
                unfocusedContainerColor = AuraSurface.copy(alpha = 0.4f),
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextSecondary,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, AuraBorder), RoundedCornerShape(16.dp))
                .testTag("search_bar")
        )
    }
}

@Composable
fun CategoryPillsList(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    val categories = listOf("All", "Cyberpunk", "Galaxy", "Minimal")

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(categories) { category ->
            val isActive = selectedCategory == category
            val borderBrush = if (isActive) {
                Brush.horizontalGradient(listOf(NeonPurple, NeonViolet))
            } else {
                SolidColor(AuraBorder)
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50.dp))
                    .background(
                        if (isActive) NeonPurple.copy(alpha = 0.2f) else AuraSurface.copy(alpha = 0.6f)
                    )
                    .border(BorderStroke(1.dp, borderBrush), RoundedCornerShape(50.dp))
                    .clickable { onCategorySelected(category) }
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .testTag("category_pill_$category"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = category,
                    color = if (isActive) TextPrimary else TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun TrendingSpotlightCard(onCardClicked: (WallpaperItem) -> Unit) {
    // Spotlight details represent "Nebula Core — Deep Space" prominently as requested
    val trendingItem = WallpaperItem(
        id = "w1",
        title = "Nebula Core — Deep Space",
        category = "Galaxy",
        resolution = "4K",
        url = "https://images.unsplash.com/photo-1462331940025-496dfbfc7564?q=80&w=1200",
        thumbnailUrl = "https://images.unsplash.com/photo-1462331940025-496dfbfc7564?q=80&w=600",
        views = 4500,
        downloads = 2400,
        creatorName = "Midjourney Studio",
        dominantColor = "#6366F1",
        tags = listOf("Deep Space", "Nebula", "Galaxy")
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .border(BorderStroke(1.dp, AuraBorder), RoundedCornerShape(20.dp))
            .clickable { onCardClicked(trendingItem) }
            .testTag("trending_card"),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(trendingItem.url)
                    .crossfade(true)
                    .build(),
                contentDescription = trendingItem.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // High contrast gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                            startY = 100f
                        )
                    )
            )

            // High priority overlay tags (4K indicator top-right)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .background(Color.Red.copy(alpha = 0.85f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text("● 4K", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            // Glassmorphic Info Panel Overlay (Bottom)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Text(
                    text = trendingItem.title,
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(NeonPurple.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("AI Generated", color = TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "by ${trendingItem.creatorName} • 2.4k 📥",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun QuickFeaturesGrid(viewModel: AuraViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Col 1: AI Generator, Color Match
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FeatureCard(
                icon = Icons.Default.Create,
                iconBg = NeonPurple.copy(alpha = 0.2f),
                iconColor = NeonPurple,
                title = "AI Generator",
                desc = "Create custom wallpapers with a prompt",
                onClick = { viewModel.selectTab(2) } // Goes directly to AI Generator Create screen
            )

            FeatureCard(
                icon = Icons.Default.Star,
                iconBg = Color(0xFFFBBF24).copy(alpha = 0.2f),
                iconColor = Color(0xFFFBBF24),
                title = "Color Match",
                desc = "Find walls matching your theme color",
                onClick = { viewModel.selectTab(1) } // Goes to Color-Theme filtration on Explore Tab
            )
        }

        // Col 2: Set Wallpaper, Auto-Rotate
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FeatureCard(
                icon = Icons.Default.Refresh,
                iconBg = Color(0xFF10B981).copy(alpha = 0.2f),
                iconColor = Color(0xFF10B981),
                title = "Set Wallpaper",
                desc = "One-tap apply to home or lock screen",
                onClick = {
                    Toast.makeText(
                        viewModel.getApplication(),
                        "Choose any wallpaper card below or search, then tap Apply!",
                        Toast.LENGTH_LONG
                    ).show()
                }
            )

            FeatureCard(
                icon = Icons.Default.Settings,
                iconBg = Color(0xFFEF4444).copy(alpha = 0.2f),
                iconColor = Color(0xFFEF4444),
                title = "Auto-Rotate",
                desc = "Change wallpaper daily automatically",
                onClick = { viewModel.selectTab(4) } // Goes direct to Profile config screen
            )
        }
    }
}

@Composable
fun FeatureCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBg: Color,
    iconColor: Color,
    title: String,
    desc: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, AuraBorder), RoundedCornerShape(16.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = AuraSurface.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(iconBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = desc,
                color = TextMuted,
                fontSize = 10.sp,
                lineHeight = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun WallpaperCard(
    item: WallpaperItem,
    onCardClick: () -> Unit,
    onFavClick: () -> Unit,
    isSavedFlow: Flow<Boolean>
) {
    val isSaved by isSavedFlow.collectAsState(initial = false)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.7f)
            .border(BorderStroke(1.dp, AuraBorder), RoundedCornerShape(16.dp))
            .clickable { onCardClick() }
            .testTag("wallpaper_card_${item.id}"),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(item.thumbnailUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Category tag top-left overlay
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .background(NeonPurple.copy(alpha = 0.8f), RoundedCornerShape(5.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(item.category.uppercase(), color = TextPrimary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }

            // Glassmorphic bottom information footer
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                        )
                    )
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "📥 ${item.downloads}",
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Light
                        )
                    }

                    // Interactive Save icon with Ripple
                    IconButton(
                        onClick = onFavClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isSaved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Save Action",
                            tint = if (isSaved) Color.Red else TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// SCREEN TWO: EXPLORE & COLOR MATCH
// ==========================================
@Composable
fun ExploreTab(viewModel: AuraViewModel) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedColorFilter by viewModel.selectedColorFilter.collectAsState()
    val wallpapersState by viewModel.wallpapersState.collectAsState()

    // Color Theme search hex palettes
    val paletteColors = listOf(
        Pair("Red", "#EF4444"),
        Pair("Purple", "#6366F1"),
        Pair("Blue", "#06B6D4"),
        Pair("Green", "#10B981"),
        Pair("Pink", "#EC4899"),
        Pair("Cyan", "#22D3EE"),
        Pair("Gray", "#6B7280")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Explore Backdrops",
            color = TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 12.dp)
        )

        // Integrated Search Input
        TextField(
            value = searchQuery,
            onValueChange = {
                viewModel.setSearchQuery(it)
            },
            placeholder = { Text("Search by tag, color, or vibe...", color = TextMuted) },
            leadingIcon = { Icon(Icons.Default.Search, tint = TextMuted, contentDescription = "Search Icon") },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = AuraSurfaceVariant.copy(alpha = 0.6f),
                unfocusedContainerColor = AuraSurface.copy(alpha = 0.4f),
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextSecondary,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, AuraBorder), RoundedCornerShape(14.dp))
        )

        // Color-Theme Search Palettes [High Priority Input Block]
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🎨 COLOR-THEME SEARCH",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                if (selectedColorFilter != null) {
                    TextButton(onClick = { viewModel.selectColorFilter(null) }) {
                        Text("Reset filter", color = NeonViolet, fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(paletteColors) { colorPair ->
                    val colorHex = colorPair.second
                    val isSelected = selectedColorFilter == colorHex
                    val parsedColor = Color(android.graphics.Color.parseColor(colorHex))

                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 42.dp else 36.dp)
                            .background(parsedColor, CircleShape)
                            .border(
                                BorderStroke(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) TextPrimary else AuraBorder
                                ),
                                CircleShape
                            )
                            .clickable { viewModel.selectColorFilter(colorHex) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = "Selected Filter", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        Divider(color = AuraBorder)

        // Portrait grid with matching results
        Box(modifier = Modifier.weight(1f)) {
            when (val state = wallpapersState) {
                is UIState.Loading -> {
                    ShimmerLoadingGrid()
                }
                is UIState.Success -> {
                    val walls = state.data
                    if (walls.isEmpty()) {
                        EmptyStateHint()
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(walls) { element ->
                                WallpaperCard(
                                    item = element,
                                    onCardClick = { viewModel.setPreviewWallpaper(element) },
                                    onFavClick = { viewModel.toggleSaveWallpaper(element) },
                                    isSavedFlow = viewModel.isWallpaperSavedFlow(element.id)
                                )
                            }
                        }
                    }
                }
                is UIState.Error -> {
                    ErrorStateView(state.message) { viewModel.loadWallpapers() }
                }
            }
        }
    }
}

// ==========================================
// SCREEN THREE: AI PROMPT GENERATOR
// ==========================================
@Composable
fun CreateTab(viewModel: AuraViewModel) {
    val promptText by viewModel.aiPrompt.collectAsState()
    val aiGenerationState by viewModel.aiGenerationState.collectAsState()
    val context = LocalContext.current

    var selectedAspectTarget by remember { mutableStateOf(3) } // 1: Home, 2: Lock, 3: Both
    val setWallpaperOptions = listOf(
        Pair(1, "Home Screen"),
        Pair(2, "Lock Screen"),
        Pair(3, "Both Screens")
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "AI Aura Creator",
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 12.dp)
            )
            Text(
                text = "Generate high-quality 4K wallpapers instantly using Google Gemini.",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }

        // Generator prompt text field
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, AuraBorder), RoundedCornerShape(18.dp)),
                colors = CardDefaults.cardColors(containerColor = AuraSurface.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Enter detailed scene or mood:",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    TextField(
                        value = promptText,
                        onValueChange = { viewModel.setAiPrompt(it) },
                        placeholder = {
                            Text(
                                "e.g., A rainy synthwave Tokyo night with neon reflections, cinematic cyberpunk look, 4K depth...",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = AuraSurfaceVariant.copy(alpha = 0.5f),
                            unfocusedContainerColor = AuraSurfaceVariant.copy(alpha = 0.3f),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextSecondary,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .testTag("ai_prompt_input")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Buttons
                    Button(
                        onClick = { viewModel.triggerAiWallpaperGeneration() },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("ai_generate_button"),
                        enabled = aiGenerationState !is UIState.Loading
                    ) {
                        if (aiGenerationState is UIState.Loading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generating Aura...", color = Color.White)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Create, contentDescription = "Gen Icon", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Generate Wallpaper", color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }

        // Output Preview Panel
        item {
            Text(
                text = "🎨 GENERATED RESULT",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.7f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(AuraSurface.copy(alpha = 0.6f))
                    .border(BorderStroke(1.dp, AuraBorder), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                when (val state = aiGenerationState) {
                    is UIState.Loading -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = NeonPurple)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Dreaming up your artwork with Gemini...", color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                    is UIState.Success -> {
                        val bitmap = state.data
                        if (bitmap != null) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "AI Wallpaper Output",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Apply overlay config panel right on top
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .fillMaxWidth()
                                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))))
                                        .padding(16.dp)
                                ) {
                                    Column {
                                        Text("Set this AI Creation as Background:", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            setWallpaperOptions.forEach { pair ->
                                                val optionId = pair.first
                                                val labelText = pair.second
                                                val active = selectedAspectTarget == optionId

                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(if (active) NeonPurple else AuraSurface)
                                                        .clickable { selectedAspectTarget = optionId }
                                                        .padding(vertical = 8.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(labelText, color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Button(
                                            onClick = {
                                                viewModel.applyGeneratedWallpaper(context, bitmap, selectedAspectTarget) { success ->
                                                    if (success) {
                                                        // Automatically save to local Room offline saved wallpapers
                                                        val wKey = "ai_" + System.currentTimeMillis()
                                                        val filePath = viewModel.saveBitmapToFile(context, bitmap, "$wKey.png")
                                                        if (filePath != null) {
                                                            viewModel.toggleSaveWallpaper(
                                                                WallpaperItem(
                                                                    id = wKey,
                                                                    title = promptText.take(18) + "... @AI",
                                                                    category = "AI Generated",
                                                                    resolution = "4K",
                                                                    url = filePath,
                                                                    thumbnailUrl = filePath,
                                                                    views = 1,
                                                                    downloads = 1,
                                                                    creatorName = "Google Gemini AI",
                                                                    isAiGenerated = true
                                                                )
                                                            )
                                                        } else {
                                                            Toast.makeText(context, "Failed to save generated wallpaper image file", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = NeonViolet),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = "apply", modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Apply Instantly", color = Color.White, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Create, contentDescription = "AI Prompt Creator", tint = TextMuted, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Your custom generated art will show here.",
                                    color = TextMuted,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    is UIState.Error -> {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = "Error icon", tint = Color.Red, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = state.message,
                                color = Color.Red,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// ==========================================
// SCREEN FOUR: OFFLINE SAVED DATABASE
// ==========================================
@Composable
fun SavedTab(viewModel: AuraViewModel) {
    val savedItems by viewModel.savedWallpapers.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Your Saved Vault",
            color = TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 12.dp)
        )
        Text(
            text = "Keep track of your favorite collections offline natively on-device.",
            color = TextSecondary,
            fontSize = 12.sp
        )

        Divider(color = AuraBorder)

        if (savedItems.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FavoriteBorder,
                        contentDescription = "Empty Favorites",
                        tint = TextMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Your offline vault is currently empty.",
                        color = TextMuted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Browse wallpapers, open any photo, and save to access it here.",
                        color = TextMuted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(savedItems) { saved ->
                    // Map entity back to item model for reuse of Card logic
                    val item = WallpaperItem(
                        id = saved.id,
                        title = saved.title,
                        category = saved.category,
                        resolution = saved.resolution,
                        url = saved.url,
                        thumbnailUrl = saved.thumbnailUrl,
                        views = saved.views,
                        downloads = saved.downloads,
                        creatorName = saved.creatorName
                    )

                    WallpaperCard(
                        item = item,
                        onCardClick = { viewModel.setPreviewWallpaper(item) },
                        onFavClick = { viewModel.toggleSaveWallpaper(item) },
                        isSavedFlow = flowOf(true) // Always saved inside Favorites tab
                    )
                }
            }
        }
    }
}

// ==========================================
// SCREEN FIVE: CREATOR PROFILE & AUTO-ROTATE
// ==========================================
@Composable
fun ProfileTab(viewModel: AuraViewModel) {
    val autoRotateEnabled by viewModel.autoRotateEnabled.collectAsState()
    val autoRotateInterval by viewModel.autoRotateInterval.collectAsState()

    var showDropdownMenu by remember { mutableStateOf(false) }
    val rotationIntervals = listOf("Hourly", "Daily", "Weekly")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Text(
                text = "My Profile Dashboard",
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        // Profile details Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, AuraBorder), RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = AuraSurface.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(
                                Brush.linearGradient(listOf(NeonPurple, NeonViolet)),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = "Dev Avatar", tint = Color.White, modifier = Modifier.size(32.dp))
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text("Aura Enquirer", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Personal Member", color = TextSecondary, fontSize = 12.sp)

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(modifier = Modifier.background(NeonPurple.copy(alpha = 0.2f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                            Text("AURA LEVEL 4", color = TextPrimary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Daily Auto-Rotate [High Priority feature block]
        item {
            Text(
                text = "⚙️ BACKGROUND CYCLES ENGINE",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, AuraBorder), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = AuraSurface.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Daily Auto-Rotate", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(
                                "Auto-switch device wallpaper background natively using curated trending feeds.",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 14.sp
                            )
                        }

                        Switch(
                            checked = autoRotateEnabled,
                            onCheckedChange = { viewModel.toggleAutoRotate() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = NeonPurple,
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = AuraSurfaceVariant
                            )
                        )
                    }

                    if (autoRotateEnabled) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = AuraBorder)
                        Spacer(modifier = Modifier.height(16.dp))

                        // Interval Scheduler Configurations
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Rotation Schedule Cycles:", color = TextPrimary, fontSize = 13.sp)

                            Box {
                                OutlinedButton(
                                    onClick = { showDropdownMenu = true },
                                    border = BorderStroke(1.dp, AuraBorder),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(autoRotateInterval, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "dropdown")
                                }

                                DropdownMenu(
                                    expanded = showDropdownMenu,
                                    onDismissRequest = { showDropdownMenu = false },
                                    modifier = Modifier.background(AuraSurface)
                                ) {
                                    rotationIntervals.forEach { intItem ->
                                        DropdownMenuItem(
                                            text = { Text(intItem, color = TextPrimary, fontSize = 12.sp) },
                                            onClick = {
                                                viewModel.setAutoRotateInterval(intItem)
                                                showDropdownMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Additional specifications details
        item {
            Text(
                text = "✨ DEVELOPER SPECIFICATION CREDITS",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, AuraBorder), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = AuraSurface.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SpecificationRow(label = "Platform Client Integration", valText = "Kotlin / Jetpack Compose")
                    SpecificationRow(label = "Network Live Domain", valText = "https://aurawalls.qzz.io")
                    SpecificationRow(label = "Asynchronous Data Hydrator", valText = "Retrofit REST Parser")
                    SpecificationRow(label = "Image Rendering Adapter", valText = "Coil Image Pipeline")
                    SpecificationRow(label = "Native System Bridges", valText = "WallpaperManager Service")
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun SpecificationRow(label: String, valText: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextSecondary, fontSize = 12.sp)
        Text(valText, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

// ==========================================
// REUSABLE SUB-COMPONENTS
// ==========================================

// Global Sticky footer navigation containing exactly 5 icons
@Composable
fun AuraStickyFooter(
    activeTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val items = listOf(
        Triple(0, Icons.Default.Home, "Home"),
        Triple(1, Icons.Default.Search, "Explore"),
        Triple(2, Icons.Default.Create, "Create"),
        Triple(3, Icons.Default.FavoriteBorder, "Saved"),
        Triple(4, Icons.Default.Person, "Profile")
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = AuraSurface.copy(alpha = 0.95f),
        tonalElevation = 8.dp
    ) {
        // Subtle top division border
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(AuraBorder)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { data ->
                    val index = data.first
                    val icon = data.second
                    val label = data.third
                    val isSelected = activeTab == index

                    Box(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clickable { onTabSelected(index) }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = if (isSelected) NeonPurple else TextSecondary,
                                modifier = Modifier.size(if (isSelected) 24.dp else 22.dp)
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = label,
                                color = if (isSelected) TextPrimary else TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )

                            // Purple neon active tiny pill dot at footer bottom
                            if (isSelected) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .background(NeonPurple, CircleShape)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Live Preview realistic mobile phone lock screen overlay modal window
@Composable
fun LivePreviewModal(
    item: WallpaperItem,
    onClose: () -> Unit,
    onApply: (Int) -> Unit
) {
    var showTargetSelector by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Background Wallpaper stretched full-bleed
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(if (item.url.isNotEmpty()) item.url else item.thumbnailUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Blur vignetting
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.3f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.75f)
                            )
                        )
                    )
            )

            // Close actions (Top-Left)
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Close preview",
                    tint = Color.White
                )
            }

            // Realistic lock screen status: Clock (9:41), Calendar, etc.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 64.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Friday, June 5",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "09:41",
                    color = Color.White,
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = -1.sp
                )
            }

            // Bottom glassmorphic overlay dashboard drawer
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(AuraSurface.copy(alpha = 0.85f))
                    .border(
                        BorderStroke(1.dp, Color(0x1AFFFFFF)),
                        RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    )
                    .padding(20.dp)
            ) {
                Text(
                    text = item.title,
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Category: ${item.category} • Artist: ${item.creatorName}",
                    color = TextSecondary,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (!showTargetSelector) {
                    Button(
                        onClick = { showTargetSelector = true },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Apply Wallpaper", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                } else {
                    // Option targeting selectors
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onApply(1) },
                            colors = ButtonDefaults.buttonColors(containerColor = AuraSurfaceVariant),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Text("Home Screen", fontSize = 11.sp, color = TextPrimary)
                        }

                        Button(
                            onClick = { onApply(2) },
                            colors = ButtonDefaults.buttonColors(containerColor = AuraSurfaceVariant),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Text("Lock Screen", fontSize = 11.sp, color = TextPrimary)
                        }

                        Button(
                            onClick = { onApply(3) },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1.1f)
                                .height(44.dp)
                        ) {
                            Text("Both Screens", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShimmerLoadingGrid() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        repeat(3) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(0.7f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(AuraSurfaceVariant.copy(alpha = 0.5f))
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(0.7f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(AuraSurfaceVariant.copy(alpha = 0.5f))
                )
            }
        }
    }
}

@Composable
fun EmptyStateHint() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Search, contentDescription = "Search Icon Placeholder", tint = TextMuted, modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text("No wallpapers found.", color = TextMuted, fontSize = 13.sp)
        }
    }
}

@Composable
fun ErrorStateView(message: String, onRetry: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, Color(0x33EF4444)), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = AuraSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Connection Issue", color = Color.Red, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(message, color = TextSecondary, fontSize = 11.sp, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Retry", color = Color.White)
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}
