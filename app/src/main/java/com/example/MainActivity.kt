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
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import com.example.ui.screens.CustomerScreen
import com.example.ui.screens.FairnessScreen
import com.example.ui.screens.StartupScreen
import com.example.ui.screens.WorkerScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.util.AppLanguage
import com.example.util.Localization
import com.example.util.TwoDeviceSyncManager

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_ROLE = "EXTRA_ROLE"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        UserPreferences.init(applicationContext)
        CoopRepository.initPreferences(applicationContext)

        val extraRole = intent?.getStringExtra(EXTRA_ROLE)
        if (extraRole != null) {
            try {
                val role = Role.valueOf(extraRole)
                CoopRepository.setRole(role)
            } catch (e: Exception) {}
        } else {
            val savedRole = UserPreferences.getPersistedRole()
            if (savedRole != null) {
                CoopRepository.setRole(savedRole)
            }
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
    var showHubDialog by remember { mutableStateOf(false) }
    var showRoleSwitchDialog by remember { mutableStateOf(false) }

    val isHubMode by TwoDeviceSyncManager.isHubMode.collectAsState()
    val localIp by TwoDeviceSyncManager.localIp.collectAsState()
    val hubIp by TwoDeviceSyncManager.hubIp.collectAsState()
    val syncLog by TwoDeviceSyncManager.syncLog.collectAsState()
    val requestsCount by TwoDeviceSyncManager.requestsCount.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current

    // If user has not logged in or has no valid profile, route directly to StartupActivity
    if (!hasSelectedRole || activeProfile == null || !activeProfile!!.isLoggedIn) {
        androidx.compose.runtime.LaunchedEffect(Unit) {
            val intent = android.content.Intent(context, StartupActivity::class.java).apply {
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            context.startActivity(intent)
            if (context is android.app.Activity) {
                context.finish()
            }
        }
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
                    // Hub Network Status Chip
                    Surface(
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { showHubDialog = true },
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.22f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Wifi,
                                contentDescription = "Network Hub",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isHubMode) "Hub" else "Client",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }

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

                    // Switch Role Button
                    IconButton(
                        onClick = { showRoleSwitchDialog = true },
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

            // Shows selected persona's screen
            BoxContent(
                currentRole = currentRole,
                onOpenFairness = { showFairnessScreen = true },
                modifier = Modifier.weight(1f)
            )
        }
    }

    // Hub Network Setup Dialog
    if (showHubDialog) {
        var ipInput by remember { mutableStateOf(hubIp) }
        AlertDialog(
            onDismissRequest = { showHubDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Wifi, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sahayog Local Hub Network", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Connect two phones on the same Wi-Fi/Hotspot without internet.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("This Phone's IP: $localIp:8989", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                            Text("Requests stored in Hub memory & disk: $requestsCount", style = MaterialTheme.typography.bodySmall)
                            Text("Status: $syncLog", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Run this device as Sahayog Hub", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                        androidx.compose.material3.Switch(
                            checked = isHubMode,
                            onCheckedChange = { TwoDeviceSyncManager.setHubMode(it) }
                        )
                    }

                    if (!isHubMode) {
                        androidx.compose.material3.OutlinedTextField(
                            value = ipInput,
                            onValueChange = {
                                ipInput = it
                                TwoDeviceSyncManager.setHubIp(it)
                            },
                            label = { Text("Hub Device IP Address") },
                            placeholder = { Text("e.g. 192.168.1.10") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showHubDialog = false }) {
                    Text("Done")
                }
            }
        )
    }

    // Role Switch Confirmation Dialog
    if (showRoleSwitchDialog) {
        val targetRole = if (currentRole == Role.CUSTOMER) Role.WORKER else Role.CUSTOMER
        val targetRoleName = if (targetRole == Role.WORKER) {
            if (isMarathi) "कामगार / सेवा प्रदाता" else "Provider / Worker"
        } else {
            if (isMarathi) "ग्राहक" else "Customer"
        }
        AlertDialog(
            onDismissRequest = { showRoleSwitchDialog = false },
            title = {
                Text(
                    text = if (isMarathi) "भूमिका बदला" else "Switch Dashboard Role",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (currentRole == Role.CUSTOMER) {
                        if (isMarathi) "तुम्हाला कामगार / सेवा प्रदाता डॅशबोर्डवर जायचे आहे का? तुमचे ग्राहक प्रोफाइल आणि सेवा विनंत्या सुरक्षित राहतील."
                        else "Switch to Provider / Worker dashboard? Your Customer profile and service requests will be preserved safely."
                    } else {
                        if (isMarathi) "तुम्हाला ग्राहक डॅशबोर्डवर जायचे आहे का? तुमचे कामगार प्रोफाइल आणि कामे सुरक्षित राहतील."
                        else "Switch to Customer dashboard? Your Worker profile and bookings will be preserved safely."
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRoleSwitchDialog = false
                        CoopRepository.switchRole()
                    },
                    modifier = Modifier.testTag("confirm_switch_role_btn")
                ) {
                    Text(if (isMarathi) "स्विच करा ($targetRoleName)" else "Switch to $targetRoleName")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showRoleSwitchDialog = false },
                    modifier = Modifier.testTag("cancel_switch_role_btn")
                ) {
                    Text(if (isMarathi) "रद्द करा" else "Cancel")
                }
            }
        )
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
        }
    }
}
