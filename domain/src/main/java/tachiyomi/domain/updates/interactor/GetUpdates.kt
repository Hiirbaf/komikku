package tachiyomi.domain.updates.interactor

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retry
import tachiyomi.domain.updates.model.UpdatesWithRelations
import tachiyomi.domain.updates.repository.UpdatesRepository
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

class GetUpdates(
    private val repository: UpdatesRepository,
) {

    suspend fun await(seen: Boolean, after: Long): List<UpdatesWithRelations> {
        // SY -->
        return flow {
            emit(repository.awaitWithSeen(seen, after, limit = 500))
        }
            .catchNPE()
            .first()
        // SY <--
    }

    fun subscribe(instant: Instant): Flow<List<UpdatesWithRelations>> {
        return repository.subscribeAll(instant.toEpochMilli(), limit = 500)
            // SY -->
            .catchNPE()
        // SY <--
    }

    fun subscribe(seen: Boolean, after: Long): Flow<List<UpdatesWithRelations>> {
        return repository.subscribeWithSeen(seen, after, limit = 500)
            // SY -->
            .catchNPE()
        // SY <--
    }

    // SY -->
    private fun <T> Flow<T>.catchNPE() = retry {
        if (it is NullPointerException) {
            delay(5.seconds)
            true
        } else {
            false
        }
    }
    // SY <--
}
