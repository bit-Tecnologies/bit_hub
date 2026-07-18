package com.bit.bithub.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bit.bithub.data.App

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeCarousel(
    featuredApps: List<App>,
    pagerState: PagerState,
    onAppClick: (App) -> Unit
) {
    HorizontalPager(
        state = pagerState,
        contentPadding = PaddingValues(horizontal = 16.dp),
        pageSpacing = 12.dp,
        modifier = Modifier.padding(top = 8.dp)
    ) { page ->
        val app = featuredApps[page]
        PromoBanner(
            title = app.title,
            subtitle = app.developer,
            imageUrl = app.iconUrl?.replace("200/200", "800/400") ?: "",
            onClick = { onAppClick(app) }
        )
    }
}

@Composable
fun CategoriesSection(
    categories: List<Pair<String, String>>,
    onCategoryClick: (String) -> Unit
) {
    if (categories.isEmpty()) return

    Column {
        Text(
            "Категории",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 12.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(categories) { (icon, name) ->
                CategoryTile(name, icon, MaterialTheme.colorScheme.primary) {
                    onCategoryClick(name)
                }
            }
        }
    }
}

@Composable
fun WideAppSection(
    title: String,
    apps: List<App>,
    onAppClick: (App) -> Unit
) {
    if (apps.isEmpty()) return

    Column {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 12.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(apps) { app ->
                WideAppCard(app) { onAppClick(app) }
            }
        }
    }
}

@Composable
fun AppSection(
    title: String,
    apps: List<App>,
    onAppClick: (App) -> Unit
) {
    if (apps.isEmpty()) return

    Column {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 12.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(apps) { app ->
                FeaturedAppCard(app) { onAppClick(app) }
            }
        }
    }
}
