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
                    val name = preferences[KEY_USER_NAME] ?: "Ramesh Patil"
                    val phone = preferences[KEY_USER_PHONE] ?: "9845012345"
                    val area = preferences[KEY_USER_AREA] ?: "Indiranagar"
                    val city = preferences[KEY_USER_CITY] ?: "Bangalore"
                    val landmark = preferences[KEY_USER_LANDMARK] ?: ""
                    val locality = preferences[KEY_USER_LOCALITY] ?: "$area, $city"
                    val roleStr = preferences[KEY_USER_ROLE] ?: Role.CUSTOMER.name
                    val role = try {
                        Role.valueOf(roleStr)
                    } catch (e: Exception) {
                        Role.CUSTOMER
                    }
                    val skillsRaw = preferences[KEY_WORKER_SKILLS] ?: "Electrician"
                    val skills = skillsRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }.ifEmpty { listOf("Electrician") }
                    val exp = preferences[KEY_WORKER_EXP] ?: 4
                    val interestsRaw = preferences[KEY_SERVICE_INTERESTS] ?: "Floor Cleaning"
                    val interests = interestsRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }.ifEmpty { listOf("Floor Cleaning") }

                    UserProfile(
                        name = name,
                        phone = phone,
                        areaLocality = area,
                        cityDistrict = city,
                        landmark = landmark,
                        locality = locality,
                        role = role,
                        skills = skills,
                        experienceYears = exp,
                        customerServiceInterest = interests.firstOrNull() ?: "Floor Cleaning",
                        customerServiceInterests = interests,
                        isLoggedIn = true
                    )
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
            preferences[KEY_USER_NAME] = name
            preferences[KEY_USER_PHONE] = phone
            preferences[KEY_USER_AREA] = cleanArea
            preferences[KEY_USER_CITY] = cleanCity
            preferences[KEY_USER_LANDMARK] = landmark.trim()
            preferences[KEY_USER_LOCALITY] = locality
            preferences[KEY_USER_ROLE] = role.name
            preferences[KEY_WORKER_SKILLS] = cleanSkills.joinToString("," )
            preferences[KEY_WORKER_EXP] = experienceYears
            preferences[KEY_SERVICE_INTERESTS] = cleanInterests.joinToString(",")
        }
    }

    suspend fun clearUserProfile(context: Context) {
        context.userDataStore.edit { preferences ->
            preferences[KEY_IS_LOGGED_IN] = false
            preferences.remove(KEY_USER_NAME)
            preferences.remove(KEY_USER_PHONE)
            preferences.remove(KEY_USER_AREA)
            preferences.remove(KEY_USER_CITY)
            preferences.remove(KEY_USER_LANDMARK)
            preferences.remove(KEY_USER_LOCALITY)
            preferences.remove(KEY_USER_ROLE)
            preferences.remove(KEY_WORKER_SKILLS)
            preferences.remove(KEY_WORKER_EXP)
            preferences.remove(KEY_SERVICE_INTERESTS)
        }
    }
}
