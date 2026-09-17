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
        areaLocality: String = "Indiranagar",
        cityDistrict: String = "Bangalore",
        landmark: String = "",
        role: Role,
        skills: List<String> = listOf("Electrician"),
        workerSkill: String = "",
        experienceYears: Int = 4,
        cooperativeBranch: String = "",
        adminPosition: String = "",
        customerServiceInterest: String = "",
        customerServiceInterests: List<String> = listOf("Floor Cleaning"),
        onNavigated: (Role) -> Unit
    ) {
        val effectiveSkills = if (workerSkill.isNotBlank()) {
            listOf(workerSkill) + skills.filter { it != workerSkill }
        } else {
            skills
        }
        val effectiveInterests = if (customerServiceInterest.isNotBlank()) {
            listOf(customerServiceInterest) + customerServiceInterests.filter { it != customerServiceInterest }
        } else {
            customerServiceInterests
        }
        viewModelScope.launch {
            val context = getApplication<Application>()
            val locality = if (landmark.isNotBlank()) "$areaLocality, $cityDistrict (Near $landmark)" else "$areaLocality, $cityDistrict"

            // 1. Persist selection using Jetpack DataStore
            AppDataStore.saveUserProfile(
                context = context,
                name = name,
                phone = phone,
                areaLocality = areaLocality,
                cityDistrict = cityDistrict,
                landmark = landmark,
                role = role,
                skills = effectiveSkills,
                experienceYears = experienceYears,
                customerServiceInterests = effectiveInterests
            )

            // 2. Sync with Repository StateFlows and UserPreferences
            CoopRepository.loginUser(
                name = name,
                phone = phone,
                locality = locality,
                role = role,
                skills = effectiveSkills,
                areaLocality = areaLocality,
                cityDistrict = cityDistrict,
                landmark = landmark,
                experienceYears = experienceYears,
                customerServiceInterests = effectiveInterests
            )

            _uiState.value = StartupUiState.Authenticated(role, name)
            onNavigated(role)
        }
    }
}
