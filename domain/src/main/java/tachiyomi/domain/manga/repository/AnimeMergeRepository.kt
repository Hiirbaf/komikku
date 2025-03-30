package tachiyomi.domain.manga.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MergeAnimeSettingsUpdate
import tachiyomi.domain.manga.model.MergedAnimeReference

interface AnimeMergeRepository {
    suspend fun getMergedAnime(): List<Manga>

    suspend fun subscribeMergedAnime(): Flow<List<Manga>>

    suspend fun getMergedAnimeById(id: Long): List<Manga>

    suspend fun subscribeMergedAnimeById(id: Long): Flow<List<Manga>>

    suspend fun getReferencesById(id: Long): List<MergedAnimeReference>

    suspend fun subscribeReferencesById(id: Long): Flow<List<MergedAnimeReference>>

    suspend fun updateSettings(update: MergeAnimeSettingsUpdate): Boolean

    suspend fun updateAllSettings(values: List<MergeAnimeSettingsUpdate>): Boolean

    suspend fun insert(reference: MergedAnimeReference): Long?

    suspend fun insertAll(references: List<MergedAnimeReference>)

    suspend fun deleteById(id: Long)

    suspend fun deleteByMergeId(mergeId: Long)

    suspend fun getMergeAnimeForDownloading(mergeId: Long): List<Manga>
}
