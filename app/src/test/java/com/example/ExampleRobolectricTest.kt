package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.engine.FairDispatchEngine
import com.example.data.engine.LedgerEngine
import com.example.data.engine.WageEngine
import com.example.data.model.JobStatus
import com.example.data.repository.CoopRepository
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
}
