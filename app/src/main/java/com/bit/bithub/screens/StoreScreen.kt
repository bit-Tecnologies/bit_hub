package com.bit.bithub.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bit.bithub.components.*
import com.bit.bithub.data.App
import kotlinx.coroutines.launch

val appCategories = listOf(
    "\uD83D\uDCDD" to "Инструменты",
    "\uD83D\uDCF1" to "Связь",
    "\uD83D\uDE80" to "Развлечения",
    "\uD83D\uDCF7" to "Фото",
    "\uD83C\uDFA7" to "Музыка",
    "\uD83D\uDCB0" to "Финансы",
)

val gameCategories = listOf(
    "\uD83D\uDCA5" to "Экшен",
    "\uD83D\uDDE1️" to "RPG",
    "\uD83E\uDDE0" to "Головоломки",
    "\uD83C\uDFCE️" to "Гонки",
    "\uD83D\uDCAA" to "Спорт",
    "\uD83D\uDCBB" to "Симуляторы"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun StoreScreen(
    apps: List<App>,
    onAppClick: (App) -> Unit,
    onInstallClick: (App) -> Unit,
    installedApps: Map<String, Int>,
    appsWithApk: Set<Long>,
    downloadingIds: Map<Long, Float>,
    onProfileClick: () -> Unit,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    isGamesTab: Boolean = false,
    error: String? = null,
    onRetry: () -> Unit = {},
) {
    var searchQuery by remember { mutableStateOf(value = "") }
    val filteredApps = apps.filter { it.title.contains(searchQuery, ignoreCase = true) }
    val featured = apps.take(5)
    val recommended = apps.reversed().take(5)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            StoreSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onProfileClick = onProfileClick
            )
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if ((error != null) && apps.isEmpty()) {
                ErrorState(error, onRetry)
            } else {
                StoreContent(
                    searchQuery = searchQuery,
                    featuredApps = featured,
                    recommended = recommended,
                    filteredApps = filteredApps,
                    installedApps = installedApps,
                    appsWithApk = appsWithApk,
                    downloadingIds = downloadingIds,
                    onAppClick = onAppClick,
                    onInstallClick = onInstallClick,
                    isGamesTab = isGamesTab,
                    onCategoryClick = {
                        scope.launch { snackbarHostState.showSnackbar("Фильтр по категориям скоро появится...") }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StoreSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onProfileClick: () -> Unit
) {
    SearchBar(
        inputField = {
            SearchBarDefaults.InputField(
                query = query,
                onQueryChange = onQueryChange,
                onSearch = {},
                expanded = false,
                onExpandedChange = {},
                placeholder = { Text("Поиск игр и приложений") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = onProfileClick) {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = "Профиль",
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
            )
        },
        expanded = false,
        onExpandedChange = {},
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {}
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StoreContent(
    searchQuery: String,
    featuredApps: List<App>,
    recommended: List<App>,
    filteredApps: List<App>,
    installedApps: Map<String, Int>,
    appsWithApk: Set<Long>,
    downloadingIds: Map<Long, Float>,
    onAppClick: (App) -> Unit,
    onInstallClick: (App) -> Unit,
    isGamesTab: Boolean,
    onCategoryClick: (String) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (searchQuery.isEmpty()) {
            item {
                val pagerState = rememberPagerState(pageCount = { featuredApps.size })
                HomeCarousel(featuredApps, pagerState, onAppClick)
            }

            item {
                CategoriesSection(
                    categories = if (isGamesTab) gameCategories else appCategories,
                    onCategoryClick = onCategoryClick,
                )
            }

            item {
                WideAppSection(
                    title = if (isGamesTab) "Популярно сейчас" else "Рекомендуем вам",
                    apps = recommended,
                    onAppClick = onAppClick,
                )
            }

            item {
                AppSection(
                    title = if (isGamesTab) "Топ бесплатных игр" else "Топ приложений",
                    apps = featuredApps.reversed(),
                    onAppClick = onAppClick,
                )
            }

            item { Spacer(Modifier.height(24.dp)) }
        } else {
            item {
                Text(
                    "Результаты поиска",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
            }

            if (filteredApps.isEmpty()) {
                item {
                    Text(
                        "Ничего не найдено",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            items(filteredApps) { app ->
                val pkg = app.packageName
                val installedVersion = pkg?.let { installedApps[it] }
                val needsUpdate = (installedVersion != null) && (app.versionCode > installedVersion)

                AppListItem(
                    app = app,
                    isInstalled = installedVersion != null,
                    needsUpdate = needsUpdate,
                    hasApk = app.id in appsWithApk,
                    downloadProgress = app.id?.let { downloadingIds[it] },
                    onInstallClick = { onInstallClick(app) },
                    onClick = { onAppClick(app) },
                )
            }
        }
    }
}

@Composable
private fun ErrorState(error: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = if (error.contains("интернет", ignoreCase = true)) Icons.Default.CloudOff else Icons.Default.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(16.dp))
            Text(text = error, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            Button(onClick = onRetry) { Text("Повторить") }
        }
    }
}
