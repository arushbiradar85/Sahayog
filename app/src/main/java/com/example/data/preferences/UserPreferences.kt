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
    val areaLocality: String = "Indiranagar",
    val cityDistrict: String = "Bangalore",
    val landmark: String = "",
    val locality: String = "Indiranagar, Bangalore",
    val role: Role = Role.CUSTOMER,
    val workerSkill: String = "Electrician",
    val experienceYears: Int = 4,
    val cooperativeBranch: String = "Bangalore Urban Workers Cooperative",
    val adminPosition: String = "Committee Secretary",
    val customerServiceInterest: String = "Floor Cleaning",
    val isLoggedIn: Boolean = false
)

object UserPreferences {
    private const val PREFS_NAME = "sahayog_user_prefs"
    private const val KEY_IS_LOGGED_IN = "key_is_logged_in"
    private const val KEY_USER_NAME = "key_user_name"
    private const val KEY_USER_PHONE = "key_user_phone"
    private const val KEY_USER_AREA = "key_user_area"
    private const val KEY_USER_CITY = "key_user_city"
    private const val KEY_USER_LANDMARK = "key_user_landmark"
    private const val KEY_USER_LOCALITY = "key_user_locality"
    private const val KEY_USER_ROLE = "key_user_role"
    private const val KEY_WORKER_SKILL = "key_worker_skill"
    private const val KEY_WORKER_EXP = "key_worker_exp"
    private const val KEY_COOP_BRANCH = "key_coop_branch"
    private const val KEY_ADMIN_POSITION = "key_admin_position"
    private const val KEY_SERVICE_INTEREST = "key_service_interest"

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
            val area = prefs.getString(KEY_USER_AREA, "Indiranagar") ?: "Indiranagar"
            val city = prefs.getString(KEY_USER_CITY, "Bangalore") ?: "Bangalore"
            val landmark = prefs.getString(KEY_USER_LANDMARK, "") ?: ""
            val locality = prefs.getString(KEY_USER_LOCALITY, "$area, $city") ?: "$area, $city"
            val workerSkill = prefs.getString(KEY_WORKER_SKILL, "Electrician") ?: "Electrician"
            val exp = prefs.getInt(KEY_WORKER_EXP, 4)
            val branch = prefs.getString(KEY_COOP_BRANCH, "Bangalore Urban Workers Cooperative") ?: "Bangalore Urban Workers Cooperative"
            val adminPos = prefs.getString(KEY_ADMIN_POSITION, "Committee Secretary") ?: "Committee Secretary"
            val interest = prefs.getString(KEY_SERVICE_INTEREST, "Floor Cleaning") ?: "Floor Cleaning"

            val profile = UserProfile(
                name = name,
                phone = phone,
                areaLocality = area,
                cityDistrict = city,
                landmark = landmark,
                locality = locality,
                role = role,
                workerSkill = workerSkill,
                experienceYears = exp,
                cooperativeBranch = branch,
                adminPosition = adminPos,
                customerServiceInterest = interest,
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
        areaLocality: String = "Indiranagar",
        cityDistrict: String = "Bangalore",
        landmark: String = "",
        role: Role,
        workerSkill: String = "Electrician",
        experienceYears: Int = 4,
        cooperativeBranch: String = "Bangalore Urban Workers Cooperative",
        adminPosition: String = "Committee Secretary",
        customerServiceInterest: String = "Floor Cleaning"
    ) {
        val cleanName = name.trim().ifEmpty {
            when (role) {
                Role.CUSTOMER -> "Ramesh Patil"
                Role.WORKER -> "Sunil Kumar"
                Role.COOPERATIVE_ADMIN -> "Cooperative Board"
            }
        }
        val cleanPhone = phone.trim().ifEmpty { "9845012345" }
        val cleanArea = areaLocality.trim().ifEmpty { "Indiranagar" }
        val cleanCity = cityDistrict.trim().ifEmpty { "Bangalore" }
        val locality = if (landmark.isNotBlank()) "$cleanArea, $cleanCity (Near $landmark)" else "$cleanArea, $cleanCity"

        val profile = UserProfile(
            name = cleanName,
            phone = cleanPhone,
            areaLocality = cleanArea,
            cityDistrict = cleanCity,
            landmark = landmark.trim(),
            locality = locality,
            role = role,
            workerSkill = workerSkill,
            experienceYears = experienceYears,
            cooperativeBranch = cooperativeBranch,
            adminPosition = adminPosition,
            customerServiceInterest = customerServiceInterest,
            isLoggedIn = true
        )

        sharedPreferences?.edit()?.apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_USER_NAME, cleanName)
            putString(KEY_USER_PHONE, cleanPhone)
            putString(KEY_USER_AREA, cleanArea)
            putString(KEY_USER_CITY, cleanCity)
            putString(KEY_USER_LANDMARK, landmark.trim())
            putString(KEY_USER_LOCALITY, locality)
            putString(KEY_USER_ROLE, role.name)
            putString(KEY_WORKER_SKILL, workerSkill)
            putInt(KEY_WORKER_EXP, experienceYears)
            putString(KEY_COOP_BRANCH, cooperativeBranch)
            putString(KEY_ADMIN_POSITION, adminPosition)
            putString(KEY_SERVICE_INTEREST, customerServiceInterest)
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
            remove(KEY_USER_AREA)
            remove(KEY_USER_CITY)
            remove(KEY_USER_LANDMARK)
            remove(KEY_USER_LOCALITY)
            remove(KEY_USER_ROLE)
            remove(KEY_WORKER_SKILL)
            remove(KEY_WORKER_EXP)
            remove(KEY_COOP_BRANCH)
            remove(KEY_ADMIN_POSITION)
            remove(KEY_SERVICE_INTEREST)
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
