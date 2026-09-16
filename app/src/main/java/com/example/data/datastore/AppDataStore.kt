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
    val KEY_WORKER_SKILL = stringPreferencesKey("worker_skill")
    val KEY_WORKER_EXP = intPreferencesKey("worker_exp")
    val KEY_COOP_BRANCH = stringPreferencesKey("coop_branch")
    val KEY_ADMIN_POSITION = stringPreferencesKey("admin_position")
    val KEY_SERVICE_INTEREST = stringPreferencesKey("service_interest")

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
                    val workerSkill = preferences[KEY_WORKER_SKILL] ?: "Electrician"
                    val exp = preferences[KEY_WORKER_EXP] ?: 4
                    val branch = preferences[KEY_COOP_BRANCH] ?: "Bangalore Urban Workers Cooperative"
                    val adminPos = preferences[KEY_ADMIN_POSITION] ?: "Committee Secretary"
                    val interest = preferences[KEY_SERVICE_INTEREST] ?: "Floor Cleaning"

                    UserProfile(
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
        workerSkill: String = "Electrician",
        experienceYears: Int = 4,
        cooperativeBranch: String = "Bangalore Urban Workers Cooperative",
        adminPosition: String = "Committee Secretary",
        customerServiceInterest: String = "Floor Cleaning"
    ) {
        val cleanArea = areaLocality.trim().ifEmpty { "Indiranagar" }
        val cleanCity = cityDistrict.trim().ifEmpty { "Bangalore" }
        val locality = if (landmark.isNotBlank()) "$cleanArea, $cleanCity (Near $landmark)" else "$cleanArea, $cleanCity"

        context.userDataStore.edit { preferences ->
            preferences[KEY_IS_LOGGED_IN] = true
            preferences[KEY_USER_NAME] = name
            preferences[KEY_USER_PHONE] = phone
            preferences[KEY_USER_AREA] = cleanArea
            preferences[KEY_USER_CITY] = cleanCity
            preferences[KEY_USER_LANDMARK] = landmark.trim()
            preferences[KEY_USER_LOCALITY] = locality
            preferences[KEY_USER_ROLE] = role.name
            preferences[KEY_WORKER_SKILL] = workerSkill
            preferences[KEY_WORKER_EXP] = experienceYears
            preferences[KEY_COOP_BRANCH] = cooperativeBranch
            preferences[KEY_ADMIN_POSITION] = adminPosition
            preferences[KEY_SERVICE_INTEREST] = customerServiceInterest
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
            preferences.remove(KEY_WORKER_SKILL)
            preferences.remove(KEY_WORKER_EXP)
            preferences.remove(KEY_COOP_BRANCH)
            preferences.remove(KEY_ADMIN_POSITION)
            preferences.remove(KEY_SERVICE_INTEREST)
        }
    }
}
