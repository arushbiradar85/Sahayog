package com.example.data.model

enum class Role {
    CUSTOMER,
    WORKER // Acts as Provider
}

enum class JobStatus {
    PENDING,
    ACCEPTED,
    IN_PROGRESS,
    DISPUTED,
    COMPLETED,
    REFUNDED
}

data class ServiceRequirement(
    val skill: String,
    val quantity: Int = 1,
    val assignedProviderIds: List<String> = emptyList(),
    val assignedProviderNames: List<String> = emptyList()
) {
    val isFilled: Boolean get() = assignedProviderIds.size >= quantity
    val remainingNeeded: Int get() = (quantity - assignedProviderIds.size).coerceAtLeast(0)
    fun openSlots(): Int = remainingNeeded
    fun isFullyAssigned(): Boolean = isFilled
    fun matchesSkill(targetSkill: String): Boolean = skill.equals(targetSkill, ignoreCase = true)
}

data class Worker(
    val id: String,
    val name: String,
    val skills: List<String>,
    val rating: Double,
    val completedJobs: Int,
    val totalEarningsInPaise: Long,
    val verified: Boolean,
    val cooperativeId: String,
    val phone: String = "+91 98765 43210",
    val experienceYears: Int = 4
) {
    val skill: String get() = skills.firstOrNull() ?: "Electrician"
}

data class Customer(
    val id: String,
    val name: String,
    val location: String,
    val phone: String = "+91 91234 56789"
)

data class Cooperative(
    val id: String,
    val name: String,
    val adminFeePercent: Int, // e.g. 10
    val welfareFundInPaise: Long,
    val totalMembers: Int = 20,
    val city: String = "Bangalore"
)

data class Job(
    val id: String,
    val customerId: String,
    val workerId: String?,
    val cooperativeId: String,
    val skill: String,
    val location: String,
    val dateTime: String,
    val durationMinutes: Int,
    val priceInPaise: Long,
    val escrowAmountInPaise: Long,
    val status: JobStatus,
    val proofPhotoUri: String? = null,
    val proofLatitude: Double? = null,
    val proofLongitude: Double? = null,
    val proofTimestamp: Long? = null,
    val proofNotes: String? = null,
    val disputeComment: String? = null,
    val title: String = "$skill Service",
    val description: String = "",
    val createdAtTimestamp: Long = System.currentTimeMillis(),
    val preferredTime: String = "",
    val instructions: String = "",
    val customerName: String = "Customer",
    val customerPhone: String = "+91 91234 56789",
    val requirements: List<ServiceRequirement> = listOf(ServiceRequirement(skill = skill, quantity = 1, assignedProviderIds = if (workerId != null) listOf(workerId) else emptyList()))
) {
    fun isFullyAssigned(): Boolean = requirements.isNotEmpty() && requirements.all { it.isFilled }
    val totalRequiredCount: Int get() = if (requirements.isNotEmpty()) requirements.sumOf { it.quantity } else 1
    val totalAssignedCount: Int get() = requirements.sumOf { it.assignedProviderIds.size }
    fun assignedWorkersCount(): Int = totalAssignedCount
    fun totalWorkersNeeded(): Int = totalRequiredCount
    fun hasAnyAssigned(): Boolean = totalAssignedCount > 0
    fun allAssignedProviderNames(): List<String> = requirements.flatMap { it.assignedProviderNames }
    fun isWorkerAssigned(targetWorkerId: String): Boolean = workerId == targetWorkerId || requirements.any { it.assignedProviderIds.contains(targetWorkerId) }
    fun matchesWorkerSkill(workerSkills: List<String>): Boolean {
        if (requirements.isNotEmpty()) {
            return requirements.any { req ->
                workerSkills.any { ws -> req.matchesSkill(ws) } && !req.isFilled
            }
        }
        return workerSkills.any { s -> skill.contains(s, ignoreCase = true) }
    }
}

data class LedgerEntry(
    val id: String,
    val workerId: String,
    val jobId: String,
    val workerWageInPaise: Long,
    val adminFeeInPaise: Long,
    val welfareInPaise: Long,
    val previousHash: String,
    val currentHash: String,
    val timestamp: Long
)

data class ServiceCategory(
    val name: String,
    val iconName: String,
    val hourlyWagePaise: Long,
    val description: String
)
