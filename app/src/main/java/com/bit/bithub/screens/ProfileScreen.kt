package com.bit.bithub.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.provider.Settings
import android.content.Intent
import com.bit.bithub.BuildConfig
import com.bit.bithub.components.SettingsItem
import com.bit.bithub.components.SettingsSection
import com.bit.bithub.components.ThemeSelectionDialog
import com.bit.bithub.ui.theme.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    currentThemeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    onAutoUpdateSettingsClick: () -> Unit,
    installedCount: Int,
    isCheckingUpdate: Boolean,
    onCheckUpdateClick: () -> Unit,
    onClose: () -> Unit
) {
    var showThemeDialog by remember { mutableStateOf(false) }
    
    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentThemeMode = currentThemeMode,
            onDismiss = { showThemeDialog = false },
            onThemeSelect = {
                onThemeChange(it)
                showThemeDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Аккаунт") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier.size(100.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AccountCircle, null, modifier = Modifier.size(70.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text("Пользователь bit Hub", style = MaterialTheme.typography.headlineSmall)
            
            Spacer(Modifier.height(24.dp))
            
            SettingsSection(title = "Управление") {
                ListItem(
                    headlineContent = { Text("Мои приложения и игры") },
                    trailingContent = { Text("$installedCount шт.") },
                    leadingContent = { Icon(Icons.AutoMirrored.Filled.List, null) }
                )
                
                ListItem(
                    headlineContent = { Text("Обновления и сеть") },
                    supportingContent = { Text("Фоновая проверка и настройки загрузки") },
                    leadingContent = { Icon(Icons.Default.Update, null) },
                    modifier = Modifier.clickable { onAutoUpdateSettingsClick() }
                )
            }

            val context = LocalContext.current
            SettingsSection(title = "Настройки") {
                SettingsItem(Icons.Default.Palette, "Тема оформления") { showThemeDialog = true }
                SettingsItem(Icons.Default.Notifications, "Уведомления") {
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                    context.startActivity(intent)
                }
            }

            SettingsSection(title = "Инфо") {
                ListItem(
                    headlineContent = { Text("Версия") },
                    trailingContent = { 
                        if (isCheckingUpdate) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Text(BuildConfig.VERSION_NAME)
                        }
                    },
                    leadingContent = { Icon(Icons.Default.Info, null) },
                    modifier = Modifier.clickable(enabled = !isCheckingUpdate) { onCheckUpdateClick() }
                )
            }
        }
    }
}
