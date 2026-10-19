package com.nuvio.app.features.home.category

import com.nuvio.app.features.home.HomeCatalogSection
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.watchprogress.ContinueWatchingItem

/**
 * Top-level "dedicated home pages" shown in the category bar at the top of Home.
 * [Discover] is the original, unfiltered Home.
 */
enum class HomeCategory(val key: String, val label: String) {
    Discover("discover", "Discover"),
    Movies("movies", "Movies"),
    TvShows("tv_shows", "TV Shows"),
    Anime("anime", "Anime"),
    LiveTv("live_tv", "Live TV"),
    ;

    companion object {
        fun fromKey(key: String?): HomeCategory? = entries.firstOrNull { it.key == key }
    }
}

private val animeHints = listOf("anime", "kitsu", "anilist", "myanimelist")

private fun String.hasAnimeHint(): Boolean = animeHints.any { contains(it, ignoreCase = true) }

private fun categoryForType(type: String): HomeCategory? = when (type.trim().lowercase()) {
    "anime" -> HomeCategory.Anime
    "movie" -> HomeCategory.Movies
    "series" -> HomeCategory.TvShows
    "tv", "channel", "live" -> HomeCategory.LiveTv
    else -> null
}

/** Which dedicated page a catalog row belongs to (null = only shown in Discover). */
internal fun HomeCatalogSection.homeCategory(): HomeCategory? {
    if ("$key $title $addonName".hasAnimeHint()) return HomeCategory.Anime
    return categoryForType(target.contentType)
}

internal fun MetaPreview.homeCategory(): HomeCategory? {
    if (genres.any { it.equals("anime", ignoreCase = true) }) return HomeCategory.Anime
    return categoryForType(type)
}

/** Continue-watching / upcoming entries shown on each page. */
fun ContinueWatchingItem.isVisibleIn(category: HomeCategory): Boolean {
    val type = parentMetaType.trim().lowercase()
    return when (category) {
        HomeCategory.Discover -> true
        HomeCategory.Movies -> type == "movie"
        HomeCategory.TvShows -> type == "series"
        HomeCategory.Anime -> type == "anime"
        HomeCategory.LiveTv -> false
    }
}
