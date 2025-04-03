package eu.kanade.domain.track.interactor

import android.app.Application
import eu.kanade.domain.track.model.toDbTrack
import eu.kanade.domain.track.model.toDomainTrack
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.EnhancedTracker
import eu.kanade.tachiyomi.data.track.Tracker
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.util.lang.convertEpochMillisZone
import eu.kanade.tachiyomi.util.system.toast
import logcat.LogPriority
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.lang.withNonCancellableContext
import tachiyomi.core.common.util.lang.withUIContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.chapter.interactor.GetChaptersByMangaId
import tachiyomi.domain.history.interactor.GetHistory
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.track.interactor.InsertTrack
import tachiyomi.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.ZoneOffset

class AddTracks(
    private val insertTrack: InsertTrack,
    private val refreshTracks: RefreshTracks,
    private val getChaptersByMangaId: GetChaptersByMangaId,
    private val trackerManager: TrackerManager,
) {

    suspend fun bind(tracker: Tracker, item: Track, mangaId: Long) = withNonCancellableContext {
        withIOContext {
            val allChapters = getChaptersByMangaId.await(mangaId)
            val hasSeenChapters = allChapters.any { it.seen }
            tracker.bind(item, hasSeenChapters)

            var track = item.toDomainTrack(idRequired = false) ?: return@withIOContext

            insertTrack.await(track)

            // TODO: merge into [SyncChapterProgressWithTrack]?
            // Update chapter progress if newer chapters marked seen locally
            if (hasSeenChapters) {
                val latestLocalSeenChapterNumber = allChapters
                    .sortedBy { it.episodeNumber }
                    .takeWhile { it.seen }
                    .lastOrNull()
                    ?.episodeNumber ?: -1.0

                if (latestLocalSeenChapterNumber > track.lastEpisodeSeen) {
                    track = track.copy(
                        lastEpisodeSeen = latestLocalSeenChapterNumber,
                    )
                    tracker.setRemoteLastChapterRead(track.toDbTrack(), latestLocalSeenChapterNumber.toInt())
                }

                if (track.startDate <= 0) {
                    val firstSeenChapterDate = Injekt.get<GetHistory>().await(mangaId)
                        .sortedBy { it.seenAt }
                        .firstOrNull()
                        ?.seenAt

                    firstSeenChapterDate?.let {
                        val startDate = firstSeenChapterDate.time.convertEpochMillisZone(
                            ZoneOffset.systemDefault(),
                            ZoneOffset.UTC,
                        )
                        track = track.copy(
                            startDate = startDate,
                        )
                        tracker.setRemoteStartDate(track.toDbTrack(), startDate)
                    }
                }
            }

            // KMK -->
            val context = Injekt.get<Application>()
            refreshTracks.await(mangaId)
                .filter { it.first != null }
                .forEach { (track, e) ->
                    logcat(LogPriority.ERROR, e) {
                        "Failed to refresh track data animeId=$mangaId for service ${track!!.id}"
                    }
                    withUIContext {
                        context.toast(
                            context.stringResource(
                                MR.strings.track_error,
                                track!!.name,
                                e.message ?: "",
                            ),
                        )
                    }
                }
            // KMK <--
        }
    }

    suspend fun bindEnhancedTrackers(manga: Manga, source: Source) = withNonCancellableContext {
        withIOContext {
            trackerManager.loggedInTrackers()
                .filterIsInstance<EnhancedTracker>()
                .filter { it.accept(source) }
                .forEach { service ->
                    try {
                        service.match(manga)?.let { track ->
                            track.anime_id = manga.id
                            (service as Tracker).bind(track)
                            insertTrack.await(track.toDomainTrack(idRequired = false)!!)
                        }
                    } catch (e: Exception) {
                        logcat(
                            LogPriority.WARN,
                            e,
                        ) { "Could not match anime: ${manga.title} with service $service" }
                    }
                }

            // KMK -->
            val context = Injekt.get<Application>()
            refreshTracks.await(manga.id)
                .filter { it.first != null }
                .forEach { (track, e) ->
                    logcat(LogPriority.ERROR, e) {
                        "Failed to refresh track data animeId=${manga.id} for service ${track!!.id}"
                    }
                    withUIContext {
                        context.toast(
                            context.stringResource(
                                MR.strings.track_error,
                                track!!.name,
                                e.message ?: "",
                            ),
                        )
                    }
                }
            // KMK <--
        }
    }
}
