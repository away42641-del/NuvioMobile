package com.nuvio.app.features.home.category

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.ui.Chip
import com.nuvio.app.features.catalog.CatalogTarget
import com.nuvio.app.features.home.HomeCatalogSection
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.home.canOpenCatalog
import com.nuvio.app.features.home.components.HomeCatalogRowSection
import com.nuvio.app.features.home.components.HomeEmptyStateCard

private const val CATEGORY_PREVIEW_LIMIT = 18

internal fun LazyListScope.homeCategoryRows(
    page: HomeCategoryPage,
    showEmptyState: Boolean,
    sectionPadding: Dp,
    watchedKeys: Set<String>,
    fullyWatchedSeriesKeys: Set<String>,
    onCatalogClick: ((HomeCatalogSection) -> Unit)?,
    onPosterClick: ((MetaPreview) -> Unit)?,
    onPosterLongClick: ((MetaPreview) -> Unit)?,
) {
    if (page.rows.isEmpty()) {
        if (showEmptyState) {
            item(key = "category_empty_${page.category.key}", contentType = "empty") {
                HomeEmptyStateCard(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    title = page.emptyTitle,
                    message = page.emptyMessage,
                )
            }
        }
        return
    }

    page.rows.forEach { row ->
        when (row) {
            is HomeCategoryRow.Shelf -> item(key = row.key, contentType = "catalog") {
                val section = row.section
                HomeCatalogRowSection(
                    section = section,
                    entries = section.items.take(CATEGORY_PREVIEW_LIMIT),
                    modifier = Modifier.padding(bottom = 12.dp),
                    sectionPadding = sectionPadding,
                    onViewAllClick = if (!row.isDerived && section.canOpenCatalog(CATEGORY_PREVIEW_LIMIT)) {
                        onCatalogClick?.let { { it(section) } }
                    } else {
                        null
                    },
                    watchedKeys = watchedKeys,
                    fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
                    onPosterClick = onPosterClick,
                    onPosterLongClick = onPosterLongClick,
                )
            }

            is HomeCategoryRow.Genres -> item(key = row.key, contentType = "genres") {
                HomeGenreChipsRow(
                    title = row.title,
                    genres = row.genres,
                    sectionPadding = sectionPadding,
                    onGenreClick = { genre ->
                        val addonTarget = row.source.target as? CatalogTarget.Addon
                        if (addonTarget != null) {
                            onCatalogClick?.invoke(
                                row.source.copy(
                                    key = "${row.source.key}:genre:$genre",
                                    title = genre,
                                    target = addonTarget.copy(genre = genre),
                                ),
                            )
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun HomeGenreChipsRow(
    title: String,
    genres: List<String>,
    sectionPadding: Dp,
    onGenreClick: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = sectionPadding, vertical = 8.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = sectionPadding),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(genres, key = { it }) { genre ->
                Chip(label = genre, selected = false, onClick = { onGenreClick(genre) })
            }
        }
    }
}
