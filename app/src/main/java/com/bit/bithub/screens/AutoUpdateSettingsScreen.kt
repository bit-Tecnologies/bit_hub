package com.bit.bithub.screens

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bit.bithub.data.NetworkType
import com.bit.bithub.data.UpdateInterval

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoUpdateSettingsScreen(
    // Настройки bit Hub
    backgroundCheckEnabled: Boolean,
    onBackgroundCheckChange: (Boolean) -> Unit,
    currentInterval: UpdateInterval,
    onIntervalChange: (UpdateInterval) -> Unit,
    currentNetworkType: NetworkType,
    onNetworkTypeChange: (NetworkType) -> Unit,
    downloadPreReleases: Boolean,
    onDownloadPreReleasesChange: (Boolean) -> Unit,
    // Настройки приложений
    appDownloadWifiOnly: Boolean,
    onAppDownloadWifiOnlyChange: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Обновления и сеть") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // СЕКЦИЯ 1: КЛИЕНТ BIT HUB
            SettingsCategoryHeader("Клиент bit Hub")
            
            ListItem(
                headlineContent = { Text("Фоновая проверка") },
                supportingContent = { Text("Проверять наличие новых версий bit Hub в фоне") },
                trailingContent = {
                    Switch(
                        checked = backgroundCheckEnabled,
                        onCheckedChange = onBackgroundCheckChange
                    )
                }
            )

            AnimatedVisibility(
                visible = backgroundCheckEnabled,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Text(
                        text = "Периодичность проверки",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
                    )

                    UpdateIntervalOption(
                        title = "Раз в 6 часов",
                        selected = currentInterval == UpdateInterval.SIX_HOURS,
                        onClick = { onIntervalChange(UpdateInterval.SIX_HOURS) }
                    )

                    UpdateIntervalOption(
                        title = "Раз в сутки",
                        selected = currentInterval == UpdateInterval.TWENTY_FOUR_HOURS,
                        onClick = { onIntervalChange(UpdateInterval.TWENTY_FOUR_HOURS) }
                    )
                    
                    Text(
                        text = "Тип сети для проверки",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
                    )

                    NetworkTypeOption(
                        title = "Любая сеть",
                        selected = currentNetworkType == NetworkType.ANY,
                        onClick = { onNetworkTypeChange(NetworkType.ANY) }
                    )

                    NetworkTypeOption(
                        title = "Только Wi-Fi",
                        selected = currentNetworkType == NetworkType.WIFI_ONLY,
                        onClick = { onNetworkTypeChange(NetworkType.WIFI_ONLY) }
                    )
                }
            }

            ListItem(
                headlineContent = { Text("Скачивать пре-релизы") },
                supportingContent = { Text("Получать доступ к бета-версиям bit Hub") },
                trailingContent = {
                    Switch(
                        checked = downloadPreReleases,
                        onCheckedChange = onDownloadPreReleasesChange
                    )
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // СЕКЦИЯ 2: ЗАГРУЗКА ПРИЛОЖЕНИЙ
            SettingsCategoryHeader("Загрузка приложений")

            ListItem(
                headlineContent = { Text("Скачивать только по Wi-Fi") },
                supportingContent = { Text("Экономия мобильного трафика при загрузке игр и программ") },
                trailingContent = {
                    Switch(
                        checked = appDownloadWifiOnly,
                        onCheckedChange = onAppDownloadWifiOnlyChange
                    )
                }
            )
            
            Box(modifier = Modifier.padding(16.dp)) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = "Примечание: Обновления для установленных приложений проверяются автоматически при каждом запуске bit Hub.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsCategoryHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
    )
}

@Composable
private fun UpdateIntervalOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun NetworkTypeOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
    }
}
