
import eu.kanade.tachiyomi.data.backup.models.Backup
import kotlinx.serialization.protobuf.ProtoBuf
import okio.buffer
import okio.gzip
import okio.sink
import okio.source
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import tachiyomi.domain.manga.interactor.GetCustomMangaInfo
import tachiyomi.domain.manga.model.CustomMangaInfo
import tachiyomi.domain.manga.repository.CustomMangaRepository
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.addSingletonFactory
import java.io.File

class Tester {

    @Disabled
    @Test
    fun stripBackup() {
        val bytes = File("D:\\Downloads\\pacthiyomi_2023-05-08_13-30.proto (1).gz")
            .inputStream().source().buffer()
            .gzip().buffer()
            .readByteArray()
        val backup = ProtoBuf.decodeFromByteArray(Backup.serializer(), bytes)
        val newBytes = ProtoBuf.encodeToByteArray(
            Backup.serializer(),
            backup.copy(
                backupManga = backup.backupManga.filter { it.favorite },
            ),
        )
        File("D:\\Downloads\\pacthiyomi_2023-05-08_13-30 (2).proto.gz").outputStream().sink().gzip().buffer().use {
            it.write(newBytes)
        }
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun before() {
            Injekt.addSingletonFactory {
                GetCustomMangaInfo(
                    object : CustomMangaRepository {
                        override fun get(mangaId: Long) = null
                        override fun set(mangaInfo: CustomMangaInfo) = Unit
                    },
                )
            }
        }
    }
}
