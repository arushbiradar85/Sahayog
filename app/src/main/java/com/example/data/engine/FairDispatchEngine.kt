package com.example.data.engine

import com.example.data.model.Job
import com.example.data.model.Worker
import kotlin.math.roundToInt

object FairDispatchEngine {

    data class DispatchAssignment(
        val job: Job,
        val assignedWorker: Worker,
        val workerWagePaise: Long,
        val waitTimeMinutes: Int = 0
    )

    data class AlgorithmResult(
        val algorithmName: String,
        val assignments: List<DispatchAssignment>,
        val unassignedJobs: List<Job>,
        val workerFinalEarnings: Map<String, Long>, // workerId -> total earnings
        val workerAddedEarnings: Map<String, Long>, // workerId -> earnings added in simulation
        val giniCoefficient: Double, // 0.0 to 1.0
        val top20SharePercent: Double, // 0.0 to 100.0
        val totalDispatchedPaise: Long,
        val assignedCount: Int,
        val totalWorkersConsidered: Int,
        val averageWaitTimeMinutes: Double = 0.0,
        val maxWaitTimeMinutes: Int = 0
    )

    /**
     * Executes Rating-Greedy dispatch:
     * - Verified workers only
     * - Skill matching
     * - Avoids overloaded worker (max 2 jobs in batch to simulate scheduling window)
     * - Selects highest rating worker
     */
    fun runRatingGreedy(
        pendingJobs: List<Job>,
        workers: List<Worker>,
        adminFeePercent: Int = 10
    ): AlgorithmResult {
        val currentEarnings = workers.associate { it.id to it.totalEarningsInPaise }.toMutableMap()
        val addedEarnings = workers.associate { it.id to 0L }.toMutableMap()
        val assignedJobCount = workers.associate { it.id to 0 }.toMutableMap()
        val workerBusyMinutes = workers.associate { it.id to 0 }.toMutableMap()
        val assignments = mutableListOf<DispatchAssignment>()
        val unassigned = mutableListOf<Job>()

        for (job in pendingJobs) {
            val eligibleWorkers = workers.filter { worker ->
                worker.verified &&
                worker.skills.any { it.equals(job.skill, ignoreCase = true) } &&
                (assignedJobCount[worker.id] ?: 0) < 2 // Scheduling conflict limit
            }

            if (eligibleWorkers.isNotEmpty()) {
                // Greedy: pick highest rating, break ties with completed jobs
                val bestWorker = eligibleWorkers.maxWithOrNull(
                    compareBy<Worker> { it.rating }
                        .thenBy { it.completedJobs }
                )!!

                val payout = WageEngine.calculatePayoutSplit(job.priceInPaise, adminFeePercent)
                val waitTime = workerBusyMinutes[bestWorker.id] ?: 0
                assignments.add(DispatchAssignment(job, bestWorker, payout.workerWagePaise, waitTime))
                
                currentEarnings[bestWorker.id] = (currentEarnings[bestWorker.id] ?: 0L) + payout.workerWagePaise
                addedEarnings[bestWorker.id] = (addedEarnings[bestWorker.id] ?: 0L) + payout.workerWagePaise
                assignedJobCount[bestWorker.id] = (assignedJobCount[bestWorker.id] ?: 0) + 1
                workerBusyMinutes[bestWorker.id] = waitTime + job.durationMinutes
            } else {
                unassigned.add(job)
            }
        }

        val earningsList = currentEarnings.values.toList()
        val gini = calculateGini(earningsList)
        val top20 = calculateTop20Share(earningsList)
        val totalDispatched = assignments.sumOf { it.workerWagePaise }
        val avgWaitTime = if (assignments.isNotEmpty()) assignments.map { it.waitTimeMinutes }.average() else 0.0
        val maxWaitTime = assignments.maxOfOrNull { it.waitTimeMinutes } ?: 0

        return AlgorithmResult(
            algorithmName = "Rating-Greedy Dispatch",
            assignments = assignments,
            unassignedJobs = unassigned,
            workerFinalEarnings = currentEarnings,
            workerAddedEarnings = addedEarnings,
            giniCoefficient = gini,
            top20SharePercent = top20,
            totalDispatchedPaise = totalDispatched,
            assignedCount = assignments.size,
            totalWorkersConsidered = workers.size,
            averageWaitTimeMinutes = avgWaitTime,
            maxWaitTimeMinutes = maxWaitTime
        )
    }

    /**
     * Executes Equitable dispatch:
     * - Verified workers only
     * - Skill matching
     * - Avoids overloaded worker (max 2 jobs in batch)
     * - Prioritizes workers with LOWER accumulated earnings
     * - Rating acts as secondary tie-breaker
     */
    fun runEquitable(
        pendingJobs: List<Job>,
        workers: List<Worker>,
        adminFeePercent: Int = 10
    ): AlgorithmResult {
        val currentEarnings = workers.associate { it.id to it.totalEarningsInPaise }.toMutableMap()
        val addedEarnings = workers.associate { it.id to 0L }.toMutableMap()
        val assignedJobCount = workers.associate { it.id to 0 }.toMutableMap()
        val workerBusyMinutes = workers.associate { it.id to 0 }.toMutableMap()
        val assignments = mutableListOf<DispatchAssignment>()
        val unassigned = mutableListOf<Job>()

        for (job in pendingJobs) {
            val eligibleWorkers = workers.filter { worker ->
                worker.verified &&
                worker.skills.any { it.equals(job.skill, ignoreCase = true) } &&
                (assignedJobCount[worker.id] ?: 0) < 2 // Scheduling conflict limit
            }

            if (eligibleWorkers.isNotEmpty()) {
                // Equitable: prioritize lowest current accumulated earnings!
                val bestWorker = eligibleWorkers.minWithOrNull(
                    compareBy<Worker> { currentEarnings[it.id] ?: 0L }
                        .thenByDescending { it.rating }
                )!!

                val payout = WageEngine.calculatePayoutSplit(job.priceInPaise, adminFeePercent)
                val waitTime = workerBusyMinutes[bestWorker.id] ?: 0
                assignments.add(DispatchAssignment(job, bestWorker, payout.workerWagePaise, waitTime))

                currentEarnings[bestWorker.id] = (currentEarnings[bestWorker.id] ?: 0L) + payout.workerWagePaise
                addedEarnings[bestWorker.id] = (addedEarnings[bestWorker.id] ?: 0L) + payout.workerWagePaise
                assignedJobCount[bestWorker.id] = (assignedJobCount[bestWorker.id] ?: 0) + 1
                workerBusyMinutes[bestWorker.id] = waitTime + job.durationMinutes
            } else {
                unassigned.add(job)
            }
        }

        val earningsList = currentEarnings.values.toList()
        val gini = calculateGini(earningsList)
        val top20 = calculateTop20Share(earningsList)
        val totalDispatched = assignments.sumOf { it.workerWagePaise }
        val avgWaitTime = if (assignments.isNotEmpty()) assignments.map { it.waitTimeMinutes }.average() else 0.0
        val maxWaitTime = assignments.maxOfOrNull { it.waitTimeMinutes } ?: 0

        return AlgorithmResult(
            algorithmName = "Equitable Cooperative Dispatch",
            assignments = assignments,
            unassignedJobs = unassigned,
            workerFinalEarnings = currentEarnings,
            workerAddedEarnings = addedEarnings,
            giniCoefficient = gini,
            top20SharePercent = top20,
            totalDispatchedPaise = totalDispatched,
            assignedCount = assignments.size,
            totalWorkersConsidered = workers.size,
            averageWaitTimeMinutes = avgWaitTime,
            maxWaitTimeMinutes = maxWaitTime
        )
    }

    /**
     * Pure Gini coefficient calculation.
     * Formula for sorted array y_1 <= y_2 <= ... <= y_n:
     * G = [ sum_{i=1}^n (2*i - n - 1) * y_i ] / [ n * sum y_i ]
     * Range: 0.0 (perfect equality) to 1.0 (maximum inequality).
     */
    fun calculateGini(earnings: List<Long>): Double {
        if (earnings.isEmpty()) return 0.0
        val sorted = earnings.map { it.toDouble() }.sorted()
        val n = sorted.size
        val totalSum = sorted.sum()

        if (totalSum <= 0.0) return 0.0

        var numerator = 0.0
        for (i in 0 until n) {
            val rank = i + 1 // 1-indexed
            numerator += (2 * rank - n - 1) * sorted[i]
        }

        val gini = numerator / (n * totalSum)
        return gini.coerceIn(0.0, 1.0)
    }

    /**
     * Pure Top 20% earnings share calculation.
     * Sum of top 20% highest earning workers divided by total earnings.
     * Returns percentage 0.0 to 100.0%.
     */
    fun calculateTop20Share(earnings: List<Long>): Double {
        if (earnings.isEmpty()) return 0.0
        val sortedDesc = earnings.map { it.toDouble() }.sortedDescending()
        val totalSum = sortedDesc.sum()
        if (totalSum <= 0.0) return 0.0

        val top20Count = (sortedDesc.size * 0.20).roundToInt().coerceAtLeast(1)
        val top20Sum = sortedDesc.take(top20Count).sum()

        return (top20Sum / totalSum) * 100.0
    }
}
