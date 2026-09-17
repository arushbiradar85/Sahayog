package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StartupScreen(
    onLoginSuccess: (Role) -> Unit = {},
    onLoginSuccessWithDetails: ((Role, String, String, String, String) -> Unit)? = null,
    onCompleteOnboarding: ((Role, String, String, String, String, String, String, Int, String, String, String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val language by CoopRepository.appLanguage.collectAsState()
    val isMarathi = Localization.isMarathi(language)

    // Current step in 6-step onboarding wizard (1 = Welcome, 2 = Details, 3 = Address, 4 = Role, 5 = Role Info, 6 = Confirm)
    var currentStep by remember { mutableIntStateOf(1) }

    // Form inputs
    var nameInput by remember { mutableStateOf("Ramesh Patil") }
    var phoneInput by remember { mutableStateOf("9845012345") }
    var areaInput by remember { mutableStateOf("Indiranagar") }
    var cityInput by remember { mutableStateOf("Bangalore") }
    var landmarkInput by remember { mutableStateOf("Near Metro Station") }
    var selectedRole by remember { mutableStateOf(Role.CUSTOMER) }

    // Role-specific fields
    var workerSkills by remember { mutableStateOf(listOf("Electrician", "Gardener")) }
    var workerExperienceYears by remember { mutableIntStateOf(5) }
    var workerBranch by remember { mutableStateOf("Bangalore Urban Workers Cooperative") }
    var customerInterests by remember { mutableStateOf(listOf("Floor Cleaning", "Electrician")) }

    val availableSkills = listOf("Electrician", "Gardener", "Plumber", "Carpenter", "Cleaning", "Painter", "Mason", "Technician")
    val availableBranches = listOf(
        "Bangalore Urban Workers Cooperative",
        "Mysuru Craftsmen Sahakari Sangha",
        "North Karnataka Artisans Guild"
    )
    val customerServices = listOf("Floor Cleaning", "Electrician", "Plumber", "Carpenter", "Painter", "Gardening", "Mason", "Technician")

    var skillDropdownExpanded by remember { mutableStateOf(false) }
    var branchDropdownExpanded by remember { mutableStateOf(false) }

    // Progress (1..6)
    val progress = currentStep / 6f

    fun completeOnboarding() {
        val fullLocality = if (landmarkInput.isNotBlank()) "$areaInput, $cityInput (Near $landmarkInput)" else "$areaInput, $cityInput"
        val chosenSkills = if (selectedRole == Role.WORKER) workerSkills.ifEmpty { listOf("Electrician") } else emptyList()
        val chosenInterests = if (selectedRole == Role.CUSTOMER) customerInterests.ifEmpty { listOf("Floor Cleaning") } else listOf("Floor Cleaning")

        CoopRepository.loginUser(
            name = nameInput,
            phone = phoneInput,
            locality = fullLocality,
            role = selectedRole,
            skills = chosenSkills,
            areaLocality = areaInput,
            cityDistrict = cityInput,
            landmark = landmarkInput,
            experienceYears = workerExperienceYears,
            customerServiceInterests = chosenInterests
        )

        if (onLoginSuccessWithDetails != null) {
            val primarySkill = if (selectedRole == Role.WORKER) chosenSkills.firstOrNull() ?: "Electrician" else ""
            onLoginSuccessWithDetails(
                selectedRole,
                nameInput,
                phoneInput,
                fullLocality,
                primarySkill
            )
        } else {
            onLoginSuccess(selectedRole)
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Top Bar: Step Indicator & Language Switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentStep > 1) {
                    IconButton(
                        onClick = { currentStep-- },
                        modifier = Modifier.testTag("onboarding_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }

                Text(
                    text = if (isMarathi) "पायरी $currentStep / ६" else "Step $currentStep of 6",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )

                // Language Toggle
                OutlinedButton(
                    onClick = {
                        val nextLang = if (isMarathi) AppLanguage.ENGLISH else AppLanguage.MARATHI
                        CoopRepository.setLanguage(nextLang)
                    },
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = ButtonDefaults.TextButtonContentPadding,
                    modifier = Modifier.testTag("language_toggle_startup")
                ) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = "Language",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isMarathi) "मराठी" else "English",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            // Progress Bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Body Content by Step
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (currentStep) {
                    1 -> Step1Welcome(
                        isMarathi = isMarathi,
                        onGetStarted = { currentStep = 2 },
                        onQuickRoleSelect = { presetRole ->
                            when (presetRole) {
                                Role.CUSTOMER -> {
                                    nameInput = "Ramesh Patil"
                                    phoneInput = "9845012345"
                                    areaInput = "Indiranagar"
                                    cityInput = "Bangalore"
                                    landmarkInput = "100ft Road"
                                    selectedRole = Role.CUSTOMER
                                    customerInterests = listOf("Floor Cleaning", "Electrician")
                                }
                                Role.WORKER -> {
                                    nameInput = "Sunil Kumar"
                                    phoneInput = "9845122331"
                                    areaInput = "Koramangala"
                                    cityInput = "Bangalore"
                                    landmarkInput = "Sony World Signal"
                                    selectedRole = Role.WORKER
                                    workerSkills = listOf("Electrician", "Gardener")
                                }
                            }
                            currentRoleCompleteOrContinue(presetRole) { completeOnboarding() }
                        }
                    )
                    2 -> Step2PersonalDetails(
                        name = nameInput,
                        phone = phoneInput,
                        isMarathi = isMarathi,
                        onNameChange = { nameInput = it },
                        onPhoneChange = { phoneInput = it },
                        onNext = { currentStep = 3 }
                    )
                    3 -> Step3Address(
                        area = areaInput,
                        city = cityInput,
                        landmark = landmarkInput,
                        isMarathi = isMarathi,
                        onAreaChange = { areaInput = it },
                        onCityChange = { cityInput = it },
                        onLandmarkChange = { landmarkInput = it },
                        onNext = { currentStep = 4 }
                    )
                    4 -> Step4RoleSelection(
                        selectedRole = selectedRole,
                        isMarathi = isMarathi,
                        onRoleSelected = { selectedRole = it },
                        onNext = { currentStep = 5 }
                    )
                    5 -> Step5RoleDetails(
                        role = selectedRole,
                        isMarathi = isMarathi,
                        workerSkills = workerSkills,
                        availableSkills = availableSkills,
                        experienceYears = workerExperienceYears,
                        workerBranch = workerBranch,
                        availableBranches = availableBranches,
                        customerInterests = customerInterests,
                        customerServices = customerServices,
                        onToggleSkill = { skill ->
                            workerSkills = if (workerSkills.contains(skill)) {
                                if (workerSkills.size > 1) workerSkills - skill else workerSkills
                            } else {
                                workerSkills + skill
                            }
                        },
                        onExperienceChange = { workerExperienceYears = it },
                        onBranchChange = { workerBranch = it },
                        onToggleCustomerInterest = { service ->
                            customerInterests = if (customerInterests.contains(service)) {
                                if (customerInterests.size > 1) customerInterests - service else customerInterests
                            } else {
                                customerInterests + service
                            }
                        },
                        onNext = { currentStep = 6 }
                    )
                    6 -> Step6Confirmation(
                        name = nameInput,
                        phone = phoneInput,
                        area = areaInput,
                        city = cityInput,
                        landmark = landmarkInput,
                        role = selectedRole,
                        workerSkills = workerSkills,
                        experienceYears = workerExperienceYears,
                        branch = workerBranch,
                        interests = customerInterests,
                        isMarathi = isMarathi,
                        onConfirm = { completeOnboarding() }
                    )
                }
            }
        }
    }
}

private fun currentRoleCompleteOrContinue(role: Role, onComplete: () -> Unit) {
    onComplete()
}

// -------------------------------------------------------------------------------------
// STEP 1: WELCOME
// -------------------------------------------------------------------------------------
@Composable
private fun Step1Welcome(
    isMarathi: Boolean,
    onGetStarted: () -> Unit,
    onQuickRoleSelect: (Role) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.VerifiedUser,
                    contentDescription = "Sahayog Logo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        item {
            Text(
                text = if (isMarathi) "सहयोग सेवा सहकारी संघ" else "Sahayog Service Cooperative",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isMarathi)
                    "कामगारांची मालकी • हमीभाव किमान वेतन • क्रिप्टोग्राफिक एस्क्रो"
                else
                    "Worker-Owned • Guaranteed Wage Floors • Cryptographic Escrow",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isMarathi) "१००% सुरक्षित एस्क्रो आणि पारदर्शक वेतन" else "100% Escrow Protection & Transparent Payouts",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Engineering, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isMarathi) "किमान वेतन कायद्यानुसार हमी भाव (नो अंडरकटिंग)" else "Strict Minimum Wage Floor Guarantee (No Undercutting)",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isMarathi) "स्थानिक सुरक्षित डेटा - संपूर्ण ऑफलाइन सहकार्य" else "Offline-First Data Storage • No Cloud Dependencies",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        item {
            Button(
                onClick = onGetStarted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("onboarding_get_started_btn"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (isMarathi) "सुरुवात करा (६ पायऱ्या)" else "Get Started (Setup Profile)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
        }

        item {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text(
                text = if (isMarathi) "किंवा १-क्लिक जलद चाचणी निवडा (परीक्षकांसाठी):" else "Or Quick-Select Demo Profile (For Evaluators):",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onQuickRoleSelect(Role.CUSTOMER) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_customer_btn"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(if (isMarathi) "ग्राहक\n(Customer)" else "Customer\n(Ramesh)", textAlign = TextAlign.Center, fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = { onQuickRoleSelect(Role.WORKER) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_worker_btn"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(if (isMarathi) "कामगार\n(Worker)" else "Worker / Provider\n(Sunil)", textAlign = TextAlign.Center, fontSize = 12.sp)
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// STEP 2: PERSONAL DETAILS
// -------------------------------------------------------------------------------------
@Composable
private fun Step2PersonalDetails(
    name: String,
    phone: String,
    isMarathi: Boolean,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (isMarathi) "आपली वैयक्तिक माहिती प्रविष्ट करा" else "Personal Details",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = if (isMarathi)
                "सहकारी संघामध्ये आपली ओळख नोंदवण्यासाठी संपूर्ण नाव आणि १०-अंकी मोबाईल नंबर द्या."
            else
                "Provide your full name and 10-digit mobile number for cooperative member registration.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text(if (isMarathi) "संपूर्ण नाव (Full Name)" else "Full Name") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("onboarding_name_input")
        )

        OutlinedTextField(
            value = phone,
            onValueChange = { if (it.length <= 10 && it.all { char -> char.isDigit() }) onPhoneChange(it) },
            label = { Text(if (isMarathi) "मोबाईल नंबर (१० अंक)" else "Mobile Number (10 digits)") },
            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
            prefix = { Text("+91 ") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("onboarding_phone_input")
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onNext,
            enabled = name.isNotBlank() && phone.length >= 8,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("step2_next_btn"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(if (isMarathi) "पुढील पायरी: पत्ता" else "Next: Address")
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
        }
    }
}

// -------------------------------------------------------------------------------------
// STEP 3: ADDRESS
// -------------------------------------------------------------------------------------
@Composable
private fun Step3Address(
    area: String,
    city: String,
    landmark: String,
    isMarathi: Boolean,
    onAreaChange: (String) -> Unit,
    onCityChange: (String) -> Unit,
    onLandmarkChange: (String) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (isMarathi) "आपला पत्ता आणि परिसर" else "Address & Locality",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = if (isMarathi)
                "स्थानिक कामगारांशी संपर्क साधण्यासाठी आणि सेवा विनंत्यांसाठी आपला परिसर निश्चित करा."
            else
                "Specify your area/locality so services can be matched and dispatched with minimal travel time.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = area,
            onValueChange = onAreaChange,
            label = { Text(if (isMarathi) "परिसर / कॉलनी (Area / Locality)" else "Area / Locality (e.g. Indiranagar)") },
            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("onboarding_area_input")
        )

        OutlinedTextField(
            value = city,
            onValueChange = onCityChange,
            label = { Text(if (isMarathi) "शहर / जिल्हा (City / District)" else "City / District (e.g. Bangalore)") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("onboarding_city_input")
        )

        OutlinedTextField(
            value = landmark,
            onValueChange = onLandmarkChange,
            label = { Text(if (isMarathi) "जवळची खूण (ऐच्छिक Landmark)" else "Optional Landmark (e.g. Near Metro)") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("onboarding_landmark_input")
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onNext,
            enabled = area.isNotBlank() && city.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("step3_next_btn"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(if (isMarathi) "पुढील पायरी: भूमिका निवड" else "Next: Select Role")
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
        }
    }
}

// -------------------------------------------------------------------------------------
// STEP 4: ROLE SELECTION
// -------------------------------------------------------------------------------------
@Composable
private fun Step4RoleSelection(
    selectedRole: Role,
    isMarathi: Boolean,
    onRoleSelected: (Role) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = if (isMarathi) "सहकार्यात आपली भूमिका निवडा" else "Select Your Cooperative Role",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = if (isMarathi)
                "प्रत्येक भूमिकेसाठी स्वतंत्र, वेगळा डॅशबोर्ड उपलब्ध आहे. ही एकच निवड आपल्या अनुभवाची रचना ठरवते."
            else
                "Each role provides a strictly isolated dashboard. This choice determines your application experience.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Customer Card
        RoleCard(
            title = if (isMarathi) "ग्राहक (Customer)" else "Customer",
            subtitle = if (isMarathi)
                "घरगुती सेवांसाठी हमीभावानुसार कामगार बुक करा. १००% एस्क्रो सुरक्षा."
            else
                "Book verified household services with wage-floor protection & secure local escrow.",
            icon = Icons.Default.Home,
            isSelected = selectedRole == Role.CUSTOMER,
            testTag = "role_card_customer",
            onClick = { onRoleSelected(Role.CUSTOMER) }
        )

        // Worker Card
        RoleCard(
            title = if (isMarathi) "कामगार / सेवा प्रदाता (Worker / Provider)" else "Worker / Service Provider",
            subtitle = if (isMarathi)
                "कौशल्यानुसार कामाच्या विनंत्या पहा, काम स्वीकारा आणि थेट हमी वेतनाचा हक्क मिळवा."
            else
                "Receive matched service requests based on your trade skills and accept bookings.",
            icon = Icons.Default.Engineering,
            isSelected = selectedRole == Role.WORKER,
            testTag = "role_card_worker",
            onClick = { onRoleSelected(Role.WORKER) }
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("step4_next_btn"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(if (isMarathi) "पुढील पायरी: भूमिकेनुसार माहिती" else "Next: Role-Specific Details")
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
        }
    }
}

@Composable
private fun RoleCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// STEP 5: ROLE-SPECIFIC INFORMATION
// -------------------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun Step5RoleDetails(
    role: Role,
    isMarathi: Boolean,
    workerSkills: List<String>,
    availableSkills: List<String>,
    experienceYears: Int,
    workerBranch: String,
    availableBranches: List<String>,
    customerInterests: List<String>,
    customerServices: List<String>,
    onToggleSkill: (String) -> Unit,
    onExperienceChange: (Int) -> Unit,
    onBranchChange: (String) -> Unit,
    onToggleCustomerInterest: (String) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = when (role) {
                Role.CUSTOMER -> if (isMarathi) "ग्राहक पसंती" else "Customer Preferences"
                Role.WORKER -> if (isMarathi) "कामगार कौशल्ये व अनुभव" else "Provider Trades & Experience"
            },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        when (role) {
            Role.CUSTOMER -> {
                Text(
                    text = if (isMarathi)
                        "आपल्याला वारंवार लागणाऱ्या सेवा निवडा (एकापेक्षा जास्त निवडू शकता):"
                    else
                        "Select services you frequently request (Tap to multi-select):",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    customerServices.forEach { service ->
                        val selected = customerInterests.contains(service)
                        FilterChip(
                            selected = selected,
                            onClick = { onToggleCustomerInterest(service) },
                            label = { Text(service) },
                            leadingIcon = if (selected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }

                Text(
                    text = "Selected: ${customerInterests.size} categories",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Role.WORKER -> {
                Text(
                    text = if (isMarathi)
                        "आपली कौशल्ये / व्यवसाय निवडा (उदा. Electrician, Gardener):"
                    else
                        "Select your verified trade skills (Multi-select supported):",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableSkills.forEach { skill ->
                        val selected = workerSkills.contains(skill)
                        FilterChip(
                            selected = selected,
                            onClick = { onToggleSkill(skill) },
                            label = { Text(skill) },
                            leadingIcon = if (selected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Active Trades (${workerSkills.size}): ${workerSkills.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (isMarathi) "कामाचा अनुभव: $experienceYears वर्षे" else "Experience: $experienceYears years",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { if (experienceYears > 1) onExperienceChange(experienceYears - 1) },
                        shape = CircleShape,
                        modifier = Modifier.size(40.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }

                    Text(
                        text = "$experienceYears",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedButton(
                        onClick = { if (experienceYears < 30) onExperienceChange(experienceYears + 1) },
                        shape = CircleShape,
                        modifier = Modifier.size(40.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("step5_next_btn"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(if (isMarathi) "पुढील पायरी: पडताळणी व पुष्टी" else "Next: Summary & Confirmation")
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
        }
    }
}

// -------------------------------------------------------------------------------------
// STEP 6: CONFIRMATION & REVIEW
// -------------------------------------------------------------------------------------
@Composable
private fun Step6Confirmation(
    name: String,
    phone: String,
    area: String,
    city: String,
    landmark: String,
    role: Role,
    workerSkills: List<String>,
    experienceYears: Int,
    branch: String,
    interests: List<String>,
    isMarathi: Boolean,
    onConfirm: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (isMarathi) "माहिती पडताळा आणि पुष्टी करा" else "Review & Confirm Profile",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isMarathi) "निवडलेली भूमिका" else "Selected Role",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = when (role) {
                            Role.CUSTOMER -> if (isMarathi) "ग्राहक (Customer)" else "Customer"
                            Role.WORKER -> if (isMarathi) "कामगार / सेवा प्रदाता (Provider)" else "Worker / Provider"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                HorizontalDivider()

                ReviewRow(label = if (isMarathi) "नाव" else "Full Name", value = name)
                ReviewRow(label = if (isMarathi) "मोबाईल" else "Mobile", value = "+91 $phone")
                ReviewRow(
                    label = if (isMarathi) "पत्ता" else "Locality",
                    value = if (landmark.isNotBlank()) "$area, $city (Near $landmark)" else "$area, $city"
                )

                when (role) {
                    Role.CUSTOMER -> {
                        ReviewRow(label = if (isMarathi) "प्राधान्य सेवा" else "Preferred Services", value = interests.joinToString(", "))
                    }
                    Role.WORKER -> {
                        ReviewRow(label = if (isMarathi) "कौशल्ये (Trades)" else "Trade Skills", value = workerSkills.joinToString(", "))
                        ReviewRow(label = if (isMarathi) "अनुभव" else "Experience", value = "$experienceYears years")
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (isMarathi)
                        "स्थानिक डेटास्टोअरमध्ये जतन केले जाईल. ॲप पुन्हा सुरू केल्यावर थेट डॅशबोर्ड उघडेल."
                    else
                        "Persisted locally in DataStore. Onboarding will not appear again on app restart.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onConfirm,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("onboarding_confirm_btn"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isMarathi) "पुष्टी करा आणि सहकार्यात प्रवेश करा" else "Confirm & Enter Cooperative",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ReviewRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}
