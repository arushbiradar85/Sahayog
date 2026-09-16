package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.model.Role
import com.example.data.preferences.UserPreferences
import com.example.data.repository.CoopRepository
import com.example.ui.screens.StartupScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.StartupUiState
import com.example.ui.viewmodel.StartupViewModel

class StartupActivity : ComponentActivity() {

    private val viewModel: StartupViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Preferences and Repository cache
        UserPreferences.init(applicationContext)
        CoopRepository.initPreferences(applicationContext)

        setContent {
            MyApplicationTheme {
                val uiState by viewModel.uiState.collectAsState()

                // StateFlow-driven navigation: Immediately route to role-based UI upon authentication
                LaunchedEffect(uiState) {
                    if (uiState is StartupUiState.Authenticated) {
                        val auth = uiState as StartupUiState.Authenticated
                        navigateToMain(auth.role)
                    }
                }

                when (uiState) {
                    is StartupUiState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(36.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    is StartupUiState.NeedsLogin -> {
                        StartupScreen(
                            onLoginSuccessWithDetails = { role, name, phone, locality, skill ->
                                viewModel.submitLoginAndRole(
                                    name = name,
                                    phone = phone,
                                    areaLocality = locality.substringBefore(",").trim(),
                                    cityDistrict = locality.substringAfter(",", "Bangalore").trim(),
                                    role = role,
                                    workerSkill = skill,
                                    onNavigated = { targetRole ->
                                        navigateToMain(targetRole)
                                    }
                                )
                            },
                            onCompleteOnboarding = { role, name, phone, area, city, landmark, skill, exp, branch, adminPos, interest ->
                                viewModel.submitLoginAndRole(
                                    name = name,
                                    phone = phone,
                                    areaLocality = area,
                                    cityDistrict = city,
                                    landmark = landmark,
                                    role = role,
                                    workerSkill = skill,
                                    experienceYears = exp,
                                    cooperativeBranch = branch,
                                    adminPosition = adminPos,
                                    customerServiceInterest = interest,
                                    onNavigated = { targetRole ->
                                        navigateToMain(targetRole)
                                    }
                                )
                            },
                            onLoginSuccess = { role ->
                                navigateToMain(role)
                            }
                        )
                    }
                    is StartupUiState.Authenticated -> {
                        // Already routing via LaunchedEffect
                    }
                }
            }
        }
    }

    private fun navigateToMain(role: Role) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(MainActivity.EXTRA_ROLE, role.name)
        }
        startActivity(intent)
        finish()
    }
}
