package com.bit.bithub.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import com.bit.bithub.components.*
import com.bit.bithub.data.App
import com.bit.bithub.data.MockData
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    apps: List<App>,
    categories: List<String>,
    onAppClick: (App) -> Unit,
    onSearchClick: () -> Unit,
    onProfileClick: () -> Unit,
    windowWidthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact
) {
    val featured = apps.take(3)
    val recommended = apps.asSequence().filter { it.category != "Игры" }.take(5).toList()
    val latestGames = apps.asSequence().filter { it.category == "Игры" }.toList().reversed().take(5)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("bit Hub", fontWeight = FontWeight.ExtraBold) },
                actions = {
                    IconButton(onClick = onSearchClick) { Icon(Icons.Default.Search, null) }
                    IconButton(onClick = onProfileClick) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = if (windowWidthSizeClass == WindowWidthSizeClass.Expanded) 1200.dp else Double.MAX_VALUE.dp)
                    .align(Alignment.TopCenter),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
            item {
                val pagerState = rememberPagerState { featured.size }
                HomeCarousel(featured, pagerState, onAppClick)
            }

            item {
                CategoriesSection(
                    categories = categories,
                    onCategoryClick = {
                        scope.launch { snackbarHostState.showSnackbar("Фильтр по категориям скоро появится...") }
                    },
                )
            }

            item {
                WideAppSection("Выбор редакции", recommended, onAppClick)
            }

            item {
                Column(modifier = Modifier.padding(vertical = 16.dp)) {
                    Text(
                        "События и новости",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(MockData.newsList.size) { index ->
                            val news = MockData.newsList[index]
                            NewsCard(
                                title = news.title,
                                description = news.description,
                                imageUrl = news.imageUrl,
                                onClick = news.onClick
                            )
                        }
                    }
                }
            }

            item {
                AppSection("Новинки гейминга", latestGames, onAppClick)
            }
        }
    }
}
}
