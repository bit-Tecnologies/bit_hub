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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.bit.bithub.data.AppItem
import com.bit.bithub.components.AppStatItem
import com.bit.bithub.components.DownloadButton
import com.bit.bithub.util.toColor
import com.bit.bithub.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailScreen(
    app: AppItem,
    isFavorite: Boolean,
    isInstalled: Boolean,
    needsUpdate: Boolean,
    hasApk: Boolean,
    isDownloading: Boolean,
    downloadProgress: Float,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onInstall: () -> Unit,
) {
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
                            val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_TEXT, "Скачай ${app.title} в bit Hub! Приложение от ${app.developer}")
                            }
                            context.startActivity(android.content.Intent.createChooser(sendIntent, null))
                        }
                    ) {
                        Icon(Icons.Default.Share, null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
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
                        .background(app.iconColorHex.toColor()),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(app.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(app.developer, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
            }

            Surface(
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AppStatItem("${app.rating} ★", "отзывов")
                    VerticalDivider(Modifier.height(40.dp))
                    AppStatItem(app.size, "Размер")
                    VerticalDivider(Modifier.height(40.dp))
                    AppStatItem(app.versionCode, "Версия")
                }
            }

            Spacer(Modifier.height(24.dp))

            val buttonText = when {
                needsUpdate -> "Обновить"
                isInstalled -> stringResource(R.string.btn_installed)
                hasApk -> stringResource(R.string.btn_install)
                else -> stringResource(R.string.btn_install)
            }

            DownloadButton(
                text = if (isInstalled && !needsUpdate) stringResource(R.string.btn_installed) else if (hasApk && !isInstalled) stringResource(R.string.btn_install) else buttonText,
                progress = if (isDownloading) downloadProgress else null,
                onClick = onInstall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                enabled = !isInstalled || needsUpdate || hasApk
            )

            Spacer(Modifier.height(24.dp))

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
                items(5) { index ->
                    AsyncImage(
                        model = "https://picsum.photos/seed/${(app.id ?: 0) + index}/300/500",
                        contentDescription = null,
                        modifier = Modifier
                            .width(150.dp)
                            .height(250.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(app.iconColorHex.toColor().copy(alpha = 0.3f)),
                        contentScale = ContentScale.Crop
                    )
                }
            }

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
}
