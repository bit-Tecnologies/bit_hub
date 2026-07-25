package com.bit.bithub.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import coil.compose.AsyncImage
import com.bit.bithub.data.App
import com.bit.bithub.components.AppStatItem
import com.bit.bithub.components.DownloadButton
import com.bit.bithub.R
import com.bit.bithub.util.formatFileSize

private val defaultIconColor = Color(0xFF2C6CFF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailScreen(
    app: App,
    isFavorite: Boolean,
    isInstalled: Boolean,
    needsUpdate: Boolean,
    hasApk: Boolean,
    isDownloading: Boolean,
    downloadProgress: Float,
    windowWidthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onInstall: () -> Unit,
    onDeleteApk: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.btn_cancel))
                    }
                },
                actions = {
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = stringResource(R.string.favorites),
                            tint = if (isFavorite) Color.Red else LocalContentColor.current
                        )
                    }
                    val context = LocalContext.current
                    IconButton(
                        onClick = {
                            val appId = app.id
                            val shareText = if (appId != null) {
                                "Посмотри ${app.title} в bit Hub! Открыть: https://bit-tecnologies.pages.dev/app?id=$appId"
                            } else {
                                "Скачай ${app.title} в bit Hub! Приложение от ${app.developer}"
                            }
                            val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(android.content.Intent.createChooser(sendIntent, null))
                        }
                    ) {
                        Icon(Icons.Default.Share, null)
                    }

                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Удалить загрузочный APK") },
                                onClick = {
                                    showMenu = false
                                    showDeleteDialog = true
                                },
                                enabled = hasApk,
                                leadingIcon = { Icon(Icons.Default.MoreVert, null) }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        val isWide = windowWidthSizeClass != WindowWidthSizeClass.Compact
        
        if (isWide) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                // Левая колонка - основная инфо и кнопка
                Column(
                    modifier = Modifier
                        .weight(0.4f)
                        .verticalScroll(rememberScrollState())
                ) {
                    AppHeader(app)
                    Spacer(Modifier.height(24.dp))
                    AppStats(app)
                    Spacer(Modifier.height(24.dp))
                    
                    val buttonText = getButtonText(needsUpdate, isInstalled, hasApk)
                    DownloadButton(
                        text = buttonText,
                        progress = if (isDownloading) downloadProgress else null,
                        onClick = onInstall,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isInstalled || needsUpdate || hasApk
                    )
                }
                
                Spacer(Modifier.width(32.dp))
                
                // Правая колонка - описание и скриншоты
                Column(
                    modifier = Modifier
                        .weight(0.6f)
                        .verticalScroll(rememberScrollState())
                ) {
                    AppScreenshots(app)
                    Spacer(Modifier.height(24.dp))
                    AppDescription(app)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                AppHeader(app)
                AppStats(app)
                Spacer(Modifier.height(24.dp))
                
                val buttonText = getButtonText(needsUpdate, isInstalled, hasApk)
                DownloadButton(
                    text = buttonText,
                    progress = if (isDownloading) downloadProgress else null,
                    onClick = onInstall,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    enabled = !isInstalled || needsUpdate || hasApk
                )
                
                Spacer(Modifier.height(24.dp))
                AppScreenshots(app)
                AppDescription(app)
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удалить файл?") },
            text = { Text("Вы действительно хотите удалить скачанный APK-файл приложения ${app.title}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteApk()
                    }
                ) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
private fun AppHeader(app: App) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = app.iconUrl,
            contentDescription = null,
            modifier = Modifier
                .size(88.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(defaultIconColor),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.width(16.dp))
        Column {
            Text(app.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(app.developer, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun AppStats(app: App) {
    Surface(
        modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                AppStatItem("${app.rating} ★", "Рейтинг")
            }
            VerticalDivider(Modifier.height(32.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                val context = LocalContext.current
                AppStatItem(app.sizeBytes.formatFileSize(context), "Размер")
            }
            VerticalDivider(Modifier.height(32.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                AppStatItem(app.versionName, "Версия")
            }
        }
    }
}

@Composable
private fun AppScreenshots(app: App) {
    val screenshots = app.screenshots
    if (!screenshots.isNullOrEmpty()) {
        Text(
            "Скриншоты",
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        LazyRow(
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(screenshots.size) { index ->
                AsyncImage(
                    model = screenshots[index],
                    contentDescription = null,
                    modifier = Modifier
                        .width(150.dp)
                        .height(250.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(defaultIconColor.copy(alpha = 0.3f)),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

@Composable
private fun AppDescription(app: App) {
    Column {
        Text(
            "Описание",
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            app.description,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun getButtonText(needsUpdate: Boolean, isInstalled: Boolean, hasApk: Boolean): String {
    return when {
        needsUpdate -> "Обновить"
        isInstalled -> stringResource(R.string.btn_installed)
        hasApk -> stringResource(R.string.btn_install)
        else -> stringResource(R.string.btn_download)
    }
}
