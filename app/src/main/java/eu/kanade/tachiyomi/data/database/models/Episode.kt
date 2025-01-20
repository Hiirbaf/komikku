@file:Suppress("PropertyName")

package eu.kanade.tachiyomi.data.database.models

import eu.kanade.tachiyomi.animesource.model.SEpisode
import java.io.Serializable
import tachiyomi.domain.episode.model.Episode as DomainChapter

interface Episode : SEpisode, Serializable {

    var id: Long?

    var manga_id: Long?

    var read: Boolean

    var bookmark: Boolean

    var last_page_read: Int

    var date_fetch: Long

    var source_order: Int

    var last_modified: Long

    var version: Long
}

fun Episode.toDomainChapter(): DomainChapter? {
    if (id == null || manga_id == null) return null
    return DomainChapter(
        id = id!!,
        mangaId = manga_id!!,
        read = read,
        bookmark = bookmark,
        lastPageRead = last_page_read.toLong(),
        dateFetch = date_fetch,
        sourceOrder = source_order.toLong(),
        url = url,
        name = name,
        dateUpload = date_upload,
        chapterNumber = episode_number.toDouble(),
        scanlator = scanlator,
        lastModifiedAt = last_modified,
        version = version,
    )
}
