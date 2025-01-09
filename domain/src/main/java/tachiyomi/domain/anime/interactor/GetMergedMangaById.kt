package tachiyomi.domain.anime.interactor

import kotlinx.coroutines.flow.Flow
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.anime.model.Manga
import tachiyomi.domain.anime.repository.MangaMergeRepository

class GetMergedMangaById(
    private val mangaMergeRepository: MangaMergeRepository,
) {

    suspend fun await(id: Long): List<Manga> {
        return try {
            mangaMergeRepository.getMergedMangaById(id)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            emptyList()
        }
    }

    suspend fun subscribe(id: Long): Flow<List<Manga>> {
        return mangaMergeRepository.subscribeMergedMangaById(id)
    }
}
