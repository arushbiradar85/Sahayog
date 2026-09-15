package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.PriceCheck
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.engine.WageEngine
import com.example.data.model.Job
import com.example.data.model.JobStatus
import com.example.data.model.Worker
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
    val language by repository.appLanguage.collectAsState()
    val isMarathi = Localization.isMarathi(language)

    val workers by repository.workers.collectAsState()
    val jobs by repository.jobs.collectAsState()
    val coop = repository.getActiveCooperative()

    var activeTab by remember { mutableIntStateOf(0) }
    var selectedProofJob by remember { mutableStateOf<Job?>(null) }
    var refundTargetJob by remember { mutableStateOf<Job?>(null) }

    val totalMembers = workers.size
    val verifiedMembers = workers.count { it.verified }
    val pendingReleasesAndDisputes = jobs.filter { it.status == JobStatus.IN_PROGRESS || it.status == JobStatus.DISPUTED }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("admin_screen")
    ) {
        // Cooperative Summary Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isMarathi) "सहकारी संस्था प्रशासन" else "Cooperative Administration",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = coop.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${coop.city} ${if (isMarathi) "विभाग" else "Chapter"} • ${if (isMarathi) "कमिशन" else "Admin Fee"}: ${coop.adminFeePercent}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Button to launch Fair Dispatch Comparison
                    OutlinedButton(
                        onClick = onOpenFairnessComparison,
                        modifier = Modifier.testTag("open_fairness_button")
                    ) {
                        Icon(Icons.Default.CompareArrows, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isMarathi) "पारदर्शक वाटप" else "Fair Dispatch", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))

                // Stats grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricBox(
                        label = if (isMarathi) "सभासद कामगार" else "Members",
                        value = "$totalMembers ($verifiedMembers ✓)",
                        icon = Icons.Default.Group
                    )
                    MetricBox(
                        label = if (isMarathi) "प्रलंबित कामे" else "Pending Queue",
                        value = "${pendingReleasesAndDisputes.size}",
                        icon = Icons.Default.ReceiptLong,
                        isAlert = pendingReleasesAndDisputes.isNotEmpty()
                    )
                    MetricBox(
                        label = if (isMarathi) "कल्याण निधी" else "Welfare Fund",
                        value = WageEngine.formatPaiseCompact(coop.welfareFundInPaise),
                        icon = Icons.Default.Shield
                    )
                }
            }
        }

        // Sub tabs
        TabRow(
            selectedTabIndex = activeTab,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Tab(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                text = { Text(if (isMarathi) "मंजुरी रांग (${pendingReleasesAndDisputes.size})" else "Release Queue (${pendingReleasesAndDisputes.size})", fontWeight = FontWeight.SemiBold) }
            )
            Tab(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                text = { Text(if (isMarathi) "कामगार यादी (${workers.size})" else "Members (${workers.size})", fontWeight = FontWeight.SemiBold) }
            )
            Tab(
                selected = activeTab == 2,
                onClick = { activeTab = 2 },
                text = { Text(if (isMarathi) "नियम व मतदान" else "Governance", fontWeight = FontWeight.SemiBold) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (activeTab) {
            0 -> {
                // Dispute & Release Queue
                if (pendingReleasesAndDisputes.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("All releases & disputes cleared", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            Text("When workers submit proof, jobs will appear here for cooperative release.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp)
                    ) {
                        items(pendingReleasesAndDisputes) { job ->
                            val worker = workers.firstOrNull { it.id == job.workerId }
                            val payout = WageEngine.calculatePayoutSplit(job.priceInPaise, coop.adminFeePercent)

                            AdminQueueCard(
                                job = job,
                                worker = worker,
                                payout = payout,
                                onInspectProof = { selectedProofJob = job },
                                onReleaseToWorker = {
                                    repository.adminReleaseJob(job.id)
                                },
                                onRefundCustomer = {
                                    refundTargetJob = job
                                }
                            )
                        }
                    }
                }
            }

            1 -> {
                // Member Management (Worker verification)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp)
                ) {
                    items(workers) { worker ->
                        MemberManagementCard(
                            worker = worker,
                            onToggleVerification = {
                                repository.toggleWorkerVerification(worker.id)
                            }
                        )
                    }
                }
            }

            2 -> {
                // Governance & Welfare Fund Distribution
                GovernanceTab(
                    repository = repository,
                    currentFeePercent = coop.adminFeePercent,
                    welfareFundPaise = coop.welfareFundInPaise
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
                    Text("Audit Work Proof", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(job.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Text("Worker: ${worker?.name ?: "Unknown"} (${job.skill})", style = MaterialTheme.typography.bodySmall)
                    Text("Location: ${job.location}", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Proof Details", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text("GPS: ${job.proofLatitude ?: 12.9716}° N, ${job.proofLongitude ?: 77.5946}° E", style = MaterialTheme.typography.bodySmall)
                            Text("Notes: \"${job.proofNotes ?: "Work completed satisfactorily"}\"", style = MaterialTheme.typography.bodySmall)
                            if (job.status == JobStatus.DISPUTED) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Customer Dispute: \"${job.disputeComment}\"", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Escrow Release Split:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Worker Wage (Net):", style = MaterialTheme.typography.bodySmall)
                        Text(WageEngine.formatPaiseCompact(payout.workerWagePaise), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Coop Admin Fee (${payout.adminFeePercent}%):", style = MaterialTheme.typography.bodySmall)
                        Text(WageEngine.formatPaiseCompact(payout.adminFeePaise), style = MaterialTheme.typography.bodySmall)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("└ Welfare Fund (50%):", style = MaterialTheme.typography.bodySmall)
                        Text(WageEngine.formatPaiseCompact(payout.welfarePaise), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.adminReleaseJob(job.id)
                        selectedProofJob = null
                    },
                    modifier = Modifier.testTag("modal_release_button")
                ) {
                    Text("Release to Worker")
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
                Column {
                    Text("Refund amount: ${WageEngine.formatPaiseCompact(job.priceInPaise)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    Text("Notice: Escrow will be returned to customer. Worker will NOT receive a payout for this job.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(8.dp))
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
}

@Composable
private fun MetricBox(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isAlert: Boolean = false
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isAlert) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        }
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = if (isAlert) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun AdminQueueCard(
    job: Job,
    worker: Worker?,
    payout: WageEngine.PayoutBreakdown,
    onInspectProof: () -> Unit,
    onReleaseToWorker: () -> Unit,
    onRefundCustomer: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_queue_card_${job.id}"),
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
                    Text(
                        text = "Worker: ${worker?.name ?: "Unassigned"} • Job #${job.id}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                StatusBadge(status = job.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Escrow Held:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        Text(WageEngine.formatPaiseCompact(job.priceInPaise), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Worker Payout:", style = MaterialTheme.typography.bodySmall)
                        Text(WageEngine.formatPaiseCompact(payout.workerWagePaise), style = MaterialTheme.typography.bodySmall)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Welfare Allocation:", style = MaterialTheme.typography.bodySmall)
                        Text(WageEngine.formatPaiseCompact(payout.welfarePaise), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                    }

                    if (job.status == JobStatus.DISPUTED) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "⚠ Dispute: \"${job.disputeComment}\"",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onInspectProof,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Inspect Proof", fontSize = 12.sp)
                }
                Button(
                    onClick = onReleaseToWorker,
                    modifier = Modifier
                        .weight(1.3f)
                        .testTag("admin_release_button_${job.id}"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Release to Worker", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = onRefundCustomer,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.weight(0.9f)
                ) {
                    Text("Refund", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun MemberManagementCard(
    worker: Worker,
    onToggleVerification: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("member_card_${worker.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (worker.verified) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(worker.name.take(1), fontWeight = FontWeight.Bold, color = if (worker.verified) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(worker.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.width(4.dp))
                        if (worker.verified) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = "Verified", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                        }
                    }
                    Text("${worker.skills.joinToString()} • ${worker.completedJobs} jobs", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    Text("Earned: ${WageEngine.formatPaiseCompact(worker.totalEarningsInPaise)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }

            Button(
                onClick = onToggleVerification,
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

@Composable
private fun GovernanceTab(
    repository: CoopRepository,
    currentFeePercent: Int,
    welfareFundPaise: Long
) {
    var feeSlider by remember(currentFeePercent) { mutableFloatStateOf(currentFeePercent.toFloat()) }
    var voteSimulatedMessage by remember { mutableStateOf<String?>(null) }
    val coop = repository.getActiveCooperative()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Democratic Governance & Fee Voting", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Cooperative members vote to set the administrative fee. 50% of the fee funds worker health & emergency welfare.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Cooperative Admin Fee", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text("${feeSlider.toInt()}%", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Slider(
                        value = feeSlider,
                        onValueChange = { feeSlider = it },
                        valueRange = 0f..20f,
                        steps = 19,
                        modifier = Modifier.testTag("admin_fee_slider")
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            repository.updateAdminFeePercent(coop.id, feeSlider.toInt())
                            voteSimulatedMessage = "Vote passed with 84% cooperative quorum. New fee set to ${feeSlider.toInt()}%."
                        },
                        modifier = Modifier.fillMaxWidth().testTag("simulate_vote_button")
                    ) {
                        Icon(Icons.Default.HowToVote, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Simulate Member Quorum Vote")
                    }

                    voteSimulatedMessage?.let {
                        Spacer(modifier = Modifier.height(8.dp))
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

        // Fund Distribution Chart (Compose Canvas)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Cooperative Fund Distribution", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Total Accumulated Welfare Fund: ${WageEngine.formatPaiseCompact(welfareFundPaise)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Canvas Bar visualization
                    FundDistributionBarChart(
                        welfareFundPaise = welfareFundPaise,
                        reserveFundPaise = welfareFundPaise * 12L / 10L,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        LegendItem(color = Color(0xFF006A58), label = "Welfare Fund (Health/Emergency)")
                        LegendItem(color = Color(0xFF825500), label = "Admin & Tooling Reserves")
                    }
                }
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
        val barHeight = 28.dp.toPx()
        val cornerRadius = 14.dp.toPx()
        val width = size.width
        val welfareWidth = width * welfareRatio

        // Background / container
        drawRoundRect(
            color = Color(0xFF825500),
            topLeft = Offset(0f, 20f),
            size = Size(width, barHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius)
        )

        // Welfare segment
        drawRoundRect(
            color = Color(0xFF006A58),
            topLeft = Offset(0f, 20f),
            size = Size(welfareWidth, barHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius)
        )
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
