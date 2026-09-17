package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SignalCellularConnectedNoInternet0Bar
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.repository.CoopRepository
import com.example.util.Localization
import androidx.compose.ui.unit.sp
import com.example.data.model.JobStatus
import com.example.data.model.Role
import com.example.ui.theme.StatusAccepted
import com.example.ui.theme.StatusCompleted
import com.example.ui.theme.StatusDisputed
import com.example.ui.theme.StatusInProgress
import com.example.ui.theme.StatusPending
import com.example.ui.theme.StatusRefunded

@Composable
fun RoleSwitcherBar(
    currentRole: Role,
    onRoleSelected: (Role) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .testTag("role_switcher_bar")
    ) {
        // Obvious visual title for elderly / low-literacy users
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CHOOSE YOUR ROLE • अपनी भूमिका चुनें",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 0.5.sp
            )
            // Visual indicator showing active mode in plain Hindi/English
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text = when (currentRole) {
                        Role.CUSTOMER -> "🟢 Customer Mode Active"
                        Role.WORKER -> "🟢 Provider Mode Active"
                    },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Large high-contrast visual cards for 2 roles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RoleCardItem(
                role = Role.CUSTOMER,
                englishTitle = "CUSTOMER",
                hindiTitle = "ग्राहक",
                actionSubtitle = "Book Help",
                icon = Icons.Default.Person,
                isSelected = currentRole == Role.CUSTOMER,
                activeColor = Color(0xFF0284C7), // Sky/Blue
                onClick = { onRoleSelected(Role.CUSTOMER) },
                modifier = Modifier.weight(1f)
            )
            RoleCardItem(
                role = Role.WORKER,
                englishTitle = "PROVIDER",
                hindiTitle = "कामगार",
                actionSubtitle = "Earn & Work",
                icon = Icons.Default.Engineering,
                isSelected = currentRole == Role.WORKER,
                activeColor = Color(0xFF059669), // Emerald Green
                onClick = { onRoleSelected(Role.WORKER) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun RoleCardItem(
    role: Role,
    englishTitle: String,
    hindiTitle: String,
    actionSubtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isSelected) activeColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
    val borderStroke = if (isSelected) BorderStroke(2.5.dp, activeColor) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(borderStroke, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("role_tab_${role.name.lowercase()}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Top active badge
            if (isSelected) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = activeColor,
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Text(
                        text = "ACTIVE ✓",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(15.dp))
            }

            // Big prominent pictorial icon
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) activeColor else MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = "$englishTitle role",
                    tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // English & Hindi Titles
            Text(
                text = englishTitle,
                color = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 0.3.sp
            )
            Text(
                text = hindiTitle,
                color = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp
            )

            // Short action hint for low-literacy users
            Text(
                text = actionSubtitle,
                color = MaterialTheme.colorScheme.outline,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun OfflineNoticeBanner(modifier: Modifier = Modifier) {
    val language by CoopRepository.appLanguage.collectAsState()
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.SignalCellularConnectedNoInternet0Bar,
                contentDescription = "Offline indicator",
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = Localization.offlineNotice(language),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun StatusBadge(status: JobStatus, modifier: Modifier = Modifier) {
    val language by CoopRepository.appLanguage.collectAsState()
    val (bgColor, textColor, label, icon) = when (status) {
        JobStatus.PENDING -> Quadruple(StatusPending.copy(alpha = 0.15f), StatusPending, Localization.statusPending(language), Icons.Default.HourglassTop)
        JobStatus.ACCEPTED -> Quadruple(StatusAccepted.copy(alpha = 0.15f), StatusAccepted, Localization.statusAccepted(language), Icons.Default.ThumbUp)
        JobStatus.IN_PROGRESS -> Quadruple(StatusInProgress.copy(alpha = 0.15f), StatusInProgress, Localization.statusInProgress(language), Icons.Default.PhotoCamera)
        JobStatus.COMPLETED -> Quadruple(StatusCompleted.copy(alpha = 0.15f), StatusCompleted, Localization.statusCompleted(language), Icons.Default.CheckCircle)
        JobStatus.DISPUTED -> Quadruple(StatusDisputed.copy(alpha = 0.15f), StatusDisputed, Localization.statusDisputed(language), Icons.Default.ReportProblem)
        JobStatus.REFUNDED -> Quadruple(StatusRefunded.copy(alpha = 0.15f), StatusRefunded, Localization.statusRefunded(language), Icons.Default.RestartAlt)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
