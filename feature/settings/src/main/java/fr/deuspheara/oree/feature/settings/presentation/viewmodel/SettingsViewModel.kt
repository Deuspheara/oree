package fr.deuspheara.oree.feature.settings.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.deuspheara.oree.core.domain.concurrency.PreferenceWriter
import fr.deuspheara.oree.core.domain.repository.BackupRepository
import fr.deuspheara.oree.core.domain.repository.ProfileRepository
import fr.deuspheara.oree.core.domain.usecase.RestoreBackupUseCase
import fr.deuspheara.oree.core.model.BackupReview
import java.io.IOException
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RestoreProgress(
    val review: BackupReview,
    val saving: Boolean = true,
    val failed: Boolean = false,
    val completed: Boolean = false,
)

/** Pending automatic saves and errors survive Activity recreation without retaining Android UI. */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SettingsViewModel(
    repository: ProfileRepository,
    private val backups: BackupRepository,
    private val restoreBackup: RestoreBackupUseCase,
) : ViewModel() {
    private val writer =
        PreferenceWriter(viewModelScope) { id, value -> repository.setPreferences(id, value) }
    val preferenceStates = writer.states

    fun retainProfiles(ids: Set<String>) = writer.retainProfiles(ids)

    suspend fun flushPreferences() = writer.flush()

    private val mutableRestore = MutableStateFlow<RestoreProgress?>(null)
    val restoring = mutableRestore.asStateFlow()

    fun setPreferences(id: String, value: fr.deuspheara.oree.core.model.LauncherPreferences) {
        if (mutableRestore.value?.saving != true) writer.submit(id, value)
    }

    fun retryPreferences(id: String) {
        if (mutableRestore.value?.saving != true) writer.retry(id)
    }

    fun restore(review: BackupReview) {
        if (mutableRestore.value?.saving == true) return
        mutableRestore.value = RestoreProgress(review)
        viewModelScope.launch {
            try {
                restoreBackup(review, writer)
                mutableRestore.value = RestoreProgress(review, saving = false, completed = true)
            } catch (_: IOException) {
                mutableRestore.value = RestoreProgress(review, saving = false, failed = true)
            } catch (_: SecurityException) {
                mutableRestore.value = RestoreProgress(review, saving = false, failed = true)
            }
        }
    }

    fun clearRestore() {
        if (mutableRestore.value?.saving != true) mutableRestore.value = null
    }

    suspend fun export(uri: String) = viewModelScope
        .async {
            writer.flush()
            backups.export(uri)
        }
        .await()

    suspend fun read(uri: String) = viewModelScope.async { backups.read(uri) }.await()

    suspend fun previous() = viewModelScope.async { backups.previous() }.await()
}
