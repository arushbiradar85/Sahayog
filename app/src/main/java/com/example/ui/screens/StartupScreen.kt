package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Role
import com.example.data.repository.CoopRepository
import com.example.util.AppLanguage
import com.example.util.Localization

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartupScreen(
    onLoginSuccess: (Role) -> Unit = {},
    onLoginSuccessWithDetails: ((Role, String, String, String, String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val language by CoopRepository.appLanguage.collectAsState()
    val isMarathi = Localization.isMarathi(language)

    var selectedRole by remember { mutableStateOf(Role.CUSTOMER) }
    var nameInput by remember { mutableStateOf("Ramesh Patil") }
    var phoneInput by remember { mutableStateOf("9845012345") }
    var localityInput by remember { mutableStateOf("Indiranagar, Bangalore") }
    var workerSkill by remember { mutableStateOf("Electrician") }
    var skillDropdownExpanded by remember { mutableStateOf(false) }

    val availableSkills = listOf("Electrician", "Plumber", "Carpenter", "Cleaning", "Painter", "Mason")

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Language Switcher & Official Cooperative Header
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Translate,
                                contentDescription = "Language",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isMarathi) "भाषा निवडा:" else "Select Language:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), RoundedCornerShape(16.dp))
                                .padding(2.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val marathiActive = isMarathi
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (marathiActive) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { CoopRepository.setLanguage(AppLanguage.MARATHI) }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                                    .testTag("lang_marathi_chip")
                            ) {
                                Text(
                                    "मराठी",
                                    color = if (marathiActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (marathiActive) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (!marathiActive) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { CoopRepository.setLanguage(AppLanguage.ENGLISH) }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                                    .testTag("lang_english_chip")
                            ) {
                                Text(
                                    "English",
                                    color = if (!marathiActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (!marathiActive) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // Minimal, Normalized Service-Oriented Brand Header
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Sahayog Cooperative",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isMarathi) "सहयोग कामगार सहकारी संस्था" else "Sahayog Workers Cooperative",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (isMarathi) "सदस्य नोंदणी व भूमिका निवड (Role & Login)" else "Member Login & Role Onboarding",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Quick Demo Presets (for fast evaluation)
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = if (isMarathi) "त्वरित माहिती भरा (Quick Fill):" else "Quick Demo Fill:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Customer preset
                        PresetChip(
                            label = if (isMarathi) "ग्राहक (रमेश)" else "Customer (Ramesh)",
                            isSelected = selectedRole == Role.CUSTOMER && nameInput == "Ramesh Patil",
                            onClick = {
                                selectedRole = Role.CUSTOMER
                                nameInput = "Ramesh Patil"
                                phoneInput = "9845012345"
                                localityInput = "Indiranagar, Bangalore"
                            },
                            modifier = Modifier.weight(1f)
                        )
                        // Worker preset
                        PresetChip(
                            label = if (isMarathi) "कामगार (सुनील)" else "Worker (Sunil)",
                            isSelected = selectedRole == Role.WORKER && nameInput == "Sunil Kumar",
                            onClick = {
                                selectedRole = Role.WORKER
                                nameInput = "Sunil Kumar"
                                phoneInput = "9876543210"
                                localityInput = "Koramangala, Bangalore"
                                workerSkill = "Electrician"
                            },
                            modifier = Modifier.weight(1f)
                        )
                        // Admin preset
                        PresetChip(
                            label = if (isMarathi) "समिती (Admin)" else "Admin (Board)",
                            isSelected = selectedRole == Role.COOPERATIVE_ADMIN,
                            onClick = {
                                selectedRole = Role.COOPERATIVE_ADMIN
                                nameInput = "Cooperative Board"
                                phoneInput = "9811122233"
                                localityInput = "Bangalore Central"
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // STEP 1: LOGIN DETAILS
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isMarathi) "१. वापरकर्ता माहिती (Login Profile)" else "1. User Information",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Name
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            label = { Text(if (isMarathi) "पूर्ण नाव (Full Name)" else "Full Name") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_name_input")
                        )

                        // Phone
                        OutlinedTextField(
                            value = phoneInput,
                            onValueChange = { phoneInput = it },
                            label = { Text(if (isMarathi) "मोबाईल नंबर (Phone Number)" else "Phone Number") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_phone_input")
                        )

                        // Locality
                        OutlinedTextField(
                            value = localityInput,
                            onValueChange = { localityInput = it },
                            label = { Text(if (isMarathi) "गाव / शहर परिसर (Area / Village)" else "Village / City Locality") },
                            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_locality_input")
                        )
                    }
                }
            }

            // STEP 2: SELECT ROLE (Minimal & Service Oriented)
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (isMarathi) "२. तुमची भूमिका निवडा (Select Role)" else "2. Select Your Role",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Role 1: Consumer
                    ServiceRoleCard(
                        role = Role.CUSTOMER,
                        isSelected = selectedRole == Role.CUSTOMER,
                        title = if (isMarathi) "ग्राहक (Consumer / Home Owner)" else "Customer / Consumer",
                        tagline = if (isMarathi) "घरगुती दुरुस्ती व कारागीर सेवा हवी आहे" else "Need verified household, repair or trade services",
                        icon = Icons.Default.Person,
                        badge = if (isMarathi) "हमीभाव व एस्क्रो सुरक्षा" else "Escrow Protected",
                        accentColor = Color(0xFF0284C7),
                        onClick = { selectedRole = Role.CUSTOMER },
                        tag = "role_choice_customer"
                    )

                    // Role 2: Worker / Provider
                    ServiceRoleCard(
                        role = Role.WORKER,
                        isSelected = selectedRole == Role.WORKER,
                        title = if (isMarathi) "कारागीर / कामगार (Worker & Artisan)" else "Worker & Service Provider",
                        tagline = if (isMarathi) "हक्काची मजुरी, हमीभाव आणि थेट काम हवे आहे" else "Want verified local jobs with guaranteed minimum wage",
                        icon = Icons.Default.Engineering,
                        badge = if (isMarathi) "किमान वेतन व कल्याण निधी" else "Guaranteed Wage Floor",
                        accentColor = Color(0xFF059669),
                        onClick = { selectedRole = Role.WORKER },
                        tag = "role_choice_worker"
                    )

                    // If worker selected, show skill selector
                    if (selectedRole == Role.WORKER) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp, end = 12.dp, top = 2.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF059669).copy(alpha = 0.08f)),
                            border = BorderStroke(1.dp, Color(0xFF059669).copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = if (isMarathi) "तुमचे मुख्य कौशल्य / काम निवडा:" else "Select Your Primary Trade:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF059669)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                ExposedDropdownMenuBox(
                                    expanded = skillDropdownExpanded,
                                    onExpandedChange = { skillDropdownExpanded = it }
                                ) {
                                    OutlinedTextField(
                                        value = workerSkill,
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = skillDropdownExpanded) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                            .testTag("worker_skill_dropdown")
                                    )
                                    ExposedDropdownMenu(
                                        expanded = skillDropdownExpanded,
                                        onDismissRequest = { skillDropdownExpanded = false }
                                    ) {
                                        availableSkills.forEach { skill ->
                                            DropdownMenuItem(
                                                text = { Text(skill) },
                                                onClick = {
                                                    workerSkill = skill
                                                    skillDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Role 3: Cooperative Admin
                    ServiceRoleCard(
                        role = Role.COOPERATIVE_ADMIN,
                        isSelected = selectedRole == Role.COOPERATIVE_ADMIN,
                        title = if (isMarathi) "सहकारी संस्था समिती (Cooperative Admin)" else "Cooperative Committee Admin",
                        tagline = if (isMarathi) "एस्क्रो मंजुरी, वाद निवारण आणि पारदर्शक वाटप" else "Escrow releases, dispute resolution & equitable dispatch",
                        icon = Icons.Default.AdminPanelSettings,
                        badge = if (isMarathi) "समिती प्रशासन" else "Board Governance",
                        accentColor = Color(0xFF7C3AED),
                        onClick = { selectedRole = Role.COOPERATIVE_ADMIN },
                        tag = "role_choice_admin"
                    )
                }
            }

            // SUBMIT & PROCEED BUTTON
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = {
                        if (onLoginSuccessWithDetails != null) {
                            onLoginSuccessWithDetails(
                                selectedRole,
                                nameInput,
                                phoneInput,
                                localityInput,
                                workerSkill
                            )
                        } else {
                            CoopRepository.loginUser(
                                name = nameInput,
                                phone = phoneInput,
                                locality = localityInput,
                                role = selectedRole,
                                workerSkill = workerSkill
                            )
                            onLoginSuccess(selectedRole)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("startup_login_submit_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        text = if (isMarathi) "लॉगिन करा आणि पोर्टल सुरू करा" else "Log In & Enter Cooperative Portal",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun PresetChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        border = BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ServiceRoleCard(
    role: Role,
    isSelected: Boolean,
    title: String,
    tagline: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    badge: String,
    accentColor: Color,
    onClick: () -> Unit,
    tag: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag(tag),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) accentColor.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            if (isSelected) 2.dp else 1.dp,
            if (isSelected) accentColor else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Selected",
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = tagline,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "• $badge",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = accentColor
                )
            }
        }
    }
}
