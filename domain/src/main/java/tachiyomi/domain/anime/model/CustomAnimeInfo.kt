package tachiyomi.domain.anime.model

data class CustomAnimeInfo(
    val id: Long,
    val title: String?,
    val author: String? = null,
    val artist: String? = null,
    val thumbnailUrl: String? = null,
    val description: String? = null,
    val genre: List<String>? = null,
    val status: Long? = null,
)
