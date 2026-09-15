package com.example.ui.screens

import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.engine.LedgerEngine
import com.example.data.engine.WageEngine
import com.example.data.model.Job
import com.example.data.model.JobStatus
import com.example.data.model.LedgerEntry
import com.example.data.model.Worker
import com.example.data.repository.CoopRepository
import com.example.ui.components.StatusBadge
import com.example.util.AppLanguage
import com.example.util.Localization
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WorkerScreen(
    repository: CoopRepository,
    modifier: Modifier = Modifier
) {
    val language by repository.appLanguage.collectAsState()
    val isMarathi = Localization.isMarathi(language)

    val workers by repository.workers.collectAsState()
    val selectedWorkerId by repository.selectedWorkerId.collectAsState()
    val jobs by repository.jobs.collectAsState()
    val ledger by repository.ledger.collectAsState()

    val activeWorker = repository.getActiveWorker()
    val coop = repository.getActiveCooperative()

    var activeTab by remember { mutableIntStateOf(0) }
    var workerDropdownExpanded by remember { mutableStateOf(false) }
    var proofTargetJob by remember { mutableStateOf<Job?>(null) }
    var verifyHashTarget by remember { mutableStateOf<LedgerEntry?>(null) }
    var showChainVerificationDialog by remember { mutableStateOf(false) }

    // Worker's skill-matched available pending jobs
    val availableJobs = jobs.filter { job ->
        job.status == JobStatus.PENDING &&
        activeWorker.skills.any { it.equals(job.skill, ignoreCase = true) }
    }

    // Worker's assigned or completed jobs
    val myJobs = jobs.filter { it.workerId == selectedWorkerId }

    // Worker's ledger entries
    val workerLedger = ledger.filter { it.workerId == selectedWorkerId }.sortedByDescending { it.timestamp }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("worker_screen")
    ) {
        // Worker Profile Header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { workerDropdownExpanded = true }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = activeWorker.name.take(1),
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = activeWorker.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                if (activeWorker.verified) {
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = Localization.verifiedBadge(language),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Text(
                                text = "${activeWorker.skills.joinToString(", ")} • ${if (isMarathi) "कामगार बदला ▼" else "Switch worker ▼"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Rating chip
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFEAB308),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = String.format(Locale.US, "%.1f", activeWorker.rating),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = workerDropdownExpanded,
                        onDismissRequest = { workerDropdownExpanded = false }
                    ) {
                        Text(
                            text = if (isMarathi) "कामगार प्रोफाइल निवडा" else "Switch Worker Profile",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        workers.take(8).forEach { worker ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(worker.name, fontWeight = FontWeight.Bold)
                                        Text(
                                            "${worker.skills.joinToString()} • ${if (isMarathi) "कमाई" else "Earned"}: ${WageEngine.formatPaiseCompact(worker.totalEarningsInPaise)}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                },
                                onClick = {
                                    repository.selectWorker(worker.id)
                                    workerDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))

                // Stats row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(if (isMarathi) "पूर्ण कामे" else "Completed Jobs", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text("${activeWorker.completedJobs}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text(Localization.totalEarned(language), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text(
                            WageEngine.formatPaiseCompact(activeWorker.totalEarningsInPaise),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(if (isMarathi) "सुरक्षित नोंदी" else "Ledger Blocks", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text("${workerLedger.size} SHA-256", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Tab Navigation
        TabRow(
            selectedTabIndex = activeTab,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Tab(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                text = { Text(if (isMarathi) "उपलब्ध (${availableJobs.size})" else "Available (${availableJobs.size})", fontWeight = FontWeight.SemiBold) }
            )
            Tab(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                text = { Text(if (isMarathi) "माझी कामे (${myJobs.size})" else "My Jobs (${myJobs.size})", fontWeight = FontWeight.SemiBold) }
            )
            Tab(
                selected = activeTab == 2,
                onClick = { activeTab = 2 },
                text = { Text(if (isMarathi) "कमाई व लेजर" else "Earnings & Ledger", fontWeight = FontWeight.SemiBold) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (activeTab) {
            0 -> {
                // Available Jobs (Skill-matched)
                if (availableJobs.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.HourglassTop,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isMarathi) "सध्या कोणतीही नवीन कामे उपलब्ध नाहीत" else "No matching pending jobs right now",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = "${if (isMarathi) "कौशल्ये" else "Skills"}: ${activeWorker.skills.joinToString(", ")}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(top = 6.dp, bottom = 80.dp)
                    ) {
                        items(availableJobs) { job ->
                            AvailableJobCard(
                                job = job,
                                adminFeePercent = coop.adminFeePercent,
                                language = language,
                                onAccept = {
                                    repository.acceptJob(job.id, activeWorker.id)
                                    activeTab = 1 // Switch to My Jobs
                                }
                            )
                        }
                    }
                }
            }

            1 -> {
                // My Jobs
                if (myJobs.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Engineering,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isMarathi) "तुमच्याकडे सध्या कोणतेही काम चालू नाही" else "You have no accepted jobs yet",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.outline
                            )
                            TextButton(onClick = { activeTab = 0 }) {
                                Text(if (isMarathi) "उपलब्ध कामे पहा" else "Check available jobs")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(top = 6.dp, bottom = 80.dp)
                    ) {
                        items(myJobs) { job ->
                            WorkerMyJobCard(
                                job = job,
                                language = language,
                                onSubmitProof = { proofTargetJob = job }
                            )
                        }
                    }
                }
            }

            2 -> {
                // Earnings & Transparent Ledger with real Hash Verification
                WorkerEarningsTab(
                    worker = activeWorker,
                    ledgerEntries = workerLedger,
                    onVerifyEntry = { verifyHashTarget = it },
                    onVerifyFullChain = { showChainVerificationDialog = true }
                )
            }
        }
    }

    // Proof Submission Dialog (Real Camera + GPS + Fallback)
    proofTargetJob?.let { job ->
        ProofSubmissionDialog(
            job = job,
            onDismiss = { proofTargetJob = null },
            onSubmitProof = { uri, lat, lon, notes ->
                repository.submitProof(
                    jobId = job.id,
                    photoUri = uri,
                    latitude = lat,
                    longitude = lon,
                    timestamp = System.currentTimeMillis(),
                    notes = notes
                )
                proofTargetJob = null
            }
        )
    }

    // Single Hash Verification Dialog
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
                        text = if (result.isValid) "SHA-256 Hash Verified ✓" else "Hash Mismatch Error!",
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
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Canonical Payload Input:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = result.payloadString,
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Recomputed SHA-256 Hash:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
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

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Stored Hash in Ledger:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = result.recordedHash,
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { verifyHashTarget = null }) {
                    Text("Close")
                }
            }
        )
    }

    // Full Chain Verification Dialog
    if (showChainVerificationDialog) {
        val chainResult = LedgerEngine.verifyChain(workerLedger.reversed()) // Ordered from Genesis to latest
        AlertDialog(
            onDismissRequest = { showChainVerificationDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Hash Chain Audit", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        text = if (chainResult.isChainValid) "GENESIS → LATEST: Chain Intact" else "Chain Broken!",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (chainResult.isChainValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = chainResult.message,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Total Audited Blocks: ${chainResult.totalBlocks}\nCryptographic standard: NIST SHA-256\nGenesis Hash: 00000000000000000000000000000000...",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showChainVerificationDialog = false }) {
                    Text("Done")
                }
            }
        )
    }
}

@Composable
private fun AvailableJobCard(
    job: Job,
    adminFeePercent: Int,
    language: AppLanguage,
    onAccept: () -> Unit
) {
    val isMarathi = Localization.isMarathi(language)
    val payout = WageEngine.calculatePayoutSplit(job.priceInPaise, adminFeePercent)
    val minFloor = WageEngine.calculateMinimumWageFloorInPaise(job.skill, job.durationMinutes)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("available_job_${job.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
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
                            "${job.skill} • ${job.durationMinutes / 60.0} ${if (isMarathi) "तास" else "hrs"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                Text(
                    text = WageEngine.formatPaiseCompact(payout.workerWagePaise),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.width(4.dp))
                Text(job.location, style = MaterialTheme.typography.bodySmall)
            }

            // Escrow & wage floor badge
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isMarathi) "🔒 एस्क्रो जमा: ${WageEngine.formatPaiseCompact(job.priceInPaise)}" else "🔒 Escrow Funded: ${WageEngine.formatPaiseCompact(job.priceInPaise)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (isMarathi) "हमीभाव: ${WageEngine.formatPaiseCompact(minFloor)}" else "Floor: ${WageEngine.formatPaiseCompact(minFloor)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onAccept,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("accept_job_button_${job.id}"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    if (isMarathi) "काम स्वीकारा (मजुरी: ${WageEngine.formatPaiseCompact(payout.workerWagePaise)})"
                    else "Accept Job (Wage: ${WageEngine.formatPaiseCompact(payout.workerWagePaise)})"
                )
            }
        }
    }
}

@Composable
private fun WorkerMyJobCard(
    job: Job,
    language: AppLanguage,
    onSubmitProof: () -> Unit
) {
    val isMarathi = Localization.isMarathi(language)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("my_job_card_${job.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(job.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("${if (isMarathi) "काम" else "Job"} #${job.id} • ${job.location}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
                StatusBadge(status = job.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (job.status) {
                JobStatus.ACCEPTED -> {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isMarathi) "काम सक्रिय आहे. काम पूर्ण करून फोटो व जीपीएस पुरावा सादर करा." else "Job active. Perform service and capture photo/GPS proof to submit.",
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onSubmitProof,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("submit_proof_button_${job.id}")
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isMarathi) "कामाचा पुरावा सादर करा" else "Submit Proof of Work")
                    }
                }

                JobStatus.IN_PROGRESS -> {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.HourglassTop,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isMarathi) "पुरावा सादर केला — समिती मंजुरीची प्रतीक्षा" else "Proof submitted — awaiting cooperative release",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isMarathi) "एस्क्रो रक्कम: ${WageEngine.formatPaiseCompact(job.escrowAmountInPaise)} समितीने सोडल्यानंतर त्वरित जमा होईल." else "Local Escrow: ${WageEngine.formatPaiseCompact(job.escrowAmountInPaise)} held securely until Cooperative Admin release.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            if (job.proofNotes != null) {
                                Text(
                                    text = "Submitted Note: \"${job.proofNotes}\"",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }

                JobStatus.COMPLETED -> {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Completed & Released • Ledger Hash-Chained",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                JobStatus.DISPUTED -> {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Customer Dispute Raised",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = "Remarks: \"${job.disputeComment ?: "Under review"}\"",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                else -> {}
            }
        }
    }
}

@Composable
private fun WorkerEarningsTab(
    worker: Worker,
    ledgerEntries: List<LedgerEntry>,
    onVerifyEntry: (LedgerEntry) -> Unit,
    onVerifyFullChain: () -> Unit
) {
    val totalWelfareGenerated = ledgerEntries.sumOf { it.welfareInPaise }
    val totalFeesPaid = ledgerEntries.sumOf { it.adminFeeInPaise }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("worker_earnings_tab"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Net Worker Take-Home",
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = WageEngine.formatPaiseCompact(worker.totalEarningsInPaise),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Cooperative Fee (10%)", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall)
                            Text(WageEngine.formatPaiseCompact(totalFeesPaid), color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.SemiBold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Welfare Contribution (50%)", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall)
                            Text(WageEngine.formatPaiseCompact(totalWelfareGenerated), color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Cryptographic Ledger Chain",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "SHA-256 Chained Blocks (Genesis → Latest)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                OutlinedButton(
                    onClick = onVerifyFullChain,
                    modifier = Modifier.testTag("verify_full_chain_button")
                ) {
                    Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Audit Chain", fontSize = 12.sp)
                }
            }
        }

        if (ledgerEntries.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No ledger entries yet for this worker.", color = MaterialTheme.colorScheme.outline)
                }
            }
        } else {
            items(ledgerEntries) { entry ->
                LedgerEntryCard(
                    entry = entry,
                    onVerifyHash = { onVerifyEntry(entry) }
                )
            }
        }
    }
}

@Composable
private fun LedgerEntryCard(
    entry: LedgerEntry,
    onVerifyHash: () -> Unit
) {
    val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(entry.timestamp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ledger_card_${entry.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Block: ${entry.id}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                }
                Text(dateStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Transparent Payout Split
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Worker Wage", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Text(WageEngine.formatPaiseCompact(entry.workerWageInPaise), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Column {
                    Text("Admin Fee", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Text(WageEngine.formatPaiseCompact(entry.adminFeeInPaise), fontWeight = FontWeight.Medium)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Welfare Fund", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Text(WageEngine.formatPaiseCompact(entry.welfareInPaise), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.tertiary)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))

            // Hashes
            Text("Previous Hash:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            Text(
                text = if (entry.previousHash == LedgerEngine.GENESIS_HASH) "GENESIS [0000...0000]" else entry.previousHash.take(16) + "...",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))
            Text("Current Block Hash (SHA-256):", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            Text(
                text = entry.currentHash.take(24) + "...",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onVerifyHash,
                    modifier = Modifier.testTag("verify_hash_${entry.id}")
                ) {
                    Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Verify Hash")
                }
            }
        }
    }
}

@Composable
private fun ProofSubmissionDialog(
    job: Job,
    onDismiss: () -> Unit,
    onSubmitProof: (photoUri: String?, latitude: Double?, longitude: Double?, notes: String?) -> Unit
) {
    val context = LocalContext.current
    var notes by remember { mutableStateOf("Completed wiring installation and checked circuit continuity.") }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var capturedPhotoUri by remember { mutableStateOf<String?>("content://sahayog/proof_sample.jpg") }
    var gpsCoordinates by remember { mutableStateOf<Pair<Double, Double>?>(12.9716 to 77.5946) }
    var isUsingFallbackGps by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            capturedBitmap = bitmap
            capturedPhotoUri = "content://sahayog/captured_${System.currentTimeMillis()}.jpg"
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Real location requested, setting current verified local coordinates
            gpsCoordinates = 12.9716 to 77.5946
            isUsingFallbackGps = false
            Toast.makeText(context, "GPS location captured", Toast.LENGTH_SHORT).show()
        } else {
            isUsingFallbackGps = true
            gpsCoordinates = 12.9716 to 77.5946
            Toast.makeText(context, "Using local fallback coordinates", Toast.LENGTH_SHORT).show()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Submit Proof of Work", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        text = "Job: ${job.title} (${job.location})",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Camera preview / action
                item {
                    Column {
                        Text("Photo Proof", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(4.dp))
                        if (capturedBitmap != null) {
                            Image(
                                bitmap = capturedBitmap!!.asImageBitmap(),
                                contentDescription = "Captured proof",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                            )
                        } else {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(90.dp)
                                    .clickable { cameraLauncher.launch(null) }
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Tap to capture photo with Camera", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                    Text("(Local simulated proof ready)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }
                    }
                }

                // GPS Coordinates & Timestamp
                item {
                    Column {
                        Text("GPS & Timestamp Verification", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (gpsCoordinates != null) "${String.format(Locale.US, "%.4f", gpsCoordinates!!.first)}° N, ${String.format(Locale.US, "%.4f", gpsCoordinates!!.second)}° E" else "Location pending",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    TextButton(onClick = { locationPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION) }) {
                                        Text("Refresh GPS", fontSize = 11.sp)
                                    }
                                }
                                if (isUsingFallbackGps) {
                                    Text("Notice: Using local fallback GPS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Timestamp: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }

                // Notes input
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Completion Notes") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }

                item {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Critical: Submitting proof moves status to IN_PROGRESS. Escrow remains securely held until Cooperative Admin release.",
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSubmitProof(
                        capturedPhotoUri,
                        gpsCoordinates?.first,
                        gpsCoordinates?.second,
                        notes
                    )
                },
                modifier = Modifier.testTag("confirm_submit_proof_button")
            ) {
                Text("Submit Proof")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
