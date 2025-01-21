package tachiyomi.domain.track.interactor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import tachiyomi.domain.track.model.Track
import tachiyomi.domain.track.repository.TrackRepository

class GetTracksPerAnime(
    private val trackRepository: TrackRepository,
    private val isTrackUnfollowed: IsTrackUnfollowed,
) {

    fun subscribe(): Flow<Map<Long, List<Track>>> {
        return trackRepository.getTracksAsFlow().map { tracks ->
            tracks.groupBy { it.animeId }
                // SY -->
                .mapValues { entry ->
                    entry.value.filterNot { isTrackUnfollowed.await(it) }
                }
            // SY <--
        }
    }
}
