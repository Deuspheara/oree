package fr.deuspheara.oree.feature.settings.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.deuspheara.oree.core.domain.concurrency.PreferenceWriter
import fr.deuspheara.oree.core.domain.repository.BackupRepository
import fr.deuspheara.oree.core.domain.repository.ProfileRepository
import fr.deuspheara.oree.core.domain.usecase.RestoreBackupUseCase
import fr.deuspheara.oree.core.model.BackupReview
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class RestoreStatus { SAVING, FAILED, COMPLETED }

data class RestoreProgress(val review: BackupReview, val status: RestoreStatus = RestoreStatus.SAVING)

enum class BackupStatus { IDLE, READING, EXPORTING, EXPORTED, FAILED }

data class BackupUiState(val status: BackupStatus = BackupStatus.IDLE, val review: BackupReview? = null) {
    val busy: Boolean get() = status == BackupStatus.READING || status == BackupStatus.EXPORTING
}

/** Pending automatic saves and errors survive Activity recreation without retaining Android UI. */
class SettingsViewModel(
    private val repository: ProfileRepository,
    private val backups: BackupRepository,
    private val restoreBackup: RestoreBackupUseCase,
) : ViewModel() {
    private val writer =
        PreferenceWriter(viewModelScope) { id, value -> repository.setPreferences(id, value) }
    val preferenceStates = writer.states

    fun retainProfiles(ids: Set<String>) = writer.retainProfiles(ids)

    private val mutableBackup = MutableStateFlow(BackupUiState())
    val backup = mutableBackup.asStateFlow()

    private fun backupOperation(status: BackupStatus, action: suspend () -> Unit) {
        if (mutableBackup.value.busy || mutableRestore.value?.status == RestoreStatus.SAVING) return
        mutableBackup.value = mutableBackup.value.copy(status = status)
        viewModelScope.launch {
            try {
                action()
            } catch (_: IOException) {
                reportBackupFailure()
            } catch (_: SecurityException) {
                reportBackupFailure()
            }
        }
    }

    fun export(uri: String) = backupOperation(BackupStatus.EXPORTING) {
        writer.flush()
        backups.export(uri)
        mutableBackup.value = mutableBackup.value.copy(status = BackupStatus.EXPORTED)
    }

    fun review(uri: String) = backupOperation(BackupStatus.READING) {
        prepareReview(backups.read(uri), undo = false)
    }

    fun reviewPrevious() = backupOperation(BackupStatus.READING) {
        prepareReview(backups.previous(), undo = true)
    }

    private suspend fun prepareReview(candidate: fr.deuspheara.oree.core.model.BackupCandidate, undo: Boolean) {
        writer.flush()
        val expected = repository.readSnapshot() ?: throw IOException("Profiles unavailable")
        mutableBackup.value = BackupUiState(review = BackupReview(candidate, expected, undo))
    }

    fun reportBackupFailure() {
        mutableBackup.value = mutableBackup.value.copy(status = BackupStatus.FAILED)
    }

    fun acknowledgeBackupResult() {
        if (!mutableBackup.value.busy) mutableBackup.value = mutableBackup.value.copy(status = BackupStatus.IDLE)
    }

    fun dismissBackupReview() {
        if (!mutableBackup.value.busy && mutableRestore.value?.status != RestoreStatus.SAVING) {
            mutableBackup.value = BackupUiState()
        }
    }

    private val mutableRestore = MutableStateFlow<RestoreProgress?>(null)
    val restoring = mutableRestore.asStateFlow()

    fun setPreferences(id: String, value: fr.deuspheara.oree.core.model.LauncherPreferences) {
        if (mutableRestore.value?.status != RestoreStatus.SAVING) writer.submit(id, value)
    }

    fun retryPreferences(id: String) {
        if (mutableRestore.value?.status != RestoreStatus.SAVING) writer.retry(id)
    }

    fun restore(review: BackupReview) {
        if (mutableRestore.value?.status == RestoreStatus.SAVING || mutableBackup.value.busy) return
        mutableRestore.value = RestoreProgress(review)
        viewModelScope.launch {
            try {
                restoreBackup(review, writer)
                mutableRestore.value = RestoreProgress(review, status = RestoreStatus.COMPLETED)
            } catch (_: IOException) {
                mutableRestore.value = RestoreProgress(review, status = RestoreStatus.FAILED)
            } catch (_: SecurityException) {
                mutableRestore.value = RestoreProgress(review, status = RestoreStatus.FAILED)
            }
        }
    }

    fun clearRestore() {
        if (mutableRestore.value?.status != RestoreStatus.SAVING) mutableRestore.value = null
    }
}
