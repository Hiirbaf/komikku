package tachiyomi.domain.chapter.interactor

import exh.source.MERGED_SOURCE_ID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.manga.interactor.GetMergedReferencesById
import tachiyomi.domain.manga.model.MergedMangaReference
import tachiyomi.domain.chapter.model.Episode
import tachiyomi.domain.chapter.repository.EpisodeRepository

class GetMergedChaptersByMangaId(
    private val episodeRepository: EpisodeRepository,
    private val getMergedReferencesById: GetMergedReferencesById,
) {

    suspend fun await(
        mangaId: Long,
        dedupe: Boolean = true,
        applyScanlatorFilter: Boolean = false,
    ): List<Episode> {
        return transformMergedChapters(
            getMergedReferencesById.await(mangaId),
            getFromDatabase(mangaId, applyScanlatorFilter),
            dedupe,
        )
    }

    suspend fun subscribe(
        mangaId: Long,
        dedupe: Boolean = true,
        applyScanlatorFilter: Boolean = false,
    ): Flow<List<Episode>> {
        return try {
            episodeRepository.getMergedEpisodeByAnimeIdAsFlow(mangaId, applyScanlatorFilter)
                .combine(getMergedReferencesById.subscribe(mangaId)) { chapters, references ->
                    transformMergedChapters(references, chapters, dedupe)
                }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            flowOf(emptyList())
        }
    }

    private suspend fun getFromDatabase(
        mangaId: Long,
        applyScanlatorFilter: Boolean = false,
    ): List<Episode> {
        return try {
            episodeRepository.getMergedEpisodeByAnimeId(mangaId, applyScanlatorFilter)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            emptyList()
        }
    }

    private fun transformMergedChapters(
        mangaReferences: List<MergedMangaReference>,
        episodeList: List<Episode>,
        dedupe: Boolean,
    ): List<Episode> {
        return if (dedupe) dedupeChapterList(mangaReferences, episodeList) else episodeList
    }

    private fun dedupeChapterList(
        mangaReferences: List<MergedMangaReference>,
        episodeList: List<Episode>,
    ): List<Episode> {
        return when (mangaReferences.firstOrNull { it.mangaSourceId == MERGED_SOURCE_ID }?.chapterSortMode) {
            MergedMangaReference.EPISODE_SORT_NO_DEDUPE, MergedMangaReference.EPISODE_SORT_NONE -> episodeList
            MergedMangaReference.EPISODE_SORT_PRIORITY -> dedupeByPriority(mangaReferences, episodeList)
            MergedMangaReference.EPISODE_SORT_MOST_EPISODES -> {
                findSourceWithMostChapters(episodeList)?.let { mangaId ->
                    episodeList.filter { it.animeId == mangaId }
                } ?: episodeList
            }
            MergedMangaReference.EPISODE_SORT_HIGHEST_EPISODE_NUMBER -> {
                findSourceWithHighestChapterNumber(episodeList)?.let { mangaId ->
                    episodeList.filter { it.animeId == mangaId }
                } ?: episodeList
            }
            else -> episodeList
        }
    }

    private fun findSourceWithMostChapters(episodeList: List<Episode>): Long? {
        return episodeList.groupBy { it.animeId }.maxByOrNull { it.value.size }?.key
    }

    private fun findSourceWithHighestChapterNumber(episodeList: List<Episode>): Long? {
        return episodeList.maxByOrNull { it.episodeNumber }?.animeId
    }

    private fun dedupeByPriority(
        mangaReferences: List<MergedMangaReference>,
        episodeList: List<Episode>,
    ): List<Episode> {
        val sortedEpisodeList = mutableListOf<Episode>()

        var existingChapterIndex: Int
        episodeList.groupBy { it.animeId }
            .entries
            .sortedBy { (mangaId) ->
                mangaReferences.find { it.mangaId == mangaId }?.chapterPriority ?: Int.MAX_VALUE
            }
            .forEach { (_, chapters) ->
                existingChapterIndex = -1
                chapters.forEach { chapter ->
                    val oldChapterIndex = existingChapterIndex
                    if (chapter.isRecognizedNumber) {
                        existingChapterIndex = sortedEpisodeList.indexOfFirst {
                            // check if the chapter is not already there
                            it.isRecognizedNumber &&
                                it.episodeNumber == chapter.episodeNumber &&
                                // allow multiple chapters of the same number from the same source
                                it.animeId != chapter.animeId
                        }
                        if (existingChapterIndex == -1) {
                            sortedEpisodeList.add(oldChapterIndex + 1, chapter)
                            existingChapterIndex = oldChapterIndex + 1
                        }
                    } else {
                        sortedEpisodeList.add(oldChapterIndex + 1, chapter)
                        existingChapterIndex = oldChapterIndex + 1
                    }
                }
            }

        return sortedEpisodeList.mapIndexed { index, chapter ->
            chapter.copy(sourceOrder = index.toLong())
        }
    }
}
