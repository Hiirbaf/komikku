package tachiyomi.data.anime

import kotlinx.coroutines.flow.Flow
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MergeMangaSettingsUpdate
import tachiyomi.domain.manga.model.MergedMangaReference
import tachiyomi.domain.manga.repository.MangaMergeRepository

class MangaMergeRepositoryImpl(
    private val handler: DatabaseHandler,
) : MangaMergeRepository {

    override suspend fun getMergedManga(): List<Manga> {
        return handler.awaitList { mergedQueries.selectAllMergedAnimes(MangaMapper::mapManga) }
    }

    override suspend fun subscribeMergedManga(): Flow<List<Manga>> {
        return handler.subscribeToList { mergedQueries.selectAllMergedAnimes(MangaMapper::mapManga) }
    }

    override suspend fun getMergedMangaById(id: Long): List<Manga> {
        return handler.awaitList { mergedQueries.selectMergedAnimesById(id, MangaMapper::mapManga) }
    }

    override suspend fun subscribeMergedMangaById(id: Long): Flow<List<Manga>> {
        return handler.subscribeToList { mergedQueries.selectMergedAnimesById(id, MangaMapper::mapManga) }
    }

    override suspend fun getReferencesById(id: Long): List<MergedMangaReference> {
        return handler.awaitList { mergedQueries.selectByMergeId(id, MergedMangaMapper::map) }
    }

    override suspend fun subscribeReferencesById(id: Long): Flow<List<MergedMangaReference>> {
        return handler.subscribeToList { mergedQueries.selectByMergeId(id, MergedMangaMapper::map) }
    }

    override suspend fun updateSettings(update: MergeMangaSettingsUpdate): Boolean {
        return try {
            partialUpdate(update)
            true
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            false
        }
    }

    override suspend fun updateAllSettings(values: List<MergeMangaSettingsUpdate>): Boolean {
        return try {
            partialUpdate(*values.toTypedArray())
            true
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            false
        }
    }

    private suspend fun partialUpdate(vararg values: MergeMangaSettingsUpdate) {
        handler.await(inTransaction = true) {
            values.forEach { value ->
                mergedQueries.updateSettingsById(
                    id = value.id,
                    getEpisodeUpdates = value.getEpisodeUpdates,
                    downloadEpisodes = value.downloadEpisodes,
                    infoAnime = value.isInfoAnime,
                    episodePriority = value.episodePriority?.toLong(),
                    episodeSortMode = value.episodeSortMode?.toLong(),
                )
            }
        }
    }

    override suspend fun insert(reference: MergedMangaReference): Long? {
        return handler.awaitOneOrNullExecutable {
            mergedQueries.insert(
                infoAnime = reference.isInfoManga,
                getEpisodeUpdates = reference.getChapterUpdates,
                episodeSortMode = reference.chapterSortMode.toLong(),
                episodePriority = reference.chapterPriority.toLong(),
                downloadEpisodes = reference.downloadChapters,
                mergeId = reference.mergeId!!,
                mergeUrl = reference.mergeUrl,
                animeId = reference.mangaId,
                animeUrl = reference.mangaUrl,
                animeSource = reference.mangaSourceId,
            )
            mergedQueries.selectLastInsertedRowId()
        }
    }

    override suspend fun insertAll(references: List<MergedMangaReference>) {
        handler.await(true) {
            references.forEach { reference ->
                mergedQueries.insert(
                    infoAnime = reference.isInfoManga,
                    getEpisodeUpdates = reference.getChapterUpdates,
                    episodeSortMode = reference.chapterSortMode.toLong(),
                    episodePriority = reference.chapterPriority.toLong(),
                    downloadEpisodes = reference.downloadChapters,
                    mergeId = reference.mergeId!!,
                    mergeUrl = reference.mergeUrl,
                    animeId = reference.mangaId,
                    animeUrl = reference.mangaUrl,
                    animeSource = reference.mangaSourceId,
                )
            }
        }
    }

    override suspend fun deleteById(id: Long) {
        handler.await {
            mergedQueries.deleteById(id)
        }
    }

    override suspend fun deleteByMergeId(mergeId: Long) {
        handler.await {
            mergedQueries.deleteByMergeId(mergeId)
        }
    }

    override suspend fun getMergeMangaForDownloading(mergeId: Long): List<Manga> {
        return handler.awaitList { mergedQueries.selectMergedAnimesForDownloadingById(mergeId, MangaMapper::mapManga) }
    }
}
