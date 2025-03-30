package tachiyomi.domain.history.interactor

import exh.source.MERGED_SOURCE_ID
import tachiyomi.domain.anime.interactor.GetAnime
import tachiyomi.domain.episode.interactor.GetEpisodesByAnimeId
import tachiyomi.domain.episode.interactor.GetMergedEpisodesByAnimeId
import tachiyomi.domain.episode.model.Episode
import tachiyomi.domain.episode.service.getEpisodeSort
import tachiyomi.domain.history.repository.HistoryRepository
import kotlin.math.max

class GetNextEpisodes(
    private val getEpisodesByAnimeId: GetEpisodesByAnimeId,
    // SY -->
    private val getMergedEpisodesByAnimeId: GetMergedEpisodesByAnimeId,
    // SY <--
    private val getAnime: GetAnime,
    private val historyRepository: HistoryRepository,
) {

    suspend fun await(onlyUnseen: Boolean = true): List<Episode> {
        val history = historyRepository.getLastHistory() ?: return emptyList()
        return await(history.animeId, history.episodeId, onlyUnseen)
    }

    suspend fun await(animeId: Long, onlyUnseen: Boolean = true): List<Episode> {
        val anime = getAnime.await(animeId) ?: return emptyList()

        // SY -->
        if (anime.source == MERGED_SOURCE_ID) {
            val episodes = getMergedEpisodesByAnimeId.await(animeId, applyScanlatorFilter = true)
                .sortedWith(getEpisodeSort(anime, sortDescending = false))

            return if (onlyUnseen) {
                episodes.filterNot { it.seen }
            } else {
                episodes
            }
        }
        // SY <--

        val episodes = getEpisodesByAnimeId.await(animeId, applyScanlatorFilter = true)
            .sortedWith(getEpisodeSort(anime, sortDescending = false))

        return if (onlyUnseen) {
            episodes.filterNot { it.seen }
        } else {
            episodes
        }
    }

    suspend fun await(
        animeId: Long,
        fromEpisodeId: Long,
        onlyUnseen: Boolean = true,
    ): List<Episode> {
        val episodes = await(animeId, onlyUnseen)
        val currEpisodeIndex = episodes.indexOfFirst { it.id == fromEpisodeId }
        val nextEpisodes = episodes.subList(max(0, currEpisodeIndex), episodes.size)

        if (onlyUnseen) {
            return nextEpisodes
        }

        // The "next episode" is either:
        // - The current episode if it isn't completely seen
        // - The episodes after the current episode if the current one is completely seen
        val fromEpisode = episodes.getOrNull(currEpisodeIndex)
        return if (fromEpisode != null && !fromEpisode.seen) {
            nextEpisodes
        } else {
            nextEpisodes.drop(1)
        }
    }
}
