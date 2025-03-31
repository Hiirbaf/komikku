package eu.kanade.presentation.reader

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.manga.components.AnimeEpisodeListItem
import eu.kanade.presentation.components.AdaptiveSheet
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.ui.reader.chapter.ReaderChapterItem
import eu.kanade.tachiyomi.util.lang.toRelativeString
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.source.local.isLocal
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun ChapterListDialog(
    onDismissRequest: () -> Unit,
    chapters: ImmutableList<ReaderChapterItem>,
    onClickChapter: (Chapter) -> Unit,
    onBookmark: (Chapter) -> Unit,
    dateRelativeTime: Boolean,
) {
    val context = LocalContext.current
    val state = rememberLazyListState(chapters.indexOfFirst { it.isCurrent }.coerceAtLeast(0))
    val downloadManager: DownloadManager = remember { Injekt.get() }
    val downloadQueueState by downloadManager.queueState.collectAsState()

    AdaptiveSheet(
        onDismissRequest = onDismissRequest,
    ) {
        LazyColumn(
            state = state,
            modifier = Modifier.heightIn(min = 200.dp, max = 500.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
        ) {
            items(
                items = chapters,
                key = { "episode-list-${it.chapter.id}" },
            ) { chapterItem ->
                val activeDownload = downloadQueueState.find { it.chapter.id == chapterItem.chapter.id }
                val progress = activeDownload?.let {
                    downloadManager.progressFlow()
                        .filter { it.chapter.id == chapterItem.chapter.id }
                        .map { it.progress }
                        .collectAsState(0).value
                } ?: 0
                val downloaded = if (chapterItem.manga.isLocal()) {
                    true
                } else {
                    downloadManager.isEpisodeDownloaded(
                        chapterItem.chapter.name,
                        chapterItem.chapter.scanlator,
                        chapterItem.manga.ogTitle,
                        chapterItem.manga.source,
                    )
                }
                val downloadState = when {
                    activeDownload != null -> activeDownload.status
                    downloaded -> Download.State.DOWNLOADED
                    else -> Download.State.NOT_DOWNLOADED
                }
                AnimeEpisodeListItem(
                    title = chapterItem.chapter.name,
                    date = chapterItem.chapter.dateUpload
                        .takeIf { it > 0L }
                        ?.let {
                            LocalDate.ofInstant(
                                Instant.ofEpochMilli(it),
                                ZoneId.systemDefault(),
                            ).toRelativeString(context, dateRelativeTime, chapterItem.dateFormat)
                        },
                    watchProgress = null,
                    scanlator = chapterItem.chapter.scanlator,
                    sourceName = null,
                    seen = chapterItem.chapter.seen,
                    bookmark = chapterItem.chapter.bookmark,
                    selected = false,
                    downloadIndicatorEnabled = false,
                    downloadStateProvider = { downloadState },
                    downloadProgressProvider = { progress },
                    episodeSwipeStartAction = LibraryPreferences.EpisodeSwipeAction.ToggleBookmark,
                    episodeSwipeEndAction = LibraryPreferences.EpisodeSwipeAction.ToggleBookmark,
                    onLongClick = { /*TODO*/ },
                    onClick = { onClickChapter(chapterItem.chapter) },
                    onDownloadClick = null,
                    onEpisodeSwipe = {
                        onBookmark(chapterItem.chapter)
                    },
                )
            }
        }
    }
}
