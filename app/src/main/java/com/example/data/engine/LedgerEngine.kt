package com.example.data.engine

import com.example.data.model.LedgerEntry
import java.security.MessageDigest

object LedgerEngine {

    const val GENESIS_HASH = "0000000000000000000000000000000000000000000000000000000000000000"

    /**
     * Serializes ledger fields into standard canonical payload string.
     */
    fun buildPayload(
        workerId: String,
        jobId: String,
        workerWageInPaise: Long,
        adminFeeInPaise: Long,
        welfareInPaise: Long,
        timestamp: Long,
        previousHash: String
    ): String {
        return "$workerId|$jobId|$workerWageInPaise|$adminFeeInPaise|$welfareInPaise|$timestamp|$previousHash"
    }

    /**
     * Computes cryptographic SHA-256 hash.
     */
    fun computeSha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Creates a new LedgerEntry securely cryptographically chained to previous worker entry.
     */
    fun createEntry(
        id: String,
        workerId: String,
        jobId: String,
        workerWageInPaise: Long,
        adminFeeInPaise: Long,
        welfareInPaise: Long,
        timestamp: Long,
        previousHash: String
    ): LedgerEntry {
        val payload = buildPayload(
            workerId = workerId,
            jobId = jobId,
            workerWageInPaise = workerWageInPaise,
            adminFeeInPaise = adminFeeInPaise,
            welfareInPaise = welfareInPaise,
            timestamp = timestamp,
            previousHash = previousHash
        )
        val hash = computeSha256(payload)
        return LedgerEntry(
            id = id,
            workerId = workerId,
            jobId = jobId,
            workerWageInPaise = workerWageInPaise,
            adminFeeInPaise = adminFeeInPaise,
            welfareInPaise = welfareInPaise,
            previousHash = previousHash,
            currentHash = hash,
            timestamp = timestamp
        )
    }

    data class VerificationResult(
        val isValid: Boolean,
        val recomputedHash: String,
        val recordedHash: String,
        val payloadString: String,
        val reason: String
    )

    /**
     * Cryptographically recomputes hash from entry fields and compares against stored hash.
     * No faking!
     */
    fun verifyEntry(entry: LedgerEntry): VerificationResult {
        val payload = buildPayload(
            workerId = entry.workerId,
            jobId = entry.jobId,
            workerWageInPaise = entry.workerWageInPaise,
            adminFeeInPaise = entry.adminFeeInPaise,
            welfareInPaise = entry.welfareInPaise,
            timestamp = entry.timestamp,
            previousHash = entry.previousHash
        )
        val recomputed = computeSha256(payload)
        val matches = recomputed.equals(entry.currentHash, ignoreCase = true)
        return VerificationResult(
            isValid = matches,
            recomputedHash = recomputed,
            recordedHash = entry.currentHash,
            payloadString = payload,
            reason = if (matches) "Cryptographic signature verified: recomputed SHA-256 matches stored block hash exactly."
            else "Mismatch! Data tampering detected. Stored hash does not match recomputed SHA-256."
        )
    }

    data class ChainVerificationResult(
        val isChainValid: Boolean,
        val totalBlocks: Int,
        val brokenAtIndex: Int?,
        val message: String
    )

    /**
     * Orders a set of ledger entries from Genesis to latest block by following cryptographic hash links.
     */
    fun orderChain(entries: List<LedgerEntry>): List<LedgerEntry> {
        if (entries.size <= 1) return entries
        val byPrevHash = entries.associateBy { it.previousHash }
        val ordered = mutableListOf<LedgerEntry>()
        var currentPrev = GENESIS_HASH

        while (true) {
            val next = byPrevHash[currentPrev] ?: break
            ordered.add(next)
            currentPrev = next.currentHash
        }

        return if (ordered.size == entries.size) ordered else entries.sortedBy { it.timestamp }
    }

    /**
     * Verifies the complete hash chain of a worker from Genesis to latest block.
     */
    fun verifyChain(entries: List<LedgerEntry>): ChainVerificationResult {
        if (entries.isEmpty()) {
            return ChainVerificationResult(true, 0, null, "No entries in ledger. Chain is empty.")
        }
        val chain = orderChain(entries)
        var expectedPrev = GENESIS_HASH
        for (i in chain.indices) {
            val entry = chain[i]
            if (entry.previousHash != expectedPrev) {
                return ChainVerificationResult(
                    isChainValid = false,
                    totalBlocks = chain.size,
                    brokenAtIndex = i,
                    message = "Chain broken at index $i: expected previous hash $expectedPrev but got ${entry.previousHash}"
                )
            }
            val singleResult = verifyEntry(entry)
            if (!singleResult.isValid) {
                return ChainVerificationResult(
                    isChainValid = false,
                    totalBlocks = chain.size,
                    brokenAtIndex = i,
                    message = "Block at index $i has invalid hash!"
                )
            }
            expectedPrev = entry.currentHash
        }
        return ChainVerificationResult(
            isChainValid = true,
            totalBlocks = chain.size,
            brokenAtIndex = null,
            message = "All ${chain.size} blocks cryptographically verified intact from Genesis."
        )
    }
}
