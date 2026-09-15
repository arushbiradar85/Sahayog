package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Carpenter
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Plumbing
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Yard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.engine.WageEngine
import com.example.data.model.Customer
import com.example.data.model.Job
import com.example.data.model.JobStatus
import com.example.data.model.ServiceCategory
import com.example.data.repository.CoopRepository
import com.example.ui.components.StatusBadge
import com.example.util.AppLanguage
import com.example.util.Localization

@Composable
fun CustomerScreen(
    repository: CoopRepository,
    modifier: Modifier = Modifier
) {
    val language by repository.appLanguage.collectAsState()
    val isMarathi = Localization.isMarathi(language)

    val customers by repository.customers.collectAsState()
    val selectedCustomerId by repository.selectedCustomerId.collectAsState()
    val jobs by repository.jobs.collectAsState()
    val activeCustomer = repository.getActiveCustomer()
    val coop = repository.getActiveCooperative()

    var activeTab by remember { mutableIntStateOf(0) }
    var bookingCategory by remember { mutableStateOf<ServiceCategory?>(null) }
    var showCustomRequestDialog by remember { mutableStateOf(false) }
    var disputeJobTarget by remember { mutableStateOf<Job?>(null) }

    val customerJobs = jobs.filter { it.customerId == selectedCustomerId }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("customer_screen")
    ) {
        // Customer Header Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (isMarathi) "ग्राहक म्हणून सेवा बुक करत आहात" else "Booking as Customer",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Text(
                        text = activeCustomer.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = activeCustomer.location,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = if (isMarathi) "${customerJobs.size} बुकिंग्ज" else "${customerJobs.size} Bookings",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Sub-tabs
        TabRow(
            selectedTabIndex = activeTab,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Tab(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                text = { Text(if (isMarathi) "सेवा निवडा (Book)" else "Book Service", fontWeight = FontWeight.SemiBold) }
            )
            Tab(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                text = { Text(if (isMarathi) "माझ्या बुकिंग्ज (${customerJobs.size})" else "My Bookings (${customerJobs.size})", fontWeight = FontWeight.SemiBold) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (activeTab == 0) {
            // Service Category Grid
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // Community Trust & Service Banner (Minimal, Normalized, Service-Oriented)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
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
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Sahayog Shield",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isMarathi) "सहयोग • कामगार सहकारी संस्था" else "Sahayog Workers Cooperative",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (isMarathi) "सत्यापित स्थानिक कुशल कारागीर व पारदर्शक हमीभाव" else "Verified local artisans & guaranteed wage floors",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 4-Step Pictorial Flow for elderly and low-literacy users
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PictorialStepItem(icon = "👆", title = Localization.step1Title(language), subtitle = Localization.step1Sub(language))
                        Text("➔", color = MaterialTheme.colorScheme.outline, fontWeight = FontWeight.Bold)
                        PictorialStepItem(icon = "🛡️", title = Localization.step2Title(language), subtitle = Localization.step2Sub(language))
                        Text("➔", color = MaterialTheme.colorScheme.outline, fontWeight = FontWeight.Bold)
                        PictorialStepItem(icon = "📸", title = Localization.step3Title(language), subtitle = Localization.step3Sub(language))
                        Text("➔", color = MaterialTheme.colorScheme.outline, fontWeight = FontWeight.Bold)
                        PictorialStepItem(icon = "💰", title = Localization.step4Title(language), subtitle = Localization.step4Sub(language))
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isMarathi) "प्रमाणित सेवा • कारागीर निवडा" else "Verified Services • Book Artisan",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isMarathi) "किमान हमी वेतन आणि सुरक्षित एस्क्रो संरक्षण" else "Fair guaranteed wage floors with transparent escrow protection.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(
                        onClick = { showCustomRequestDialog = true },
                        modifier = Modifier.testTag("custom_request_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isMarathi) "इतर काम" else "Custom")
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(WageEngine.SERVICE_CATEGORIES) { category ->
                        ServiceCategoryCard(
                            category = category,
                            language = language,
                            onClick = { bookingCategory = category }
                        )
                    }
                }
            }
        } else {
            // Customer Bookings History
            if (customerJobs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No bookings yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.outline
                        )
                        TextButton(onClick = { activeTab = 0 }) {
                            Text("Book your first service")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
                ) {
                    items(customerJobs) { job ->
                        CustomerJobCard(
                            job = job,
                            onRaiseDispute = { disputeJobTarget = job }
                        )
                    }
                }
            }
        }
    }

    // Booking Dialog Sheet
    bookingCategory?.let { category ->
        BookingModalDialog(
            category = category,
            activeCustomer = activeCustomer,
            adminFeePercent = coop.adminFeePercent,
            onDismiss = { bookingCategory = null },
            onConfirmBooking = { loc, dt, durMinutes, pricePaise, desc ->
                val result = repository.bookJob(
                    skill = category.name,
                    location = loc,
                    dateTime = dt,
                    durationMinutes = durMinutes,
                    priceInPaise = pricePaise,
                    title = "${category.name} Service",
                    description = desc
                )
                bookingCategory = null
                if (result.isSuccess) {
                    activeTab = 1 // switch to bookings view
                }
            }
        )
    }

    // Custom Request Dialog
    if (showCustomRequestDialog) {
        CustomRequestDialog(
            activeCustomer = activeCustomer,
            onDismiss = { showCustomRequestDialog = false },
            onConfirm = { skill, loc, durMinutes, pricePaise, title, desc ->
                repository.bookJob(
                    skill = skill,
                    location = loc,
                    dateTime = "Within 24 Hours",
                    durationMinutes = durMinutes,
                    priceInPaise = pricePaise,
                    title = title,
                    description = desc
                )
                showCustomRequestDialog = false
                activeTab = 1
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
                disputeJobTarget = null
            }
        )
    }
}

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
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header Row: Service Icon and Audio Speaker Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = localizedName,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                }

                IconButton(
                    onClick = {
                        val audioText = if (isMarathi) {
                            "🔊 $localizedName: किमान हमीभाव ₹${category.hourlyWagePaise / 100} प्रति तास"
                        } else {
                            "🔊 ${category.name}: Guaranteed floor rate ₹${category.hourlyWagePaise / 100} per hour"
                        }
                        Toast.makeText(context, audioText, Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Listen price",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = localizedName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Text(
                text = if (isMarathi) category.name else Localization.serviceDescription(category.name, category.description, language),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF059669).copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Color(0xFF059669).copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "🪙 ${WageEngine.formatPaiseCompact(category.hourlyWagePaise)}/hr",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF059669),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Text(
                    text = if (isMarathi) "किमान हमी" else "Floor",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Clean service action button
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isMarathi) "बुक करा ➔" else "BOOK ➔",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun PictorialStepItem(icon: String, title: String, subtitle: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = icon, fontSize = 20.sp)
        Text(text = title, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(text = subtitle, fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
    }
}

private fun getServiceImageRes(name: String): Int? {
    return when (name.lowercase()) {
        "electrician" -> R.drawable.img_service_electrician
        "plumber" -> R.drawable.img_service_plumber
        "carpenter" -> R.drawable.img_service_carpenter
        "cleaner" -> R.drawable.img_service_cleaning
        else -> null
    }
}

private fun getServiceHindiName(name: String): String {
    return when (name.lowercase()) {
        "electrician" -> "बिजली मिस्त्री"
        "plumber" -> "नलसाज • प्लंबर"
        "carpenter" -> "बढ़ई • फर्नीचर"
        "painter" -> "रंग रोगन • पेंटर"
        "cleaner" -> "सफाई कर्मी"
        "driver" -> "गाड़ी चालक"
        "gardener" -> "माली • बगीचा"
        "caregiver" -> "देखभाल सहायक"
        "technician" -> "मैकेनिक • रिपेयर"
        else -> "कुशल सेवा"
    }
}

@Composable
private fun CustomerJobCard(
    job: Job,
    onRaiseDispute: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("customer_job_card_${job.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
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
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Job #${job.id} • ${job.durationMinutes / 60.0} hrs",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                StatusBadge(status = job.status)
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Location",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = job.location,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Booking Price",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = WageEngine.formatPaiseCompact(job.priceInPaise),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Escrow status banner
            Spacer(modifier = Modifier.height(10.dp))
            if (job.status == JobStatus.PENDING || job.status == JobStatus.ACCEPTED || job.status == JobStatus.IN_PROGRESS) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Payment held in escrow: ${WageEngine.formatPaiseCompact(job.escrowAmountInPaise)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            } else if (job.status == JobStatus.COMPLETED) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Escrow released to worker ledger & welfare fund",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            } else if (job.status == JobStatus.REFUNDED) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReportProblem,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Escrow refunded to customer • Reason: ${job.disputeComment ?: "Cancelled"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // Proof or Dispute info if available
            if (job.status == JobStatus.IN_PROGRESS && job.proofNotes != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Worker Proof: \"${job.proofNotes}\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            if (job.status == JobStatus.IN_PROGRESS) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onRaiseDispute,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.ReportProblem, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Raise Dispute")
                    }
                }
            }
        }
    }
}

@Composable
private fun BookingModalDialog(
    category: ServiceCategory,
    activeCustomer: Customer,
    adminFeePercent: Int,
    onDismiss: () -> Unit,
    onConfirmBooking: (location: String, dateTime: String, durationMinutes: Int, pricePaise: Long, description: String) -> Unit
) {
    val context = LocalContext.current
    var location by remember { mutableStateOf(activeCustomer.location) }
    var dateTime by remember { mutableStateOf("Tomorrow, 10:00 AM") }
    var durationHours by remember { mutableFloatStateOf(2.0f) }
    var customPriceInput by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val durationMinutes = (durationHours * 60).toInt()
    val minFloorPaise = WageEngine.calculateMinimumWageFloorInPaise(category.name, durationMinutes)
    val suggestedPricePaise = WageEngine.getSuggestedPriceInPaise(category.name, durationMinutes)

    // Current price being offered: if user typed custom price, use it; else suggested
    val offeredPricePaise = customPriceInput.toLongOrNull()?.times(100L) ?: suggestedPricePaise

    val validation = WageEngine.validatePrice(category.name, durationMinutes, offeredPricePaise)
    val payout = WageEngine.calculatePayoutSplit(offeredPricePaise, adminFeePercent)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = getCategoryIcon(category.name),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Book ${category.name}", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            val imgRes = getServiceImageRes(category.name)
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (imgRes != null) {
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                        ) {
                            Image(
                                painter = painterResource(id = imgRes),
                                contentDescription = category.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Service Location • पता") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = dateTime,
                        onValueChange = { dateTime = it },
                        label = { Text("Date & Preferred Time • समय") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Estimated Duration • समय चुनें", style = MaterialTheme.typography.labelMedium)
                            Text(
                                text = "${String.format("%.1f", durationHours)} hours",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Quick duration preset chips for easy 1-tap selection by elders
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(
                                1.0f to "1 hr",
                                2.0f to "2 hrs",
                                4.0f to "4 hrs",
                                8.0f to "Full Day (8h)"
                            ).forEach { (hrs, label) ->
                                FilterChip(
                                    selected = durationHours == hrs,
                                    onClick = { durationHours = hrs },
                                    label = {
                                        Text(
                                            label,
                                            fontSize = 11.sp,
                                            fontWeight = if (durationHours == hrs) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                )
                            }
                        }

                        Slider(
                            value = durationHours,
                            onValueChange = { durationHours = it },
                            valueRange = 1f..12f,
                            steps = 21,
                            modifier = Modifier.testTag("duration_slider")
                        )
                        if (durationHours > 8.0f) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "⚡ Overtime Protection: ${String.format("%.1f", durationHours - 8.0f)}h paid at 1.5x overtime wage floor.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(6.dp),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = customPriceInput,
                        onValueChange = { customPriceInput = it },
                        label = { Text("Customer Offer Price (₹)") },
                        placeholder = { Text(WageEngine.formatPaiseCompact(suggestedPricePaise).replace("₹", "")) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("booking_price_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }

                // Wage Floor Protection Notice
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (validation.isValid) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (validation.isValid) Icons.Default.Shield else Icons.Default.Error,
                                    contentDescription = null,
                                    tint = if (validation.isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Minimum Wage Floor: ${WageEngine.formatPaiseCompact(minFloorPaise)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (validation.isValid) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            if (!validation.isValid) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = validation.reasonMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }

                // Transparent Payout Split Preview
                if (validation.isValid) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "Transparent Escrow Split Preview",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Worker Wage (90%)", style = MaterialTheme.typography.bodySmall)
                                    Text(WageEngine.formatPaiseCompact(payout.workerWagePaise), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Cooperative Admin Fee ($adminFeePercent%)", style = MaterialTheme.typography.bodySmall)
                                    Text(WageEngine.formatPaiseCompact(payout.adminFeePaise), style = MaterialTheme.typography.bodySmall)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("└ Welfare Fund (50% of fee)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                    Text(WageEngine.formatPaiseCompact(payout.welfarePaise), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Total Escrow Held", fontWeight = FontWeight.Bold)
                                    Text(WageEngine.formatPaiseCompact(payout.totalPaise), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Specific instructions (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (validation.isValid) {
                        onConfirmBooking(location, dateTime, durationMinutes, offeredPricePaise, notes)
                        Toast.makeText(context, "Payment of ${WageEngine.formatPaiseCompact(offeredPricePaise)} held in escrow", Toast.LENGTH_LONG).show()
                    }
                },
                enabled = validation.isValid,
                modifier = Modifier.testTag("confirm_booking_button")
            ) {
                Text("Confirm & Hold Escrow")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun CustomRequestDialog(
    activeCustomer: Customer,
    onDismiss: () -> Unit,
    onConfirm: (skill: String, location: String, durationMinutes: Int, pricePaise: Long, title: String, description: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedSkill by remember { mutableStateOf("Electrician") }
    var location by remember { mutableStateOf(activeCustomer.location) }
    var durationHours by remember { mutableFloatStateOf(2.0f) }
    var priceInput by remember { mutableStateOf("600") }
    var description by remember { mutableStateOf("") }

    val durationMinutes = (durationHours * 60).toInt()
    val offeredPricePaise = priceInput.toLongOrNull()?.times(100L) ?: 0L
    val validation = WageEngine.validatePrice(selectedSkill, durationMinutes, offeredPricePaise)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Post Custom Job Request", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Job Title (e.g. Balcony Light Wiring)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = selectedSkill,
                    onValueChange = { selectedSkill = it },
                    label = { Text("Required Skill") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Duration: ${String.format("%.1f", durationHours)} hrs")
                    Slider(
                        value = durationHours,
                        onValueChange = { durationHours = it },
                        valueRange = 1f..10f,
                        modifier = Modifier.weight(1f).padding(start = 12.dp)
                    )
                }
                OutlinedTextField(
                    value = priceInput,
                    onValueChange = { priceInput = it },
                    label = { Text("Budget (₹)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                if (!validation.isValid) {
                    Text(
                        text = validation.reasonMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description & Requirements") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (validation.isValid && title.isNotBlank()) {
                        onConfirm(selectedSkill, location, durationMinutes, offeredPricePaise, title, description)
                    }
                },
                enabled = validation.isValid && title.isNotBlank(),
                modifier = Modifier.testTag("post_custom_request_button")
            ) {
                Text("Post Request")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun DisputeDialog(
    job: Job,
    onDismiss: () -> Unit,
    onSubmitDispute: (String) -> Unit
) {
    var comment by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Raise Job Dispute", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    text = "Please describe why the work completed for \"${job.title}\" does not match requirements. The escrow will remain locked until cooperative admin reviews.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(10.dp))
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
                Text("Submit Dispute")
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
        "cleaner" -> Icons.Default.CleaningServices
        "driver" -> Icons.Default.DirectionsCar
        "gardener" -> Icons.Default.Yard
        "caregiver" -> Icons.Default.Favorite
        "technician" -> Icons.Default.Build
        else -> Icons.Default.Build
    }
}
