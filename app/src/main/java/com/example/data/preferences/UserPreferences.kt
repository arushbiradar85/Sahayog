package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.Role
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserProfile(
    val name: String,
    val phone: String,
    val locality: String,
    val role: Role,
    val workerSkill: String = "Electrician",
    val isLoggedIn: Boolean = false
)

object UserPreferences {
    private const val PREFS_NAME = "sahayog_user_prefs"
    private const val KEY_IS_LOGGED_IN = "key_is_logged_in"
    private const val KEY_USER_NAME = "key_user_name"
    private const val KEY_USER_PHONE = "key_user_phone"
    private const val KEY_USER_LOCALITY = "key_user_locality"
    private const val KEY_USER_ROLE = "key_user_role"
    private const val KEY_WORKER_SKILL = "key_worker_skill"
    private const val KEY_APP_LANGUAGE = "key_app_language"

    private var sharedPreferences: SharedPreferences? = null

    private val _userProfileFlow = MutableStateFlow<UserProfile?>(null)
    val userProfileFlow: StateFlow<UserProfile?> = _userProfileFlow.asStateFlow()

    private val _isLoggedInFlow = MutableStateFlow(false)
    val isLoggedInFlow: StateFlow<Boolean> = _isLoggedInFlow.asStateFlow()

    private val _userRoleFlow = MutableStateFlow<Role?>(null)
    val userRoleFlow: StateFlow<Role?> = _userRoleFlow.asStateFlow()

    fun init(context: Context) {
        if (sharedPreferences != null) return
        sharedPreferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadFromPreferences()
    }

    private fun loadFromPreferences() {
        val prefs = sharedPreferences ?: return
        val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        val roleStr = prefs.getString(KEY_USER_ROLE, null)
        val role = roleStr?.let {
            try {
                Role.valueOf(it)
            } catch (e: Exception) {
                null
            }
        }

        if (isLoggedIn && role != null) {
            val name = prefs.getString(KEY_USER_NAME, "Ramesh Patil") ?: "Ramesh Patil"
            val phone = prefs.getString(KEY_USER_PHONE, "9845012345") ?: "9845012345"
            val locality = prefs.getString(KEY_USER_LOCALITY, "Indiranagar, Bangalore") ?: "Indiranagar, Bangalore"
            val workerSkill = prefs.getString(KEY_WORKER_SKILL, "Electrician") ?: "Electrician"

            val profile = UserProfile(
                name = name,
                phone = phone,
                locality = locality,
                role = role,
                workerSkill = workerSkill,
                isLoggedIn = true
            )
            _userProfileFlow.value = profile
            _userRoleFlow.value = role
            _isLoggedInFlow.value = true
        } else {
            _userProfileFlow.value = null
            _userRoleFlow.value = null
            _isLoggedInFlow.value = false
        }
    }

    fun saveLogin(
        name: String,
        phone: String,
        locality: String,
        role: Role,
        workerSkill: String = "Electrician"
    ) {
        val cleanName = name.trim().ifEmpty {
            when (role) {
                Role.CUSTOMER -> "Ramesh Patil"
                Role.WORKER -> "Sunil Kumar"
                Role.COOPERATIVE_ADMIN -> "Cooperative Board"
            }
        }
        val cleanPhone = phone.trim().ifEmpty { "9845012345" }
        val cleanLocality = locality.trim().ifEmpty { "Indiranagar, Bangalore" }

        val profile = UserProfile(
            name = cleanName,
            phone = cleanPhone,
            locality = cleanLocality,
            role = role,
            workerSkill = workerSkill,
            isLoggedIn = true
        )

        sharedPreferences?.edit()?.apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_USER_NAME, cleanName)
            putString(KEY_USER_PHONE, cleanPhone)
            putString(KEY_USER_LOCALITY, cleanLocality)
            putString(KEY_USER_ROLE, role.name)
            putString(KEY_WORKER_SKILL, workerSkill)
            apply()
        }

        _userProfileFlow.value = profile
        _userRoleFlow.value = role
        _isLoggedInFlow.value = true
    }

    fun clearLogin() {
        sharedPreferences?.edit()?.apply {
            putBoolean(KEY_IS_LOGGED_IN, false)
            remove(KEY_USER_NAME)
            remove(KEY_USER_PHONE)
            remove(KEY_USER_LOCALITY)
            remove(KEY_USER_ROLE)
            remove(KEY_WORKER_SKILL)
            apply()
        }
        _userProfileFlow.value = null
        _userRoleFlow.value = null
        _isLoggedInFlow.value = false
    }

    fun getUserProfile(): UserProfile? = _userProfileFlow.value

    fun getPersistedRole(): Role? = _userRoleFlow.value

    fun isUserLoggedIn(): Boolean = _isLoggedInFlow.value
}
