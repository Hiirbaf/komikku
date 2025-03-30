package tachiyomi.data.anime

import kotlinx.coroutines.flow.Flow
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.data.DatabaseHandler
import tachiyomi.data.StringListColumnAdapter
import tachiyomi.data.UpdateStrategyColumnAdapter
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaUpdate
import tachiyomi.domain.manga.repository.AnimeRepository
import tachiyomi.domain.library.model.LibraryAnime
import java.time.LocalDate
import java.time.ZoneId

class AnimeRepositoryImpl(
    private val handler: DatabaseHandler,
) : AnimeRepository {

    override suspend fun getAnimeById(id: Long): Manga {
        return handler.awaitOne { animesQueries.getAnimeById(id, AnimeMapper::mapAnime) }
    }

    override suspend fun getAnimeByIdAsFlow(id: Long): Flow<Manga> {
        return handler.subscribeToOne { animesQueries.getAnimeById(id, AnimeMapper::mapAnime) }
    }

    override suspend fun getAnimeByUrlAndSourceId(url: String, sourceId: Long): Manga? {
        return handler.awaitOneOrNull {
            animesQueries.getAnimeByUrlAndSource(
                url,
                sourceId,
                AnimeMapper::mapAnime,
            )
        }
    }

    override fun getAnimeByUrlAndSourceIdAsFlow(url: String, sourceId: Long): Flow<Manga?> {
        return handler.subscribeToOneOrNull {
            animesQueries.getAnimeByUrlAndSource(
                url,
                sourceId,
                AnimeMapper::mapAnime,
            )
        }
    }

    override suspend fun getFavorites(): List<Manga> {
        return handler.awaitList { animesQueries.getFavorites(AnimeMapper::mapAnime) }
    }

    override suspend fun getSeenAnimeNotInLibrary(): List<Manga> {
        return handler.awaitList { animesQueries.getSeenAnimeNotInLibrary(AnimeMapper::mapAnime) }
    }

    override suspend fun getLibraryAnime(): List<LibraryAnime> {
        return handler.awaitList { libraryViewQueries.library(AnimeMapper::mapLibraryAnime) }
    }

    override fun getLibraryAnimeAsFlow(): Flow<List<LibraryAnime>> {
        return handler.subscribeToList { libraryViewQueries.library(AnimeMapper::mapLibraryAnime) }
    }

    override fun getFavoritesBySourceId(sourceId: Long): Flow<List<Manga>> {
        return handler.subscribeToList { animesQueries.getFavoriteBySourceId(sourceId, AnimeMapper::mapAnime) }
    }

    override suspend fun getDuplicateLibraryAnime(id: Long, title: String): List<Manga> {
        return handler.awaitList {
            animesQueries.getDuplicateLibraryAnime(title, id, AnimeMapper::mapAnime)
        }
    }

    override suspend fun getUpcomingAnime(statuses: Set<Long>): Flow<List<Manga>> {
        val epochMillis = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000
        return handler.subscribeToList {
            animesQueries.getUpcomingAnime(epochMillis, statuses, AnimeMapper::mapAnime)
        }
    }

    override suspend fun resetViewerFlags(): Boolean {
        return try {
            handler.await { animesQueries.resetViewerFlags() }
            true
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            false
        }
    }

    override suspend fun setAnimeCategories(animeId: Long, categoryIds: List<Long>) {
        handler.await(inTransaction = true) {
            animes_categoriesQueries.deleteAnimeCategoryByAnimeId(animeId)
            categoryIds.map { categoryId ->
                animes_categoriesQueries.insert(animeId, categoryId)
            }
        }
    }

    override suspend fun insert(manga: Manga): Long? {
        return handler.awaitOneOrNullExecutable(inTransaction = true) {
            // SY -->
            if (animesQueries.getIdByUrlAndSource(manga.url, manga.source).executeAsOneOrNull() != null) {
                return@awaitOneOrNullExecutable animesQueries.getIdByUrlAndSource(manga.url, manga.source)
            }
            // SY <--
            animesQueries.insert(
                source = manga.source,
                url = manga.url,
                artist = manga.artist,
                author = manga.author,
                description = manga.description,
                genre = manga.genre,
                title = manga.title,
                status = manga.status,
                thumbnailUrl = manga.thumbnailUrl,
                favorite = manga.favorite,
                lastUpdate = manga.lastUpdate,
                nextUpdate = manga.nextUpdate,
                calculateInterval = manga.fetchInterval.toLong(),
                initialized = manga.initialized,
                viewerFlags = manga.viewerFlags,
                episodeFlags = manga.chapterFlags,
                coverLastModified = manga.coverLastModified,
                dateAdded = manga.dateAdded,
                updateStrategy = manga.updateStrategy,
                version = manga.version,
            )
            animesQueries.selectLastInsertedRowId()
        }
    }

    override suspend fun update(update: MangaUpdate): Boolean {
        return try {
            partialUpdate(update)
            true
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            false
        }
    }

    override suspend fun updateAll(mangaUpdates: List<MangaUpdate>): Boolean {
        return try {
            partialUpdate(*mangaUpdates.toTypedArray())
            true
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            false
        }
    }

    private suspend fun partialUpdate(vararg mangaUpdates: MangaUpdate) {
        handler.await(inTransaction = true) {
            mangaUpdates.forEach { value ->
                animesQueries.update(
                    source = value.source,
                    url = value.url,
                    artist = value.artist,
                    author = value.author,
                    description = value.description,
                    genre = value.genre?.let(StringListColumnAdapter::encode),
                    title = value.title,
                    status = value.status,
                    thumbnailUrl = value.thumbnailUrl,
                    favorite = value.favorite,
                    lastUpdate = value.lastUpdate,
                    nextUpdate = value.nextUpdate,
                    calculateInterval = value.fetchInterval?.toLong(),
                    initialized = value.initialized,
                    viewer = value.viewerFlags,
                    episodeFlags = value.chapterFlags,
                    coverLastModified = value.coverLastModified,
                    dateAdded = value.dateAdded,
                    animeId = value.id,
                    updateStrategy = value.updateStrategy?.let(UpdateStrategyColumnAdapter::encode),
                    version = value.version,
                    isSyncing = 0,
                )
            }
        }
    }

    // SY -->
    override suspend fun getAnimeBySourceId(sourceId: Long): List<Manga> {
        return handler.awaitList { animesQueries.getBySource(sourceId, AnimeMapper::mapAnime) }
    }

    override suspend fun getAll(): List<Manga> {
        return handler.awaitList { animesQueries.getAll(AnimeMapper::mapAnime) }
    }

    override suspend fun deleteAnime(animeId: Long) {
        handler.await { animesQueries.deleteById(animeId) }
    }

    override suspend fun getSeenAnimeNotInLibraryView(): List<LibraryAnime> {
        return handler.awaitList { libraryViewQueries.seenAnimeNonLibrary(AnimeMapper::mapLibraryAnime) }
    }
    // SY <--
}
