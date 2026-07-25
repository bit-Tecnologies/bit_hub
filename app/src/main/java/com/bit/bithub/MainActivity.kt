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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldLayout
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
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
import com.bit.bithub.util.vibrate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.time.Duration.Companion.seconds

class MainActivity : ComponentActivity() {

    private var initialAppId by mutableStateOf<Long?>(null)

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        handleIntent(intent)

        enableEdgeToEdge()
        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val settingsRepository = remember { SettingsRepository(this) }
            val currentTheme by settingsRepository.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            
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
                BitHubApp(initialAppId = initialAppId, windowWidthSizeClass = windowSizeClass.widthSizeClass)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val data = intent?.data ?: return
        android.util.Log.d("BitHubIntent", "Handling intent: $data")
        
        val appId = when {
            // bithub://app?id=...
            data.scheme == "bithub" && data.host == "app" -> data.getQueryParameter("id")
            
            // https://bit-tecnologies.github.io/bit_hub/app?id=...
            (data.scheme == "http" || data.scheme == "https") && 
            data.host == "bit-tecnologies.github.io" && 
            data.path?.startsWith("/bit_hub/app") == true -> data.getQueryParameter("id")
            
            // https://bit-tecnologies.pages.dev/app?id=...
            (data.scheme == "http" || data.scheme == "https") && 
            data.host == "bit-tecnologies.pages.dev" && 
            data.path?.startsWith("/app") == true -> data.getQueryParameter("id")
            
            else -> null
        }
        
        initialAppId = appId?.toLongOrNull()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BitHubApp(
    viewModel: MainViewModel = viewModel(),
    updateViewModel: UpdateViewModel = viewModel(),
    initialAppId: Long? = null,
    windowWidthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val settingsRepository = remember { SettingsRepository(context) }

    val stateDownloading = stringResource(R.string.state_downloading)

    var currentDestination by rememberSaveable { mutableStateOf(value = AppDestinations.HOME) }
    var selectedAppId by rememberSaveable { mutableStateOf<Long?>(value = null) }

    val appDownloadWifiOnly by settingsRepository.appDownloadWifiOnly.collectAsState(initial = false)

    LaunchedEffect(initialAppId) {
        if (initialAppId != null) {
            selectedAppId = initialAppId
        }
    }
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
        context.vibrate()
        val appId = app.id ?: return

        if (viewModel.downloadingProgress.containsKey(appId)) {
            viewModel.cancelDownload(appId)
            return
        }

        val apkFile = viewModel.getApkFile(app.title)
        val pkg = app.packageName ?: ""
        
        // Проверяем существующий файл
        if (apkFile.exists()) {
            val fileVersion = UpdateInstaller.getApkVersionCode(context, apkFile)
            if (fileVersion == null || fileVersion < app.versionCode) {
                // Файл битый или версия старая - удаляем
                apkFile.delete()
            } else {
                // Файл актуальный - устанавливаем
                UpdateInstaller.installApk(context, apkFile)
                return
            }
        }

        if (appDownloadWifiOnly && !isWifiConnected(context)) {
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
                context.vibrate()
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
        val adaptiveInfo = currentWindowAdaptiveInfo()
        val navSuiteType = NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(adaptiveInfo)
        
        // Скрываем навигацию на Compact экранах при просмотре деталей
        val isDetailsOpen = selectedAppId != null
        val shouldShowNav = !isDetailsOpen || windowWidthSizeClass != WindowWidthSizeClass.Compact

        val layoutType = if (shouldShowNav) navSuiteType else NavigationSuiteType.None

        NavigationSuiteScaffoldLayout(
            navigationSuite = {
                if (layoutType == NavigationSuiteType.NavigationRail) {
                    NavigationRail(
                        modifier = Modifier.fillMaxHeight(),
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        header = {
                            Spacer(Modifier.height(44.dp)) // Material 3 Expressive top margin
                            Icon(
                                imageVector = Icons.Default.Apps,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp).padding(4.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(36.dp))
                        }
                    ) {
                        AppDestinations.entries.forEach { dest ->
                            NavigationRailItem(
                                icon = { Icon(dest.icon, stringResource(dest.labelRes)) },
                                label = { Text(stringResource(dest.labelRes), style = MaterialTheme.typography.labelMedium) },
                                selected = dest == currentDestination,
                                alwaysShowLabel = true,
                                onClick = {
                                    if (currentDestination != dest || selectedAppId != null) {
                                        context.vibrate()
                                        currentDestination = dest
                                        selectedAppId = null
                                    }
                                }
                            )
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                } else if (layoutType != NavigationSuiteType.None) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        AppDestinations.entries.forEach { dest ->
                            NavigationBarItem(
                                icon = { Icon(dest.icon, stringResource(dest.labelRes)) },
                                label = { Text(stringResource(dest.labelRes), style = MaterialTheme.typography.labelMedium) },
                                selected = dest == currentDestination,
                                alwaysShowLabel = true,
                                onClick = {
                                    if (currentDestination != dest || selectedAppId != null) {
                                        context.vibrate()
                                        currentDestination = dest
                                        selectedAppId = null
                                    }
                                }
                            )
                        }
                    }
                }
            }
        ) {
            val app = viewModel.appsFromCloud.find { it.id == selectedAppId }
            
            if (app != null) {
                val pkg = app.packageName
                val isInstalled = pkg?.let { viewModel.installedApps.containsKey(it) } ?: false
                val currentVersion = pkg?.let { viewModel.installedApps[it] } ?: 0
                val isUpdate = isInstalled && app.versionCode > currentVersion
                val progress = viewModel.downloadingProgress[app.id]

                AppDetailScreen(
                    app = app,
                    isFavorite = viewModel.favorites.contains(app.id.toString()),
                    isInstalled = isInstalled,
                    needsUpdate = isUpdate,
                    hasApk = viewModel.appsWithApk.contains(app.id),
                    isDownloading = progress != null,
                    downloadProgress = progress ?: 0f,
                    windowWidthSizeClass = windowWidthSizeClass,
                    onBack = {
                        context.vibrate()
                        selectedAppId = null
                    },
                    onToggleFavorite = {
                        context.vibrate()
                        viewModel.toggleFavorite(app)
                    },
                    onInstall = { handleInstallClick(app) },
                    onDeleteApk = { viewModel.deleteApk(app) }
                )
            } else if (selectedAppId != null && viewModel.isLoading) {
                // Если ID задан, но данные еще грузятся - ждем
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (viewModel.isLoading && viewModel.appsFromCloud.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                if (currentDestination == AppDestinations.HOME) {
                    HomeScreen(
                        apps = viewModel.appsFromCloud,
                        categories = viewModel.categories,
                        windowWidthSizeClass = windowWidthSizeClass,
                        onAppClick = { appItem ->
                            context.vibrate()
                            selectedAppId = appItem.id
                        },
                        onSearchClick = {
                            currentDestination = AppDestinations.APPS
                        },
                        onProfileClick = {
                            context.vibrate()
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
                        categories = if (currentDestination == AppDestinations.GAMES) {
                            viewModel.categories.filter { it == "Игры" || it == "Games" } // Simplifying for now, or just pass all
                        } else {
                            viewModel.categories.filter { it != "Игры" && it != "Games" }
                        },
                        isGamesTab = currentDestination == AppDestinations.GAMES,
                        windowWidthSizeClass = windowWidthSizeClass,
                        onAppClick = { appItem ->
                            context.vibrate()
                            selectedAppId = appItem.id
                        },
                        onInstallClick = { appItem -> handleInstallClick(appItem) },
                        installedApps = viewModel.installedApps,
                        appsWithApk = viewModel.appsWithApk.toSet(),
                        downloadingIds = viewModel.downloadingProgress,
                        onProfileClick = {
                            context.vibrate()
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
                modifier = if (windowWidthSizeClass != WindowWidthSizeClass.Compact) {
                    Modifier.widthIn(max = 600.dp).fillMaxWidth()
                } else {
                    Modifier.fillMaxSize()
                }
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
                            }
                        },
                        onBack = { showAutoUpdateSettings = false }
                    )
                } else {
                    val currentThemeMode by settingsRepository.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
                    ProfileScreen(
                        currentThemeMode = currentThemeMode,
                        onThemeChange = { scope.launch { settingsRepository.setThemeMode(it) } },
                        onAutoUpdateSettingsClick = { showAutoUpdateSettings = true },
                        installedCount = viewModel.installedApps.size,
                        isCheckingUpdate = updateViewModel.isChecking,
                        updateInfo = updateViewModel.updateInfo,
                        windowWidthSizeClass = windowWidthSizeClass,
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
