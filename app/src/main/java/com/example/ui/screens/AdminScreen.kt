package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HowToVote
import java.util.Locale
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.engine.WageEngine
import com.example.data.model.Customer
import com.example.data.model.Job
import com.example.data.model.JobStatus
import com.example.data.model.Worker
import com.example.data.preferences.UserProfile
import com.example.data.repository.CoopRepository
import com.example.ui.components.StatusBadge
import com.example.util.AppLanguage
import com.example.util.Localization

@Composable
fun AdminScreen(
    repository: CoopRepository,
    onOpenFairnessComparison: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val language by repository.appLanguage.collectAsState()
    val isMarathi = Localization.isMarathi(language)

    val workers by repository.workers.collectAsState()
    val customers by repository.customers.collectAsState()
    val jobs by repository.jobs.collectAsState()
    val userProfile by repository.activeUserProfile.collectAsState()
    val coop = repository.getActiveCooperative()

    // 4 Bottom Navigation Destinations:
    // 0 = Overview & Disputes, 1 = Member Register, 2 = Governance & Surplus, 3 = Profile & Settings
    var selectedNavIndex by remember { mutableIntStateOf(0) }

    var selectedProofJob by remember { mutableStateOf<Job?>(null) }
    var refundTargetJob by remember { mutableStateOf<Job?>(null) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    val totalMembers = workers.size + customers.size
    val verifiedWorkers = workers.count { it.verified }
    val activeEscrowPaise = jobs.filter { it.status == JobStatus.PENDING || it.status == JobStatus.ACCEPTED || it.status == JobStatus.IN_PROGRESS }
        .sumOf { it.escrowAmountInPaise }
    val completedJobsCount = jobs.count { it.status == JobStatus.COMPLETED }
    val pendingReleasesAndDisputes = jobs.filter { it.status == JobStatus.IN_PROGRESS || it.status == JobStatus.DISPUTED }

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
                            Icon(Icons.Default.Shield, contentDescription = "Overview")
                            if (pendingReleasesAndDisputes.isNotEmpty()) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(16.dp)
                                ) {
                                    Text(
                                        text = "${pendingReleasesAndDisputes.size}",
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
                    label = { Text(if (isMarathi) "प्रशासन" else "Overview", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("nav_admin_overview")
                )
                NavigationBarItem(
                    selected = selectedNavIndex == 1,
                    onClick = { selectedNavIndex = 1 },
                    icon = { Icon(Icons.Default.Group, contentDescription = "Members") },
                    label = { Text(if (isMarathi) "नोंदवही" else "Members", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("nav_admin_members")
                )
                NavigationBarItem(
                    selected = selectedNavIndex == 2,
                    onClick = { selectedNavIndex = 2 },
                    icon = { Icon(Icons.Default.HowToVote, contentDescription = "Governance") },
                    label = { Text(if (isMarathi) "प्रशासन/लाभांश" else "Governance", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("nav_admin_governance")
                )
                NavigationBarItem(
                    selected = selectedNavIndex == 3,
                    onClick = { selectedNavIndex = 3 },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text(if (isMarathi) "माझे खाते" else "Profile", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("nav_admin_profile")
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
                0 -> AdminOverviewQueueTab(
                    coop = coop,
                    totalMembers = totalMembers,
                    verifiedWorkers = verifiedWorkers,
                    activeEscrowPaise = activeEscrowPaise,
                    completedJobsCount = completedJobsCount,
                    pendingReleasesAndDisputes = pendingReleasesAndDisputes,
                    workers = workers,
                    isMarathi = isMarathi,
                    onOpenFairness = onOpenFairnessComparison,
                    onInspectProof = { selectedProofJob = it },
                    onReleaseToWorker = { repository.adminReleaseJob(it.id) },
                    onRefundCustomer = { refundTargetJob = it }
                )
                1 -> AdminMemberRegisterTab(
                    workers = workers,
                    customers = customers,
                    isMarathi = isMarathi,
                    onToggleWorkerVerification = { repository.toggleWorkerVerification(it) }
                )
                2 -> AdminGovernanceSurplusTab(
                    repository = repository,
                    coop = coop,
                    isMarathi = isMarathi,
                    onOpenFairness = onOpenFairnessComparison
                )
                3 -> AdminProfileTab(
                    userProfile = userProfile,
                    coop = coop,
                    isMarathi = isMarathi,
                    onToggleLanguage = { CoopRepository.toggleLanguage() },
                    onChangeRole = { CoopRepository.logoutUser() },
                    onResetDemo = { showResetConfirmDialog = true }
                )
            }
        }
    }

    // Proof Inspector Dialog
    selectedProofJob?.let { job ->
        val worker = workers.firstOrNull { it.id == job.workerId }
        val payout = WageEngine.calculatePayoutSplit(job.priceInPaise, coop.adminFeePercent)
        AlertDialog(
            onDismissRequest = { selectedProofJob = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Audit Work Proof & Escrow Release", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(job.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Text("Worker: ${worker?.name ?: "Unassigned"} (${job.skill})", style = MaterialTheme.typography.bodySmall)
                    Text("Location: ${job.location}", style = MaterialTheme.typography.bodySmall)

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Proof Metadata", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text("GPS Coordinates: ${job.proofLatitude ?: 18.5204}° N, ${job.proofLongitude ?: 73.8567}° E", style = MaterialTheme.typography.bodySmall)
                            Text("Notes: \"${job.proofNotes ?: "Work completed satisfactorily"}\"", style = MaterialTheme.typography.bodySmall)
                            if (job.status == JobStatus.DISPUTED) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Customer Dispute Reason: \"${job.disputeComment}\"", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Text("Escrow Release Breakdown:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Worker Wage (Net):", style = MaterialTheme.typography.bodySmall)
                        Text(WageEngine.formatPaiseCompact(payout.workerWagePaise), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Coop Admin Fee (${payout.adminFeePercent}%):", style = MaterialTheme.typography.bodySmall)
                        Text(WageEngine.formatPaiseCompact(payout.adminFeePaise), style = MaterialTheme.typography.bodySmall)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("  └ Welfare Fund Allocation:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text(WageEngine.formatPaiseCompact(payout.welfarePaise), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.adminReleaseJob(job.id)
                        selectedProofJob = null
                        Toast.makeText(context, "Escrow released to worker & welfare ledger block generated.", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("modal_release_button")
                ) {
                    Text("Release Escrow to Worker")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedProofJob = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Refund Customer Dialog
    refundTargetJob?.let { job ->
        var refundReason by remember { mutableStateOf("Cooperative resolution: Escrow refunded to customer after inspection.") }
        AlertDialog(
            onDismissRequest = { refundTargetJob = null },
            title = { Text("Refund Customer & Clear Escrow", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Refund amount: ${WageEngine.formatPaiseCompact(job.priceInPaise)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    Text("Notice: Escrow will be returned to customer. Worker will NOT receive a payout for this job.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    OutlinedTextField(
                        value = refundReason,
                        onValueChange = { refundReason = it },
                        label = { Text("Refund Reason") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.adminRefundJob(job.id, refundReason)
                        refundTargetJob = null
                        Toast.makeText(context, "Customer refunded successfully.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("modal_confirm_refund_button")
                ) {
                    Text("Confirm Refund")
                }
            },
            dismissButton = {
                TextButton(onClick = { refundTargetJob = null }) { Text("Cancel") }
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
// TAB 0: COOPERATIVE OVERVIEW & DISPUTE / RELEASE QUEUE
// -------------------------------------------------------------------------------------
@Composable
private fun AdminOverviewQueueTab(
    coop: com.example.data.model.Cooperative,
    totalMembers: Int,
    verifiedWorkers: Int,
    activeEscrowPaise: Long,
    completedJobsCount: Int,
    pendingReleasesAndDisputes: List<Job>,
    workers: List<Worker>,
    isMarathi: Boolean,
    onOpenFairness: () -> Unit,
    onInspectProof: (Job) -> Unit,
    onReleaseToWorker: (Job) -> Unit,
    onRefundCustomer: (Job) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 14.dp, bottom = 90.dp)
    ) {
        // Cooperative Summary Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isMarathi) "सहकारी संस्था प्रशासन" else "Cooperative Administration",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            )
                            Text(
                                text = coop.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Text(
                                text = "${coop.city} Chapter • Admin Fee: ${coop.adminFeePercent}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                            )
                        }

                        OutlinedButton(
                            onClick = onOpenFairness,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onPrimary),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)),
                            modifier = Modifier.testTag("open_fairness_button")
                        ) {
                            Icon(Icons.Default.CompareArrows, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Fair Dispatch", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f))

                    // 4 Key Stats: Total Members, Active Escrow, Welfare Fund, Completed Jobs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Members", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                            Text("$totalMembers ($verifiedWorkers ✓)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.titleSmall)
                        }
                        Column {
                            Text("Active Escrow", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                            Text(WageEngine.formatPaiseCompact(activeEscrowPaise), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.titleSmall)
                        }
                        Column {
                            Text("Welfare Fund", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                            Text(WageEngine.formatPaiseCompact(coop.welfareFundInPaise), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.titleSmall)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Completed", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                            Text("$completedJobsCount", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }
            }
        }

        // Section Title: Dispute & Release Queue
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isMarathi) "तक्रार व मंजुरी रांग (${pendingReleasesAndDisputes.size})" else "Dispute & Release Queue (${pendingReleasesAndDisputes.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (pendingReleasesAndDisputes.any { it.status == JobStatus.DISPUTED }) {
                    Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.errorContainer) {
                        Text(
                            text = "Disputes Active",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // Queue Items
        if (pendingReleasesAndDisputes.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Queue Clear", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("No pending escrow releases or active customer disputes.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        } else {
            items(pendingReleasesAndDisputes) { job ->
                val worker = workers.firstOrNull { it.id == job.workerId }
                val payout = WageEngine.calculatePayoutSplit(job.priceInPaise, coop.adminFeePercent)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_queue_card_${job.id}"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(
                        1.dp,
                        if (job.status == JobStatus.DISPUTED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(job.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("Worker: ${worker?.name ?: "Unassigned"} • Job #${job.id.takeLast(6)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            }
                            StatusBadge(status = job.status)
                        }

                        if (job.status == JobStatus.DISPUTED) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.ReportProblem, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Dispute Reason: \"${job.disputeComment ?: "Customer contested completion"}\"",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Escrow: ${WageEngine.formatPaiseCompact(job.priceInPaise)}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                            Text(text = "Worker Net: ${WageEngine.formatPaiseCompact(payout.workerWagePaise)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onInspectProof(job) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Inspect Proof", fontSize = 11.sp)
                            }
                            Button(
                                onClick = { onReleaseToWorker(job) },
                                modifier = Modifier.weight(1.2f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Release Escrow", fontSize = 11.sp)
                            }
                            if (job.status == JobStatus.DISPUTED) {
                                Button(
                                    onClick = { onRefundCustomer(job) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Refund", fontSize = 11.sp)
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
// TAB 1: MEMBER REGISTER (20 WORKERS + 10 CUSTOMERS)
// -------------------------------------------------------------------------------------
@Composable
private fun AdminMemberRegisterTab(
    workers: List<Worker>,
    customers: List<Customer>,
    isMarathi: Boolean,
    onToggleWorkerVerification: (String) -> Unit
) {
    var registerFilter by remember { mutableStateOf("WORKERS") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = registerFilter == "WORKERS",
                onClick = { registerFilter = "WORKERS" },
                label = { Text("Workers (${workers.size})", fontWeight = FontWeight.SemiBold) }
            )
            FilterChip(
                selected = registerFilter == "CUSTOMERS",
                onClick = { registerFilter = "CUSTOMERS" },
                label = { Text("Customers (${customers.size})", fontWeight = FontWeight.SemiBold) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (registerFilter == "WORKERS") {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 90.dp)
            ) {
                items(workers) { worker ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(worker.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    if (worker.verified) {
                                        Icon(Icons.Default.VerifiedUser, contentDescription = "Verified", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                    }
                                }
                                Text("${worker.skills.joinToString()} • ${worker.completedJobs} jobs completed", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                Text("Earned: ${WageEngine.formatPaiseCompact(worker.totalEarningsInPaise)} • ⭐ ${String.format(Locale.US, "%.1f", worker.rating)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }

                            Button(
                                onClick = { onToggleWorkerVerification(worker.id) },
                                shape = RoundedCornerShape(8.dp),
                                colors = if (worker.verified) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                                else ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.testTag("toggle_verify_${worker.id}")
                            ) {
                                Text(if (worker.verified) "Unverify" else "Verify", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 90.dp)
            ) {
                items(customers) { customer ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(customer.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            Text("Location: ${customer.location}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            Text("Phone: ${customer.phone}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// TAB 2: GOVERNANCE, WELFARE & SURPLUS DIVIDEND
// -------------------------------------------------------------------------------------
@Composable
private fun AdminGovernanceSurplusTab(
    repository: CoopRepository,
    coop: com.example.data.model.Cooperative,
    isMarathi: Boolean,
    onOpenFairness: () -> Unit
) {
    var feeSlider by remember(coop.adminFeePercent) { mutableFloatStateOf(coop.adminFeePercent.toFloat()) }
    var voteMessage by remember { mutableStateOf<String?>(null) }
    var surplusSliderPaise by remember { mutableStateOf(1000000L) }
    var surplusMessage by remember { mutableStateOf<String?>(null) }

    val verifiedWorkers = repository.workers.collectAsState().value.filter { it.cooperativeId == coop.id && it.verified }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 14.dp, bottom = 90.dp)
    ) {
        // Democratic Fee Quorum Voting
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Democratic Governance & Fee Voting", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Cooperative members vote to determine the platform administrative fee. 50% of this fee directly funds member emergency welfare.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Cooperative Admin Fee", fontWeight = FontWeight.SemiBold)
                        Text("${feeSlider.toInt()}%", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Slider(
                        value = feeSlider,
                        onValueChange = { feeSlider = it },
                        valueRange = 0f..20f,
                        steps = 19,
                        modifier = Modifier.testTag("admin_fee_slider")
                    )

                    Button(
                        onClick = {
                            repository.updateAdminFeePercent(coop.id, feeSlider.toInt())
                            voteMessage = "Quorum vote passed with 86% cooperative consensus. New admin fee set to ${feeSlider.toInt()}%."
                        },
                        modifier = Modifier.fillMaxWidth().testTag("simulate_vote_button")
                    ) {
                        Icon(Icons.Default.HowToVote, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Simulate Member Quorum Vote")
                    }

                    voteMessage?.let {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = it, modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        // Welfare Fund Balance & Distribution Chart
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Welfare Fund Reserves", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Accumulated Balance: ${WageEngine.formatPaiseCompact(coop.welfareFundInPaise)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                    FundDistributionBarChart(
                        welfareFundPaise = coop.welfareFundInPaise,
                        reserveFundPaise = coop.welfareFundInPaise * 12L / 10L,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                    )
                }
            }
        }

        // Democratic Surplus & Patronage Dividend Distribution
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Surplus & Patronage Dividend", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "Cooperative Principle: Accumulated annual surplus is distributed equally among all verified worker members via hash-chained ledger blocks.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Dividend Pool Amount", fontWeight = FontWeight.SemiBold)
                        Text(WageEngine.formatPaise(surplusSliderPaise), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }

                    Slider(
                        value = surplusSliderPaise.toFloat(),
                        onValueChange = { surplusSliderPaise = (it / 100000L).toLong() * 100000L },
                        valueRange = 200000f..coop.welfareFundInPaise.coerceAtLeast(200000L).toFloat(),
                        modifier = Modifier.testTag("surplus_slider")
                    )

                    val perMemberEstimate = if (verifiedWorkers.isNotEmpty()) surplusSliderPaise / verifiedWorkers.size else 0L
                    Text(
                        text = "Estimated payout: ${WageEngine.formatPaise(perMemberEstimate)} each across ${verifiedWorkers.size} verified members",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                    Button(
                        onClick = {
                            val result = repository.distributeSurplus(coop.id, surplusSliderPaise)
                            result.onSuccess { total ->
                                surplusMessage = "Distributed ${WageEngine.formatPaise(total)} across ${verifiedWorkers.size} verified members (${WageEngine.formatPaise(perMemberEstimate)} each). Cryptographic dividend ledger entries created!"
                            }.onFailure { err ->
                                surplusMessage = "Error: ${err.message}"
                            }
                        },
                        enabled = verifiedWorkers.isNotEmpty() && (surplusSliderPaise <= coop.welfareFundInPaise),
                        modifier = Modifier.fillMaxWidth().testTag("distribute_surplus_button")
                    ) {
                        Icon(Icons.Default.Paid, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Distribute Patronage Dividend")
                    }

                    surplusMessage?.let {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = it, modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// TAB 3: ADMIN PROFILE & SETTINGS
// -------------------------------------------------------------------------------------
@Composable
private fun AdminProfileTab(
    userProfile: UserProfile?,
    coop: com.example.data.model.Cooperative,
    isMarathi: Boolean,
    onToggleLanguage: () -> Unit,
    onChangeRole: () -> Unit,
    onResetDemo: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 14.dp, bottom = 90.dp)
    ) {
        // Admin Profile Card
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
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = userProfile?.name ?: "District Administrator", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(text = userProfile?.adminPosition ?: "Executive Cooperative Secretary", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(text = "Cooperative Chapter: ${coop.name} (${coop.city})", style = MaterialTheme.typography.bodySmall)
                    Text(text = "Registered Cluster: ${userProfile?.cooperativeBranch ?: "Main District Cluster"}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // Language Setting
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
                            text = if (isMarathi) "ग्राहक किंवा कामगार भूमिकेत जाण्यासाठी" else "Switch to Customer or Worker onboarding",
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

@Composable
private fun FundDistributionBarChart(
    welfareFundPaise: Long,
    reserveFundPaise: Long,
    modifier: Modifier = Modifier
) {
    val total = (welfareFundPaise + reserveFundPaise).coerceAtLeast(1L).toFloat()
    val welfareRatio = (welfareFundPaise.toFloat() / total).coerceIn(0.1f, 0.9f)

    Canvas(modifier = modifier) {
        val barHeight = 24.dp.toPx()
        val cornerRadius = 12.dp.toPx()
        val width = size.width
        val welfareWidth = width * welfareRatio

        drawRoundRect(
            color = Color(0xFF825500),
            topLeft = Offset(0f, 10f),
            size = Size(width, barHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius)
        )

        drawRoundRect(
            color = Color(0xFF006A58),
            topLeft = Offset(0f, 10f),
            size = Size(welfareWidth, barHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius)
        )
    }
}
