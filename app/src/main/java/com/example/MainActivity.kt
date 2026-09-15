package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Role
import com.example.data.preferences.UserPreferences
import com.example.data.repository.CoopRepository
import com.example.ui.components.OfflineNoticeBanner
import com.example.ui.screens.AdminScreen
import com.example.ui.screens.CustomerScreen
import com.example.ui.screens.FairnessScreen
import com.example.ui.screens.StartupScreen
import com.example.ui.screens.WorkerScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.util.AppLanguage
import com.example.util.Localization

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_ROLE = "EXTRA_ROLE"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        UserPreferences.init(applicationContext)
        CoopRepository.initPreferences(applicationContext)

        intent?.getStringExtra(EXTRA_ROLE)?.let { roleName ->
            try {
                val role = Role.valueOf(roleName)
                CoopRepository.setRole(role)
            } catch (e: Exception) {}
        }

        setContent {
            MyApplicationTheme {
                SahayogMainApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SahayogMainApp() {
    val currentRole by CoopRepository.currentRole.collectAsState()
    val hasSelectedRole by CoopRepository.hasSelectedRole.collectAsState()
    val activeProfile by CoopRepository.activeUserProfile.collectAsState()
    val language by CoopRepository.appLanguage.collectAsState()
    val isMarathi = Localization.isMarathi(language)

    var showFairnessScreen by remember { mutableStateOf(false) }
    var showResetConfirmationDialog by remember { mutableStateOf(false) }

    // If user has not chosen role / logged in, show minimal service-oriented StartupScreen
    if (!hasSelectedRole) {
        StartupScreen(
            onLoginSuccess = { role ->
                CoopRepository.selectInitialPersona(role)
            }
        )
        return
    }

    if (showFairnessScreen) {
        FairnessScreen(
            repository = CoopRepository,
            onBack = { showFairnessScreen = false }
        )
        return
    }

    val roleSubtitle = when (currentRole) {
        Role.CUSTOMER -> if (isMarathi) "ग्राहक सेवा मंच" else "Consumer Services"
        Role.WORKER -> if (isMarathi) "कामगार डॅशबोर्ड" else "Artisan Dashboard"
        Role.COOPERATIVE_ADMIN -> if (isMarathi) "सहकारी समिती प्रशासन" else "Cooperative Admin"
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = activeProfile?.name?.take(18) ?: Localization.appTitle(language),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = when (currentRole) {
                                        Role.CUSTOMER -> if (isMarathi) "ग्राहक" else "Consumer"
                                        Role.WORKER -> if (isMarathi) "कामगार" else "Worker"
                                        Role.COOPERATIVE_ADMIN -> if (isMarathi) "समिती" else "Admin"
                                    },
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = if (activeProfile != null) "${activeProfile?.locality?.take(16)} • $roleSubtitle" else roleSubtitle,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                        )
                    }
                },
                navigationIcon = {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Sahayog Logo",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .size(24.dp)
                    )
                },
                actions = {
                    // 1-Tap Language Toggle Button (English <-> Marathi)
                    Surface(
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { CoopRepository.toggleLanguage() }
                            .testTag("language_toggle_button"),
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.22f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Translate,
                                contentDescription = "Toggle Language",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isMarathi) "मराठी" else "Eng",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Switch Role / Logout Button (Allows re-selecting Consumer / Provider without locking user out)
                    IconButton(
                        onClick = { CoopRepository.logoutUser() },
                        modifier = Modifier.testTag("switch_role_action_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = Localization.switchRole(language),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    // Reset Demo Data
                    IconButton(
                        onClick = { showResetConfirmationDialog = true },
                        modifier = Modifier.testTag("reset_demo_data_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = Localization.resetDemo(language),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Offline security badge
            OfflineNoticeBanner()

            Spacer(modifier = Modifier.height(2.dp))

            // Shows ONLY the selected persona's screen!
            BoxContent(
                currentRole = currentRole,
                onOpenFairness = { showFairnessScreen = true },
                modifier = Modifier.weight(1f)
            )
        }
    }

    // Reset Confirmation Dialog
    if (showResetConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmationDialog = false },
            title = {
                Text(
                    text = if (isMarathi) "डेमो डेटा पुन्हा सुरू करायचा?" else "Reset Hackathon Demo Data?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (isMarathi)
                        "यामुळे सर्व २० कामगार, १० ग्राहक, ३ सहकारी संस्था आणि ३०+ कामांचा खराखुरा हिशोब पुन्हा मूळ स्थितीत येईल."
                    else
                        "This will restore all 20 workers, 10 customers, 3 cooperatives, 30+ jobs across all statuses, and valid SHA-256 ledger blocks."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        CoopRepository.resetToSeedData()
                        showResetConfirmationDialog = false
                    }
                ) {
                    Text(if (isMarathi) "सर्व रीसेट करा" else "Reset Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmationDialog = false }) {
                    Text(Localization.cancel(language))
                }
            }
        )
    }
}

@Composable
private fun BoxContent(
    currentRole: Role,
    onOpenFairness: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.layout.Box(modifier = modifier) {
        when (currentRole) {
            Role.CUSTOMER -> {
                CustomerScreen(repository = CoopRepository)
            }
            Role.WORKER -> {
                WorkerScreen(repository = CoopRepository)
            }
            Role.COOPERATIVE_ADMIN -> {
                AdminScreen(
                    repository = CoopRepository,
                    onOpenFairnessComparison = onOpenFairness
                )
            }
        }
    }
}
