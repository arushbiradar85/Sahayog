package com.example.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
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
    val KEY_USER_LOCALITY = stringPreferencesKey("user_locality")
    val KEY_USER_ROLE = stringPreferencesKey("user_role")
    val KEY_WORKER_SKILL = stringPreferencesKey("worker_skill")

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
                    val locality = preferences[KEY_USER_LOCALITY] ?: "Indiranagar, Bangalore"
                    val roleStr = preferences[KEY_USER_ROLE] ?: Role.CUSTOMER.name
                    val role = try {
                        Role.valueOf(roleStr)
                    } catch (e: Exception) {
                        Role.CUSTOMER
                    }
                    val workerSkill = preferences[KEY_WORKER_SKILL] ?: "Electrician"
                    UserProfile(
                        name = name,
                        phone = phone,
                        locality = locality,
                        role = role,
                        workerSkill = workerSkill,
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
        locality: String,
        role: Role,
        workerSkill: String = "Electrician"
    ) {
        context.userDataStore.edit { preferences ->
            preferences[KEY_IS_LOGGED_IN] = true
            preferences[KEY_USER_NAME] = name
            preferences[KEY_USER_PHONE] = phone
            preferences[KEY_USER_LOCALITY] = locality
            preferences[KEY_USER_ROLE] = role.name
            preferences[KEY_WORKER_SKILL] = workerSkill
        }
    }

    suspend fun clearUserProfile(context: Context) {
        context.userDataStore.edit { preferences ->
            preferences[KEY_IS_LOGGED_IN] = false
            preferences.remove(KEY_USER_NAME)
            preferences.remove(KEY_USER_PHONE)
            preferences.remove(KEY_USER_LOCALITY)
            preferences.remove(KEY_USER_ROLE)
            preferences.remove(KEY_WORKER_SKILL)
        }
    }
}
