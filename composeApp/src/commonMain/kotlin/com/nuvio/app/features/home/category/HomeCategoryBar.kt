package com.nuvio.app.features.home.category

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.ui.Chip
import com.nuvio.app.core.ui.nuvio

/** Height of the bar below the status bar; Home reserves this much space when there is no hero. */
internal val HOME_CATEGORY_BAR_HEIGHT: Dp = 56.dp

@Composable
internal fun HomeCategoryBar(
    categories: List<HomeCategory>,
    selected: HomeCategory,
    onSelect: (HomeCategory) -> Unit,
    onCustomizeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = MaterialTheme.nuvio.colors.background
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(background.copy(alpha = 0.92f), background.copy(alpha = 0f)),
                ),
            )
            .statusBarsPadding()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LazyRow(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items(categories, key = { it.key }) { category ->
                Chip(
                    label = category.label,
                    selected = category == selected,
                    onClick = { onSelect(category) },
                )
            }
        }
        IconButton(onClick = onCustomizeClick) {
            Icon(
                imageVector = Icons.Rounded.Tune,
                contentDescription = "Customize categories",
                tint = MaterialTheme.nuvio.colors.textMuted,
            )
        }
    }
}
