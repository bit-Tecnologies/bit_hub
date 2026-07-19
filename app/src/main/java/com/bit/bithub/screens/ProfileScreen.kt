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
import androidx.compose.ui.res.stringResource
import com.bit.bithub.R
import com.bit.bithub.BuildConfig
import com.bit.bithub.components.SettingsItem
import com.bit.bithub.components.SettingsSection
import com.bit.bithub.components.ThemeSelectionDialog
import com.bit.bithub.data.UpdateInfo
import com.bit.bithub.ui.theme.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    currentThemeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    onAutoUpdateSettingsClick: () -> Unit,
    installedCount: Int,
    isCheckingUpdate: Boolean,
    updateInfo: UpdateInfo? = null,
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
                title = { Text(stringResource(R.string.title_account)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.btn_cancel))
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
            Text(stringResource(R.string.user_name_placeholder), style = MaterialTheme.typography.headlineSmall)
            
            Spacer(Modifier.height(24.dp))
            
            SettingsSection(title = stringResource(R.string.section_management)) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.my_apps)) },
                    trailingContent = { Text(stringResource(R.string.unit_pcs, installedCount)) },
                    leadingContent = { Icon(Icons.AutoMirrored.Filled.List, null) }
                )
                
                ListItem(
                    headlineContent = { Text(stringResource(R.string.section_network_updates)) },
                    supportingContent = { Text(stringResource(R.string.section_network_updates_desc)) },
                    leadingContent = { Icon(Icons.Default.Update, null) },
                    modifier = Modifier.clickable { onAutoUpdateSettingsClick() }
                )
            }

            val context = LocalContext.current
            SettingsSection(title = stringResource(R.string.section_settings)) {
                SettingsItem(Icons.Default.Palette, stringResource(R.string.theme_settings)) { showThemeDialog = true }
                SettingsItem(Icons.Default.Notifications, stringResource(R.string.notifications)) {
                    val intent = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                    } else {
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = android.net.Uri.fromParts("package", context.packageName, null)
                        }
                    }
                    context.startActivity(intent)
                }
            }

            SettingsSection(title = stringResource(R.string.section_info)) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.app_version)) },
                    trailingContent = { 
                        if (isCheckingUpdate) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (updateInfo != null) {
                                    Text(
                                        stringResource(R.string.update_available),
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                }
                                Text(BuildConfig.VERSION_NAME)
                            }
                        }
                    },
                    leadingContent = { Icon(Icons.Default.Info, null) },
                    modifier = Modifier.clickable(enabled = !isCheckingUpdate) { onCheckUpdateClick() }
                )
            }
        }
    }
}
