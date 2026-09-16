package com.example.data.model

enum class Role {
    CUSTOMER,
    WORKER,
    COOPERATIVE_ADMIN
}

enum class JobStatus {
    PENDING,
    ACCEPTED,
    IN_PROGRESS,
    DISPUTED,
    COMPLETED,
    REFUNDED
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
)

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
    val instructions: String = ""
)

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
