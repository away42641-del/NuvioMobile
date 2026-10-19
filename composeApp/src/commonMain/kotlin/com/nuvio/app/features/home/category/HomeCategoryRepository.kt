package com.nuvio.app.features.home.category

import com.nuvio.app.core.storage.ProfileScopedKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class HomeCategoryEntry(
    val category: HomeCategory,
    val enabled: Boolean = true,
)

data class HomeCategoryUiState(
    val entries: List<HomeCategoryEntry> = HomeCategory.entries.map { HomeCategoryEntry(it) },
    val selected: HomeCategory = HomeCategory.Discover,
) {
    /** Enabled categories, in the user's chosen order. */
    val visible: List<HomeCategory>
        get() = entries.filter { it.enabled }.map { it.category }

    /** The selected category, or the first visible one if the selection was hidden. */
    val effectiveSelected: HomeCategory
        get() = if (selected in visible) selected else visible.firstOrNull() ?: HomeCategory.Discover
}

/**
 * Holds which categories appear in the Home category bar, their order, and the current selection.
 * Visibility/order persist per profile; the selection itself resets on app start.
 */
object HomeCategoryRepository {
    private val _uiState = MutableStateFlow(HomeCategoryUiState())
    val uiState: StateFlow<HomeCategoryUiState> = _uiState.asStateFlow()

    private var loadedScopeKey: String? = null

    fun ensureLoaded() {
        val scopeKey = ProfileScopedKey.of("home_category_scope")
        if (loadedScopeKey == scopeKey) return
        loadedScopeKey = scopeKey
        val entries = decode(HomeCategorySettingsStorage.loadPayload())
        _uiState.update { it.copy(entries = entries) }
    }

    fun select(category: HomeCategory) {
        _uiState.update { it.copy(selected = category) }
    }

    fun setEnabled(category: HomeCategory, enabled: Boolean) {
        _uiState.update { state ->
            val next = state.entries.map { if (it.category == category) it.copy(enabled = enabled) else it }
            // Always keep at least one category visible.
            if (next.none { it.enabled }) state else state.copy(entries = next)
        }
        persist()
    }

    /** Moves [category] by [delta] positions (-1 = earlier, +1 = later). */
    fun move(category: HomeCategory, delta: Int) {
        _uiState.update { state ->
            val index = state.entries.indexOfFirst { it.category == category }
            val target = index + delta
            if (index < 0 || target !in state.entries.indices) return@update state
            val mutable = state.entries.toMutableList()
            val item = mutable.removeAt(index)
            mutable.add(target, item)
            state.copy(entries = mutable)
        }
        persist()
    }

    fun reset() {
        _uiState.update { it.copy(entries = HomeCategory.entries.map { c -> HomeCategoryEntry(c) }) }
        persist()
    }

    private fun persist() {
        HomeCategorySettingsStorage.savePayload(encode(_uiState.value.entries))
    }

    // Format: "discover:1,movies:1,tv_shows:0,..." (order = list order, 1/0 = enabled)
    private fun encode(entries: List<HomeCategoryEntry>): String =
        entries.joinToString(",") { "${it.category.key}:${if (it.enabled) 1 else 0}" }

    private fun decode(payload: String?): List<HomeCategoryEntry> {
        val parsed = payload.orEmpty()
            .split(",")
            .mapNotNull { token ->
                val parts = token.trim().split(":")
                val category = HomeCategory.fromKey(parts.getOrNull(0)) ?: return@mapNotNull null
                HomeCategoryEntry(category, enabled = parts.getOrNull(1) != "0")
            }
            .distinctBy { it.category }
        // Append any categories added in a newer version that aren't stored yet.
        val missing = HomeCategory.entries
            .filter { c -> parsed.none { it.category == c } }
            .map { HomeCategoryEntry(it) }
        val all = parsed + missing
        return if (all.none { it.enabled }) all.map { it.copy(enabled = true) } else all
    }
}
