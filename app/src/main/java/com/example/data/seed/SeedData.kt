package com.example.data.seed

import com.example.data.engine.LedgerEngine
import com.example.data.engine.WageEngine
import com.example.data.model.Cooperative
import com.example.data.model.Customer
import com.example.data.model.Job
import com.example.data.model.JobStatus
import com.example.data.model.LedgerEntry
import com.example.data.model.Worker

object SeedData {

    val cooperatives: List<Cooperative> = listOf(
        Cooperative(
            id = "coop_blr",
            name = "Bangalore Urban Workers Cooperative",
            adminFeePercent = 10,
            welfareFundInPaise = 8450000L, // ₹84,500
            totalMembers = 20,
            city = "Bangalore"
        ),
        Cooperative(
            id = "coop_mys",
            name = "Mysuru Craftsmen Sahakari Sangha",
            adminFeePercent = 8,
            welfareFundInPaise = 5200000L, // ₹52,000
            totalMembers = 15,
            city = "Mysuru"
        ),
        Cooperative(
            id = "coop_hub",
            name = "North Karnataka Artisans Guild",
            adminFeePercent = 12,
            welfareFundInPaise = 3900000L, // ₹39,000
            totalMembers = 12,
            city = "Hubballi"
        )
    )

    val customers: List<Customer> = listOf(
        Customer("cust_1", "Ananya Sharma", "Indiranagar, 100ft Road"),
        Customer("cust_2", "Vikramaditya Rao", "Koramangala 4th Block"),
        Customer("cust_3", "Priyanka Nair", "HSR Layout Sector 2"),
        Customer("cust_4", "Arjun Hegde", "Whitefield, ITPL Main Rd"),
        Customer("cust_5", "Deepa Sundaram", "Jayanagar 4th T Block"),
        Customer("cust_6", "Rajeshwari Iyer", "Malleshwaram 15th Cross"),
        Customer("cust_7", "Karthik Venkatesh", "Electronic City Phase 1"),
        Customer("cust_8", "Meera Kulkarni", "JP Nagar 6th Phase"),
        Customer("cust_9", "Suresh Shenoy", "BTM Layout 2nd Stage"),
        Customer("cust_10", "Tanvi Joshi", "Bellandur Outer Ring Road")
    )

    val workers: List<Worker> = listOf(
        // High earners (often favored by rating-greedy dispatch)
        Worker("w_1", "Ramesh Kumar", listOf("Electrician", "Technician"), 4.9, 42, 3850000L, true, "coop_blr", "+91 98451 22331", 6),
        Worker("w_2", "Suresh Patil", listOf("Plumber"), 4.8, 38, 3120000L, true, "coop_blr", "+91 98452 44552", 5),
        Worker("w_3", "Manjunath Gowda", listOf("Carpenter"), 4.9, 36, 2980000L, true, "coop_blr", "+91 98453 66773", 7),
        Worker("w_4", "Basavaraj Shinde", listOf("Painter"), 4.7, 31, 2450000L, true, "coop_blr", "+91 98454 88994", 4),

        // Mid earners
        Worker("w_5", "Kavita Reddy", listOf("Cleaner", "Caregiver"), 4.6, 22, 1680000L, true, "coop_blr", "+91 98455 11225", 3),
        Worker("w_6", "Prakash Shetty", listOf("Electrician"), 4.5, 19, 1540000L, true, "coop_blr", "+91 98456 33446", 4),
        Worker("w_7", "Mohammad Rafiq", listOf("Technician"), 4.6, 17, 1420000L, true, "coop_blr", "+91 98457 55667", 3),
        Worker("w_8", "Lakshmi Devi", listOf("Caregiver", "Cleaner"), 4.7, 18, 1390000L, true, "coop_blr", "+91 98458 77888", 5),
        Worker("w_9", "Ganesh Naik", listOf("Plumber"), 4.4, 15, 1220000L, true, "coop_blr", "+91 98459 99009", 3),
        Worker("w_10", "Anil Kamble", listOf("Gardener"), 4.5, 16, 1150000L, true, "coop_blr", "+91 98450 12340", 4),

        // Low earners / Newer verified members (need equitable dispatch!)
        Worker("w_11", "Santosh Biradar", listOf("Electrician"), 4.3, 7, 490000L, true, "coop_blr", "+91 97411 22331", 2),
        Worker("w_12", "Sunita Jadhav", listOf("Cleaner"), 4.4, 6, 420000L, true, "coop_blr", "+91 97412 33442", 1),
        Worker("w_13", "Dinesh Pujari", listOf("Plumber"), 4.2, 5, 380000L, true, "coop_blr", "+91 97413 44553", 2),
        Worker("w_14", "Vijay More", listOf("Carpenter"), 4.3, 5, 360000L, true, "coop_blr", "+91 97414 55664", 2),
        Worker("w_15", "Shweta Deshmukh", listOf("Painter"), 4.1, 4, 290000L, true, "coop_blr", "+91 97415 66775", 1),
        Worker("w_16", "Nagaraj Acharya", listOf("Technician"), 4.3, 4, 310000L, true, "coop_blr", "+91 97416 77886", 2),
        Worker("w_17", "Ravi Solanki", listOf("Driver"), 4.5, 6, 480000L, true, "coop_blr", "+91 97417 88997", 3),
        Worker("w_18", "Preeti Bhosle", listOf("Gardener"), 4.2, 3, 220000L, true, "coop_blr", "+91 97418 99008", 1),

        // Unverified workers (pending admin member verification)
        Worker("w_19", "Akash Chavan", listOf("Electrician"), 3.8, 1, 60000L, false, "coop_blr", "+91 97419 11229", 1),
        Worker("w_20", "Geeta Sawant", listOf("Cleaner"), 3.9, 1, 50000L, false, "coop_blr", "+91 97420 22330", 1)
    )

    fun createInitialJobsAndLedger(): Pair<List<Job>, List<LedgerEntry>> {
        val jobs = mutableListOf<Job>()
        val ledger = mutableListOf<LedgerEntry>()

        // 1. Initial COMPLETED jobs that generated historical ledger entries
        data class CompletedJobSeed(
            val id: String,
            val custId: String,
            val workerId: String,
            val skill: String,
            val loc: String,
            val mins: Int,
            val pricePaise: Long,
            val timeOffsetSec: Long
        )

        val completedSeeds = listOf(
            // w_1 (Electrician - star earner)
            CompletedJobSeed("job_c1", "cust_1", "w_1", "Electrician", "Indiranagar", 120, 60000L, 1726000000L),
            CompletedJobSeed("job_c2", "cust_2", "w_1", "Electrician", "Koramangala", 180, 90000L, 1726086400L),
            CompletedJobSeed("job_c3", "cust_4", "w_1", "Electrician", "Whitefield", 240, 120000L, 1726172800L),
            CompletedJobSeed("job_c4", "cust_5", "w_1", "Electrician", "Jayanagar", 180, 85000L, 1726259200L),
            CompletedJobSeed("job_c5", "cust_7", "w_1", "Electrician", "Electronic City", 120, 60000L, 1726345600L),

            // w_2 (Plumber - star earner)
            CompletedJobSeed("job_c6", "cust_3", "w_2", "Plumber", "HSR Layout", 120, 55000L, 1726000000L),
            CompletedJobSeed("job_c7", "cust_4", "w_2", "Plumber", "Whitefield", 240, 110000L, 1726086400L),
            CompletedJobSeed("job_c8", "cust_8", "w_2", "Plumber", "JP Nagar", 180, 80000L, 1726172800L),
            CompletedJobSeed("job_c9", "cust_9", "w_2", "Plumber", "BTM Layout", 120, 60000L, 1726259200L),

            // w_3 (Carpenter - high earner)
            CompletedJobSeed("job_c10", "cust_5", "w_3", "Carpenter", "Jayanagar", 180, 85000L, 1726000000L),
            CompletedJobSeed("job_c11", "cust_2", "w_3", "Carpenter", "Koramangala", 240, 115000L, 1726086400L),
            CompletedJobSeed("job_c12", "cust_6", "w_3", "Carpenter", "Malleshwaram", 180, 90000L, 1726172800L),
            CompletedJobSeed("job_c13", "cust_10", "w_3", "Carpenter", "Bellandur", 120, 60000L, 1726259200L),

            // w_4 (Painter - high earner)
            CompletedJobSeed("job_c14", "cust_6", "w_4", "Painter", "Malleshwaram", 480, 180000L, 1726000000L),
            CompletedJobSeed("job_c15", "cust_1", "w_4", "Painter", "Indiranagar", 360, 140000L, 1726086400L),
            CompletedJobSeed("job_c16", "cust_3", "w_4", "Painter", "HSR Layout", 240, 95000L, 1726172800L),

            // w_5 (Cleaner - mid earner)
            CompletedJobSeed("job_c17", "cust_7", "w_5", "Cleaner", "Electronic City", 180, 60000L, 1726000000L),
            CompletedJobSeed("job_c18", "cust_8", "w_5", "Cleaner", "JP Nagar", 240, 75000L, 1726086400L),
            CompletedJobSeed("job_c19", "cust_9", "w_5", "Cleaner", "BTM Layout", 180, 55000L, 1726172800L),

            // w_6 (Electrician - mid earner)
            CompletedJobSeed("job_c20", "cust_8", "w_6", "Electrician", "JP Nagar", 120, 60000L, 1726000000L),
            CompletedJobSeed("job_c21", "cust_10", "w_6", "Electrician", "Bellandur", 180, 85000L, 1726086400L),

            // w_7 (Technician - mid earner)
            CompletedJobSeed("job_c22", "cust_9", "w_7", "Technician", "BTM Layout", 120, 65000L, 1726000000L),
            CompletedJobSeed("job_c23", "cust_2", "w_7", "Technician", "Koramangala", 180, 90000L, 1726086400L),

            // w_8 (Caregiver - mid earner)
            CompletedJobSeed("job_c24", "cust_10", "w_8", "Caregiver", "Bellandur", 240, 95000L, 1726000000L),
            CompletedJobSeed("job_c25", "cust_5", "w_8", "Caregiver", "Jayanagar", 180, 70000L, 1726086400L),

            // w_9 (Plumber - mid earner)
            CompletedJobSeed("job_c26", "cust_1", "w_9", "Plumber", "Indiranagar", 150, 68000L, 1726090000L),
            CompletedJobSeed("job_c27", "cust_3", "w_9", "Plumber", "HSR Layout", 120, 55000L, 1726172800L),

            // w_10 (Gardener - mid earner)
            CompletedJobSeed("job_c28", "cust_3", "w_10", "Gardener", "HSR Sector 1", 180, 65000L, 1726100000L),
            CompletedJobSeed("job_c29", "cust_4", "w_10", "Gardener", "Whitefield", 120, 45000L, 1726172800L),

            // w_11 to w_18 (Low earners / newer members - 1 job each)
            CompletedJobSeed("job_c30", "cust_5", "w_11", "Electrician", "Jayanagar 4th", 120, 50000L, 1726110000L),
            CompletedJobSeed("job_c31", "cust_7", "w_12", "Cleaner", "Electronic City Phase 1", 180, 55000L, 1726120000L),
            CompletedJobSeed("job_c32", "cust_2", "w_13", "Plumber", "Koramangala 1st Block", 120, 50000L, 1726130000L),
            CompletedJobSeed("job_c33", "cust_6", "w_14", "Carpenter", "Malleshwaram 7th Cross", 120, 55000L, 1726140000L),
            CompletedJobSeed("job_c34", "cust_8", "w_15", "Painter", "JP Nagar 1st Phase", 240, 90000L, 1726150000L),
            CompletedJobSeed("job_c35", "cust_9", "w_16", "Technician", "BTM 2nd Stage", 120, 55000L, 1726160000L),
            CompletedJobSeed("job_c36", "cust_4", "w_17", "Driver", "Whitefield Main Rd", 180, 75000L, 1726170000L),
            CompletedJobSeed("job_c37", "cust_10", "w_18", "Gardener", "Bellandur Gate", 120, 40000L, 1726180000L)
            // w_19 and w_20 are unverified with 0 completed jobs and 0 ledger entries
        )

        // Generate chained ledger entries per worker
        val lastWorkerHash = mutableMapOf<String, String>()
        for (seed in completedSeeds) {
            val prevHash = lastWorkerHash[seed.workerId] ?: LedgerEngine.GENESIS_HASH
            val payout = WageEngine.calculatePayoutSplit(seed.pricePaise, 10)
            val entry = LedgerEngine.createEntry(
                id = "led_${seed.id}",
                workerId = seed.workerId,
                jobId = seed.id,
                workerWageInPaise = payout.workerWagePaise,
                adminFeeInPaise = payout.adminFeePaise,
                welfareInPaise = payout.welfarePaise,
                timestamp = seed.timeOffsetSec * 1000L,
                previousHash = prevHash
            )
            ledger.add(entry)
            lastWorkerHash[seed.workerId] = entry.currentHash

            jobs.add(
                Job(
                    id = seed.id,
                    customerId = seed.custId,
                    workerId = seed.workerId,
                    cooperativeId = "coop_blr",
                    skill = seed.skill,
                    location = seed.loc,
                    dateTime = "2026-09-12 10:00",
                    durationMinutes = seed.mins,
                    priceInPaise = seed.pricePaise,
                    escrowAmountInPaise = 0L, // Cleared on completion!
                    status = JobStatus.COMPLETED,
                    proofPhotoUri = "content://sahayog/proof_${seed.id}.jpg",
                    proofLatitude = 12.9716,
                    proofLongitude = 77.5946,
                    proofTimestamp = seed.timeOffsetSec * 1000L,
                    proofNotes = "Job inspected and completed satisfactorily.",
                    title = "${seed.skill} Service at ${seed.loc}"
                )
            )
        }

        // 2. ACCEPTED jobs (Worker accepted, not yet submitted proof)
        jobs.add(
            Job(
                id = "job_acc_1",
                customerId = "cust_1",
                workerId = "w_1",
                cooperativeId = "coop_blr",
                skill = "Electrician",
                location = "Indiranagar 12th Main",
                dateTime = "Today, 02:30 PM",
                durationMinutes = 120,
                priceInPaise = 65000L, // ₹650
                escrowAmountInPaise = 65000L, // Escrow held!
                status = JobStatus.ACCEPTED,
                title = "Distribution Board Fuse Tripping Repair",
                description = "Main MCB trips every time kitchen appliances run."
            )
        )
        jobs.add(
            Job(
                id = "job_acc_2",
                customerId = "cust_2",
                workerId = "w_3",
                cooperativeId = "coop_blr",
                skill = "Carpenter",
                location = "Koramangala 80ft Road",
                dateTime = "Tomorrow, 11:00 AM",
                durationMinutes = 180,
                priceInPaise = 90000L, // ₹900
                escrowAmountInPaise = 90000L,
                status = JobStatus.ACCEPTED,
                title = "Teak Dining Table Leg Reinforcement",
                description = "Heavy 6-seater dining table wobbles; joint dowels loose."
            )
        )
        jobs.add(
            Job(
                id = "job_acc_3",
                customerId = "cust_6",
                workerId = "w_4",
                cooperativeId = "coop_blr",
                skill = "Painter",
                location = "Malleshwaram 18th Cross",
                dateTime = "Tomorrow, 09:00 AM",
                durationMinutes = 360,
                priceInPaise = 150000L, // ₹1,500
                escrowAmountInPaise = 150000L,
                status = JobStatus.ACCEPTED,
                title = "Living Room Accent Wall Texture Painting",
                description = "Dulux Velvet touch copper metallic finish required."
            )
        )
        jobs.add(
            Job(
                id = "job_acc_4",
                customerId = "cust_8",
                workerId = "w_5",
                cooperativeId = "coop_blr",
                skill = "Cleaner",
                location = "JP Nagar Phase 2",
                dateTime = "Today, 03:00 PM",
                durationMinutes = 180,
                priceInPaise = 65000L, // ₹650
                escrowAmountInPaise = 65000L,
                status = JobStatus.ACCEPTED,
                title = "Kitchen Deep Degreasing & Sanitization",
                description = "Tile grout cleaning and chimney duct exhaust wash."
            )
        )

        // 3. IN_PROGRESS jobs (Worker has submitted proof, escrow held, awaiting admin release)
        jobs.add(
            Job(
                id = "job_inp_1",
                customerId = "cust_3",
                workerId = "w_1",
                cooperativeId = "coop_blr",
                skill = "Electrician",
                location = "HSR Layout Sector 2, 19th Main",
                dateTime = "Today, 10:00 AM",
                durationMinutes = 120,
                priceInPaise = 60000L, // ₹600
                escrowAmountInPaise = 60000L, // Escrow remains held!
                status = JobStatus.IN_PROGRESS,
                proofPhotoUri = "content://sahayog/proof_inp_1.jpg",
                proofLatitude = 12.9116,
                proofLongitude = 77.6389,
                proofTimestamp = System.currentTimeMillis() - 3600000L,
                proofNotes = "Replaced charred 32A DP switch with Havells industrial unit. Tested on full load.",
                title = "Emergency Geyser DP Switch Replacement",
                description = "Switch sparked and stopped working."
            )
        )
        jobs.add(
            Job(
                id = "job_inp_2",
                customerId = "cust_4",
                workerId = "w_7",
                cooperativeId = "coop_blr",
                skill = "Technician",
                location = "Whitefield ECC Road",
                dateTime = "Today, 11:30 AM",
                durationMinutes = 150,
                priceInPaise = 75000L,
                escrowAmountInPaise = 75000L,
                status = JobStatus.IN_PROGRESS,
                proofPhotoUri = "content://sahayog/proof_inp_2.jpg",
                proofLatitude = 12.9698,
                proofLongitude = 77.7500,
                proofTimestamp = System.currentTimeMillis() - 1800000L,
                proofNotes = "AC indoor coil cleared of dust choke, refrigerant pressure verified at 125 PSI.",
                title = "Split AC Cooling Diagnostics & Coil Service",
                description = "Inverter AC running fan but not cooling room."
            )
        )
        jobs.add(
            Job(
                id = "job_inp_3",
                customerId = "cust_9",
                workerId = "w_6",
                cooperativeId = "coop_blr",
                skill = "Electrician",
                location = "BTM Layout 2nd Stage",
                dateTime = "Today, 01:00 PM",
                durationMinutes = 120,
                priceInPaise = 55000L,
                escrowAmountInPaise = 55000L,
                status = JobStatus.IN_PROGRESS,
                proofPhotoUri = "content://sahayog/proof_inp_3.jpg",
                proofLatitude = 12.9165,
                proofLongitude = 77.6101,
                proofTimestamp = System.currentTimeMillis() - 900000L,
                proofNotes = "Installed ceiling fan regulator and repaired earthing connection on bedroom board.",
                title = "Ceiling Fan Regulator & Earthing Check",
                description = "Fan running only on speed 5, humming noise."
            )
        )

        // 4. DISPUTED jobs (Dispute raised by customer, pending admin resolution)
        jobs.add(
            Job(
                id = "job_disp_1",
                customerId = "cust_5",
                workerId = "w_2",
                cooperativeId = "coop_blr",
                skill = "Plumber",
                location = "Jayanagar 9th Block",
                dateTime = "Yesterday, 04:00 PM",
                durationMinutes = 180,
                priceInPaise = 80000L,
                escrowAmountInPaise = 80000L,
                status = JobStatus.DISPUTED,
                proofPhotoUri = "content://sahayog/proof_disp_1.jpg",
                proofLatitude = 12.9250,
                proofLongitude = 77.5938,
                proofTimestamp = System.currentTimeMillis() - 86400000L,
                proofNotes = "Fitted new brass ball valve in overhead tank line.",
                disputeComment = "Customer claims slight drip remains on the union elbow after worker left.",
                title = "Overhead Tank Pipeline Valve Leakage Fix",
                description = "Water leaking from main PVC outlet pipe."
            )
        )
        jobs.add(
            Job(
                id = "job_disp_2",
                customerId = "cust_10",
                workerId = "w_7",
                cooperativeId = "coop_blr",
                skill = "Technician",
                location = "Bellandur Green Glen",
                dateTime = "2 Days Ago, 02:00 PM",
                durationMinutes = 150,
                priceInPaise = 70000L,
                escrowAmountInPaise = 70000L,
                status = JobStatus.DISPUTED,
                proofPhotoUri = "content://sahayog/proof_disp_2.jpg",
                proofLatitude = 12.9304,
                proofLongitude = 77.6784,
                proofTimestamp = System.currentTimeMillis() - 172800000L,
                proofNotes = "Cleaned washing machine drain filter and cleared lint blockage in pump.",
                disputeComment = "Customer reported machine showed error E03 again next morning during spin cycle.",
                title = "Front Load Washing Machine Drain Pump Jam",
                description = "Washing machine stops with error code during spin cycle."
            )
        )

        // 5. PENDING jobs (Available in pool for booking, worker acceptance, and Fair Dispatch comparison)
        val pendingSeeds = listOf(
            Triple("Electrician", "Koramangala 5th Block", 120 to 65000L),
            Triple("Electrician", "Malleshwaram 8th Main", 180 to 90000L),
            Triple("Electrician", "Indiranagar 100ft Rd", 600 to 320000L), // Overtime job! 10 hours
            Triple("Plumber", "HSR Layout Sector 4", 120 to 55000L),
            Triple("Plumber", "BTM Layout 1st Stage", 180 to 80000L),
            Triple("Carpenter", "Whitefield Hope Farm", 240 to 110000L),
            Triple("Carpenter", "Jayanagar 3rd Block", 180 to 85000L),
            Triple("Painter", "JP Nagar 7th Phase", 480 to 190000L),
            Triple("Painter", "Indiranagar Defence Colony", 540 to 245000L), // Overtime 9 hours
            Triple("Cleaner", "Electronic City Phase 2", 180 to 65000L),
            Triple("Cleaner", "Bellandur Green Glen", 240 to 80000L),
            Triple("Technician", "Koramangala Sony Signal", 120 to 70000L),
            Triple("Technician", "HSR Layout 27th Main", 150 to 80000L),
            Triple("Gardener", "Sadashivanagar Palace Rd", 180 to 65000L),
            Triple("Caregiver", "Malleshwaram Margosa Rd", 240 to 95000L),
            Triple("Driver", "Airport Road Domlur", 300 to 130000L)
        )

        var pId = 1
        for (seed in pendingSeeds) {
            val cust = customers[(pId - 1) % customers.size]
            jobs.add(
                Job(
                    id = "job_pend_$pId",
                    customerId = cust.id,
                    workerId = null,
                    cooperativeId = "coop_blr",
                    skill = seed.first,
                    location = seed.second,
                    dateTime = "2026-09-16 11:00 AM",
                    durationMinutes = seed.third.first,
                    priceInPaise = seed.third.second,
                    escrowAmountInPaise = seed.third.second, // Held in escrow at booking time
                    status = JobStatus.PENDING,
                    title = "${seed.first} Service at ${seed.second.split(" ").first()}",
                    description = "Booked by ${cust.name}. Local cooperative verified request."
                )
            )
            pId++
        }

        return jobs to ledger
    }
}
