package fr.deuspheara.oree.core.domain.repository

import fr.deuspheara.oree.core.model.BackupCandidate
import fr.deuspheara.oree.core.model.BackupReview

interface BackupRepository {
    suspend fun export(uri: String)

    suspend fun read(uri: String): BackupCandidate

    suspend fun previous(): BackupCandidate

    suspend fun restore(review: BackupReview)
}
