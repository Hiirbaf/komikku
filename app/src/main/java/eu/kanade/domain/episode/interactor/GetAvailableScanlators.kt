package eu.kanade.domain.episode.interactor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import tachiyomi.domain.episode.repository.EpisodeRepository

class GetAvailableScanlators(
    private val repository: EpisodeRepository,
) {

    private fun List<String>.cleanupAvailableScanlators(): Set<String> {
        return mapNotNull { it.ifBlank { null } }.toSet()
    }

    suspend fun await(mangaId: Long): Set<String> {
        return repository.getScanlatorsByAnimeId(mangaId)
            .cleanupAvailableScanlators()
    }

    fun subscribe(mangaId: Long): Flow<Set<String>> {
        return repository.getScanlatorsByAnimeIdAsFlow(mangaId)
            .map { it.cleanupAvailableScanlators() }
    }

    // SY -->
    suspend fun awaitMerge(mangaId: Long): Set<String> {
        return repository.getScanlatorsByMergeId(mangaId)
            .cleanupAvailableScanlators()
    }

    fun subscribeMerge(mangaId: Long): Flow<Set<String>> {
        return repository.getScanlatorsByMergeIdAsFlow(mangaId)
            .map { it.cleanupAvailableScanlators() }
    }
    // SY <--
}
