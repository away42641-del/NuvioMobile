package com.nuvio.app.features.home.category

import com.nuvio.app.features.catalog.CatalogTarget
import com.nuvio.app.features.home.HomeCatalogSection
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.home.stableKey

internal sealed interface HomeCategoryRow {
    val key: String

    /** A poster shelf. [section] is either a real addon catalog or a derived row. */
    data class Shelf(
        override val key: String,
        val section: HomeCatalogSection,
        val isDerived: Boolean,
    ) : HomeCategoryRow

    /** A row of genre chips. Tapping one opens [source] filtered by that genre. */
    data class Genres(
        override val key: String,
        val title: String,
        val genres: List<String>,
        val source: HomeCatalogSection,
    ) : HomeCategoryRow
}

internal data class HomeCategoryPage(
    val category: HomeCategory,
    val heroItems: List<MetaPreview>,
    val showHero: Boolean,
    val rows: List<HomeCategoryRow>,
    val emptyTitle: String,
    val emptyMessage: String,
)

private const val DERIVED_ROW_LIMIT = 20
private const val DERIVED_ROW_MIN_ITEMS = 5
private const val GENRE_CHIP_LIMIT = 12
private const val HERO_LIMIT = 8

private fun HomeCategory.priorityKeywords(): List<String> = when (this) {
    HomeCategory.Movies -> listOf("trending", "popular", "now playing", "new", "top", "rated", "upcoming")
    HomeCategory.TvShows -> listOf("trending", "popular", "airing", "new", "top", "rated")
    HomeCategory.Anime -> listOf("trending", "airing", "popular", "season", "top", "new")
    else -> emptyList()
}

private fun HomeCatalogSection.priority(keywords: List<String>): Int {
    val index = keywords.indexOfFirst { title.contains(it, ignoreCase = true) }
    return if (index < 0) Int.MAX_VALUE else index
}

private fun MetaPreview.ratingValue(): Double? = imdbRating?.trim()?.toDoubleOrNull()

private fun List<HomeCatalogSection>.anyTitleHas(vararg words: String): Boolean =
    any { section -> words.any { section.title.contains(it, ignoreCase = true) } }

private fun derivedSection(
    category: HomeCategory,
    id: String,
    title: String,
    items: List<MetaPreview>,
    template: HomeCatalogSection,
): HomeCatalogSection = HomeCatalogSection(
    key = "category_${category.key}_$id",
    title = title,
    subtitle = category.label,
    addonName = category.label,
    target = template.target,
    items = items,
    availableItemCount = items.size,
    hasMore = false,
)

/**
 * Builds the dedicated page for a category from the rows Home already loaded.
 * Each category gets its own row order, plus rows computed from the items themselves
 * (top rated, new releases, genres, recommendations) instead of just filtering Home.
 */
internal fun buildHomeCategoryPage(
    category: HomeCategory,
    sections: List<HomeCatalogSection>,
    heroItems: List<MetaPreview>,
    isWatched: (MetaPreview) -> Boolean,
): HomeCategoryPage {
    val own = sections.filter { it.homeCategory() == category && it.items.isNotEmpty() }

    if (category == HomeCategory.LiveTv) {
        return HomeCategoryPage(
            category = category,
            heroItems = emptyList(),
            showHero = false,
            rows = own.map { HomeCategoryRow.Shelf("cat_${category.key}_${it.key}", it, isDerived = false) },
            emptyTitle = "No live channels yet",
            emptyMessage = "Install an add-on that provides live TV catalogs and its channels will appear here, grouped by category.",
        )
    }

    val pool = own.flatMap { it.items }.distinctBy { it.stableKey() }
    val template = own.firstOrNull()

    val ordered = own.sortedBy { it.priority(category.priorityKeywords()) }
    val leading = ordered.take(2)
    val trailing = ordered.drop(2)

    fun shelf(section: HomeCatalogSection) =
        HomeCategoryRow.Shelf("cat_${category.key}_${section.key}", section, isDerived = false)

    val topRated: HomeCategoryRow? = if (template != null && !own.anyTitleHas("rated", "top")) {
        pool.filter { it.ratingValue() != null }
            .sortedByDescending { it.ratingValue() }
            .take(DERIVED_ROW_LIMIT)
            .takeIf { it.size >= DERIVED_ROW_MIN_ITEMS }
            ?.let { items ->
                val section = derivedSection(category, "top_rated", "Top Rated", items, template)
                HomeCategoryRow.Shelf(section.key, section, isDerived = true)
            }
    } else null

    val newest: HomeCategoryRow? = if (template != null && !own.anyTitleHas("new", "latest", "release")) {
        pool.filter { !it.rawReleaseDate.isNullOrBlank() }
            .sortedByDescending { it.rawReleaseDate }
            .take(DERIVED_ROW_LIMIT)
            .takeIf { it.size >= DERIVED_ROW_MIN_ITEMS }
            ?.let { items ->
                val title = when (category) {
                    HomeCategory.Movies -> "New Releases"
                    HomeCategory.TvShows -> "Recently Released"
                    else -> "Newest"
                }
                val section = derivedSection(category, "newest", title, items, template)
                HomeCategoryRow.Shelf(section.key, section, isDerived = true)
            }
    } else null

    val recommended: HomeCategoryRow? = if (template != null) {
        // Genre affinity comes from anything the user has watched across all loaded rows.
        val likedGenres = sections.asSequence()
            .flatMap { it.items.asSequence() }
            .distinctBy { it.stableKey() }
            .filter(isWatched)
            .flatMap { it.genres.asSequence() }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(4)
            .map { it.key }
            .toSet()
        if (likedGenres.isEmpty()) {
            null
        } else {
            pool.filter { !isWatched(it) }
                .map { it to it.genres.count { genre -> genre in likedGenres } }
                .filter { it.second > 0 }
                .sortedByDescending { it.second }
                .map { it.first }
                .take(DERIVED_ROW_LIMIT)
                .takeIf { it.size >= DERIVED_ROW_MIN_ITEMS }
                ?.let { items ->
                    val section = derivedSection(category, "recommended", "Recommended for You", items, template)
                    HomeCategoryRow.Shelf(section.key, section, isDerived = true)
                }
        }
    } else null

    val genreSource = own.firstOrNull { it.target is CatalogTarget.Addon }
    val genres: HomeCategoryRow? = genreSource?.let { source ->
        val top = pool.asSequence()
            .flatMap { it.genres.asSequence() }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(GENRE_CHIP_LIMIT)
            .map { it.key }
            .toList()
        if (top.size >= 3) HomeCategoryRow.Genres("cat_${category.key}_genres", "Genres", top, source) else null
    }

    val rows: List<HomeCategoryRow> = buildList {
        leading.forEach { add(shelf(it)) }
        when (category) {
            HomeCategory.Movies -> {
                topRated?.let { add(it) }
                newest?.let { add(it) }
                genres?.let { add(it) }
                recommended?.let { add(it) }
            }
            HomeCategory.TvShows -> {
                newest?.let { add(it) }
                recommended?.let { add(it) }
                genres?.let { add(it) }
                topRated?.let { add(it) }
            }
            else -> {
                topRated?.let { add(it) }
                genres?.let { add(it) }
                recommended?.let { add(it) }
            }
        }
        trailing.forEach { add(shelf(it)) }
    }

    val hero = heroItems.filter { it.homeCategory() == category }.ifEmpty {
        pool.filter { !it.banner.isNullOrBlank() || !it.poster.isNullOrBlank() }.take(HERO_LIMIT)
    }

    return HomeCategoryPage(
        category = category,
        heroItems = hero,
        showHero = true,
        rows = rows,
        emptyTitle = "Nothing here yet",
        emptyMessage = "Your installed add-ons don't provide any ${category.label} catalogs. Add one in Settings to fill this page.",
    )
}
