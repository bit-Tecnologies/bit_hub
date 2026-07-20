package com.bit.bithub

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.*
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bit.bithub.components.UpdateBottomSheet
import com.bit.bithub.data.App
import com.bit.bithub.data.NetworkType
import com.bit.bithub.data.SettingsRepository
import com.bit.bithub.data.UpdateInterval
import com.bit.bithub.data.UpdateViewModel
import com.bit.bithub.navigation.AppDestinations
import com.bit.bithub.screens.*
import com.bit.bithub.ui.theme.BitHubTheme
import com.bit.bithub.ui.theme.ThemeMode
import com.bit.bithub.util.UpdateInstaller
import com.bit.bithub.util.isWifiConnected
import com.bit.bithub.settings.SettingsManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.time.Duration.Companion.seconds

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            val currentTheme = SettingsManager.themeMode
            val isDarkTheme = if (currentTheme == ThemeMode.SYSTEM) isSystemInDarkTheme() else currentTheme == ThemeMode.DARK

            LaunchedEffect(isDarkTheme) {
                val style = if (isDarkTheme) {
                    SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                }
                enableEdgeToEdge(
                    statusBarStyle = style,
                    navigationBarStyle = style,
                )
            }

            BitHubTheme(themeMode = currentTheme) {
                BitHubApp()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BitHubApp(
    viewModel: MainViewModel = viewModel(),
    updateViewModel: UpdateViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val settingsRepository = remember { SettingsRepository(context) }

    val stateDownloading = stringResource(R.string.state_downloading)

    var currentDestination by rememberSaveable { mutableStateOf(value = AppDestinations.HOME) }
    var selectedAppId by rememberSaveable { mutableStateOf<Long?>(value = null) }
    var showProfileSheet by rememberSaveable { mutableStateOf(value = false) }
    var appToConfirmDownload by remember { mutableStateOf<App?>(value = null) }

    val profileSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var pendingDownload by remember { mutableStateOf<Pair<App, String>?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    DisposableEffect(lifecycleOwner) {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                viewModel.refreshInstalledApps()
            }
        }
        context.registerReceiver(receiver, filter)

        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshInstalledApps()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            try { context.unregisterReceiver(receiver) } catch (_: Exception) {}
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    fun vibrate() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            vibrator?.let { v ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(15)
                }
            }
        } catch (_: Exception) { }
    }

    LaunchedEffect(Unit) {
        viewModel.loadData()
        updateViewModel.checkForUpdates()

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                scope.launch {
                    delay(2.seconds)
                    viewModel.loadData()
                    updateViewModel.checkForUpdates()
                }
            }
        }

        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            cm.registerNetworkCallback(request, networkCallback)
        } catch (_: Exception) {}
    }

    fun startDownload(app: App) {
        if ((Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) &&
            (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        ) {
            pendingDownload = Pair(app, app.downloadUrl ?: "")
            permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            return
        }
        viewModel.download(app, stateDownloading)
    }

    fun handleInstallClick(app: App) {
        vibrate()
        val appId = app.id ?: return

        if (viewModel.downloadingProgress.containsKey(appId)) {
            viewModel.cancelDownload(appId)
            return
        }

        val apkFile = viewModel.getApkFile(app.title)
        val pkg = app.packageName ?: ""
        
        // If file exists, check if it's the right version or if we can just try installing it
        // For simplicity, if APK exists and app is either not installed OR installed but older version, try install
        val isInstalled = viewModel.installedApps.containsKey(pkg)
        val installedVersion = viewModel.installedApps[pkg] ?: -1
        val needsUpdate = isInstalled && app.versionCode > installedVersion

        if (apkFile.exists() && (!isInstalled || needsUpdate)) {
            UpdateInstaller.installApk(context, apkFile)
            return
        }

        if (SettingsManager.downloadWifiOnly && !isWifiConnected(context)) {
            appToConfirmDownload = app
            return
        }

        startDownload(app)
    }

    LaunchedEffect(pendingDownload) {
        val currentPending = pendingDownload
        if (currentPending != null && ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            viewModel.download(currentPending.first, stateDownloading)
            pendingDownload = null
        }
    }

    if (selectedAppId != null) BackHandler { selectedAppId = null }

    val snackbarHostState = remember { SnackbarHostState() }
    val updatesMessage = stringResource(R.string.msg_updates_available, viewModel.appsWithUpdates.size)
    val viewLabel = stringResource(R.string.msg_btn_view)

    LaunchedEffect(viewModel.appsWithUpdates.size) {
        val updatesCount = viewModel.appsWithUpdates.size
        if (updatesCount > 0 && !viewModel.isLoading) {
            val result = snackbarHostState.showSnackbar(
                message = updatesMessage,
                actionLabel = viewLabel,
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) {
                vibrate()
                showProfileSheet = true
            }
        }
    }

    if (updateViewModel.showNoUpdateMessage) {
        LaunchedEffect(Unit) {
            snackbarHostState.showSnackbar("У вас установлена последняя версия bit Hub")
            updateViewModel.resetNoUpdateMessage()
        }
    }

    Box(Modifier.fillMaxSize()) {
        NavigationSuiteScaffold(
            navigationSuiteItems = {
                AppDestinations.entries.forEach { dest ->
                    item(
                        icon = { Icon(dest.icon, stringResource(dest.labelRes), modifier = Modifier.size(24.dp)) },
                        label = { Text(text = stringResource(dest.labelRes), maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 11.sp) },
                        selected = dest == currentDestination && selectedAppId == null,
                        onClick = {
                            if (currentDestination != dest || selectedAppId != null) {
                                vibrate()
                                currentDestination = dest
                                selectedAppId = null
                            }
                        }
                    )
                }
            }
        ) {
            if (viewModel.isLoading && viewModel.appsFromCloud.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val app = viewModel.appsFromCloud.find { it.id == selectedAppId }
                if (app != null) {
                    val pkg = app.packageName
                    val isInstalled = pkg?.let { viewModel.installedApps.containsKey(it) } ?: false
                    val currentVersion = pkg?.let { viewModel.installedApps[it] } ?: 0
                    val isUpdate = isInstalled && app.versionCode > currentVersion
                    val progress = viewModel.downloadingProgress[app.id]

                    AppDetailScreen(
                        app = app,
                        isFavorite = false,
                        isInstalled = isInstalled,
                        needsUpdate = isUpdate,
                        hasApk = viewModel.appsWithApk.contains(app.id),
                        isDownloading = progress != null,
                        downloadProgress = progress ?: 0f,
                        onBack = {
                            vibrate()
                            selectedAppId = null
                        },
                        onToggleFavorite = {},
                        onInstall = { handleInstallClick(app) },
                    )
                } else if (currentDestination == AppDestinations.HOME) {
                    HomeScreen(
                        apps = viewModel.appsFromCloud,
                        onAppClick = { appItem ->
                            vibrate()
                            selectedAppId = appItem.id
                        },
                        onSearchClick = {
                            currentDestination = AppDestinations.APPS
                        },
                        onProfileClick = {
                            vibrate()
                            showProfileSheet = true
                        }
                    )
                } else {
                    StoreScreen(
                        apps = when (currentDestination) {
                            AppDestinations.GAMES -> viewModel.appsFromCloud.filter { it.category == "Игры" }
                            AppDestinations.APPS -> viewModel.appsFromCloud.filter { it.category != "Игры" }
                            else -> viewModel.appsFromCloud
                        },
                        isGamesTab = currentDestination == AppDestinations.GAMES,
                        onAppClick = { appItem ->
                            vibrate()
                            selectedAppId = appItem.id
                        },
                        onInstallClick = { appItem -> handleInstallClick(appItem) },
                        installedApps = viewModel.installedApps,
                        appsWithApk = viewModel.appsWithApk.toSet(),
                        downloadingIds = viewModel.downloadingProgress,
                        onProfileClick = {
                            vibrate()
                            showProfileSheet = true
                        },
                        isRefreshing = viewModel.isLoading,
                        onRefresh = { viewModel.loadData() },
                        error = viewModel.errorMessage,
                        onRetry = { viewModel.loadData() }
                    )
                }
            }
        }

        if (showProfileSheet) {
            var showAutoUpdateSettings by remember { mutableStateOf(false) }
            val backgroundCheck by settingsRepository.backgroundUpdateCheck.collectAsState(initial = true)
            val interval by settingsRepository.updateInterval.collectAsState(initial = UpdateInterval.TWENTY_FOUR_HOURS)
            val networkType by settingsRepository.networkType.collectAsState(initial = NetworkType.WIFI_ONLY)
            val downloadPreReleases by settingsRepository.downloadPreReleases.collectAsState(initial = false)
            val appDownloadWifiOnly by settingsRepository.appDownloadWifiOnly.collectAsState(initial = false)

            ModalBottomSheet(
                onDismissRequest = { showProfileSheet = false },
                sheetState = profileSheetState,
                dragHandle = { if (!showAutoUpdateSettings) BottomSheetDefaults.DragHandle() },
                modifier = Modifier.fillMaxSize()
            ) {
                if (showAutoUpdateSettings) {
                    AutoUpdateSettingsScreen(
                        backgroundCheckEnabled = backgroundCheck,
                        onBackgroundCheckChange = { scope.launch { settingsRepository.setBackgroundUpdateCheck(it) } },
                        currentInterval = interval,
                        onIntervalChange = { scope.launch { settingsRepository.setUpdateInterval(it) } },
                        currentNetworkType = networkType,
                        onNetworkTypeChange = { scope.launch { settingsRepository.setNetworkType(it) } },
                        downloadPreReleases = downloadPreReleases,
                        onDownloadPreReleasesChange = { scope.launch { settingsRepository.setDownloadPreReleases(it) } },
                        appDownloadWifiOnly = appDownloadWifiOnly,
                        onAppDownloadWifiOnlyChange = {
                            scope.launch {
                                settingsRepository.setAppDownloadWifiOnly(it)
                                SettingsManager.downloadWifiOnly = it
                            }
                        },
                        onBack = { showAutoUpdateSettings = false }
                    )
                } else {
                    ProfileScreen(
                        currentThemeMode = SettingsManager.themeMode,
                        onThemeChange = { SettingsManager.themeMode = it },
                        onAutoUpdateSettingsClick = { showAutoUpdateSettings = true },
                        installedCount = viewModel.installedApps.size,
                        isCheckingUpdate = updateViewModel.isChecking,
                        updateInfo = updateViewModel.updateInfo,
                        onCheckUpdateClick = {
                            updateViewModel.checkForUpdates(manual = true)
                        },
                        onClose = {
                            scope.launch { profileSheetState.hide() }.invokeOnCompletion {
                                if (!profileSheetState.isVisible) {
                                    showProfileSheet = false
                                }
                            }
                        }
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp)
        )

        if (updateViewModel.isUpdatePromptVisible) {
            updateViewModel.updateInfo?.let { update ->
                UpdateBottomSheet(
                    updateInfo = update,
                    onDismiss = { updateViewModel.dismissUpdate() },
                    onUpdate = {
                        updateViewModel.startUpdate(context, update)
                    }
                )
            }
        }

        appToConfirmDownload?.let { app ->
            AlertDialog(
                onDismissRequest = { appToConfirmDownload = null },
                title = { Text(stringResource(R.string.dialog_mobile_data_title)) },
                text = { Text(stringResource(R.string.dialog_mobile_data_desc)) },
                confirmButton = {
                    TextButton(onClick = {
                        appToConfirmDownload = null
                        startDownload(app)
                    }) {
                        Text(stringResource(R.string.btn_download))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { appToConfirmDownload = null }) {
                        Text(stringResource(R.string.btn_cancel))
                    }
                }
            )
        }
    }
}
