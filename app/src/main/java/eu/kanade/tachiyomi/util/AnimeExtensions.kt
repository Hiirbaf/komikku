package eu.kanade.tachiyomi.util

import eu.kanade.domain.anime.interactor.UpdateAnime
import eu.kanade.domain.anime.model.hasCustomCover
import eu.kanade.domain.anime.model.toSAnime
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.source.model.SAnime
import tachiyomi.domain.manga.model.Manga
import tachiyomi.source.local.image.LocalCoverManager
import tachiyomi.source.local.isLocal
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.InputStream
import java.time.Instant

/**
 * Call before updating [Manga.thumbnailUrl] to ensure old cover can be cleared from cache
 */
fun Manga.prepUpdateCover(coverCache: CoverCache, remoteManga: SAnime, refreshSameUrl: Boolean): Manga {
    // Never refresh covers if the new url is null, as the current url has possibly become invalid
    val newUrl = remoteManga.thumbnail_url ?: return this

    // Never refresh covers if the url is empty to avoid "losing" existing covers
    if (newUrl.isEmpty()) return this

    if (!refreshSameUrl && thumbnailUrl == newUrl) return this

    return when {
        isLocal() -> {
            this.copy(coverLastModified = Instant.now().toEpochMilli())
        }
        hasCustomCover(coverCache) -> {
            coverCache.deleteFromCache(this, false)
            this
        }
        else -> {
            coverCache.deleteFromCache(this, false)
            this.copy(coverLastModified = Instant.now().toEpochMilli())
        }
    }
}

fun Manga.removeCovers(coverCache: CoverCache = Injekt.get()): Manga {
    if (isLocal()) return this
    return if (coverCache.deleteFromCache(this, true) > 0) {
        return copy(coverLastModified = Instant.now().toEpochMilli())
    } else {
        this
    }
}

suspend fun Manga.editCover(
    coverManager: LocalCoverManager,
    stream: InputStream,
    updateAnime: UpdateAnime = Injekt.get(),
    coverCache: CoverCache = Injekt.get(),
) {
    if (isLocal()) {
        coverManager.update(toSAnime(), stream)
        updateAnime.awaitUpdateCoverLastModified(id)
    } else if (favorite) {
        coverCache.setCustomCoverToCache(this, stream)
        updateAnime.awaitUpdateCoverLastModified(id)
    }
}
