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
    val skills: List<String> = emptyList(),
    val experienceYears: Int = 4,
    val customerServiceInterest: String = "Floor Cleaning",
    val customerServiceInterests: List<String> = listOf("Floor Cleaning"),
    val isLoggedIn: Boolean = false
) {
    val workerSkill: String get() = skills.firstOrNull() ?: ""
    val workerPrimarySkill: String get() = skills.firstOrNull() ?: ""
    val workerSecondarySkills: List<String> get() = skills.drop(1)
    val cooperativeBranch: String get() = "Main District Cluster"
    val adminPosition: String get() = "Operations"
}

object UserPreferences {
    private const val PREFS_NAME = "sahayog_user_prefs"
    private const val KEY_IS_LOGGED_IN = "key_is_logged_in"
    private const val KEY_ACTIVE_ROLE = "key_active_role"

    // Legacy keys for backward compatibility
    private const val KEY_USER_NAME = "key_user_name"
    private const val KEY_USER_PHONE = "key_user_phone"
    private const val KEY_USER_AREA = "key_user_area"
    private const val KEY_USER_CITY = "key_user_city"
    private const val KEY_USER_LANDMARK = "key_user_landmark"
    private const val KEY_USER_LOCALITY = "key_user_locality"
    private const val KEY_USER_ROLE = "key_user_role"
    private const val KEY_WORKER_SKILLS = "key_worker_skills"
    private const val KEY_WORKER_EXP = "key_worker_exp"
    private const val KEY_SERVICE_INTERESTS = "key_service_interests"

    // Customer-isolated keys
    private const val KEY_CUSTOMER_LOGGED_IN = "key_customer_logged_in"
    private const val KEY_CUSTOMER_NAME = "key_customer_name"
    private const val KEY_CUSTOMER_PHONE = "key_customer_phone"
    private const val KEY_CUSTOMER_AREA = "key_customer_area"
    private const val KEY_CUSTOMER_CITY = "key_customer_city"
    private const val KEY_CUSTOMER_LANDMARK = "key_customer_landmark"
    private const val KEY_CUSTOMER_LOCALITY = "key_customer_locality"
    private const val KEY_CUSTOMER_INTERESTS = "key_customer_interests"

    // Worker-isolated keys
    private const val KEY_WORKER_LOGGED_IN = "key_worker_logged_in"
    private const val KEY_WORKER_NAME_SAVED = "key_worker_name_saved"
    private const val KEY_WORKER_PHONE_SAVED = "key_worker_phone_saved"
    private const val KEY_WORKER_AREA_SAVED = "key_worker_area_saved"
    private const val KEY_WORKER_CITY_SAVED = "key_worker_city_saved"
    private const val KEY_WORKER_LANDMARK_SAVED = "key_worker_landmark_saved"
    private const val KEY_WORKER_LOCALITY_SAVED = "key_worker_locality_saved"
    private const val KEY_WORKER_SKILLS_SAVED = "key_worker_skills_saved"
    private const val KEY_WORKER_EXP_SAVED = "key_worker_exp_saved"

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
        if (!isLoggedIn) {
            _userProfileFlow.value = null
            _userRoleFlow.value = null
            _isLoggedInFlow.value = false
            return
        }

        val activeRoleStr = prefs.getString(KEY_ACTIVE_ROLE, null) ?: prefs.getString(KEY_USER_ROLE, null)
        val role = activeRoleStr?.let {
            try {
                Role.valueOf(it)
            } catch (e: Exception) {
                null
            }
        }

        if (role == null) {
            _userProfileFlow.value = null
            _userRoleFlow.value = null
            _isLoggedInFlow.value = false
            return
        }

        if (role == Role.CUSTOMER) {
            val name = prefs.getString(KEY_CUSTOMER_NAME, null)
                ?: prefs.getString(KEY_USER_NAME, "Ramesh Patil") ?: "Ramesh Patil"
            val phone = prefs.getString(KEY_CUSTOMER_PHONE, null)
                ?: prefs.getString(KEY_USER_PHONE, "9845012345") ?: "9845012345"
            val area = prefs.getString(KEY_CUSTOMER_AREA, null)
                ?: prefs.getString(KEY_USER_AREA, "Indiranagar") ?: "Indiranagar"
            val city = prefs.getString(KEY_CUSTOMER_CITY, null)
                ?: prefs.getString(KEY_USER_CITY, "Bangalore") ?: "Bangalore"
            val landmark = prefs.getString(KEY_CUSTOMER_LANDMARK, null)
                ?: prefs.getString(KEY_USER_LANDMARK, "") ?: ""
            val locality = prefs.getString(KEY_CUSTOMER_LOCALITY, null)
                ?: prefs.getString(KEY_USER_LOCALITY, "$area, $city") ?: "$area, $city"
            val interestsRaw = prefs.getString(KEY_CUSTOMER_INTERESTS, null)
                ?: prefs.getString(KEY_SERVICE_INTERESTS, "Floor Cleaning") ?: "Floor Cleaning"
            val interests = interestsRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }.ifEmpty { listOf("Floor Cleaning") }

            val profile = UserProfile(
                name = name,
                phone = phone,
                areaLocality = area,
                cityDistrict = city,
                landmark = landmark,
                locality = locality,
                role = Role.CUSTOMER,
                skills = emptyList(), // Customers have no worker skills
                experienceYears = 0,
                customerServiceInterest = interests.firstOrNull() ?: "Floor Cleaning",
                customerServiceInterests = interests,
                isLoggedIn = true
            )
            _userProfileFlow.value = profile
            _userRoleFlow.value = Role.CUSTOMER
            _isLoggedInFlow.value = true
        } else {
            val name = prefs.getString(KEY_WORKER_NAME_SAVED, null)
                ?: prefs.getString(KEY_USER_NAME, "Sunil Kumar") ?: "Sunil Kumar"
            val phone = prefs.getString(KEY_WORKER_PHONE_SAVED, null)
                ?: prefs.getString(KEY_USER_PHONE, "9812345678") ?: "9812345678"
            val area = prefs.getString(KEY_WORKER_AREA_SAVED, null)
                ?: prefs.getString(KEY_USER_AREA, "Indiranagar") ?: "Indiranagar"
            val city = prefs.getString(KEY_WORKER_CITY_SAVED, null)
                ?: prefs.getString(KEY_USER_CITY, "Bangalore") ?: "Bangalore"
            val landmark = prefs.getString(KEY_WORKER_LANDMARK_SAVED, null)
                ?: prefs.getString(KEY_USER_LANDMARK, "") ?: ""
            val locality = prefs.getString(KEY_WORKER_LOCALITY_SAVED, null)
                ?: prefs.getString(KEY_USER_LOCALITY, "$area, $city") ?: "$area, $city"
            val skillsRaw = prefs.getString(KEY_WORKER_SKILLS_SAVED, null)
                ?: prefs.getString(KEY_WORKER_SKILLS, "Electrician") ?: "Electrician"
            val skills = skillsRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }.ifEmpty { listOf("Electrician") }
            val exp = if (prefs.contains(KEY_WORKER_EXP_SAVED)) prefs.getInt(KEY_WORKER_EXP_SAVED, 4) else prefs.getInt(KEY_WORKER_EXP, 4)

            val profile = UserProfile(
                name = name,
                phone = phone,
                areaLocality = area,
                cityDistrict = city,
                landmark = landmark,
                locality = locality,
                role = Role.WORKER,
                skills = skills,
                experienceYears = exp,
                customerServiceInterest = "Floor Cleaning",
                customerServiceInterests = listOf("Floor Cleaning"),
                isLoggedIn = true
            )
            _userProfileFlow.value = profile
            _userRoleFlow.value = Role.WORKER
            _isLoggedInFlow.value = true
        }
    }

    fun saveLogin(
        name: String,
        phone: String,
        areaLocality: String = "Indiranagar",
        cityDistrict: String = "Bangalore",
        landmark: String = "",
        role: Role,
        skills: List<String> = listOf("Electrician"),
        experienceYears: Int = 4,
        customerServiceInterests: List<String> = listOf("Floor Cleaning")
    ) {
        val cleanName = name.trim().ifEmpty {
            when (role) {
                Role.CUSTOMER -> "Ramesh Patil"
                Role.WORKER -> "Sunil Kumar"
            }
        }
        val cleanPhone = phone.trim().ifEmpty { "9845012345" }
        val cleanArea = areaLocality.trim().ifEmpty { "Indiranagar" }
        val cleanCity = cityDistrict.trim().ifEmpty { "Bangalore" }
        val locality = if (landmark.isNotBlank()) "$cleanArea, $cleanCity (Near $landmark)" else "$cleanArea, $cleanCity"
        val cleanSkills = skills.filter { it.isNotBlank() }.ifEmpty { listOf("Electrician") }
        val cleanInterests = customerServiceInterests.filter { it.isNotBlank() }.ifEmpty { listOf("Floor Cleaning") }

        sharedPreferences?.edit()?.apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_ACTIVE_ROLE, role.name)
            putString(KEY_USER_NAME, cleanName)
            putString(KEY_USER_PHONE, cleanPhone)
            putString(KEY_USER_AREA, cleanArea)
            putString(KEY_USER_CITY, cleanCity)
            putString(KEY_USER_LANDMARK, landmark.trim())
            putString(KEY_USER_LOCALITY, locality)
            putString(KEY_USER_ROLE, role.name)

            if (role == Role.CUSTOMER) {
                putBoolean(KEY_CUSTOMER_LOGGED_IN, true)
                putString(KEY_CUSTOMER_NAME, cleanName)
                putString(KEY_CUSTOMER_PHONE, cleanPhone)
                putString(KEY_CUSTOMER_AREA, cleanArea)
                putString(KEY_CUSTOMER_CITY, cleanCity)
                putString(KEY_CUSTOMER_LANDMARK, landmark.trim())
                putString(KEY_CUSTOMER_LOCALITY, locality)
                putString(KEY_CUSTOMER_INTERESTS, cleanInterests.joinToString(","))
                putString(KEY_SERVICE_INTERESTS, cleanInterests.joinToString(","))
            } else {
                putBoolean(KEY_WORKER_LOGGED_IN, true)
                putString(KEY_WORKER_NAME_SAVED, cleanName)
                putString(KEY_WORKER_PHONE_SAVED, cleanPhone)
                putString(KEY_WORKER_AREA_SAVED, cleanArea)
                putString(KEY_WORKER_CITY_SAVED, cleanCity)
                putString(KEY_WORKER_LANDMARK_SAVED, landmark.trim())
                putString(KEY_WORKER_LOCALITY_SAVED, locality)
                putString(KEY_WORKER_SKILLS_SAVED, cleanSkills.joinToString(","))
                putInt(KEY_WORKER_EXP_SAVED, experienceYears)
                putString(KEY_WORKER_SKILLS, cleanSkills.joinToString(","))
                putInt(KEY_WORKER_EXP, experienceYears)
            }
            apply()
        }

        loadFromPreferences()
    }

    fun switchRole(targetRole: Role) {
        val prefs = sharedPreferences ?: return
        val currentCustName = prefs.getString(KEY_CUSTOMER_NAME, null)
        val currentWorkerName = prefs.getString(KEY_WORKER_NAME_SAVED, null)

        if (targetRole == Role.WORKER && currentWorkerName == null) {
            // Initialize separate worker identity so customer name does not bleed into worker
            val providerName = if (!currentCustName.isNullOrBlank()) "$currentCustName (Provider)" else "Sunil Kumar"
            val phone = prefs.getString(KEY_CUSTOMER_PHONE, "9812345678") ?: "9812345678"
            val area = prefs.getString(KEY_CUSTOMER_AREA, "Indiranagar") ?: "Indiranagar"
            val city = prefs.getString(KEY_CUSTOMER_CITY, "Bangalore") ?: "Bangalore"
            val landmark = prefs.getString(KEY_CUSTOMER_LANDMARK, "") ?: ""
            val locality = prefs.getString(KEY_CUSTOMER_LOCALITY, "$area, $city") ?: "$area, $city"

            prefs.edit().apply {
                putBoolean(KEY_WORKER_LOGGED_IN, true)
                putString(KEY_WORKER_NAME_SAVED, providerName)
                putString(KEY_WORKER_PHONE_SAVED, phone)
                putString(KEY_WORKER_AREA_SAVED, area)
                putString(KEY_WORKER_CITY_SAVED, city)
                putString(KEY_WORKER_LANDMARK_SAVED, landmark)
                putString(KEY_WORKER_LOCALITY_SAVED, locality)
                putString(KEY_WORKER_SKILLS_SAVED, "Electrician,Gardener")
                putInt(KEY_WORKER_EXP_SAVED, 4)
                apply()
            }
        } else if (targetRole == Role.CUSTOMER && currentCustName == null) {
            val custName = if (!currentWorkerName.isNullOrBlank()) currentWorkerName.replace(" (Provider)", "") else "Ramesh Patil"
            val phone = prefs.getString(KEY_WORKER_PHONE_SAVED, "9845012345") ?: "9845012345"
            val area = prefs.getString(KEY_WORKER_AREA_SAVED, "Indiranagar") ?: "Indiranagar"
            val city = prefs.getString(KEY_WORKER_CITY_SAVED, "Bangalore") ?: "Bangalore"
            val landmark = prefs.getString(KEY_WORKER_LANDMARK_SAVED, "") ?: ""
            val locality = prefs.getString(KEY_WORKER_LOCALITY_SAVED, "$area, $city") ?: "$area, $city"

            prefs.edit().apply {
                putBoolean(KEY_CUSTOMER_LOGGED_IN, true)
                putString(KEY_CUSTOMER_NAME, custName)
                putString(KEY_CUSTOMER_PHONE, phone)
                putString(KEY_CUSTOMER_AREA, area)
                putString(KEY_CUSTOMER_CITY, city)
                putString(KEY_CUSTOMER_LANDMARK, landmark)
                putString(KEY_CUSTOMER_LOCALITY, locality)
                putString(KEY_CUSTOMER_INTERESTS, "Floor Cleaning")
                apply()
            }
        }

        prefs.edit().apply {
            putString(KEY_ACTIVE_ROLE, targetRole.name)
            putString(KEY_USER_ROLE, targetRole.name)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }

        loadFromPreferences()
    }

    fun updateWorkerSkills(newSkills: List<String>) {
        val cleanSkills = newSkills.filter { it.isNotBlank() }.ifEmpty { listOf("Electrician") }
        sharedPreferences?.edit()?.apply {
            putString(KEY_WORKER_SKILLS, cleanSkills.joinToString(","))
            putString(KEY_WORKER_SKILLS_SAVED, cleanSkills.joinToString(","))
            apply()
        }
        loadFromPreferences()
    }

    fun clearLogin() {
        sharedPreferences?.edit()?.clear()?.apply()
        _userProfileFlow.value = null
        _userRoleFlow.value = null
        _isLoggedInFlow.value = false
    }

    fun getUserProfile(): UserProfile? = _userProfileFlow.value

    fun getPersistedRole(): Role? = _userRoleFlow.value

    fun isUserLoggedIn(): Boolean = _isLoggedInFlow.value
}
