package eu.kanade.domain.track.interactor

import eu.kanade.domain.track.model.toDbTrack
import eu.kanade.domain.track.service.TrackPreferences
import eu.kanade.tachiyomi.data.track.Tracker
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.chapter.interactor.GetChaptersByMangaId
import tachiyomi.domain.chapter.interactor.UpdateChapter
import tachiyomi.domain.chapter.model.toChapterUpdate
import tachiyomi.domain.track.interactor.InsertTrack
import tachiyomi.domain.track.model.Track
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.math.max

class SyncChapterProgressWithTrack(
    private val updateChapter: UpdateChapter,
    private val insertTrack: InsertTrack,
    private val getChaptersByMangaId: GetChaptersByMangaId,
) {
    val trackPreferences: TrackPreferences = Injekt.get()

    suspend fun await(
        mangaId: Long,
        remoteTrack: Track,
        tracker: Tracker,
    ): Int? {
        // KKM -->
        // if (tracker !is EnhancedTracker) {
        //     return
        // }
        // <-- KKM

        // Current chapters in database, sort by source's order because database's order is a mess
        val dbChapters = getChaptersByMangaId.await(mangaId)
            // KMK -->
            .sortedByDescending { it.sourceOrder }
            // KMK <--
            .filter { it.isRecognizedNumber }

        val sortedChapters = dbChapters
            .sortedBy { it.episodeNumber }

        // KMK -->
        var lastCheckChapter: Double
        var checkingChapter = 0.0

        /**
         * Chapters to update to follow tracker: only continuous incremental chapters
         * any abnormal chapter number will stop it from updating seen status further.
         * Some mangas has name such as Volume 2 Chapter 1 which will corrupt the order
         * if we sort by chapterNumber.
         */
        val chapterUpdates = dbChapters
            .takeWhile { chapter ->
                lastCheckChapter = checkingChapter
                checkingChapter = chapter.episodeNumber
                chapter.episodeNumber >= lastCheckChapter && chapter.episodeNumber <= remoteTrack.lastEpisodeSeen
            }
            .filter { chapter -> !chapter.seen }
            // KMK <--
            .map { it.copy(seen = true).toChapterUpdate() }

        // only take into account continuous watching
        val localLastSeen = sortedChapters.takeWhile { it.seen }.lastOrNull()?.episodeNumber ?: 0F
        // Tracker will update to latest seen chapter
        val lastSeen = max(remoteTrack.lastEpisodeSeen, localLastSeen.toDouble())
        val updatedTrack = remoteTrack.copy(lastEpisodeSeen = lastSeen)

        try {
            // Update Tracker to localLastSeen if needed
            if (lastSeen > remoteTrack.lastEpisodeSeen) {
                tracker.update(updatedTrack.toDbTrack())
                // update Track in database
                insertTrack.await(updatedTrack)
            }
            // KMK -->
            // Always update local chapters following Tracker even past chapters
            if (chapterUpdates.isNotEmpty() &&
                trackPreferences.autoSyncProgressFromTrackers().get() &&
                !tracker.hasNotStartedWatching(remoteTrack.status)
            ) {
                updateChapter.awaitAll(chapterUpdates)
                return lastSeen.toInt()
            }
            // KMK <--
        } catch (e: Throwable) {
            logcat(LogPriority.WARN, e)
        }
        // KMK -->
        return null
        // KMK <--
    }
}
