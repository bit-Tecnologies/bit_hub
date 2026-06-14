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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bit.bithub.data.NetworkType
import com.bit.bithub.data.UpdateInterval

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoUpdateSettingsScreen(
    backgroundCheckEnabled: Boolean,
    onBackgroundCheckChange: (Boolean) -> Unit,
    currentInterval: UpdateInterval,
    onIntervalChange: (UpdateInterval) -> Unit,
    currentNetworkType: NetworkType,
    onNetworkTypeChange: (NetworkType) -> Unit,
    downloadPreReleases: Boolean,
    onDownloadPreReleasesChange: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Автообновление приложений") },
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
            ListItem(
                headlineContent = { Text("Фоновая проверка обновлений") },
                supportingContent = { Text("Проверять наличие новых версий в фоновом режиме") },
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
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    
                    Text(
                        text = "Периодичность",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
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

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Тип сети",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
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

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Другое",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
                    )

                    ListItem(
                        headlineContent = { Text("Скачивать пре-релизы") },
                        supportingContent = { Text("Получать доступ к новым функциям раньше других (может быть нестабильно)") },
                        trailingContent = {
                            Switch(
                                checked = downloadPreReleases,
                                onCheckedChange = onDownloadPreReleasesChange
                            )
                        }
                    )
                }
            }
        }
    }
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
            .padding(horizontal = 16.dp, vertical = 12.dp),
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
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
    }
}
