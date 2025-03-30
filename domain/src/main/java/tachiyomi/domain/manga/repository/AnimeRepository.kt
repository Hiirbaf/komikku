package tachiyomi.domain.manga.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaUpdate
import tachiyomi.domain.library.model.LibraryAnime

interface AnimeRepository {

    suspend fun getAnimeById(id: Long): Manga

    suspend fun getAnimeByIdAsFlow(id: Long): Flow<Manga>

    suspend fun getAnimeByUrlAndSourceId(url: String, sourceId: Long): Manga?

    fun getAnimeByUrlAndSourceIdAsFlow(url: String, sourceId: Long): Flow<Manga?>

    suspend fun getFavorites(): List<Manga>

    suspend fun getSeenAnimeNotInLibrary(): List<Manga>

    suspend fun getLibraryAnime(): List<LibraryAnime>

    fun getLibraryAnimeAsFlow(): Flow<List<LibraryAnime>>

    fun getFavoritesBySourceId(sourceId: Long): Flow<List<Manga>>

    suspend fun getDuplicateLibraryAnime(id: Long, title: String): List<Manga>

    suspend fun getUpcomingAnime(statuses: Set<Long>): Flow<List<Manga>>

    suspend fun resetViewerFlags(): Boolean

    suspend fun setAnimeCategories(animeId: Long, categoryIds: List<Long>)

    suspend fun insert(manga: Manga): Long?

    suspend fun update(update: MangaUpdate): Boolean

    suspend fun updateAll(mangaUpdates: List<MangaUpdate>): Boolean

    // SY -->
    suspend fun getAnimeBySourceId(sourceId: Long): List<Manga>

    suspend fun getAll(): List<Manga>

    suspend fun deleteAnime(animeId: Long)

    suspend fun getSeenAnimeNotInLibraryView(): List<LibraryAnime>
    // SY <--
}
