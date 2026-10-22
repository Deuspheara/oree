package fr.deuspheara.oree.core.data.repository

import android.content.Context
import android.net.Uri
import fr.deuspheara.oree.core.data.serialization.MAX_BACKUP_BYTES
import fr.deuspheara.oree.core.data.serialization.decodeBackup
import fr.deuspheara.oree.core.data.serialization.encodeBackup
import fr.deuspheara.oree.core.domain.concurrency.AppDispatchers
import fr.deuspheara.oree.core.domain.repository.BackupRepository
import fr.deuspheara.oree.core.domain.repository.ProfileRepository
import fr.deuspheara.oree.core.model.BackupCandidate
import fr.deuspheara.oree.core.model.BackupReview
import fr.deuspheara.oree.core.model.ProfileSnapshot
import fr.deuspheara.oree.core.model.WallpaperMode
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/** Document URIs only. Configuration is local; personal wallpaper binaries are excluded. */
class SafBackupRepository(
    private val context: Context,
    private val profiles: ProfileRepository,
    private val dispatchers: AppDispatchers = AppDispatchers(Dispatchers.IO, Dispatchers.Default),
) : BackupRepository {
    override suspend fun export(uri: String) = export(Uri.parse(uri))

    suspend fun export(uri: Uri) = withContext(dispatchers.io) {
        validateDocument(uri)
        val snapshot = profiles.readSnapshot() ?: throw IOException("Profiles unavailable")
        val bytes = encodeBackup(snapshot).toByteArray(Charsets.UTF_8)
        context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(bytes) }
            ?: throw IOException("Document unavailable")
    }

    override suspend fun read(uri: String): BackupCandidate = read(Uri.parse(uri))

    suspend fun read(uri: Uri): BackupCandidate = withContext(dispatchers.io) {
        validateDocument(uri)
        val bytes =
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(16 * 1024)
                while (true) {
                    ensureActive()
                    val size = stream.read(buffer)
                    if (size < 0) break
                    if (output.size() + size > MAX_BACKUP_BYTES) {
                        throw IOException("Backup too large")
                    }
                    output.write(buffer, 0, size)
                }
                output.toByteArray()
            } ?: throw IOException("Document unavailable")
        val decoder =
            Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
        prepare(decodeBackup(decoder.decode(ByteBuffer.wrap(bytes)).toString()))
    }

    override suspend fun previous(): BackupCandidate = withContext(dispatchers.io) { prepare(profiles.previousRestore()) }

    private fun prepare(snapshot: ProfileSnapshot): BackupCandidate {
        var missing = 0
        return BackupCandidate(
            snapshot.copy(
                profiles =
                    snapshot.profiles.map { profile ->
                        if (
                            profile.image != null &&
                            !java.io.File(context.filesDir, profile.image).isFile
                        ) {
                            missing++
                            profile.copy(
                                image = null,
                                wallpaper =
                                    if (profile.wallpaper == WallpaperMode.PERSONAL) {
                                        WallpaperMode.LANDSCAPE
                                    } else {
                                        profile.wallpaper
                                    },
                            )
                        } else {
                            profile
                        }
                    },
            ),
            missing,
        )
    }

    override suspend fun restore(review: BackupReview) = profiles.restoreSnapshot(review.expected, review.candidate.snapshot, review.undo)

    private fun validateDocument(uri: Uri) {
        if (
            uri.scheme != "content" ||
            uri.authority.isNullOrBlank() ||
            uri.toString().length > 4096 ||
            uri.toString().any(Char::isISOControl)
        ) {
            throw IOException("Document URI required")
        }
    }
}
