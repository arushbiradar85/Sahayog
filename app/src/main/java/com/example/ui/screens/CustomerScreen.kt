package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Carpenter
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Plumbing
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Yard
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.engine.WageEngine
import com.example.data.model.Customer
import com.example.data.model.Job
import com.example.data.model.JobStatus
import com.example.data.model.Role
import com.example.data.model.ServiceCategory
import com.example.data.preferences.UserPreferences
import com.example.data.repository.CoopRepository
import com.example.ui.components.StatusBadge
import com.example.util.AppLanguage
import com.example.util.Localization
import com.example.util.NotificationHelper
import com.example.util.TwoDeviceSyncManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CustomerScreen(
    repository: CoopRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val language by repository.appLanguage.collectAsState()
    val isMarathi = Localization.isMarathi(language)

    val jobs by repository.jobs.collectAsState()
    val activeCustomer = repository.getActiveCustomer()
    val userProfile by repository.activeUserProfile.collectAsState()
    val coop = repository.getActiveCooperative()

    // 3 Clean Destinations: 0 = Explore/Services, 1 = My Requests & Bookings, 2 = Profile & Settings
    var selectedNavIndex by remember { mutableIntStateOf(0) }

    // Dialog States
    var showCustomRequestDialog by remember { mutableStateOf(false) }
    var preselectedServiceCategory by remember { mutableStateOf<ServiceCategory?>(null) }
    var disputeJobTarget by remember { mutableStateOf<Job?>(null) }
    var showReceiptJob by remember { mutableStateOf<Job?>(null) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    // Filter for requests
    var bookingFilter by remember { mutableStateOf("ALL") }

    // Customer's jobs
    val customerJobs = jobs.filter { it.customerId == activeCustomer.id || it.customerId == "user_cust" || it.customerId == "cust_1" }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                NavigationBarItem(
                    selected = selectedNavIndex == 0,
                    onClick = { selectedNavIndex = 0 },
                    icon = { Icon(Icons.Default.Explore, contentDescription = "Services") },
                    label = { Text(if (isMarathi) "सेवा" else "Services", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("nav_customer_services")
                )
                NavigationBarItem(
                    selected = selectedNavIndex == 1,
                    onClick = { selectedNavIndex = 1 },
                    icon = {
                        Box {
                            Icon(Icons.Default.ReceiptLong, contentDescription = "Requests")
                            val pendingCount = customerJobs.count { it.status == JobStatus.PENDING || it.status == JobStatus.ACCEPTED }
                            if (pendingCount > 0) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(16.dp)
                                ) {
                                    Text(
                                        text = "$pendingCount",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(top = 1.dp)
                                    )
                                }
                            }
                        }
                    },
                    label = { Text(if (isMarathi) "माझ्या विनंत्या" else "My Requests", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("nav_customer_bookings")
                )
                NavigationBarItem(
                    selected = selectedNavIndex == 2,
                    onClick = { selectedNavIndex = 2 },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text(if (isMarathi) "माझे खाते" else "Profile", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("nav_customer_profile")
                )
            }
        },
        floatingActionButton = {
            if (selectedNavIndex == 0 || selectedNavIndex == 1) {
                FloatingActionButton(
                    onClick = {
                        preselectedServiceCategory = null
                        showCustomRequestDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("customer_request_fab")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Request Service")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isMarathi) "सेवा विनंती करा" else "Request Service",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedNavIndex) {
                0 -> CustomerExploreTab(
                    activeCustomer = activeCustomer,
                    userProfileName = userProfile?.name ?: activeCustomer.name,
                    userLocality = userProfile?.locality ?: activeCustomer.location,
                    language = language,
                    isMarathi = isMarathi,
                    onSelectCategory = { category ->
                        preselectedServiceCategory = category
                        showCustomRequestDialog = true
                    },
                    onRequestCustomService = {
                        preselectedServiceCategory = null
                        showCustomRequestDialog = true
                    }
                )
                1 -> CustomerRequestsTab(
                    customerJobs = customerJobs,
                    activeFilter = bookingFilter,
                    onFilterChange = { bookingFilter = it },
                    isMarathi = isMarathi,
                    onRaiseDispute = { disputeJobTarget = it },
                    onViewReceipt = { showReceiptJob = it },
                    onSimulateAccept = { job ->
                        TwoDeviceSyncManager.simulateWorkerAcceptanceForJob(job.id, 500)
                        Toast.makeText(context, "Simulating worker acceptance...", Toast.LENGTH_SHORT).show()
                    }
                )
                2 -> CustomerProfileTab(
                    activeCustomer = activeCustomer,
                    userProfile = userProfile,
                    isMarathi = isMarathi,
                    onToggleLanguage = { CoopRepository.toggleLanguage() },
                    onChangeRole = { CoopRepository.logoutUser() },
                    onResetDemo = { showResetConfirmDialog = true }
                )
            }
        }
    }

    // Custom Request Modal Flow
    if (showCustomRequestDialog) {
        CustomerRequestModal(
            preselectedCategory = preselectedServiceCategory,
            defaultLocation = userProfile?.locality ?: activeCustomer.location,
            adminFeePercent = coop.adminFeePercent,
            isMarathi = isMarathi,
            onDismiss = { showCustomRequestDialog = false },
            onConfirmBooking = { skill, location, date, time, durationMinutes, pricePaise, notes ->
                val result = repository.bookJob(
                    skill = skill,
                    location = location,
                    dateTime = "$date, $time",
                    durationMinutes = durationMinutes,
                    priceInPaise = pricePaise,
                    title = "$skill Service Request",
                    description = notes,
                    date = date,
                    preferredTime = time,
                    instructions = notes
                )
                showCustomRequestDialog = false
                if (result.isSuccess) {
                    val createdJob = result.getOrNull()
                    if (createdJob != null) {
                        TwoDeviceSyncManager.broadcastNewJob(createdJob)
                        NotificationHelper.notifyNewRequest(
                            context,
                            skill,
                            pricePaise,
                            location
                        )
                        // If demo simulation enabled, trigger auto-accept after 3 seconds
                        if (TwoDeviceSyncManager.demoSimulationEnabled.value) {
                            TwoDeviceSyncManager.simulateWorkerAcceptanceForJob(createdJob.id, 3000)
                        }
                    }
                    selectedNavIndex = 1 // Switch to My Requests
                    Toast.makeText(context, "Request created! Payment held safely in escrow.", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, result.exceptionOrNull()?.message ?: "Failed", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    // Dispute Dialog
    disputeJobTarget?.let { job ->
        DisputeDialog(
            job = job,
            onDismiss = { disputeJobTarget = null },
            onSubmitDispute = { comment ->
                repository.disputeJob(job.id, comment)
                NotificationHelper.notifyDisputeCreated(context, job.title, comment)
                disputeJobTarget = null
                Toast.makeText(context, "Dispute recorded. Escrow held for admin review.", Toast.LENGTH_LONG).show()
            }
        )
    }

    // Receipt Dialog
    showReceiptJob?.let { job ->
        ReceiptDialog(
            job = job,
            adminFeePercent = coop.adminFeePercent,
            onDismiss = { showReceiptJob = null }
        )
    }

    // Reset Confirmation Dialog
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text(if (isMarathi) "डेमो डेटा रीसेट करावा?" else "Reset Demo Data?") },
            text = {
                Text(
                    if (isMarathi)
                        "यामुळे सर्व तात्पुरत्या नोकऱ्या आणि लेजर नोंदी मूळ स्थितीत परत येतील."
                    else
                        "This will reset all jobs, escrow balances, and ledger blocks back to the initial seeded state."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.resetToSeedData()
                        showResetConfirmDialog = false
                        Toast.makeText(context, "Demo data reset successfully.", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text(if (isMarathi) "होय, रीसेट करा" else "Yes, Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text(if (isMarathi) "रद्द करा" else "Cancel")
                }
            }
        )
    }
}

// -------------------------------------------------------------------------------------
// TAB 0: EXPLORE & SERVICES
// -------------------------------------------------------------------------------------
@Composable
private fun CustomerExploreTab(
    activeCustomer: Customer,
    userProfileName: String,
    userLocality: String,
    language: AppLanguage,
    isMarathi: Boolean,
    onSelectCategory: (ServiceCategory) -> Unit,
    onRequestCustomService: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val categories = WageEngine.SERVICE_CATEGORIES.filter {
        searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) ||
                it.description.contains(searchQuery, ignoreCase = true)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 14.dp, bottom = 90.dp)
    ) {
        // Customer Greeting & Locality Card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userProfileName.take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isMarathi) "नमस्ते, $userProfileName" else "Hello, $userProfileName",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = userLocality,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Prominent Request a Service Action Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onRequestCustomService)
                    .testTag("prominent_request_service_card"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isMarathi) "नवीन सेवा विनंती पोस्ट करा" else "Post a Service Request",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isMarathi)
                                "कौशल्य, तारीख व वेळ निवडून थेट हमीभावाने कामगार मिळवा"
                            else
                                "Choose skill, time & offer fair wage floor with escrow protection",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Search Field
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(if (isMarathi) "सेवा शोधा (उदा. साफसफाई, वायरमन...)" else "Search services (e.g. Cleaning, Electrician...)") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("service_search_input")
            )
        }

        // Section Title: Guaranteed Cooperative Services
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isMarathi) "सहकारी सेवा श्रेणी (किमान हमीभाव)" else "Cooperative Services & Wage Floors",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isMarathi) "कायदेशीर हमी" else "Wage Protected",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Service Cards Grid
        item {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp),
                userScrollEnabled = true
            ) {
                items(categories) { category ->
                    ServiceCategoryCard(
                        category = category,
                        language = language,
                        onClick = { onSelectCategory(category) }
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// TAB 1: MY REQUESTS & BOOKINGS
// -------------------------------------------------------------------------------------
@Composable
private fun CustomerRequestsTab(
    customerJobs: List<Job>,
    activeFilter: String,
    onFilterChange: (String) -> Unit,
    isMarathi: Boolean,
    onRaiseDispute: (Job) -> Unit,
    onViewReceipt: (Job) -> Unit,
    onSimulateAccept: (Job) -> Unit
) {
    val filteredJobs = when (activeFilter) {
        "OPEN" -> customerJobs.filter { it.status == JobStatus.PENDING }
        "ACCEPTED" -> customerJobs.filter { it.status == JobStatus.ACCEPTED }
        "IN_PROGRESS" -> customerJobs.filter { it.status == JobStatus.IN_PROGRESS }
        "COMPLETED" -> customerJobs.filter { it.status == JobStatus.COMPLETED }
        "DISPUTED" -> customerJobs.filter { it.status == JobStatus.DISPUTED }
        else -> customerJobs
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Filter Chips Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = activeFilter == "ALL",
                onClick = { onFilterChange("ALL") },
                label = { Text(if (isMarathi) "सर्व (${customerJobs.size})" else "All (${customerJobs.size})", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer)
            )
            FilterChip(
                selected = activeFilter == "OPEN",
                onClick = { onFilterChange("OPEN") },
                label = { Text(if (isMarathi) "उघड्या" else "Looking", fontSize = 11.sp) }
            )
            FilterChip(
                selected = activeFilter == "ACCEPTED",
                onClick = { onFilterChange("ACCEPTED") },
                label = { Text(if (isMarathi) "स्वीकृत" else "Accepted", fontSize = 11.sp) }
            )
            FilterChip(
                selected = activeFilter == "COMPLETED",
                onClick = { onFilterChange("COMPLETED") },
                label = { Text(if (isMarathi) "पूर्ण" else "Done", fontSize = 11.sp) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (filteredJobs.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isMarathi) "या फिल्टरमध्ये कोणतीही विनंती नाही" else "No requests in this category",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 90.dp)
            ) {
                items(filteredJobs) { job ->
                    CustomerJobCardDetailed(
                        job = job,
                        isMarathi = isMarathi,
                        onRaiseDispute = { onRaiseDispute(job) },
                        onViewReceipt = { onViewReceipt(job) },
                        onSimulateAccept = { onSimulateAccept(job) }
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// TAB 2: PROFILE & SETTINGS
// -------------------------------------------------------------------------------------
@Composable
private fun CustomerProfileTab(
    activeCustomer: Customer,
    userProfile: com.example.data.preferences.UserProfile?,
    isMarathi: Boolean,
    onToggleLanguage: () -> Unit,
    onChangeRole: () -> Unit,
    onResetDemo: () -> Unit
) {
    val syncLog by TwoDeviceSyncManager.syncLog.collectAsState()
    val localIp by TwoDeviceSyncManager.localIp.collectAsState()
    val partnerIp by TwoDeviceSyncManager.partnerIp.collectAsState()
    val demoSimulation by TwoDeviceSyncManager.demoSimulationEnabled.collectAsState()

    var partnerIpInput by remember { mutableStateOf(partnerIp) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 14.dp, bottom = 90.dp)
    ) {
        // User Identity Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (userProfile?.name ?: activeCustomer.name).take(1).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = userProfile?.name ?: activeCustomer.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "+91 ${userProfile?.phone ?: "9845012345"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text(
                        text = "Address: ${userProfile?.locality ?: activeCustomer.location}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Preferred Services: ${userProfile?.customerServiceInterest ?: "General Household"}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // Two-Device Live Network & Demo Sync
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Wifi, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isMarathi) "दोन उपकरणे थेट सिंक (Local Wi-Fi)" else "Two-Device Local Wi-Fi Sync",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "My Device IP: $localIp (Port 8989)",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    OutlinedTextField(
                        value = partnerIpInput,
                        onValueChange = {
                            partnerIpInput = it
                            TwoDeviceSyncManager.setPartnerIp(it)
                        },
                        label = { Text("Worker Phone IP Address") },
                        placeholder = { Text("e.g. 192.168.1.5") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Auto-Simulation Mode (Single Device)",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Switch(
                            checked = demoSimulation,
                            onCheckedChange = { TwoDeviceSyncManager.setDemoSimulation(it) }
                        )
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Sync Status: $syncLog",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        }

        // App Language Setting
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleLanguage),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Translate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = if (isMarathi) "भाषा बदला (मराठी / English)" else "Language (English / मराठी)", fontWeight = FontWeight.Medium)
                    }
                    Text(
                        text = if (isMarathi) "मराठी" else "English",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Change Role / Reset Profile
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onChangeRole)
                    .testTag("change_role_button"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = if (isMarathi) "भूमिका बदला / प्रोफाइल रीसेट" else "Change Role / Edit Profile", fontWeight = FontWeight.Medium)
                        Text(
                            text = if (isMarathi) "कामगार किंवा व्यवस्थापक भूमिकेत जाण्यासाठी" else "Switch to Worker or Admin onboarding",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Reset Hackathon Demo Data
        item {
            OutlinedButton(
                onClick = onResetDemo,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reset_demo_data_button"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isMarathi) "डेमो डेटा रीसेट करा" else "Reset Hackathon Demo Data")
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// DETAILED CUSTOMER JOB CARD (WITH 'WORKER ACCEPTED' BANNER)
// -------------------------------------------------------------------------------------
@Composable
private fun CustomerJobCardDetailed(
    job: Job,
    isMarathi: Boolean,
    onRaiseDispute: () -> Unit,
    onViewReceipt: () -> Unit,
    onSimulateAccept: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
    val formattedPostedTime = remember(job.createdAtTimestamp) {
        sdf.format(Date(job.createdAtTimestamp))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("customer_job_card_${job.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Header Row: Skill & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getCategoryIcon(job.skill),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = job.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Job #${job.id.takeLast(6)} • ${job.durationMinutes / 60.0} hrs",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                StatusBadge(status = job.status)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            // Location, Date/Time & Budget
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Location", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Text(text = job.location, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Posted: $formattedPostedTime", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Escrow Budget", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Text(
                        text = WageEngine.formatPaiseCompact(job.priceInPaise),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Time: ${job.dateTime}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
            }

            // CRITICAL SPEC REQUIREMENT: WHEN WORKER ACCEPTS -> CUSTOMER MUST SEE: "Worker accepted"
            if (job.status == JobStatus.ACCEPTED || (job.workerId != null && (job.status == JobStatus.IN_PROGRESS || job.status == JobStatus.COMPLETED))) {
                val assignedWorker = CoopRepository.workers.value.firstOrNull { it.id == job.workerId }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFE8F5E9),
                    border = BorderStroke(1.dp, Color(0xFF81C784)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isMarathi) "कामगाराने काम स्वीकारले! (Worker accepted)" else "Worker accepted",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B5E20)
                            )
                        }
                        Text(
                            text = "Worker: ${assignedWorker?.name ?: "Sunil Kumar"} • Skill: ${job.skill}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1B5E20)
                        )
                        Text(
                            text = "Date/Time: ${job.dateTime} • Status: ${job.status.name}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            } else if (job.status == JobStatus.PENDING) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.HourglassTop, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Status: OPEN / LOOKING FOR WORKER",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        TextButton(
                            onClick = onSimulateAccept,
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                        ) {
                            Text("Simulate Accept", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Proof if In Progress
            if (job.status == JobStatus.IN_PROGRESS && job.proofNotes != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(text = "Completion Proof Uploaded by Worker:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text(text = "\"${job.proofNotes}\"", style = MaterialTheme.typography.bodySmall)
                        if (job.proofLatitude != null) {
                            Text(text = "GPS: ${job.proofLatitude}, ${job.proofLongitude}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }

            // Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (job.status == JobStatus.IN_PROGRESS || job.status == JobStatus.ACCEPTED) {
                    TextButton(
                        onClick = onRaiseDispute,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.ReportProblem, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isMarathi) "तक्रार नोंदवा" else "Raise Dispute")
                    }
                }
                if (job.status == JobStatus.COMPLETED) {
                    OutlinedButton(
                        onClick = onViewReceipt,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isMarathi) "पावती पहा" else "View Receipt", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// SERVICE CATEGORY CARD
// -------------------------------------------------------------------------------------
@Composable
private fun ServiceCategoryCard(
    category: ServiceCategory,
    language: AppLanguage,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val icon = getCategoryIcon(category.name)
    val localizedName = Localization.serviceName(category.name, language)
    val isMarathi = Localization.isMarathi(language)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("service_card_${category.name.lowercase()}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = localizedName,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(
                    onClick = {
                        val text = if (isMarathi)
                            "🔊 $localizedName: किमान हमीभाव ₹${category.hourlyWagePaise / 100} प्रति तास"
                        else
                            "🔊 ${category.name}: Guaranteed wage floor ₹${category.hourlyWagePaise / 100} per hour"
                        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.VolumeUp, contentDescription = "Audio rate", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = localizedName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            ) {
                Text(
                    text = "Floor: ₹${category.hourlyWagePaise / 100}/hr",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// CUSTOM REQUEST MODAL WITH STRICT WAGE-FLOOR VALIDATION
// -------------------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomerRequestModal(
    preselectedCategory: ServiceCategory?,
    defaultLocation: String,
    adminFeePercent: Int,
    isMarathi: Boolean,
    onDismiss: () -> Unit,
    onConfirmBooking: (skill: String, location: String, date: String, time: String, durationMinutes: Int, pricePaise: Long, notes: String) -> Unit
) {
    val skills = listOf("Cleaning", "Electrician", "Plumber", "Carpenter", "Painter", "Gardening", "Mason", "Technician")
    var selectedSkill by remember { mutableStateOf(preselectedCategory?.name ?: "Cleaning") }
    var skillDropdownExpanded by remember { mutableStateOf(false) }

    var locationInput by remember { mutableStateOf(defaultLocation) }
    var dateSelection by remember { mutableStateOf("Today") }
    var preferredTimeInput by remember { mutableStateOf("2:00 PM") }
    var durationHours by remember { mutableFloatStateOf(2.0f) }
    var offerInput by remember { mutableStateOf(if (preselectedCategory != null) "${(preselectedCategory.hourlyWagePaise * 2) / 100}" else "600") }
    var instructionsInput by remember { mutableStateOf("") }

    val durationMinutes = (durationHours * 60).toInt()
    val offeredPricePaise = (offerInput.toLongOrNull() ?: 0L) * 100L

    // Strict cooperative wage floor check
    val minFloorPaise = WageEngine.calculateMinimumWageFloorInPaise(selectedSkill, durationMinutes)
    val validation = WageEngine.validatePrice(selectedSkill, durationMinutes, offeredPricePaise)
    val payout = WageEngine.calculatePayoutSplit(offeredPricePaise, adminFeePercent)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isMarathi) "सेवा विनंती पोस्ट करा" else "Request Cooperative Service",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Service Selector
                item {
                    ExposedDropdownMenuBox(
                        expanded = skillDropdownExpanded,
                        onExpandedChange = { skillDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedSkill,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Service Type") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = skillDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = skillDropdownExpanded,
                            onDismissRequest = { skillDropdownExpanded = false }
                        ) {
                            skills.forEach { skill ->
                                DropdownMenuItem(
                                    text = { Text(skill) },
                                    onClick = {
                                        selectedSkill = skill
                                        skillDropdownExpanded = false
                                        // Auto-adjust offer if below floor
                                        val newFloor = WageEngine.calculateMinimumWageFloorInPaise(skill, durationMinutes)
                                        if (offeredPricePaise < newFloor) {
                                            offerInput = "${newFloor / 100}"
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // Location
                item {
                    OutlinedTextField(
                        value = locationInput,
                        onValueChange = { locationInput = it },
                        label = { Text("Service Location") },
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Date & Time
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = dateSelection,
                            onValueChange = { dateSelection = it },
                            label = { Text("Date") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = preferredTimeInput,
                            onValueChange = { preferredTimeInput = it },
                            label = { Text("Preferred Time") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }

                // Duration Slider
                item {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Estimated Duration", style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = "${String.format(Locale.US, "%.1f", durationHours)} hours",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = durationHours,
                            onValueChange = {
                                durationHours = it
                                val newFloor = WageEngine.calculateMinimumWageFloorInPaise(selectedSkill, (it * 60).toInt())
                                if (offeredPricePaise < newFloor) {
                                    offerInput = "${newFloor / 100}"
                                }
                            },
                            valueRange = 1f..10f,
                            steps = 8
                        )
                    }
                }

                // Customer Offer Input
                item {
                    OutlinedTextField(
                        value = offerInput,
                        onValueChange = { offerInput = it },
                        label = { Text("Customer Offer Amount (₹)") },
                        leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("customer_offer_input")
                    )
                }

                // Wage Floor Validation Alert Box (STRICT COMPLIANCE)
                item {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (validation.isValid) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        border = BorderStroke(
                            1.dp,
                            if (validation.isValid) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (validation.isValid) Icons.Default.CheckCircle else Icons.Default.Error,
                                    contentDescription = null,
                                    tint = if (validation.isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (validation.isValid)
                                        "Minimum Wage Floor: ${WageEngine.formatPaiseCompact(minFloorPaise)} (Compliant)"
                                    else
                                        "Rejected: Below Wage Floor (${WageEngine.formatPaiseCompact(minFloorPaise)})",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (validation.isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                            }
                            if (!validation.isValid) {
                                Text(
                                    text = validation.reasonMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            } else {
                                Text(
                                    text = "Transparent Split: Worker gets ₹${payout.workerWagePaise / 100} • Coop Fee ₹${payout.adminFeePaise / 100} (₹${payout.welfarePaise / 100} to Welfare Reserve)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Optional Instructions
                item {
                    OutlinedTextField(
                        value = instructionsInput,
                        onValueChange = { instructionsInput = it },
                        label = { Text("Optional Instructions (e.g. bring mop)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (validation.isValid && locationInput.isNotBlank()) {
                        onConfirmBooking(
                            selectedSkill,
                            locationInput,
                            dateSelection,
                            preferredTimeInput,
                            durationMinutes,
                            offeredPricePaise,
                            instructionsInput
                        )
                    }
                },
                enabled = validation.isValid && locationInput.isNotBlank(),
                modifier = Modifier.testTag("confirm_post_request_btn")
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Post & Hold Escrow")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// -------------------------------------------------------------------------------------
// RECEIPT DIALOG (TRANSPARENT COOPERATIVE BREAKDOWN)
// -------------------------------------------------------------------------------------
@Composable
private fun ReceiptDialog(
    job: Job,
    adminFeePercent: Int,
    onDismiss: () -> Unit
) {
    val payout = WageEngine.calculatePayoutSplit(job.priceInPaise, adminFeePercent)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cooperative Payout Receipt", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Job: ${job.title} (#${job.id.takeLast(6)})", fontWeight = FontWeight.Bold)
                Text(text = "Completed at: ${job.location}", style = MaterialTheme.typography.bodySmall)

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                ReceiptRow(label = "Worker Earnings (90%)", value = WageEngine.formatPaiseCompact(payout.workerWagePaise), isBold = true)
                ReceiptRow(label = "Cooperative Admin Fee ($adminFeePercent%)", value = WageEngine.formatPaiseCompact(payout.adminFeePaise), isBold = false)
                ReceiptRow(label = "  └ Welfare Fund Allocation", value = WageEngine.formatPaiseCompact(payout.welfarePaise), isBold = false)

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                ReceiptRow(label = "Total Escrow Released", value = WageEngine.formatPaiseCompact(job.priceInPaise), isBold = true)
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun ReceiptRow(label: String, value: String, isBold: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal)
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal)
    }
}

// -------------------------------------------------------------------------------------
// DISPUTE DIALOG
// -------------------------------------------------------------------------------------
@Composable
private fun DisputeDialog(
    job: Job,
    onDismiss: () -> Unit,
    onSubmitDispute: (String) -> Unit
) {
    var comment by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Raise Dispute for #${job.id.takeLast(6)}", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "The full escrow payment will remain locked securely. A cooperative admin committee will review worker completion proof against your complaint.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Reason for Dispute") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmitDispute(comment) },
                enabled = comment.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Lock Escrow & Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

fun getCategoryIcon(name: String): ImageVector {
    return when (name.lowercase()) {
        "electrician" -> Icons.Default.Bolt
        "plumber" -> Icons.Default.Plumbing
        "carpenter" -> Icons.Default.Carpenter
        "painter" -> Icons.Default.FormatPaint
        "cleaner", "cleaning" -> Icons.Default.CleaningServices
        "driver" -> Icons.Default.DirectionsCar
        "gardener", "gardening" -> Icons.Default.Yard
        "caregiver" -> Icons.Default.Favorite
        "technician" -> Icons.Default.Build
        else -> Icons.Default.Build
    }
}
