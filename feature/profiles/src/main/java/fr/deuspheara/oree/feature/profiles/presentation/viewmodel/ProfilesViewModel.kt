package fr.deuspheara.oree.feature.profiles.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.deuspheara.oree.core.domain.repository.ProfileRepository
import fr.deuspheara.oree.core.domain.usecase.DeleteProfileUseCase
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ProfileEditStatus { IDLE, SAVING, FAILED, COMPLETED }

data class ProfilesUiState(val status: ProfileEditStatus = ProfileEditStatus.IDLE)

class ProfilesViewModel(private val profiles: ProfileRepository, private val deleteProfile: DeleteProfileUseCase) : ViewModel() {
    private val mutable = MutableStateFlow(ProfilesUiState())
    val state = mutable.asStateFlow()

    private fun edit(action: suspend () -> Unit) {
        if (mutable.value.status == ProfileEditStatus.SAVING) return
        mutable.value = ProfilesUiState(status = ProfileEditStatus.SAVING)
        viewModelScope.launch {
            try {
                action()
                mutable.value = ProfilesUiState(status = ProfileEditStatus.COMPLETED)
            } catch (_: IOException) {
                mutable.value = ProfilesUiState(status = ProfileEditStatus.FAILED)
            } catch (_: SecurityException) {
                mutable.value = ProfilesUiState(status = ProfileEditStatus.FAILED)
            }
        }
    }

    fun select(id: String) = edit { profiles.select(id) }

    fun save(id: String?, name: String, avatar: String) = edit {
        if (id == null) profiles.create(name, avatar) else profiles.rename(id, name, avatar)
    }

    fun delete(id: String) = edit { deleteProfile(id) }

    fun acknowledge() {
        if (mutable.value.status != ProfileEditStatus.SAVING) mutable.value = ProfilesUiState()
    }
}
