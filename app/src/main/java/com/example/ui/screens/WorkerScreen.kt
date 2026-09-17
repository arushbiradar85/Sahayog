package com.example.ui.screens

import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.engine.LedgerEngine
import com.example.data.engine.WageEngine
import com.example.data.model.Job
import com.example.data.model.JobStatus
import com.example.data.model.LedgerEntry
import com.example.data.model.Worker
import com.example.data.preferences.UserProfile
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
fun WorkerScreen(
    repository: CoopRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val language by repository.appLanguage.collectAsState()
    val isMarathi = Localization.isMarathi(language)

    val workers by repository.workers.collectAsState()
    val activeWorker = workers.firstOrNull { it.id == repository.getActiveWorker().id } ?: repository.getActiveWorker()
    val userProfile by repository.activeUserProfile.collectAsState()
    val jobs by repository.jobs.collectAsState()
    val ledger by repository.ledger.collectAsState()
    val coop = repository.getActiveCooperative()

    // 4 Bottom Navigation Destinations:
    // 0 = Available Requests, 1 = My Jobs, 2 = Earnings & Ledger, 3 = Profile & Settings
    var selectedNavIndex by remember { mutableIntStateOf(0) }

    // Dialog States
    var proofTargetJob by remember { mutableStateOf<Job?>(null) }
    var verifyHashTarget by remember { mutableStateOf<LedgerEntry?>(null) }
    var showChainVerificationDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    // Filter declined jobs locally in this session
    var declinedJobIds by remember { mutableStateOf(setOf<String>()) }

    // Determine all active trades/skills for this worker
    val workerSkills = remember(activeWorker, userProfile) {
        val list = mutableListOf<String>()
        list.addAll(activeWorker.skills)
        if (activeWorker.skill.isNotBlank()) list.add(activeWorker.skill)
        userProfile?.let { prof ->
            if (prof.workerPrimarySkill.isNotBlank()) list.add(prof.workerPrimarySkill)
            prof.workerSecondarySkills.forEach { s -> if (s.isNotBlank()) list.add(s) }
        }
        list.filter { it.isNotBlank() }.distinct()
    }

    // Available jobs: match worker skill, not declined, worker not assigned yet, and has open slot
    val availableJobs = jobs.filter { job ->
        !declinedJobIds.contains(job.id) &&
        job.matchesWorkerSkill(workerSkills) &&
        !job.isWorkerAssigned(activeWorker.id) &&
        (job.status == JobStatus.PENDING || (!job.isFullyAssigned() && job.requirements.isNotEmpty()))
    }

    // Worker's assigned or completed jobs
    val myJobs = jobs.filter { job ->
        job.isWorkerAssigned(activeWorker.id) || job.workerId == activeWorker.id
    }

    // Worker's ledger entries (sorted latest first)
    val workerLedger = ledger.filter { it.workerId == activeWorker.id }.sortedByDescending { it.timestamp }

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
                    icon = {
                        Box {
                            Icon(Icons.Default.HourglassTop, contentDescription = "Requests")
                            if (availableJobs.isNotEmpty()) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(16.dp)
                                ) {
                                    Text(
                                        text = "${availableJobs.size}",
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
                    label = { Text(if (isMarathi) "उपलब्ध कामे" else "Requests", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("nav_worker_requests")
                )
                NavigationBarItem(
                    selected = selectedNavIndex == 1,
                    onClick = { selectedNavIndex = 1 },
                    icon = { Icon(Icons.Default.Engineering, contentDescription = "My Jobs") },
                    label = { Text(if (isMarathi) "माझी कामे" else "My Jobs", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("nav_worker_jobs")
                )
                NavigationBarItem(
                    selected = selectedNavIndex == 2,
                    onClick = { selectedNavIndex = 2 },
                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "Earnings") },
                    label = { Text(if (isMarathi) "कमाई व लेजर" else "Earnings", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("nav_worker_earnings")
                )
                NavigationBarItem(
                    selected = selectedNavIndex == 3,
                    onClick = { selectedNavIndex = 3 },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text(if (isMarathi) "माझे खाते" else "Profile", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("nav_worker_profile")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedNavIndex) {
                0 -> WorkerAvailableRequestsTab(
                    availableJobs = availableJobs,
                    activeWorker = activeWorker,
                    workerSkills = workerSkills,
                    adminFeePercent = coop.adminFeePercent,
                    isMarathi = isMarathi,
                    onAccept = { job, targetSkill ->
                        val skillToAccept = targetSkill ?: workerSkills.firstOrNull { ws -> job.requirements.any { it.matchesSkill(ws) } } ?: job.skill
                        repository.acceptJobRequirement(job.id, skillToAccept, activeWorker.id, activeWorker.name)
                        val updatedJob = repository.jobs.value.firstOrNull { it.id == job.id } ?: job
                        TwoDeviceSyncManager.postAcceptanceToHub(updatedJob, activeWorker.id, skillToAccept, activeWorker.name)
                        NotificationHelper.notifyWorkerAccepted(context, activeWorker.name, skillToAccept)
                        selectedNavIndex = 1 // Switch to My Jobs
                        Toast.makeText(context, "Accepted as $skillToAccept! Synced to Hub.", Toast.LENGTH_SHORT).show()
                    },
                    onDecline = { job ->
                        declinedJobIds = declinedJobIds + job.id
                        Toast.makeText(context, "Request dismissed.", Toast.LENGTH_SHORT).show()
                    }
                )
                1 -> WorkerMyJobsTab(
                    myJobs = myJobs,
                    isMarathi = isMarathi,
                    onSubmitProof = { proofTargetJob = it }
                )
                2 -> WorkerEarningsLedgerTab(
                    worker = activeWorker,
                    workerLedger = workerLedger,
                    isMarathi = isMarathi,
                    onVerifyEntry = { verifyHashTarget = it },
                    onVerifyFullChain = { showChainVerificationDialog = true }
                )
                3 -> WorkerProfileTab(
                    worker = activeWorker,
                    userProfile = userProfile,
                    workerSkills = workerSkills,
                    isMarathi = isMarathi,
                    onToggleLanguage = { CoopRepository.toggleLanguage() },
                    onUpdateSkill = { newSkill ->
                        repository.updateWorkerSkill(newSkill)
                        Toast.makeText(context, "Trade updated to $newSkill", Toast.LENGTH_SHORT).show()
                    },
                    onChangeRole = { CoopRepository.logoutUser() },
                    onResetDemo = { showResetConfirmDialog = true }
                )
            }
        }
    }

    // Proof Submission Dialog (Photo + GPS + Timestamp + Notes)
    proofTargetJob?.let { job ->
        ProofSubmissionDialog(
            job = job,
            onDismiss = { proofTargetJob = null },
            onSubmitProof = { photoUri, lat, lon, notes ->
                repository.submitProof(
                    jobId = job.id,
                    photoUri = photoUri,
                    latitude = lat,
                    longitude = lon,
                    timestamp = System.currentTimeMillis(),
                    notes = notes
                )
                NotificationHelper.notifyProofSubmitted(context, activeWorker.name, job.skill)
                proofTargetJob = null
                Toast.makeText(context, "Proof submitted with GPS and timestamp! Escrow held pending review.", Toast.LENGTH_LONG).show()
            }
        )
    }

    // Single Entry SHA-256 Hash Verification Dialog
    verifyHashTarget?.let { entry ->
        val result = LedgerEngine.verifyEntry(entry)
        AlertDialog(
            onDismissRequest = { verifyHashTarget = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (result.isValid) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (result.isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (result.isValid) "SHA-256 Cryptographic Match ✓" else "Hash Tamper Detected!",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = result.reason,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (result.isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Stored Hash:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = result.recordedHash,
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Recomputed SHA-256:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = result.recomputedHash,
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { verifyHashTarget = null }) { Text("Close") }
            }
        )
    }

    // Full Chain Verification Dialog
    if (showChainVerificationDialog) {
        val chainResult = LedgerEngine.verifyChain(workerLedger.reversed())
        AlertDialog(
            onDismissRequest = { showChainVerificationDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ledger Integrity Certificate", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (chainResult.isChainValid) "GENESIS → HEAD: All Hashes Chained Validly" else "Chain Compromised",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (chainResult.isChainValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                    Text(text = chainResult.message, style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = "Total Validated Blocks: ${chainResult.totalBlocks}\nStandard: SHA-256 Hash Chaining\nImmutable append-only local storage.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showChainVerificationDialog = false }) { Text("Done") }
            }
        )
    }

    // Reset Demo Confirmation Dialog
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
// TAB 0: AVAILABLE REQUESTS (SHOWS CUSTOMER REQUESTS WITH ACCEPT/DECLINE)
// -------------------------------------------------------------------------------------
@Composable
private fun WorkerAvailableRequestsTab(
    availableJobs: List<Job>,
    activeWorker: Worker,
    workerSkills: List<String>,
    adminFeePercent: Int,
    isMarathi: Boolean,
    onAccept: (Job, String?) -> Unit,
    onDecline: (Job) -> Unit
) {
    val sdf = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Worker Greeting Banner
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = activeWorker.name.take(1).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isMarathi) "नमस्ते, ${activeWorker.name}" else "Hello, ${activeWorker.name}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        if (activeWorker.verified) {
                            Icon(Icons.Default.Verified, contentDescription = "Verified", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        }
                    }
                    Text(
                        text = "Your Trade: ${workerSkills.joinToString(", ")} • ⭐ ${String.format(Locale.US, "%.1f", activeWorker.rating)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isMarathi) "उपलब्ध ग्राहक विनंत्या (${availableJobs.size})" else "Matching Job Requests (${availableJobs.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = if (isMarathi) "एस्क्रो हमी" else "Escrow Funded",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (availableJobs.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.HourglassTop,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isMarathi) "तुमच्या कौशल्यांशी जुळणारी विनंती नाही" else "No matching requests waiting for ${workerSkills.joinToString("/")}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (isMarathi) "सहयोग हब वरून नवीन कामे आपोआप दिसतील" else "New requests from Customers via Sahayog Hub appear here in real time",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 90.dp)
            ) {
                items(availableJobs) { job ->
                    val payout = WageEngine.calculatePayoutSplit(job.priceInPaise, adminFeePercent)
                    val postedTimeStr = sdf.format(Date(job.createdAtTimestamp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("available_job_${job.id}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Service Title & Worker Payout
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
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = getCategoryIcon(job.skill),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(job.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Text(
                                            "${job.skill} • ${job.durationMinutes / 60.0} hrs",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Pooled Payout",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Text(
                                        text = WageEngine.formatPaiseCompact(payout.workerWagePaise),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                            // Location, Time & Customer Offer
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.outline)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(job.location, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Posted: $postedTimeStr",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Offer: ${WageEngine.formatPaiseCompact(job.priceInPaise)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Time: ${job.dateTime}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }

                            // Multi-requirement breakdown
                            if (job.requirements.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = "Workers Needed (${job.assignedWorkersCount()}/${job.totalWorkersNeeded()} Assigned):",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        job.requirements.forEach { req ->
                                            val matchesWorker = workerSkills.any { ws -> ws.equals(req.skill, ignoreCase = true) }
                                            val isWorkerAssignedToThis = req.assignedProviderIds.contains(activeWorker.id)
                                            val hasSlot = !req.isFullyAssigned()
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(getCategoryIcon(req.skill), contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "${req.quantity}x ${req.skill}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = if (req.isFullyAssigned()) "(Filled ✓)" else "(${req.openSlots()} open)",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (req.isFullyAssigned()) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outline
                                                    )
                                                }

                                                if (isWorkerAssignedToThis) {
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = Color(0xFFE8F5E9)
                                                    ) {
                                                        Text(
                                                            text = "You Accepted ✓",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF1B5E20),
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                } else if (matchesWorker && hasSlot) {
                                                    Button(
                                                        onClick = { onAccept(job, req.skill) },
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                                        shape = RoundedCornerShape(6.dp),
                                                        modifier = Modifier.testTag("accept_req_${job.id}_${req.skill.lowercase()}")
                                                    ) {
                                                        Text("Accept as ${req.skill}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                } else if (!matchesWorker) {
                                                    Text(
                                                        text = "Requires ${req.skill}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.outline
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Optional Instructions
                            if (!job.instructions.isNullOrBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Instructions: \"${job.instructions}\"",
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(6.dp)
                                    )
                                }
                            }

                            // Escrow guarantee banner
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "🔒 Payment held in escrow: ${WageEngine.formatPaiseCompact(job.priceInPaise)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }

                            // Bottom Decline & Single-Job Accept Button (if job has no requirements list)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { onDecline(job) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isMarathi) "नकार द्या" else "Decline")
                                }

                                if (job.requirements.isEmpty()) {
                                    Button(
                                        onClick = { onAccept(job, null) },
                                        modifier = Modifier
                                            .weight(1.5f)
                                            .testTag("accept_job_button_${job.id}"),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isMarathi) "स्वीकारा" else "Accept Job")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// TAB 1: MY JOBS (ACTIVE, IN-PROGRESS & COMPLETED WITH PROOF SUBMISSION)
// -------------------------------------------------------------------------------------
@Composable
private fun WorkerMyJobsTab(
    myJobs: List<Job>,
    isMarathi: Boolean,
    onSubmitProof: (Job) -> Unit
) {
    var statusFilter by remember { mutableStateOf("ALL") }
    val filteredJobs = when (statusFilter) {
        "ACTIVE" -> myJobs.filter { it.status == JobStatus.ACCEPTED || it.status == JobStatus.IN_PROGRESS }
        "COMPLETED" -> myJobs.filter { it.status == JobStatus.COMPLETED }
        else -> myJobs
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = statusFilter == "ALL",
                onClick = { statusFilter = "ALL" },
                label = { Text(if (isMarathi) "सर्व (${myJobs.size})" else "All (${myJobs.size})", fontSize = 11.sp) }
            )
            FilterChip(
                selected = statusFilter == "ACTIVE",
                onClick = { statusFilter = "ACTIVE" },
                label = { Text(if (isMarathi) "सक्रिय" else "Active / In-Progress", fontSize = 11.sp) }
            )
            FilterChip(
                selected = statusFilter == "COMPLETED",
                onClick = { statusFilter = "COMPLETED" },
                label = { Text(if (isMarathi) "पूर्ण" else "Completed", fontSize = 11.sp) }
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
                        imageVector = Icons.Default.Engineering,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isMarathi) "कोणतीही कामे नाहीत" else "No assigned jobs in this section",
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
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("my_job_card_${job.id}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(job.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text("Job #${job.id.takeLast(6)} • ${job.location}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                }
                                StatusBadge(status = job.status)
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Scheduled: ${job.dateTime}", style = MaterialTheme.typography.bodySmall)
                                Text(
                                    text = "Budget: ${WageEngine.formatPaiseCompact(job.priceInPaise)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            when (job.status) {
                                JobStatus.ACCEPTED -> {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = if (isMarathi) "काम सक्रिय आहे. काम पूर्ण करून फोटो व जीपीएस पुरावा सादर करा." else "Job accepted. Complete the work and submit Photo + GPS proof to release escrow.",
                                            modifier = Modifier.padding(8.dp),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                    Button(
                                        onClick = { onSubmitProof(job) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("submit_proof_button_${job.id}"),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(if (isMarathi) "कामाचा पुरावा सादर करा (फोटो + GPS)" else "Submit Completion Proof (Photo + GPS)")
                                    }
                                }
                                JobStatus.IN_PROGRESS -> {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text(text = "Proof Submitted (Escrow Locked in Review):", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                            Text(text = "\"${job.proofNotes ?: "Work finished as requested"}\"", style = MaterialTheme.typography.bodySmall)
                                            if (job.proofLatitude != null) {
                                                Text(text = "GPS: ${job.proofLatitude}, ${job.proofLongitude}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                            }
                                        }
                                    }
                                }
                                JobStatus.COMPLETED -> {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (isMarathi) "काम पूर्ण झाले • मजुरी लेजरमध्ये जमा झाली" else "Completed • Wage credited to cryptographic ledger",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                                else -> {}
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// TAB 2: EARNINGS & CRYPTOGRAPHIC LEDGER
// -------------------------------------------------------------------------------------
@Composable
private fun WorkerEarningsLedgerTab(
    worker: Worker,
    workerLedger: List<LedgerEntry>,
    isMarathi: Boolean,
    onVerifyEntry: (LedgerEntry) -> Unit,
    onVerifyFullChain: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 14.dp, bottom = 90.dp)
    ) {
        // Earnings Summary Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isMarathi) "एकूण कमाई (लेजर प्रमाणित)" else "Total Earnings (Ledger Verified)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                            )
                            Text(
                                text = WageEngine.formatPaiseCompact(worker.totalEarningsInPaise),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Completed Jobs", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                            Text("${workerLedger.size}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Welfare Contribution", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                            Text(WageEngine.formatPaiseCompact(workerLedger.sumOf { it.welfareInPaise }), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            }
        }

        // Audit Button
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onVerifyFullChain),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Verified, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Verify Entire Ledger Chain Integrity", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            Text("Audit NIST SHA-256 links from Genesis block", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                    Text("Audit", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Section Title: Chained Ledger Blocks
        item {
            Text(
                text = if (isMarathi) "क्रिप्टोग्राफिक लेजर नोंदी (${workerLedger.size})" else "Cryptographic Ledger Blocks (${workerLedger.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // Ledger Block Items
        items(workerLedger) { entry ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Block #${entry.id.takeLast(6)} • Job #${entry.jobId.takeLast(6)}",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "+${WageEngine.formatPaiseCompact(entry.workerWageInPaise)}",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }

                    Text(
                        text = "Prev Hash: ${entry.previousHash.take(16)}...",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "Curr Hash: ${entry.currentHash.take(16)}...",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Admin: ${WageEngine.formatPaiseCompact(entry.adminFeeInPaise)} • Welfare: ${WageEngine.formatPaiseCompact(entry.welfareInPaise)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        TextButton(
                            onClick = { onVerifyEntry(entry) },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                        ) {
                            Text("Verify Hash", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// TAB 3: WORKER PROFILE & SETTINGS
// -------------------------------------------------------------------------------------
@Composable
private fun WorkerProfileTab(
    worker: Worker,
    userProfile: UserProfile?,
    workerSkills: List<String>,
    isMarathi: Boolean,
    onToggleLanguage: () -> Unit,
    onUpdateSkill: (String) -> Unit,
    onChangeRole: () -> Unit,
    onResetDemo: () -> Unit
) {
    val syncLog by TwoDeviceSyncManager.syncLog.collectAsState()
    val localIp by TwoDeviceSyncManager.localIp.collectAsState()
    val isHubMode by TwoDeviceSyncManager.isHubMode.collectAsState()
    val hubIp by TwoDeviceSyncManager.hubIp.collectAsState()
    val demoSimulation by TwoDeviceSyncManager.demoSimulationEnabled.collectAsState()
    val requestsCount by TwoDeviceSyncManager.requestsCount.collectAsState()

    var hubIpInput by remember(hubIp) { mutableStateOf(hubIp) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 14.dp, bottom = 90.dp)
    ) {
        // Worker Identity Card
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
                                text = (userProfile?.name ?: worker.name).take(1).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = userProfile?.name ?: worker.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                if (worker.verified) {
                                    Icon(Icons.Default.Verified, contentDescription = "Verified", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                }
                            }
                            Text(
                                text = "+91 ${userProfile?.phone ?: "9812345678"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text(text = "Active Trades: ${workerSkills.joinToString(", ")}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    Text(text = "Cooperative Branch: ${userProfile?.cooperativeBranch ?: "Main District Cluster"}", style = MaterialTheme.typography.bodySmall)
                    Text(text = "Rating: ⭐ ${String.format(Locale.US, "%.1f", worker.rating)} • Total Earnings: ${WageEngine.formatPaiseCompact(worker.totalEarningsInPaise)}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // Trade / Skill Selector Card for Hackathon multi-phone demo
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isMarathi) "कौशल्य / व्यवसाय निवडा (डेमो फोन)" else "Provider Trade / Role (Demo Phone)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Set this device's skill to demonstrate targeted job filtering across phones:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    val allTrades = listOf("Electrician", "Gardener", "Plumber", "Carpenter", "Cleaning", "Painter", "Mason", "Technician")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(allTrades) { trade ->
                            val isSelected = workerSkills.any { it.equals(trade, ignoreCase = true) }
                            FilterChip(
                                selected = isSelected,
                                onClick = { onUpdateSkill(trade) },
                                label = { Text(trade, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                leadingIcon = {
                                    Icon(getCategoryIcon(trade), contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            )
                        }
                    }
                }
            }
        }

        // Sahayog Hub & Two-Device Multi-Phone Sync
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
                            text = if (isMarathi) "सहयोग हब आणि मल्टी-फोन सिंक" else "Sahayog Multi-Phone Hub Sync",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "This Device IP: $localIp (Port 8989)",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Run this device as Sahayog Hub", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                if (isHubMode) "Hub active ($requestsCount stored requests)" else "Clients connect to Hub IP",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Switch(
                            checked = isHubMode,
                            onCheckedChange = { TwoDeviceSyncManager.setHubMode(it) }
                        )
                    }

                    if (!isHubMode) {
                        OutlinedTextField(
                            value = hubIpInput,
                            onValueChange = {
                                hubIpInput = it
                                TwoDeviceSyncManager.setHubIp(it)
                            },
                            label = { Text("Dedicated Hub Phone IP Address") },
                            placeholder = { Text("e.g. 192.168.1.5") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Auto-Simulation (Single Device)",
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
                            text = "Status: $syncLog",
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
                            text = if (isMarathi) "ग्राहक भूमिकेत जाण्यासाठी" else "Switch to Customer onboarding",
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
// PROOF SUBMISSION DIALOG (PHOTO + GPS + TIMESTAMP + NOTES)
// -------------------------------------------------------------------------------------
@Composable
private fun ProofSubmissionDialog(
    job: Job,
    onDismiss: () -> Unit,
    onSubmitProof: (photoUri: String?, latitude: Double, longitude: Double, notes: String) -> Unit
) {
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var notes by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf(18.5204) }
    var longitude by remember { mutableStateOf(73.8567) }
    var gpsCaptured by remember { mutableStateOf(true) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            capturedBitmap = bitmap
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Submit Work Proof", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Job: ${job.title} (#${job.id.takeLast(6)})\nUpload work completion photo and verify location stamping to release escrow.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )

                // Photo preview or camera button
                if (capturedBitmap != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = capturedBitmap!!.asImageBitmap(),
                            contentDescription = "Captured Proof",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            try {
                                cameraLauncher.launch(null)
                            } catch (e: Exception) {
                                // Fallback simulated photo
                                capturedBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Capture Photo")
                    }
                }

                // GPS Coordinate info
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text("GPS Location & Timestamp Stamp:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Text(
                                text = "Lat: $latitude, Lon: $longitude\nTime: ${SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(Date())}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Completion Notes (optional)") },
                    placeholder = { Text("e.g. Fixed main circuit breaker wiring.") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val fakeUri = if (capturedBitmap != null) "content://sahayog.proof/${System.currentTimeMillis()}" else "simulated://proof_photo_${job.id}.jpg"
                    onSubmitProof(fakeUri, latitude, longitude, notes.ifBlank { "Work completed satisfactorily as per cooperative standards." })
                }
            ) {
                Text("Submit Proof")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
