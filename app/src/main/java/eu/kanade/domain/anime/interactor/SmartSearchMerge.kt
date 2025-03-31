package eu.kanade.domain.anime.interactor

import android.app.Application
import eu.kanade.domain.anime.model.copyFrom
import eu.kanade.domain.anime.model.toSAnime
import exh.source.MERGED_SOURCE_ID
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.lang.withNonCancellableContext
import tachiyomi.domain.manga.interactor.DeleteAnimeById
import tachiyomi.domain.manga.interactor.DeleteByMergeId
import tachiyomi.domain.manga.interactor.GetAnime
import tachiyomi.domain.manga.interactor.GetMergedReferencesById
import tachiyomi.domain.manga.interactor.InsertMergedReference
import tachiyomi.domain.manga.interactor.NetworkToLocalAnime
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MergedMangaReference
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.interactor.SetMangaCategories
import tachiyomi.i18n.sy.SYMR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SmartSearchMerge(
    private val getAnime: GetAnime = Injekt.get(),
    private val getMergedReferencesById: GetMergedReferencesById = Injekt.get(),
    private val insertMergedReference: InsertMergedReference = Injekt.get(),
    private val networkToLocalAnime: NetworkToLocalAnime = Injekt.get(),
    private val deleteAnimeById: DeleteAnimeById = Injekt.get(),
    private val deleteByMergeId: DeleteByMergeId = Injekt.get(),
    private val getCategories: GetCategories = Injekt.get(),
    private val setMangaCategories: SetMangaCategories = Injekt.get(),
) {
    suspend fun smartSearchMerge(manga: Manga, originalAnimeId: Long): Manga {
        // KMK -->
        val context = Injekt.get<Application>()
        // KMK <--
        val originalAnime = getAnime.await(originalAnimeId)
            ?: throw IllegalArgumentException(context.stringResource(SYMR.strings.merge_unknown_entry, originalAnimeId))
        if (originalAnime.source == MERGED_SOURCE_ID) {
            val children = getMergedReferencesById.await(originalAnimeId)
            if (children.any { it.mangaSourceId == manga.source && it.mangaUrl == manga.url }) {
                // Merged already
                return originalAnime
            }

            val animeReferences = mutableListOf(
                MergedMangaReference(
                    id = -1,
                    isInfoManga = false,
                    getChapterUpdates = true,
                    chapterSortMode = 0,
                    chapterPriority = 0,
                    downloadChapters = true,
                    mergeId = originalAnime.id,
                    mergeUrl = originalAnime.url,
                    mangaId = manga.id,
                    mangaUrl = manga.url,
                    mangaSourceId = manga.source,
                ),
            )

            if (children.isEmpty() || children.all { it.mangaSourceId != MERGED_SOURCE_ID }) {
                animeReferences += MergedMangaReference(
                    id = -1,
                    isInfoManga = false,
                    getChapterUpdates = false,
                    chapterSortMode = 0,
                    chapterPriority = -1,
                    downloadChapters = false,
                    mergeId = originalAnime.id,
                    mergeUrl = originalAnime.url,
                    mangaId = originalAnime.id,
                    mangaUrl = originalAnime.url,
                    mangaSourceId = MERGED_SOURCE_ID,
                )
            }

            // todo
            insertMergedReference.awaitAll(animeReferences)

            return originalAnime
        } else {
            if (manga.id == originalAnimeId) {
                // Merged already
                return originalAnime
            }
            var mergedManga = Manga.create()
                .copy(
                    url = originalAnime.url,
                    ogTitle = originalAnime.title,
                    source = MERGED_SOURCE_ID,
                )
                .copyFrom(originalAnime.toSAnime())
                .copy(
                    favorite = true,
                    lastUpdate = originalAnime.lastUpdate,
                    viewerFlags = originalAnime.viewerFlags,
                    chapterFlags = originalAnime.chapterFlags,
                    dateAdded = System.currentTimeMillis(),
                )

            var existingAnime = getAnime.await(mergedManga.url, mergedManga.source)
            while (existingAnime != null) {
                if (existingAnime.favorite) {
                    // Duplicate entry found -> use it instead
                    mergedManga = existingAnime
                    break
                } else {
                    withNonCancellableContext {
                        existingAnime?.id?.let {
                            deleteByMergeId.await(it)
                            deleteAnimeById.await(it)
                        }
                    }
                }
                // Remove previously merged entry from database (user already removed from favorites)
                existingAnime = getAnime.await(mergedManga.url, mergedManga.source)
            }

            mergedManga = networkToLocalAnime.await(mergedManga)

            getCategories.await(originalAnimeId)
                .let { categories ->
                    setMangaCategories.await(mergedManga.id, categories.map { it.id })
                }

            val originalAnimeReference = MergedMangaReference(
                id = -1,
                isInfoManga = true,
                getChapterUpdates = true,
                chapterSortMode = 0,
                chapterPriority = 0,
                downloadChapters = true,
                mergeId = mergedManga.id,
                mergeUrl = mergedManga.url,
                mangaId = originalAnime.id,
                mangaUrl = originalAnime.url,
                mangaSourceId = originalAnime.source,
            )

            val newAnimeReference = MergedMangaReference(
                id = -1,
                isInfoManga = false,
                getChapterUpdates = true,
                chapterSortMode = 0,
                chapterPriority = 0,
                downloadChapters = true,
                mergeId = mergedManga.id,
                mergeUrl = mergedManga.url,
                mangaId = manga.id,
                mangaUrl = manga.url,
                mangaSourceId = manga.source,
            )

            val mergedMangaReference = MergedMangaReference(
                id = -1,
                isInfoManga = false,
                getChapterUpdates = false,
                chapterSortMode = 0,
                chapterPriority = -1,
                downloadChapters = false,
                mergeId = mergedManga.id,
                mergeUrl = mergedManga.url,
                mangaId = mergedManga.id,
                mangaUrl = mergedManga.url,
                mangaSourceId = MERGED_SOURCE_ID,
            )

            insertMergedReference.awaitAll(listOf(originalAnimeReference, newAnimeReference, mergedMangaReference))

            return mergedManga
        }

        // Note that if the manga are merged in a different order, this won't trigger, but I don't care lol
    }
}
