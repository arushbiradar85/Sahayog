package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.datastore.AppDataStore
import com.example.data.model.Role
import com.example.data.repository.CoopRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface StartupUiState {
    data object Loading : StartupUiState
    data object NeedsLogin : StartupUiState
    data class Authenticated(val role: Role, val name: String) : StartupUiState
}

class StartupViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<StartupUiState>(StartupUiState.Loading)
    val uiState: StateFlow<StartupUiState> = _uiState.asStateFlow()

    init {
        checkPersistedUser()
    }

    fun checkPersistedUser() {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val profile = AppDataStore.getUserProfile(context)
            if (profile != null && profile.isLoggedIn) {
                CoopRepository.applyUserProfile(profile)
                _uiState.value = StartupUiState.Authenticated(profile.role, profile.name)
            } else {
                _uiState.value = StartupUiState.NeedsLogin
            }
        }
    }

    fun submitLoginAndRole(
        name: String,
        phone: String,
        locality: String,
        role: Role,
        workerSkill: String = "Electrician",
        onNavigated: (Role) -> Unit
    ) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            // 1. Persist selection using Jetpack DataStore
            AppDataStore.saveUserProfile(
                context = context,
                name = name,
                phone = phone,
                locality = locality,
                role = role,
                workerSkill = workerSkill
            )

            // 2. Sync with Repository StateFlows
            CoopRepository.loginUser(
                name = name,
                phone = phone,
                locality = locality,
                role = role,
                workerSkill = workerSkill
            )

            _uiState.value = StartupUiState.Authenticated(role, name)
            onNavigated(role)
        }
    }
}
