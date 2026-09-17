package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.engine.FairDispatchEngine
import com.example.data.engine.LedgerEngine
import com.example.data.engine.WageEngine
import com.example.data.model.JobStatus
import com.example.data.model.Role
import com.example.data.model.ServiceRequirement
import com.example.data.repository.CoopRepository
import com.example.util.TwoDeviceSyncManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Before
    fun setup() {
        CoopRepository.resetToSeedData()
    }

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Sahayog", appName)
    }

    @Test
    fun `wage engine validates minimum wage floor and overtime correctly`() {
        // Electrician hourly rate = 25000 paise (₹250/hr)
        // 2 hours = 50000 paise (₹500)
        val validCheck = WageEngine.validatePrice("Electrician", 120, 50000L)
        assertTrue(validCheck.isValid)

        // Below wage floor (₹400 for 2h of Electrician)
        val invalidCheck = WageEngine.validatePrice("Electrician", 120, 40000L)
        assertFalse(invalidCheck.isValid)

        // Overtime check: 10 hours of Electrician (8h standard + 2h at 1.5x)
        // 8 * 25000 + 2 * 37500 = 200000 + 75000 = 275000 paise
        val overtimeFloor = WageEngine.calculateMinimumWageFloorInPaise("Electrician", 600)
        assertEquals(275000L, overtimeFloor)
    }

    @Test
    fun `wage engine transparent payout breakdown maintains exact paise sums`() {
        val payout = WageEngine.calculatePayoutSplit(priceInPaise = 100000L, adminFeePercent = 10)
        assertEquals(90000L, payout.workerWagePaise)
        assertEquals(10000L, payout.adminFeePaise)
        assertEquals(5000L, payout.welfarePaise) // 50% of admin fee
        assertEquals(100000L, payout.workerWagePaise + payout.adminFeePaise)
    }

    @Test
    fun `ledger engine cryptographically chains hashes and detects tamper`() {
        val entry1 = LedgerEngine.createEntry(
            id = "entry_1",
            workerId = "w_1",
            jobId = "job_1",
            workerWageInPaise = 90000L,
            adminFeeInPaise = 10000L,
            welfareInPaise = 5000L,
            timestamp = 1700000000000L,
            previousHash = LedgerEngine.GENESIS_HASH
        )
        val verify1 = LedgerEngine.verifyEntry(entry1)
        assertTrue(verify1.isValid)

        val entry2 = LedgerEngine.createEntry(
            id = "entry_2",
            workerId = "w_1",
            jobId = "job_2",
            workerWageInPaise = 45000L,
            adminFeeInPaise = 5000L,
            welfareInPaise = 2500L,
            timestamp = 1700001000000L,
            previousHash = entry1.currentHash
        )
        val verify2 = LedgerEngine.verifyEntry(entry2)
        assertTrue(verify2.isValid)

        val chainResult = LedgerEngine.verifyChain(listOf(entry1, entry2))
        assertTrue(chainResult.isChainValid)
        assertEquals(2, chainResult.totalBlocks)

        // Tamper test
        val tamperedEntry = entry1.copy(workerWageInPaise = 999999L)
        val tamperedVerification = LedgerEngine.verifyEntry(tamperedEntry)
        assertFalse(tamperedVerification.isValid)
    }

    @Test
    fun `full lifecycle escrow held upon proof and released by admin`() {
        // 1. Customer books job
        val bookResult = CoopRepository.bookJob(
            skill = "Plumber",
            location = "Indiranagar, Bangalore",
            dateTime = "Today, 4:00 PM",
            durationMinutes = 60,
            priceInPaise = 30000L // ₹300
        )
        assertTrue(bookResult.isSuccess)
        val job = bookResult.getOrThrow()
        assertEquals(JobStatus.PENDING, job.status)
        assertEquals(30000L, job.escrowAmountInPaise)

        // 2. Worker accepts job
        val accepted = CoopRepository.acceptJob(job.id, "w_2")
        assertTrue(accepted)
        val acceptedJob = CoopRepository.jobs.value.first { it.id == job.id }
        assertEquals(JobStatus.ACCEPTED, acceptedJob.status)

        // 3. Worker submits proof: status becomes IN_PROGRESS, ESCROW REMAINS HELD!
        val proofSubmitted = CoopRepository.submitProof(
            jobId = job.id,
            photoUri = "content://sahayog/proof.jpg",
            latitude = 12.9716,
            longitude = 77.5946,
            timestamp = System.currentTimeMillis(),
            notes = "Replaced pipe valve"
        )
        assertTrue(proofSubmitted)
        val inProgressJob = CoopRepository.jobs.value.first { it.id == job.id }
        assertEquals(JobStatus.IN_PROGRESS, inProgressJob.status)
        assertEquals(30000L, inProgressJob.escrowAmountInPaise) // Strict requirement: Escrow held!

        // 4. Admin releases job: status becomes COMPLETED, ledger block created, escrow cleared to 0
        val initialLedgerCount = CoopRepository.ledger.value.size
        val released = CoopRepository.adminReleaseJob(job.id)
        assertTrue(released)

        val completedJob = CoopRepository.jobs.value.first { it.id == job.id }
        assertEquals(JobStatus.COMPLETED, completedJob.status)
        assertEquals(0L, completedJob.escrowAmountInPaise)
        assertEquals(initialLedgerCount + 1, CoopRepository.ledger.value.size)
    }

    @Test
    fun `user preferences persists profile and reflects in repository state flow`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        com.example.data.preferences.UserPreferences.init(context)
        CoopRepository.initPreferences(context)

        // Login as a worker
        CoopRepository.loginUser(
            name = "Ramesh Pawar",
            phone = "9822012345",
            locality = "Solapur Market",
            role = com.example.data.model.Role.WORKER,
            workerSkill = "Electrician"
        )

        val profile = com.example.data.preferences.UserPreferences.getUserProfile()
        assertNotNull(profile)
        assertEquals("Ramesh Pawar", profile?.name)
        assertEquals(com.example.data.model.Role.WORKER, profile?.role)
        assertEquals("Electrician", profile?.workerSkill)
        assertTrue(profile?.isLoggedIn == true)

        // Verify CoopRepository StateFlow reflects active profile and role
        assertEquals(com.example.data.model.Role.WORKER, CoopRepository.currentRole.value)
        assertTrue(CoopRepository.hasSelectedRole.value)
        assertEquals("Ramesh Pawar", CoopRepository.activeUserProfile.value?.name)

        // Verify active worker is created and registered
        val activeWorker = CoopRepository.getActiveWorker()
        assertEquals("Ramesh Pawar", activeWorker.name)
        assertTrue(activeWorker.skills.contains("Electrician"))

        // Logout
        CoopRepository.logoutUser()
        assertFalse(CoopRepository.hasSelectedRole.value)
        assertFalse(com.example.data.preferences.UserPreferences.isUserLoggedIn())
    }

    @Test
    fun `data store persists role choice and profile correctly`() = kotlinx.coroutines.test.runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        com.example.data.datastore.AppDataStore.saveUserProfile(
            context = context,
            name = "Suresh Shinde",
            phone = "9876543210",
            areaLocality = "Pune Camp",
            cityDistrict = "Pune",
            role = com.example.data.model.Role.CUSTOMER
        )

        val profile = com.example.data.datastore.AppDataStore.getUserProfile(context)
        assertNotNull(profile)
        assertEquals("Suresh Shinde", profile?.name)
        assertEquals(com.example.data.model.Role.CUSTOMER, profile?.role)
        assertTrue(profile?.locality?.contains("Pune Camp") == true)
        assertTrue(profile?.isLoggedIn == true)

        com.example.data.datastore.AppDataStore.clearUserProfile(context)
        val cleared = com.example.data.datastore.AppDataStore.getUserProfile(context)
        assertTrue(cleared == null || !cleared.isLoggedIn)
    }

    @Test
    fun `fair dispatch reduces inequality compared to rating greedy`() {
        val (greedy, equitable) = CoopRepository.runFairDispatchComparison()
        assertNotNull(greedy)
        assertNotNull(equitable)
        assertTrue(greedy.assignments.isNotEmpty())
        assertTrue(equitable.assignments.isNotEmpty())
        // Equitable dispatch lowers or matches Gini inequality compared to Greedy
        assertTrue(equitable.giniCoefficient <= greedy.giniCoefficient)
        // Comparable waiting-time metric
        assertTrue(equitable.averageWaitTimeMinutes <= greedy.averageWaitTimeMinutes)
    }

    @Test
    fun `requirement 1 and 2 - completed jobs create ledger entries and worker earnings remain 100 percent consistent`() {
        val workers = CoopRepository.workers.value
        val ledger = CoopRepository.ledger.value
        val jobs = CoopRepository.jobs.value

        // Check consistency for every worker in initial seed data
        for (worker in workers) {
            val workerEntries = ledger.filter { it.workerId == worker.id }
            val workerCompletedJobs = jobs.filter { it.workerId == worker.id && it.status == JobStatus.COMPLETED }

            assertEquals("Worker ${worker.id} earnings mismatch with ledger", workerEntries.sumOf { it.workerWageInPaise }, worker.totalEarningsInPaise)
            assertEquals("Worker ${worker.id} completed job count mismatch with ledger", workerEntries.size, worker.completedJobs)
            assertEquals("Worker ${worker.id} completed job count mismatch with completed jobs", workerCompletedJobs.size, worker.completedJobs)
        }

        // Complete a new job and verify consistency persists
        val bookResult = CoopRepository.bookJob("Technician", "Whitefield", "Now", 60, 40000L)
        val job = bookResult.getOrThrow()
        CoopRepository.acceptJob(job.id, "w_16")
        CoopRepository.submitProof(job.id, "content://proof.jpg", 12.9, 77.6, System.currentTimeMillis(), "Fixed")

        val initialWorker = CoopRepository.workers.value.first { it.id == "w_16" }
        val releaseSuccess = CoopRepository.adminReleaseJob(job.id)
        assertTrue(releaseSuccess)

        val updatedWorker = CoopRepository.workers.value.first { it.id == "w_16" }
        val updatedLedger = CoopRepository.ledger.value.filter { it.workerId == "w_16" }
        assertEquals(updatedLedger.sumOf { it.workerWageInPaise }, updatedWorker.totalEarningsInPaise)
        assertEquals(initialWorker.completedJobs + 1, updatedWorker.completedJobs)
        assertEquals(updatedLedger.size, updatedWorker.completedJobs)
    }

    @Test
    fun `requirement 3 - SHA-256 previous-hash current-hash chaining is cryptographically verified`() {
        val workers = CoopRepository.workers.value
        val ledger = CoopRepository.ledger.value

        for (worker in workers) {
            val workerLedger = ledger.filter { it.workerId == worker.id }
            if (workerLedger.isNotEmpty()) {
                val chainResult = LedgerEngine.verifyChain(workerLedger)
                assertTrue("Chain broken for worker ${worker.name}: ${chainResult.message}", chainResult.isChainValid)
            }
        }
    }

    @Test
    fun `requirement 4 - wage floor rejection blocks booking at engine and repository level`() {
        val initialJobCount = CoopRepository.jobs.value.size
        // Plumber rate = ₹220/hr (22000 paise). Offering ₹100 for 1 hr (10000 paise)
        val lowPriceResult = CoopRepository.bookJob("Plumber", "Indiranagar", "Today", 60, 10000L)
        assertFalse(lowPriceResult.isSuccess)
        assertEquals(initialJobCount, CoopRepository.jobs.value.size)

        // Valid price above floor succeeds
        val validResult = CoopRepository.bookJob("Plumber", "Indiranagar", "Today", 60, 25000L)
        assertTrue(validResult.isSuccess)
        assertEquals(initialJobCount + 1, CoopRepository.jobs.value.size)
    }

    @Test
    fun `requirement 5 - overtime calculation strictly enforces 1_5x beyond 8 hours`() {
        // Hourly rate for Carpenter = 24000 paise (₹240/hr)
        // 8 hours = 8 * 24000 = 192000 paise
        assertEquals(192000L, WageEngine.calculateMinimumWageFloorInPaise("Carpenter", 480))

        // 9 hours = 8h regular + 1h overtime @ 1.5x (36000 paise) = 228000 paise
        assertEquals(228000L, WageEngine.calculateMinimumWageFloorInPaise("Carpenter", 540))

        // 12 hours = 8h regular (192000) + 4h overtime (4 * 36000 = 144000) = 336000 paise
        assertEquals(336000L, WageEngine.calculateMinimumWageFloorInPaise("Carpenter", 720))
    }

    @Test
    fun `requirement 7 - completion proof records photo and GPS and timestamp`() {
        val bookResult = CoopRepository.bookJob("Painter", "BTM Layout", "Now", 120, 50000L)
        val job = bookResult.getOrThrow()
        CoopRepository.acceptJob(job.id, "w_4")

        val testTime = 1726500000000L
        val submitted = CoopRepository.submitProof(
            jobId = job.id,
            photoUri = "content://media/external/images/media/42",
            latitude = 12.9150,
            longitude = 77.6100,
            timestamp = testTime,
            notes = "Completed 2 coats of emulsion"
        )
        assertTrue(submitted)

        val inProgJob = CoopRepository.jobs.value.first { it.id == job.id }
        assertEquals("content://media/external/images/media/42", inProgJob.proofPhotoUri)
        assertEquals(12.9150, inProgJob.proofLatitude!!, 0.0001)
        assertEquals(77.6100, inProgJob.proofLongitude!!, 0.0001)
        assertEquals(testTime, inProgJob.proofTimestamp)
        assertEquals("Completed 2 coats of emulsion", inProgJob.proofNotes)
    }

    @Test
    fun `requirement 8 - disputes prevent automatic escrow release and appear in Admin`() {
        val bookResult = CoopRepository.bookJob("Electrician", "Koramangala", "Now", 60, 35000L)
        val job = bookResult.getOrThrow()
        CoopRepository.acceptJob(job.id, "w_1")

        // Dispute raised
        val disputed = CoopRepository.disputeJob(job.id, "Wiring left uninsulated")
        assertTrue(disputed)

        val disputedJob = CoopRepository.jobs.value.first { it.id == job.id }
        assertEquals(JobStatus.DISPUTED, disputedJob.status)
        assertEquals(35000L, disputedJob.escrowAmountInPaise) // Escrow remains firmly held
        assertEquals("Wiring left uninsulated", disputedJob.disputeComment)

        // Refund via admin clears escrow
        val refunded = CoopRepository.adminRefundJob(job.id, "Refunded after dispute review")
        assertTrue(refunded)
        val finalJob = CoopRepository.jobs.value.first { it.id == job.id }
        assertEquals(JobStatus.REFUNDED, finalJob.status)
        assertEquals(0L, finalJob.escrowAmountInPaise)
    }

    @Test
    fun `requirement 9 - governance fee vote and surplus distribution work correctly`() {
        val coop = CoopRepository.getActiveCooperative()
        val initialWelfare = coop.welfareFundInPaise

        // Update fee
        CoopRepository.updateAdminFeePercent(coop.id, 15)
        assertEquals(15, CoopRepository.getActiveCooperative().adminFeePercent)

        // Distribute surplus dividend: ₹10,000 (1000000 paise)
        val distResult = CoopRepository.distributeSurplus(coop.id, 1000000L)
        assertTrue(distResult.isSuccess)

        val updatedCoop = CoopRepository.getActiveCooperative()
        assertTrue(updatedCoop.welfareFundInPaise < initialWelfare)

        // Check that verified workers received dividends and their ledger chain remains valid
        val verifiedWorkers = CoopRepository.workers.value.filter { it.cooperativeId == coop.id && it.verified }
        for (worker in verifiedWorkers) {
            val workerLedger = CoopRepository.ledger.value.filter { it.workerId == worker.id }
            assertTrue(workerLedger.any { it.jobId == "SURPLUS_DIVIDEND" })
            val chainCheck = LedgerEngine.verifyChain(workerLedger)
            assertTrue(chainCheck.isChainValid)
            assertEquals(workerLedger.sumOf { it.workerWageInPaise }, worker.totalEarningsInPaise)
        }
    }

    @Test
    fun `requirement 10 - fair dispatch comparison evaluates same pending jobs with wait time metric`() {
        val (greedy, equitable) = CoopRepository.runFairDispatchComparison()

        // Evaluates same pending dataset
        assertEquals(greedy.assignments.size + greedy.unassignedJobs.size, equitable.assignments.size + equitable.unassignedJobs.size)
        // Gini coefficient
        assertTrue(equitable.giniCoefficient <= greedy.giniCoefficient)
        // Top 20% wealth concentration
        assertTrue(equitable.top20SharePercent <= greedy.top20SharePercent)
        // Comparable waiting-time metric
        assertTrue(equitable.averageWaitTimeMinutes <= greedy.averageWaitTimeMinutes)
    }

    @Test
    fun `multi-phone audit - provider persists multiple skills`() {
        // Step 5 multi-skill registration: provider registers with Electrician, Plumber, Painter
        val multiSkills = listOf("Electrician", "Plumber", "Painter")
        CoopRepository.loginUser(
            name = "Anil Multi-Tech",
            phone = "9876500001",
            role = Role.WORKER,
            skills = multiSkills
        )

        val profile = CoopRepository.activeUserProfile.value
        assertNotNull(profile)
        val currentWorker = CoopRepository.getActiveWorker()
        assertEquals(3, currentWorker.skills.size)
        assertTrue(currentWorker.skills.contains("Electrician"))
        assertTrue(currentWorker.skills.contains("Plumber"))
        assertTrue(currentWorker.skills.contains("Painter"))
    }

    @Test
    fun `multi-phone audit - multi-worker request splits, filters, and supports independent acceptance`() {
        // Customer creates request: Electrician x 1, Gardener x 2
        val reqs = listOf(
            ServiceRequirement(skill = "Electrician", quantity = 1),
            ServiceRequirement(skill = "Gardener", quantity = 2)
        )
        val bookResult = CoopRepository.bookMultiRequirementJob(
            requirements = reqs,
            location = "Indiranagar, Bangalore",
            dateTime = "Today, 3:00 PM",
            durationMinutes = 120,
            priceInPaise = 150000L,
            title = "Multi-worker job",
            instructions = "Require electrician and gardeners"
        )
        assertTrue(bookResult.isSuccess)
        val job = bookResult.getOrNull()!!
        assertEquals(3, job.totalWorkersNeeded())
        assertEquals(0, job.assignedWorkersCount())
        assertFalse(job.isFullyAssigned())
        assertEquals(JobStatus.PENDING, job.status)

        // Providers filter:
        // Electrician provider (w_1) matches Electrician slot
        val electricianSkills = listOf("Electrician")
        assertTrue(job.matchesWorkerSkill(electricianSkills))

        // Gardener provider matches Gardener slot
        val gardenerSkills = listOf("Gardener")
        assertTrue(job.matchesWorkerSkill(gardenerSkills))

        // Unrelated provider (e.g. Driver) does NOT match
        val driverSkills = listOf("Driver")
        assertFalse(job.matchesWorkerSkill(driverSkills))

        // Provider 1 (Electrician) accepts Electrician slot
        val accept1 = CoopRepository.acceptJobRequirement(
            jobId = job.id,
            skill = "Electrician",
            workerId = "w_elec_1",
            workerName = "Sunil Electrician"
        )
        assertTrue(accept1)

        val jobAfter1 = CoopRepository.jobs.value.first { it.id == job.id }
        assertEquals(1, jobAfter1.assignedWorkersCount())
        assertFalse(jobAfter1.isFullyAssigned())
        assertTrue(jobAfter1.hasAnyAssigned())
        assertEquals(JobStatus.PENDING, jobAfter1.status)

        // Electrician slot is now FULL (1/1). Electrician should NO LONGER match open slots
        assertFalse(jobAfter1.matchesWorkerSkill(electricianSkills))

        // But Gardener still has open slots (0/2)
        assertTrue(jobAfter1.matchesWorkerSkill(gardenerSkills))

        // Second Electrician provider cannot accept the full Electrician slot
        val acceptDupElectrician = CoopRepository.acceptJobRequirement(
            jobId = job.id,
            skill = "Electrician",
            workerId = "w_elec_2",
            workerName = "Rajesh Electrician"
        )
        assertFalse(acceptDupElectrician)

        // Provider 2 (Gardener A) accepts 1 Gardener slot
        val accept2 = CoopRepository.acceptJobRequirement(
            jobId = job.id,
            skill = "Gardener",
            workerId = "w_gard_1",
            workerName = "Ramesh Gardener"
        )
        assertTrue(accept2)

        val jobAfter2 = CoopRepository.jobs.value.first { it.id == job.id }
        assertEquals(2, jobAfter2.assignedWorkersCount())
        assertFalse(jobAfter2.isFullyAssigned())

        // Duplicate acceptance prevention: Same provider (Ramesh Gardener) cannot accept again
        val acceptDupProvider = CoopRepository.acceptJobRequirement(
            jobId = job.id,
            skill = "Gardener",
            workerId = "w_gard_1",
            workerName = "Ramesh Gardener"
        )
        assertFalse(acceptDupProvider)

        // Provider 3 (Gardener B) accepts second Gardener slot
        val accept3 = CoopRepository.acceptJobRequirement(
            jobId = job.id,
            skill = "Gardener",
            workerId = "w_gard_2",
            workerName = "Suresh Gardener"
        )
        assertTrue(accept3)

        // All 3 slots filled! (1 Electrician + 2 Gardeners)
        val jobAfter3 = CoopRepository.jobs.value.first { it.id == job.id }
        assertEquals(3, jobAfter3.assignedWorkersCount())
        assertTrue(jobAfter3.isFullyAssigned())
        assertEquals(JobStatus.ACCEPTED, jobAfter3.status)

        // No more open slots for Gardener
        assertFalse(jobAfter3.matchesWorkerSkill(gardenerSkills))

        // Customer sees all assigned provider names
        val allNames = jobAfter3.allAssignedProviderNames()
        assertEquals(3, allNames.size)
        assertTrue(allNames.contains("Sunil Electrician"))
        assertTrue(allNames.contains("Ramesh Gardener"))
        assertTrue(allNames.contains("Suresh Gardener"))
    }

    @Test
    fun `multi-phone audit - job serialization preserves multi-worker requirements across restart`() {
        val reqs = listOf(
            ServiceRequirement(
                skill = "Electrician",
                quantity = 1,
                assignedProviderIds = listOf("w_1"),
                assignedProviderNames = listOf("Sunil Electrician")
            ),
            ServiceRequirement(
                skill = "Gardener",
                quantity = 2,
                assignedProviderIds = listOf("w_2"),
                assignedProviderNames = listOf("Ramesh Gardener")
            )
        )
        val originalJob = com.example.data.model.Job(
            id = "audit_req_101",
            customerId = "cust_demo",
            workerId = null,
            cooperativeId = "coop_blr",
            skill = "Electrician",
            location = "Indiranagar, Bangalore",
            dateTime = "Today, 3:00 PM",
            durationMinutes = 120,
            priceInPaise = 150000L,
            escrowAmountInPaise = 150000L,
            status = JobStatus.PENDING,
            title = "Multi-worker job",
            description = "Electrician & Gardener needed",
            createdAtTimestamp = System.currentTimeMillis(),
            requirements = reqs
        )

        // Serialize to JSON
        val json = TwoDeviceSyncManager.jobToJson(originalJob)
        assertNotNull(json)

        // Deserialize back
        val restored = TwoDeviceSyncManager.jsonToJob(json)
        assertNotNull(restored)
        assertEquals(originalJob.id, restored!!.id)
        assertEquals(2, restored.requirements.size)
        assertEquals(1, restored.requirements[0].quantity)
        assertEquals(listOf("w_1"), restored.requirements[0].assignedProviderIds)
        assertEquals(listOf("Sunil Electrician"), restored.requirements[0].assignedProviderNames)
        assertEquals(2, restored.requirements[1].quantity)
        assertEquals(listOf("w_2"), restored.requirements[1].assignedProviderIds)
        assertEquals(listOf("Ramesh Gardener"), restored.requirements[1].assignedProviderNames)
    }

    @Test
    fun `bugfix 1 - fresh install starts with needs login and does not default to provider`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        com.example.data.preferences.UserPreferences.init(app)
        com.example.data.preferences.UserPreferences.clearLogin()
        kotlinx.coroutines.runBlocking {
            com.example.data.datastore.AppDataStore.clearUserProfile(app)
        }

        val viewModel = com.example.ui.viewmodel.StartupViewModel(app)
        val start = System.currentTimeMillis()
        while (viewModel.uiState.value is com.example.ui.viewmodel.StartupUiState.Loading && System.currentTimeMillis() - start < 3000) {
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            Thread.sleep(50)
        }
        val state = viewModel.uiState.value
        assertEquals(com.example.ui.viewmodel.StartupUiState.NeedsLogin, state)
    }

    @Test
    fun `bugfix 1 - customer onboarding restores only customer role and does not overwrite with provider`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        com.example.data.preferences.UserPreferences.init(app)
        com.example.data.preferences.UserPreferences.clearLogin()
        kotlinx.coroutines.runBlocking {
            com.example.data.datastore.AppDataStore.clearUserProfile(app)
        }

        // Customer completes onboarding
        CoopRepository.initPreferences(app)
        CoopRepository.loginUser(
            name = "Aarti Sharma",
            phone = "9811122233",
            locality = "Koramangala, Bangalore",
            role = Role.CUSTOMER
        )

        // Verify active role and profile
        assertEquals(Role.CUSTOMER, CoopRepository.currentRole.value)
        assertEquals("Aarti Sharma", CoopRepository.activeUserProfile.value?.name)
        val customer = CoopRepository.getActiveCustomer()
        assertEquals("Aarti Sharma", customer.name)

        // Provider identity must not be overwritten or shared
        val activeWorker = CoopRepository.getActiveWorker()
        assertFalse("Customer name must not leak into worker profile", activeWorker.name == "Aarti Sharma")

        // Simulate app reopening
        val viewModel = com.example.ui.viewmodel.StartupViewModel(app)
        val start = System.currentTimeMillis()
        while (viewModel.uiState.value is com.example.ui.viewmodel.StartupUiState.Loading && System.currentTimeMillis() - start < 3000) {
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            Thread.sleep(50)
        }
        val restoredState = viewModel.uiState.value
        assertTrue("Expected Authenticated state, got $restoredState", restoredState is com.example.ui.viewmodel.StartupUiState.Authenticated)
        val auth = restoredState as com.example.ui.viewmodel.StartupUiState.Authenticated
        assertEquals(Role.CUSTOMER, auth.role)
        assertEquals("Aarti Sharma", auth.name)
    }

    @Test
    fun `bugfix 2 - job booking creates zero auto acceptance with open slots`() {
        // Customer books single skill job
        val singleJobResult = CoopRepository.bookJob(
            skill = "Electrician",
            location = "Indiranagar",
            dateTime = "Today, 10:00 AM",
            durationMinutes = 60,
            priceInPaise = 30000L
        )
        assertTrue(singleJobResult.isSuccess)
        val singleJob = singleJobResult.getOrThrow()
        assertEquals(JobStatus.PENDING, singleJob.status)
        assertEquals(0, singleJob.assignedWorkersCount())
        assertFalse(singleJob.isFullyAssigned())
        assertFalse(singleJob.hasAnyAssigned())

        // Customer books multi skill job
        val multiJobResult = CoopRepository.bookMultiRequirementJob(
            requirements = listOf(
                ServiceRequirement(skill = "Gardener", quantity = 2),
                ServiceRequirement(skill = "Plumber", quantity = 1)
            ),
            location = "Whitefield",
            dateTime = "Tomorrow, 2:00 PM",
            durationMinutes = 120,
            priceInPaise = 90000L
        )
        assertTrue(multiJobResult.isSuccess)
        val multiJob = multiJobResult.getOrThrow()
        assertEquals(JobStatus.PENDING, multiJob.status)
        assertEquals(0, multiJob.assignedWorkersCount())
        assertEquals(3, multiJob.totalWorkersNeeded())
        assertFalse(multiJob.isFullyAssigned())
        assertFalse(multiJob.hasAnyAssigned())
    }

    @Test
    fun `bugfix 3 - role switching preserves user data and does not clear user preferences`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        com.example.data.preferences.UserPreferences.init(app)
        CoopRepository.initPreferences(app)

        CoopRepository.loginUser(
            name = "Sunil Electrician",
            phone = "9822334455",
            locality = "MG Road, Bangalore",
            role = Role.WORKER,
            workerSkill = "Electrician"
        )
        assertEquals(Role.WORKER, CoopRepository.currentRole.value)

        // Switch to Customer
        CoopRepository.switchRoleTo(Role.CUSTOMER)
        assertEquals(Role.CUSTOMER, CoopRepository.currentRole.value)
        assertTrue(com.example.data.preferences.UserPreferences.isUserLoggedIn())

        // Switch back to Worker
        CoopRepository.switchRoleTo(Role.WORKER)
        assertEquals(Role.WORKER, CoopRepository.currentRole.value)
        assertTrue(com.example.data.preferences.UserPreferences.isUserLoggedIn())
    }
}
