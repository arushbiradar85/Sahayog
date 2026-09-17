package com.example.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.Role
import com.example.data.preferences.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.userDataStore: DataStore<Preferences> by preferencesDataStore(name = "sahayog_user_datastore")

object AppDataStore {

    val KEY_IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
    val KEY_ACTIVE_ROLE = stringPreferencesKey("active_role")

    // Legacy keys
    val KEY_USER_NAME = stringPreferencesKey("user_name")
    val KEY_USER_PHONE = stringPreferencesKey("user_phone")
    val KEY_USER_AREA = stringPreferencesKey("user_area")
    val KEY_USER_CITY = stringPreferencesKey("user_city")
    val KEY_USER_LANDMARK = stringPreferencesKey("user_landmark")
    val KEY_USER_LOCALITY = stringPreferencesKey("user_locality")
    val KEY_USER_ROLE = stringPreferencesKey("user_role")
    val KEY_WORKER_SKILLS = stringPreferencesKey("worker_skills")
    val KEY_WORKER_EXP = intPreferencesKey("worker_exp")
    val KEY_SERVICE_INTERESTS = stringPreferencesKey("service_interests")

    // Customer keys
    val KEY_CUSTOMER_NAME = stringPreferencesKey("customer_name")
    val KEY_CUSTOMER_PHONE = stringPreferencesKey("customer_phone")
    val KEY_CUSTOMER_AREA = stringPreferencesKey("customer_area")
    val KEY_CUSTOMER_CITY = stringPreferencesKey("customer_city")
    val KEY_CUSTOMER_LANDMARK = stringPreferencesKey("customer_landmark")
    val KEY_CUSTOMER_LOCALITY = stringPreferencesKey("customer_locality")
    val KEY_CUSTOMER_INTERESTS = stringPreferencesKey("customer_interests")

    // Worker keys
    val KEY_WORKER_NAME_SAVED = stringPreferencesKey("worker_name_saved")
    val KEY_WORKER_PHONE_SAVED = stringPreferencesKey("worker_phone_saved")
    val KEY_WORKER_AREA_SAVED = stringPreferencesKey("worker_area_saved")
    val KEY_WORKER_CITY_SAVED = stringPreferencesKey("worker_city_saved")
    val KEY_WORKER_LANDMARK_SAVED = stringPreferencesKey("worker_landmark_saved")
    val KEY_WORKER_LOCALITY_SAVED = stringPreferencesKey("worker_locality_saved")
    val KEY_WORKER_SKILLS_SAVED = stringPreferencesKey("worker_skills_saved")
    val KEY_WORKER_EXP_SAVED = intPreferencesKey("worker_exp_saved")

    fun getUserProfileFlow(context: Context): Flow<UserProfile?> {
        return context.userDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                val isLoggedIn = preferences[KEY_IS_LOGGED_IN] ?: false
                if (!isLoggedIn) {
                    null
                } else {
                    val roleStr = preferences[KEY_ACTIVE_ROLE] ?: preferences[KEY_USER_ROLE]
                    if (roleStr == null) {
                        null
                    } else {
                        val role = try {
                            Role.valueOf(roleStr)
                        } catch (e: Exception) {
                            null
                        }

                        if (role == null) {
                            null
                        } else if (role == Role.CUSTOMER) {
                            val name = preferences[KEY_CUSTOMER_NAME] ?: preferences[KEY_USER_NAME] ?: "Ramesh Patil"
                            val phone = preferences[KEY_CUSTOMER_PHONE] ?: preferences[KEY_USER_PHONE] ?: "9845012345"
                            val area = preferences[KEY_CUSTOMER_AREA] ?: preferences[KEY_USER_AREA] ?: "Indiranagar"
                            val city = preferences[KEY_CUSTOMER_CITY] ?: preferences[KEY_USER_CITY] ?: "Bangalore"
                            val landmark = preferences[KEY_CUSTOMER_LANDMARK] ?: preferences[KEY_USER_LANDMARK] ?: ""
                            val locality = preferences[KEY_CUSTOMER_LOCALITY] ?: preferences[KEY_USER_LOCALITY] ?: "$area, $city"
                            val interestsRaw = preferences[KEY_CUSTOMER_INTERESTS] ?: preferences[KEY_SERVICE_INTERESTS] ?: "Floor Cleaning"
                            val interests = interestsRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }.ifEmpty { listOf("Floor Cleaning") }

                            UserProfile(
                                name = name,
                                phone = phone,
                                areaLocality = area,
                                cityDistrict = city,
                                landmark = landmark,
                                locality = locality,
                                role = Role.CUSTOMER,
                                skills = emptyList(),
                                experienceYears = 0,
                                customerServiceInterest = interests.firstOrNull() ?: "Floor Cleaning",
                                customerServiceInterests = interests,
                                isLoggedIn = true
                            )
                        } else {
                            val name = preferences[KEY_WORKER_NAME_SAVED] ?: preferences[KEY_USER_NAME] ?: "Sunil Kumar"
                            val phone = preferences[KEY_WORKER_PHONE_SAVED] ?: preferences[KEY_USER_PHONE] ?: "9812345678"
                            val area = preferences[KEY_WORKER_AREA_SAVED] ?: preferences[KEY_USER_AREA] ?: "Indiranagar"
                            val city = preferences[KEY_WORKER_CITY_SAVED] ?: preferences[KEY_USER_CITY] ?: "Bangalore"
                            val landmark = preferences[KEY_WORKER_LANDMARK_SAVED] ?: preferences[KEY_USER_LANDMARK] ?: ""
                            val locality = preferences[KEY_WORKER_LOCALITY_SAVED] ?: preferences[KEY_USER_LOCALITY] ?: "$area, $city"
                            val skillsRaw = preferences[KEY_WORKER_SKILLS_SAVED] ?: preferences[KEY_WORKER_SKILLS] ?: "Electrician"
                            val skills = skillsRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }.ifEmpty { listOf("Electrician") }
                            val exp = preferences[KEY_WORKER_EXP_SAVED] ?: preferences[KEY_WORKER_EXP] ?: 4

                            UserProfile(
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
                        }
                    }
                }
            }
    }

    suspend fun getUserProfile(context: Context): UserProfile? {
        return getUserProfileFlow(context).firstOrNull()
    }

    suspend fun saveUserProfile(
        context: Context,
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
        val cleanArea = areaLocality.trim().ifEmpty { "Indiranagar" }
        val cleanCity = cityDistrict.trim().ifEmpty { "Bangalore" }
        val locality = if (landmark.isNotBlank()) "$cleanArea, $cleanCity (Near $landmark)" else "$cleanArea, $cleanCity"
        val cleanSkills = skills.filter { it.isNotBlank() }.ifEmpty { listOf("Electrician") }
        val cleanInterests = customerServiceInterests.filter { it.isNotBlank() }.ifEmpty { listOf("Floor Cleaning") }

        context.userDataStore.edit { preferences ->
            preferences[KEY_IS_LOGGED_IN] = true
            preferences[KEY_ACTIVE_ROLE] = role.name
            preferences[KEY_USER_ROLE] = role.name
            preferences[KEY_USER_NAME] = name
            preferences[KEY_USER_PHONE] = phone
            preferences[KEY_USER_AREA] = cleanArea
            preferences[KEY_USER_CITY] = cleanCity
            preferences[KEY_USER_LANDMARK] = landmark.trim()
            preferences[KEY_USER_LOCALITY] = locality

            if (role == Role.CUSTOMER) {
                preferences[KEY_CUSTOMER_NAME] = name
                preferences[KEY_CUSTOMER_PHONE] = phone
                preferences[KEY_CUSTOMER_AREA] = cleanArea
                preferences[KEY_CUSTOMER_CITY] = cleanCity
                preferences[KEY_CUSTOMER_LANDMARK] = landmark.trim()
                preferences[KEY_CUSTOMER_LOCALITY] = locality
                preferences[KEY_CUSTOMER_INTERESTS] = cleanInterests.joinToString(",")
                preferences[KEY_SERVICE_INTERESTS] = cleanInterests.joinToString(",")
            } else {
                preferences[KEY_WORKER_NAME_SAVED] = name
                preferences[KEY_WORKER_PHONE_SAVED] = phone
                preferences[KEY_WORKER_AREA_SAVED] = cleanArea
                preferences[KEY_WORKER_CITY_SAVED] = cleanCity
                preferences[KEY_WORKER_LANDMARK_SAVED] = landmark.trim()
                preferences[KEY_WORKER_LOCALITY_SAVED] = locality
                preferences[KEY_WORKER_SKILLS_SAVED] = cleanSkills.joinToString(",")
                preferences[KEY_WORKER_EXP_SAVED] = experienceYears
                preferences[KEY_WORKER_SKILLS] = cleanSkills.joinToString(",")
                preferences[KEY_WORKER_EXP] = experienceYears
            }
        }
    }

    suspend fun switchRole(context: Context, targetRole: Role) {
        context.userDataStore.edit { preferences ->
            preferences[KEY_ACTIVE_ROLE] = targetRole.name
            preferences[KEY_USER_ROLE] = targetRole.name
        }
    }

    suspend fun clearUserProfile(context: Context) {
        context.userDataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
