package com.nuvio.app.features.home.category

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.ui.NuvioModalBottomSheet
import com.nuvio.app.core.ui.NuvioPrimaryButton
import com.nuvio.app.core.ui.dismissNuvioBottomSheet
import kotlinx.coroutines.launch
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.action_done
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeCategoryCustomizeSheet(
    entries: List<HomeCategoryEntry>,
    onToggle: (HomeCategory, Boolean) -> Unit,
    onMove: (HomeCategory, Int) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val dismiss: () -> Unit = {
        scope.launch { dismissNuvioBottomSheet(sheetState, onDismiss) }
    }
    val enabledCount = entries.count { it.enabled }

    NuvioModalBottomSheet(onDismissRequest = dismiss, sheetState = sheetState) {
        LazyColumn(Modifier.fillMaxWidth()) {
            item(key = "header") {
                Text(
                    text = "Customize categories",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                )
                Text(
                    text = "Choose which pages appear in the top bar and arrange their order.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp),
                )
            }
            itemsIndexed(entries, key = { _, entry -> entry.category.key }) { index, entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = entry.category.label,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = { onMove(entry.category, -1) },
                        enabled = index > 0,
                    ) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move ${entry.category.label} up")
                    }
                    IconButton(
                        onClick = { onMove(entry.category, 1) },
                        enabled = index < entries.lastIndex,
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move ${entry.category.label} down")
                    }
                    Switch(
                        checked = entry.enabled,
                        onCheckedChange = { onToggle(entry.category, it) },
                        enabled = !(entry.enabled && enabledCount == 1),
                    )
                }
            }
            item(key = "footer") {
                TextButton(
                    onClick = onReset,
                    modifier = Modifier.padding(horizontal = 12.dp),
                ) {
                    Text("Reset to default")
                }
                NuvioPrimaryButton(
                    text = stringResource(Res.string.action_done),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    onClick = dismiss,
                )
            }
        }
    }
}
