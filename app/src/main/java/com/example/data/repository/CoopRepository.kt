package com.example.data.repository

import com.example.data.engine.FairDispatchEngine
import com.example.data.engine.LedgerEngine
import com.example.data.engine.WageEngine
import com.example.data.model.Cooperative
import com.example.data.model.Customer
import com.example.data.model.Job
import com.example.data.model.JobStatus
import com.example.data.model.LedgerEntry
import com.example.data.model.Role
import com.example.data.model.ServiceRequirement
import com.example.data.model.Worker
import com.example.data.preferences.UserPreferences
import com.example.data.preferences.UserProfile
import com.example.data.seed.SeedData
import com.example.util.AppLanguage
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

object CoopRepository {

    private val _currentRole = MutableStateFlow(Role.CUSTOMER)
    val currentRole: StateFlow<Role> = _currentRole.asStateFlow()

    private val _appLanguage = MutableStateFlow(AppLanguage.MARATHI)
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    private val _hasSelectedRole = MutableStateFlow(false)
    val hasSelectedRole: StateFlow<Boolean> = _hasSelectedRole.asStateFlow()

    private val _activeUserProfile = MutableStateFlow<UserProfile?>(null)
    val activeUserProfile: StateFlow<UserProfile?> = _activeUserProfile.asStateFlow()

    private val _selectedWorkerId = MutableStateFlow("w_1") // Ramesh Kumar
    val selectedWorkerId: StateFlow<String> = _selectedWorkerId.asStateFlow()

    private val _selectedCustomerId = MutableStateFlow("cust_1") // Ananya Sharma
    val selectedCustomerId: StateFlow<String> = _selectedCustomerId.asStateFlow()

    private val _selectedCoopId = MutableStateFlow("coop_blr")
    val selectedCoopId: StateFlow<String> = _selectedCoopId.asStateFlow()

    private val _workers = MutableStateFlow<List<Worker>>(emptyList())
    val workers: StateFlow<List<Worker>> = _workers.asStateFlow()

    private val _customers = MutableStateFlow<List<Customer>>(emptyList())
    val customers: StateFlow<List<Customer>> = _customers.asStateFlow()

    private val _cooperatives = MutableStateFlow<List<Cooperative>>(emptyList())
    val cooperatives: StateFlow<List<Cooperative>> = _cooperatives.asStateFlow()

    private val _jobs = MutableStateFlow<List<Job>>(emptyList())
    val jobs: StateFlow<List<Job>> = _jobs.asStateFlow()

    private val _ledger = MutableStateFlow<List<LedgerEntry>>(emptyList())
    val ledger: StateFlow<List<LedgerEntry>> = _ledger.asStateFlow()

    init {
        resetToSeedData()
    }

    fun resetToSeedData() {
        val (initJobs, initLedger) = SeedData.createInitialJobsAndLedger()
        // Strictly synchronize worker total earnings and completed jobs with cryptographic ledger
        _workers.value = SeedData.workers.map { worker ->
            val workerLedger = initLedger.filter { it.workerId == worker.id }
            worker.copy(
                totalEarningsInPaise = workerLedger.sumOf { it.workerWageInPaise },
                completedJobs = workerLedger.size
            )
        }
        _customers.value = SeedData.customers
        _cooperatives.value = SeedData.cooperatives
        _jobs.value = initJobs
        _ledger.value = initLedger
        _currentRole.value = Role.CUSTOMER
        _hasSelectedRole.value = false
        _selectedWorkerId.value = "w_1"
        _selectedCustomerId.value = "cust_1"
        _selectedCoopId.value = "coop_blr"
    }

    fun setLanguage(language: AppLanguage) {
        _appLanguage.value = language
    }

    fun toggleLanguage() {
        _appLanguage.value = if (_appLanguage.value == AppLanguage.MARATHI) AppLanguage.ENGLISH else AppLanguage.MARATHI
    }

    fun initPreferences(context: Context) {
        UserPreferences.init(context)
        val profile = UserPreferences.getUserProfile()
        if (profile != null && profile.isLoggedIn) {
            applyUserProfile(profile)
        }
    }

    fun applyUserProfile(profile: UserProfile) {
        _activeUserProfile.value = profile
        _currentRole.value = profile.role
        _hasSelectedRole.value = true

        when (profile.role) {
            Role.CUSTOMER -> {
                val custId = if (profile.phone.isNotBlank()) "cust_" + profile.phone.takeLast(6) else "cust_" + profile.name.trim().replace(" ", "_").lowercase()
                val existing = _customers.value.firstOrNull { it.id == custId }
                val customer = existing?.copy(
                    name = profile.name,
                    phone = profile.phone,
                    location = profile.locality
                ) ?: Customer(
                    id = custId,
                    name = profile.name,
                    phone = profile.phone,
                    location = profile.locality
                )
                _customers.value = listOf(customer) + _customers.value.filter { it.id != custId }
                _selectedCustomerId.value = custId
            }
            Role.WORKER -> {
                val workerId = if (profile.phone.isNotBlank()) "worker_" + profile.phone.takeLast(6) else "worker_" + profile.name.trim().replace(" ", "_").lowercase()
                val existing = _workers.value.firstOrNull { it.id == workerId }
                val workerSkills = profile.skills.ifEmpty { listOf("Electrician") }
                val worker = existing?.copy(
                    name = profile.name,
                    phone = profile.phone,
                    skills = workerSkills,
                    experienceYears = profile.experienceYears
                ) ?: Worker(
                    id = workerId,
                    name = profile.name,
                    skills = workerSkills,
                    rating = 4.9,
                    completedJobs = 6,
                    totalEarningsInPaise = 180000L,
                    verified = true,
                    cooperativeId = "coop_1",
                    phone = profile.phone,
                    experienceYears = profile.experienceYears
                )
                _workers.value = listOf(worker) + _workers.value.filter { it.id != workerId }
                _selectedWorkerId.value = workerId
            }
        }
    }

    fun loginUser(
        name: String,
        phone: String,
        locality: String = "Indiranagar, Bangalore",
        role: Role,
        skills: List<String> = listOf("Electrician"),
        workerSkill: String = "",
        areaLocality: String = "Indiranagar",
        cityDistrict: String = "Bangalore",
        landmark: String = "",
        experienceYears: Int = 4,
        customerServiceInterests: List<String> = listOf("Floor Cleaning")
    ) {
        val effectiveSkills = if (workerSkill.isNotBlank()) listOf(workerSkill) + skills.filter { it != workerSkill } else skills
        UserPreferences.saveLogin(
            name = name,
            phone = phone,
            areaLocality = areaLocality,
            cityDistrict = cityDistrict,
            landmark = landmark,
            role = role,
            skills = effectiveSkills,
            experienceYears = experienceYears,
            customerServiceInterests = customerServiceInterests
        )
        val profile = UserPreferences.getUserProfile()
        if (profile != null) {
            applyUserProfile(profile)
        }
    }

    fun updateWorkerSkill(newSkill: String) {
        val currentWorker = getActiveWorker()
        val updatedSkills = (listOf(newSkill) + currentWorker.skills).distinct()
        _workers.value = _workers.value.map {
            if (it.id == currentWorker.id) it.copy(skills = updatedSkills) else it
        }
    }

    fun addExternalJob(job: Job) {
        _jobs.update { list ->
            if (list.none { it.id == job.id }) listOf(job) + list else list
        }
    }

    fun setAuthoritativeJobs(newJobs: List<Job>) {
        _jobs.value = newJobs
    }

    fun logoutUser() {
        UserPreferences.clearLogin()
        _activeUserProfile.value = null
        _hasSelectedRole.value = false
    }

    fun selectInitialPersona(role: Role) {
        _currentRole.value = role
        _hasSelectedRole.value = true
        // Default login profile if not set
        if (_activeUserProfile.value == null) {
            val defaultName = when (role) {
                Role.CUSTOMER -> "Ramesh Patil"
                Role.WORKER -> "Sunil Kumar"
            }
            val defaultSkills = when (role) {
                Role.CUSTOMER -> listOf("Floor Cleaning")
                Role.WORKER -> listOf("Electrician", "Gardener")
            }
            loginUser(
                name = defaultName,
                phone = "9845012345",
                locality = "Indiranagar, Bangalore",
                role = role,
                skills = defaultSkills
            )
        }
    }

    fun clearSelectedPersona() {
        _hasSelectedRole.value = false
    }

    fun setRole(role: Role) {
        _currentRole.value = role
        _hasSelectedRole.value = true
    }

    fun selectWorker(workerId: String) {
        _selectedWorkerId.value = workerId
    }

    fun selectCustomer(customerId: String) {
        _selectedCustomerId.value = customerId
    }

    fun selectCooperative(coopId: String) {
        _selectedCoopId.value = coopId
    }

    fun getActiveCooperative(): Cooperative {
        return _cooperatives.value.firstOrNull { it.id == _selectedCoopId.value }
            ?: _cooperatives.value.first()
    }

    fun getActiveWorker(): Worker {
        return _workers.value.firstOrNull { it.id == _selectedWorkerId.value }
            ?: _workers.value.first()
    }

    fun getActiveCustomer(): Customer {
        return _customers.value.firstOrNull { it.id == _selectedCustomerId.value }
            ?: _customers.value.first()
    }

    /**
     * Customer Booking:
     * - Enforces minimum wage floor
     * - Creates PENDING job with escrowAmountInPaise = priceInPaise
     */
    fun bookJob(
        skill: String,
        location: String,
        dateTime: String,
        durationMinutes: Int,
        priceInPaise: Long,
        title: String = "$skill Service",
        description: String = "",
        date: String = "Today",
        preferredTime: String = "2:00 PM",
        instructions: String = ""
    ): Result<Job> {
        val validation = WageEngine.validatePrice(skill, durationMinutes, priceInPaise)
        if (!validation.isValid) {
            return Result.failure(IllegalArgumentException(validation.reasonMessage))
        }

        val formattedDateTime = if (dateTime.isNotBlank()) dateTime else "$date, $preferredTime"
        val newJob = Job(
            id = "job_" + UUID.randomUUID().toString().take(8),
            customerId = _selectedCustomerId.value,
            workerId = null,
            cooperativeId = _selectedCoopId.value,
            skill = skill,
            location = location,
            dateTime = formattedDateTime,
            durationMinutes = durationMinutes,
            priceInPaise = priceInPaise,
            escrowAmountInPaise = priceInPaise, // Full customer price held in local escrow
            status = JobStatus.PENDING,
            title = title,
            description = if (instructions.isNotBlank()) instructions else description,
            createdAtTimestamp = System.currentTimeMillis(),
            preferredTime = preferredTime,
            instructions = instructions
        )

        _jobs.update { listOf(newJob) + it }
        return Result.success(newJob)
    }

    /**
     * Customer Booking with Multi-Requirement Support:
     * e.g. 1 Electrician + 2 Gardeners
     */
    fun bookMultiRequirementJob(
        requirements: List<ServiceRequirement>,
        location: String,
        dateTime: String,
        durationMinutes: Int,
        priceInPaise: Long,
        title: String = "",
        instructions: String = ""
    ): Result<Job> {
        val cleanReqs = requirements.filter { it.quantity > 0 }
        val primarySkill = if (cleanReqs.isNotEmpty()) {
            cleanReqs.joinToString(", ") { "${it.quantity}x ${it.skill}" }
        } else {
            "General Service"
        }

        val newJob = Job(
            id = "req_" + UUID.randomUUID().toString().take(8),
            customerId = _selectedCustomerId.value,
            workerId = null,
            cooperativeId = _selectedCoopId.value,
            skill = primarySkill,
            location = location,
            dateTime = dateTime,
            durationMinutes = durationMinutes,
            priceInPaise = priceInPaise,
            escrowAmountInPaise = priceInPaise,
            status = JobStatus.PENDING,
            title = if (title.isNotBlank()) title else "$primarySkill Service",
            description = instructions,
            createdAtTimestamp = System.currentTimeMillis(),
            preferredTime = dateTime,
            instructions = instructions,
            requirements = cleanReqs
        )

        _jobs.update { listOf(newJob) + it.filter { j -> j.id != newJob.id } }
        return Result.success(newJob)
    }

    /**
     * Provider accepts a specific requirement in a job:
     * Checks if provider's skill matches the requirement,
     * provider hasn't already accepted, and slots remain open.
     */
    fun acceptJobRequirement(jobId: String, skill: String, workerId: String, workerName: String): Boolean {
        var updated = false
        _jobs.update { list ->
            list.map { job ->
                if (job.id == jobId) {
                    var reqMatched = false
                    val updatedReqs = job.requirements.map { req ->
                        val isMatchingSkill = req.skill.equals(skill, ignoreCase = true)
                        val notAlreadyAssigned = !req.assignedProviderIds.contains(workerId)
                        val hasOpenSlot = req.assignedProviderIds.size < req.quantity

                        if (isMatchingSkill && notAlreadyAssigned && hasOpenSlot) {
                            reqMatched = true
                            updated = true
                            req.copy(
                                assignedProviderIds = req.assignedProviderIds + workerId,
                                assignedProviderNames = req.assignedProviderNames + workerName
                            )
                        } else {
                            req
                        }
                    }

                    if (reqMatched) {
                        val allFilled = updatedReqs.all { it.assignedProviderIds.size >= it.quantity }
                        job.copy(
                            workerId = workerId,
                            requirements = updatedReqs,
                            status = if (allFilled) JobStatus.ACCEPTED else JobStatus.PENDING
                        )
                    } else if (job.requirements.isEmpty() && job.status == JobStatus.PENDING) {
                        // Fallback for legacy single-skill job
                        updated = true
                        job.copy(
                            workerId = workerId,
                            status = JobStatus.ACCEPTED
                        )
                    } else {
                        job
                    }
                } else {
                    job
                }
            }
        }
        return updated
    }

    /**
     * Worker accepts job:
     * PENDING -> ACCEPTED
     */
    fun acceptJob(jobId: String, workerId: String): Boolean {
        var updated = false
        _jobs.update { list ->
            list.map { job ->
                if (job.id == jobId && job.status == JobStatus.PENDING) {
                    updated = true
                    job.copy(
                        workerId = workerId,
                        status = JobStatus.ACCEPTED
                    )
                } else {
                    job
                }
            }
        }
        return updated
    }

    /**
     * Worker submits proof:
     * ACCEPTED -> IN_PROGRESS
     * CRITICAL: Escrow remains held! Does NOT release payout.
     */
    fun submitProof(
        jobId: String,
        photoUri: String?,
        latitude: Double?,
        longitude: Double?,
        timestamp: Long,
        notes: String?
    ): Boolean {
        var updated = false
        _jobs.update { list ->
            list.map { job ->
                if (job.id == jobId && (job.status == JobStatus.ACCEPTED || job.status == JobStatus.PENDING)) {
                    updated = true
                    job.copy(
                        status = JobStatus.IN_PROGRESS,
                        proofPhotoUri = photoUri,
                        proofLatitude = latitude,
                        proofLongitude = longitude,
                        proofTimestamp = timestamp,
                        proofNotes = notes
                        // escrowAmountInPaise remains strictly held!
                    )
                } else {
                    job
                }
            }
        }
        return updated
    }

    /**
     * Customer or Worker flags a dispute:
     * IN_PROGRESS -> DISPUTED
     */
    fun disputeJob(jobId: String, comment: String): Boolean {
        var updated = false
        _jobs.update { list ->
            list.map { job ->
                if (job.id == jobId && (job.status == JobStatus.IN_PROGRESS || job.status == JobStatus.ACCEPTED)) {
                    updated = true
                    job.copy(
                        status = JobStatus.DISPUTED,
                        disputeComment = comment
                    )
                } else {
                    job
                }
            }
        }
        return updated
    }

    /**
     * Release Job Payout:
     * Releases held escrow, credits worker ledger with SHA-256 block,
     * updates cooperative welfare fund, and sets status to COMPLETED.
     */
    fun releaseJobPayout(jobId: String): Boolean {
        val job = _jobs.value.firstOrNull { it.id == jobId } ?: return false
        if (job.status != JobStatus.IN_PROGRESS && job.status != JobStatus.DISPUTED && job.status != JobStatus.ACCEPTED) {
            return false
        }
        val workerId = job.workerId ?: return false

        // Guard against duplicate ledger entries
        val alreadyHasLedger = _ledger.value.any { it.jobId == jobId }
        if (alreadyHasLedger) {
            // Just ensure job status is completed
            _jobs.update { list ->
                list.map { if (it.id == jobId) it.copy(status = JobStatus.COMPLETED, escrowAmountInPaise = 0L) else it }
            }
            return true
        }

        val coop = _cooperatives.value.firstOrNull { it.id == job.cooperativeId } ?: getActiveCooperative()
        val payout = WageEngine.calculatePayoutSplit(job.priceInPaise, coop.adminFeePercent)

        // Find worker's previous ledger entry
        val workerLedger = _ledger.value.filter { it.workerId == workerId }.sortedBy { it.timestamp }
        val prevHash = workerLedger.lastOrNull()?.currentHash ?: LedgerEngine.GENESIS_HASH
        val timestamp = System.currentTimeMillis()

        val newLedgerEntry = LedgerEngine.createEntry(
            id = "led_${UUID.randomUUID().toString().take(8)}",
            workerId = workerId,
            jobId = jobId,
            workerWageInPaise = payout.workerWagePaise,
            adminFeeInPaise = payout.adminFeePaise,
            welfareInPaise = payout.welfarePaise,
            timestamp = timestamp,
            previousHash = prevHash
        )

        // Update ledger
        _ledger.update { it + newLedgerEntry }

        // Update worker earnings and job count
        _workers.update { list ->
            list.map { worker ->
                if (worker.id == workerId) {
                    worker.copy(
                        totalEarningsInPaise = worker.totalEarningsInPaise + payout.workerWagePaise,
                        completedJobs = worker.completedJobs + 1
                    )
                } else {
                    worker
                }
            }
        }

        // Update cooperative welfare fund
        _cooperatives.update { list ->
            list.map { c ->
                if (c.id == coop.id) {
                    c.copy(welfareFundInPaise = c.welfareFundInPaise + payout.welfarePaise)
                } else {
                    c
                }
            }
        }

        // Update job status and clear escrow
        _jobs.update { list ->
            list.map {
                if (it.id == jobId) {
                    it.copy(
                        status = JobStatus.COMPLETED,
                        escrowAmountInPaise = 0L
                    )
                } else {
                    it
                }
            }
        }

        return true
    }

    fun adminReleaseJob(jobId: String): Boolean = releaseJobPayout(jobId)

    /**
     * Admin Refunds Customer:
     * - Escrow cleared
     * - Status changed to REFUNDED
     * - NO worker payout, NO ledger entry
     */
    fun adminRefundJob(jobId: String, reason: String): Boolean {
        var updated = false
        _jobs.update { list ->
            list.map { job ->
                if (job.id == jobId && (job.status == JobStatus.DISPUTED || job.status == JobStatus.IN_PROGRESS || job.status == JobStatus.PENDING || job.status == JobStatus.ACCEPTED)) {
                    updated = true
                    job.copy(
                        status = JobStatus.REFUNDED,
                        escrowAmountInPaise = 0L,
                        disputeComment = reason
                    )
                } else {
                    job
                }
            }
        }
        return updated
    }

    /**
     * Member Management: verify or unverify worker
     */
    fun toggleWorkerVerification(workerId: String) {
        _workers.update { list ->
            list.map { if (it.id == workerId) it.copy(verified = !it.verified) else it }
        }
    }

    /**
     * Cooperative Governance: update admin fee percentage
     */
    fun updateAdminFeePercent(coopId: String, newPercent: Int) {
        val clamped = newPercent.coerceIn(0, 30)
        _cooperatives.update { list ->
            list.map { if (it.id == coopId) it.copy(adminFeePercent = clamped) else it }
        }
    }

    /**
     * Cooperative Governance: Democratic Surplus & Patronage Dividend Distribution.
     * Distributes surplus from the cooperative welfare fund equally among all verified members,
     * maintaining hash-chained ledger consistency and updating member earnings.
     */
    fun distributeSurplus(coopId: String, distributionAmountPaise: Long): Result<Long> {
        val coop = _cooperatives.value.firstOrNull { it.id == coopId }
            ?: return Result.failure(IllegalArgumentException("Cooperative not found"))

        if (distributionAmountPaise <= 0L) {
            return Result.failure(IllegalArgumentException("Distribution amount must be greater than zero"))
        }

        if (distributionAmountPaise > coop.welfareFundInPaise) {
            return Result.failure(IllegalArgumentException("Insufficient welfare fund balance for distribution"))
        }

        val verifiedWorkers = _workers.value.filter { it.cooperativeId == coopId && it.verified }
        if (verifiedWorkers.isEmpty()) {
            return Result.failure(IllegalStateException("No verified members available to receive dividend"))
        }

        val dividendPerMemberPaise = distributionAmountPaise / verifiedWorkers.size
        val actualTotalDistributed = dividendPerMemberPaise * verifiedWorkers.size
        val timestamp = System.currentTimeMillis()

        // 1. Deduct distributed surplus from cooperative welfare fund
        _cooperatives.update { list ->
            list.map {
                if (it.id == coopId) {
                    it.copy(welfareFundInPaise = it.welfareFundInPaise - actualTotalDistributed)
                } else it
            }
        }

        // 2. Add chained ledger dividend entry for each verified worker
        val newLedgerEntries = mutableListOf<LedgerEntry>()
        val currentLedger = _ledger.value

        for (worker in verifiedWorkers) {
            val workerLedger = currentLedger.filter { it.workerId == worker.id }.sortedBy { it.timestamp }
            val prevHash = workerLedger.lastOrNull()?.currentHash ?: LedgerEngine.GENESIS_HASH
            val entry = LedgerEngine.createEntry(
                id = "div_${UUID.randomUUID().toString().take(8)}",
                workerId = worker.id,
                jobId = "SURPLUS_DIVIDEND",
                workerWageInPaise = dividendPerMemberPaise,
                adminFeeInPaise = 0L,
                welfareInPaise = 0L,
                timestamp = timestamp,
                previousHash = prevHash
            )
            newLedgerEntries.add(entry)
        }
        _ledger.update { it + newLedgerEntries }

        // 3. Update worker earnings
        _workers.update { list ->
            list.map { worker ->
                if (worker.cooperativeId == coopId && worker.verified) {
                    worker.copy(totalEarningsInPaise = worker.totalEarningsInPaise + dividendPerMemberPaise)
                } else worker
            }
        }

        return Result.success(actualTotalDistributed)
    }

    /**
     * Get ledger entries for a specific worker
     */
    fun getLedgerForWorker(workerId: String): List<LedgerEntry> {
        return _ledger.value.filter { it.workerId == workerId }.sortedBy { it.timestamp }
    }

    /**
     * Run Fair Dispatch comparison on current pending jobs and verified workers
     */
    fun runFairDispatchComparison(): Pair<FairDispatchEngine.AlgorithmResult, FairDispatchEngine.AlgorithmResult> {
        val pending = _jobs.value.filter { it.status == JobStatus.PENDING }
        val currentWorkers = _workers.value
        val coop = getActiveCooperative()

        val ratingGreedyResult = FairDispatchEngine.runRatingGreedy(
            pendingJobs = pending,
            workers = currentWorkers,
            adminFeePercent = coop.adminFeePercent
        )

        val equitableResult = FairDispatchEngine.runEquitable(
            pendingJobs = pending,
            workers = currentWorkers,
            adminFeePercent = coop.adminFeePercent
        )

        return ratingGreedyResult to equitableResult
    }
}
