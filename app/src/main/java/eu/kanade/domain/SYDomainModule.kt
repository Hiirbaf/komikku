package eu.kanade.domain

import android.app.Application
import eu.kanade.domain.anime.interactor.CreateSortTag
import eu.kanade.domain.anime.interactor.DeleteSortTag
import eu.kanade.domain.anime.interactor.GetSortTag
import eu.kanade.domain.anime.interactor.ReorderSortTag
import eu.kanade.domain.anime.interactor.SmartSearchMerge
import eu.kanade.domain.source.interactor.CreateSourceCategory
import eu.kanade.domain.source.interactor.DeleteSourceCategory
import eu.kanade.domain.source.interactor.GetExhSavedSearch
import eu.kanade.domain.source.interactor.GetShowLatest
import eu.kanade.domain.source.interactor.GetSourceCategories
import eu.kanade.domain.source.interactor.RenameSourceCategory
import eu.kanade.domain.source.interactor.SetSourceCategories
import eu.kanade.domain.source.interactor.ToggleExcludeFromDataSaver
import exh.search.SearchEngine
import tachiyomi.data.anime.MangaMergeRepositoryImpl
import tachiyomi.data.anime.CustomMangaRepositoryImpl
import tachiyomi.data.source.FeedSavedSearchRepositoryImpl
import tachiyomi.data.source.SavedSearchRepositoryImpl
import tachiyomi.domain.manga.interactor.DeleteAnimeById
import tachiyomi.domain.manga.interactor.DeleteByMergeId
import tachiyomi.domain.manga.interactor.DeleteMergeById
import tachiyomi.domain.manga.interactor.GetAllAnime
import tachiyomi.domain.manga.interactor.GetAnimeBySource
import tachiyomi.domain.manga.interactor.GetCustomMangaInfo
import tachiyomi.domain.manga.interactor.GetMergedAnime
import tachiyomi.domain.manga.interactor.GetMergedAnimeById
import tachiyomi.domain.manga.interactor.GetMergedAnimeForDownloading
import tachiyomi.domain.manga.interactor.GetMergedReferencesById
import tachiyomi.domain.manga.interactor.GetSeenAnimeNotInLibraryView
import tachiyomi.domain.manga.interactor.InsertMergedReference
import tachiyomi.domain.manga.interactor.SetCustomAnimeInfo
import tachiyomi.domain.manga.interactor.UpdateMergedSettings
import tachiyomi.domain.manga.repository.MangaMergeRepository
import tachiyomi.domain.manga.repository.CustomMangaRepository
import tachiyomi.domain.chapter.interactor.DeleteEpisodes
import tachiyomi.domain.chapter.interactor.GetEpisodeByUrl
import tachiyomi.domain.chapter.interactor.GetMergedChaptersByMangaId
import tachiyomi.domain.history.interactor.GetHistoryByAnimeId
import tachiyomi.domain.source.interactor.CountFeedSavedSearchBySourceId
import tachiyomi.domain.source.interactor.CountFeedSavedSearchGlobal
import tachiyomi.domain.source.interactor.DeleteFeedSavedSearchById
import tachiyomi.domain.source.interactor.DeleteSavedSearchById
import tachiyomi.domain.source.interactor.GetFeedSavedSearchBySourceId
import tachiyomi.domain.source.interactor.GetFeedSavedSearchGlobal
import tachiyomi.domain.source.interactor.GetSavedSearchById
import tachiyomi.domain.source.interactor.GetSavedSearchBySourceId
import tachiyomi.domain.source.interactor.GetSavedSearchBySourceIdFeed
import tachiyomi.domain.source.interactor.GetSavedSearchGlobalFeed
import tachiyomi.domain.source.interactor.InsertFeedSavedSearch
import tachiyomi.domain.source.interactor.InsertSavedSearch
import tachiyomi.domain.source.interactor.ReorderFeed
import tachiyomi.domain.source.repository.FeedSavedSearchRepository
import tachiyomi.domain.source.repository.SavedSearchRepository
import tachiyomi.domain.track.interactor.IsTrackUnfollowed
import uy.kohesive.injekt.api.InjektModule
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.addFactory
import uy.kohesive.injekt.api.addSingletonFactory
import uy.kohesive.injekt.api.get
import xyz.nulldev.ts.api.http.serializer.FilterSerializer

class SYDomainModule : InjektModule {

    override fun InjektRegistrar.registerInjectables() {
        addFactory { GetShowLatest(get()) }
        addFactory { ToggleExcludeFromDataSaver(get()) }
        addFactory { SetSourceCategories(get()) }
        addFactory { GetAllAnime(get()) }
        addFactory { GetAnimeBySource(get()) }
        addFactory { DeleteEpisodes(get()) }
        addFactory { DeleteAnimeById(get()) }
        addFactory { FilterSerializer() }
        addFactory { GetHistoryByAnimeId(get()) }
        addFactory { GetEpisodeByUrl(get()) }
        addFactory { GetSourceCategories(get()) }
        addFactory { CreateSourceCategory(get()) }
        addFactory { RenameSourceCategory(get(), get()) }
        addFactory { DeleteSourceCategory(get()) }
        addFactory { GetSortTag(get()) }
        addFactory { CreateSortTag(get(), get()) }
        addFactory { DeleteSortTag(get(), get()) }
        addFactory { ReorderSortTag(get(), get()) }
        addFactory { SearchEngine() }
        addFactory { IsTrackUnfollowed() }
        addFactory { GetSeenAnimeNotInLibraryView(get()) }

        addSingletonFactory<MangaMergeRepository> { MangaMergeRepositoryImpl(get()) }
        addFactory { GetMergedAnime(get()) }
        addFactory { GetMergedAnimeById(get()) }
        addFactory { GetMergedReferencesById(get()) }
        addFactory { GetMergedChaptersByMangaId(get(), get()) }
        addFactory { InsertMergedReference(get()) }
        addFactory { UpdateMergedSettings(get()) }
        addFactory { DeleteByMergeId(get()) }
        addFactory { DeleteMergeById(get()) }
        addFactory { GetMergedAnimeForDownloading(get()) }
        // KMK -->
        addFactory { SmartSearchMerge(get()) }
        // KMK <--

        addSingletonFactory<SavedSearchRepository> { SavedSearchRepositoryImpl(get()) }
        addFactory { GetSavedSearchById(get()) }
        addFactory { GetSavedSearchBySourceId(get()) }
        addFactory { DeleteSavedSearchById(get()) }
        addFactory { InsertSavedSearch(get()) }
        addFactory { GetExhSavedSearch(get(), get(), get()) }

        addSingletonFactory<FeedSavedSearchRepository> { FeedSavedSearchRepositoryImpl(get()) }
        addFactory { InsertFeedSavedSearch(get()) }
        addFactory { DeleteFeedSavedSearchById(get()) }
        addFactory { GetFeedSavedSearchGlobal(get()) }
        addFactory { GetFeedSavedSearchBySourceId(get()) }
        addFactory { CountFeedSavedSearchGlobal(get()) }
        addFactory { CountFeedSavedSearchBySourceId(get()) }
        addFactory { GetSavedSearchGlobalFeed(get()) }
        addFactory { GetSavedSearchBySourceIdFeed(get()) }
        // KMK -->
        addFactory { ReorderFeed(get()) }
        // KMK <--

        addSingletonFactory<CustomMangaRepository> { CustomMangaRepositoryImpl(get<Application>()) }
        addFactory { GetCustomMangaInfo(get()) }
        addFactory { SetCustomAnimeInfo(get()) }
    }
}
