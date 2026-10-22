package fr.deuspheara.oree.core.domain.usecase

import fr.deuspheara.oree.core.domain.concurrency.PreferenceWriter
import fr.deuspheara.oree.core.domain.repository.BackupRepository
import fr.deuspheara.oree.core.model.BackupReview

class RestoreBackupUseCase(private val backups: BackupRepository) {
    suspend operator fun invoke(review: BackupReview, writer: PreferenceWriter) {
        writer.flush()
        backups.restore(review)
        writer.resetAfterRestore()
    }
}
