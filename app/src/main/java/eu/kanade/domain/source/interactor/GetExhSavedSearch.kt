package eu.kanade.domain.source.interactor

import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import exh.log.xLogE
import exh.util.nullIfBlank
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.source.interactor.GetSavedSearchById
import tachiyomi.domain.source.interactor.GetSavedSearchBySourceId
import tachiyomi.domain.source.model.EXHSavedSearch
import tachiyomi.domain.source.model.SavedSearch
import xyz.nulldev.ts.api.http.serializer.FilterSerializer

class GetExhSavedSearch(
    private val getSavedSearchById: GetSavedSearchById,
    private val getSavedSearchBySourceId: GetSavedSearchBySourceId,
    private val filterSerializer: FilterSerializer,
) {

    suspend fun awaitOne(savedSearchId: Long, getFilterList: () -> AnimeFilterList): EXHSavedSearch? {
        val search = getSavedSearchById.awaitOrNull(savedSearchId) ?: return null
        return withIOContext { loadSearch(search, getFilterList) }
    }

    suspend fun await(sourceId: Long, getFilterList: () -> AnimeFilterList): List<EXHSavedSearch> {
        return withIOContext { loadSearches(getSavedSearchBySourceId.await(sourceId), getFilterList) }
    }

    fun subscribe(sourceId: Long, getFilterList: () -> AnimeFilterList): Flow<List<EXHSavedSearch>> {
        return getSavedSearchBySourceId.subscribe(sourceId)
            .map { loadSearches(it, getFilterList) }
            .flowOn(Dispatchers.IO)
    }

    private fun loadSearches(searches: List<SavedSearch>, getFilterList: () -> AnimeFilterList): List<EXHSavedSearch> {
        return searches.map { loadSearch(it, getFilterList) }
    }

    private fun loadSearch(search: SavedSearch, getFilterList: () -> AnimeFilterList): EXHSavedSearch {
        val originalFilters = getFilterList()
        val filters = getFilters(search.filtersJson)

        return EXHSavedSearch(
            id = search.id,
            name = search.name,
            query = search.query?.nullIfBlank(),
            filterList = filters?.let { deserializeFilters(it, originalFilters) },
        )
    }

    private fun getFilters(filtersJson: String?): JsonArray? {
        return runCatching {
            filtersJson?.let { Json.decodeFromString<JsonArray>(it) }
        }.onFailure {
            xLogE("Failed to load saved search!", it)
        }.getOrNull()
    }

    private fun deserializeFilters(filters: JsonArray, originalFilters: AnimeFilterList): AnimeFilterList? {
        return runCatching {
            filterSerializer.deserialize(originalFilters, filters)
            originalFilters
        }.onFailure {
            xLogE("Failed to load saved search!", it)
        }.getOrNull()
    }
}
