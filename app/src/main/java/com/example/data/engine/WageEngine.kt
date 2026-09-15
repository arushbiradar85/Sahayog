package com.example.data.engine

import com.example.data.model.ServiceCategory
import java.text.NumberFormat
import java.util.Locale

object WageEngine {

    // Centralized hourly minimum wage rates in paise (1 INR = 100 paise)
    val SERVICE_CATEGORIES: List<ServiceCategory> = listOf(
        ServiceCategory("Electrician", "bolt", 25000L, "Wiring, appliance fixes, circuit repair"),
        ServiceCategory("Plumber", "plumbing", 22000L, "Pipes, faucets, drain clearing"),
        ServiceCategory("Carpenter", "carpenter", 24000L, "Furniture repair, assembly, woodwork"),
        ServiceCategory("Painter", "format_paint", 20000L, "Interior & exterior wall painting"),
        ServiceCategory("Cleaner", "cleaning_services", 16000L, "Deep home cleaning & sanitization"),
        ServiceCategory("Driver", "directions_car", 22000L, "Local & outstation driving"),
        ServiceCategory("Gardener", "yard", 18000L, "Lawn maintenance, trimming, planting"),
        ServiceCategory("Caregiver", "favorite", 20000L, "Elderly care, nursing assistance"),
        ServiceCategory("Technician", "build", 26000L, "AC, RO, refrigerator diagnostics")
    )

    private val WAGE_MAP: Map<String, Long> = SERVICE_CATEGORIES.associate { it.name to it.hourlyWagePaise }

    fun getHourlyRateInPaise(skill: String): Long {
        return WAGE_MAP[skill] ?: 20000L // Default fallback ₹200/hr
    }

    /**
     * Calculates legal minimum wage floor in paise.
     * Enforces overtime rule: Work beyond 8 hours (480 mins) is paid at 1.5x regular hourly wage.
     * Uses strictly Long integer arithmetic.
     */
    fun calculateMinimumWageFloorInPaise(skill: String, durationMinutes: Int): Long {
        val hourlyRate = getHourlyRateInPaise(skill)
        val clampedMinutes = durationMinutes.coerceAtLeast(30)
        
        val regularMinutes = clampedMinutes.coerceAtMost(480)
        val overtimeMinutes = (clampedMinutes - 480).coerceAtLeast(0)

        // Using precise integer math in paise
        // regularWage = (regularMinutes * hourlyRate) / 60
        val regularWage = (regularMinutes.toLong() * hourlyRate) / 60L

        // Overtime rate is 1.5x: (overtimeMinutes * hourlyRate * 3) / (60 * 2)
        val overtimeWage = if (overtimeMinutes > 0) {
            (overtimeMinutes.toLong() * hourlyRate * 3L) / 120L
        } else {
            0L
        }

        return regularWage + overtimeWage
    }

    /**
     * Suggests a fair booking price above the minimum wage floor (+15% recommended buffer for skilled service).
     */
    fun getSuggestedPriceInPaise(skill: String, durationMinutes: Int): Long {
        val floor = calculateMinimumWageFloorInPaise(skill, durationMinutes)
        return floor + (floor * 15L / 100L)
    }

    data class ValidationResult(
        val isValid: Boolean,
        val minimumFloorPaise: Long,
        val differencePaise: Long,
        val reasonMessage: String
    )

    /**
     * Validates if the offered price satisfies the legally enforced minimum wage.
     */
    fun validatePrice(skill: String, durationMinutes: Int, offeredPricePaise: Long): ValidationResult {
        val minFloor = calculateMinimumWageFloorInPaise(skill, durationMinutes)
        return if (offeredPricePaise >= minFloor) {
            ValidationResult(
                isValid = true,
                minimumFloorPaise = minFloor,
                differencePaise = offeredPricePaise - minFloor,
                reasonMessage = "Complies with cooperative wage floor."
            )
        } else {
            val deficit = minFloor - offeredPricePaise
            val durationHours = durationMinutes / 60.0
            val overtimeNote = if (durationMinutes > 480) " (includes 1.5x overtime beyond 8h)" else ""
            ValidationResult(
                isValid = false,
                minimumFloorPaise = minFloor,
                differencePaise = deficit,
                reasonMessage = "Blocked: Offered price ${formatPaise(offeredPricePaise)} is below the legally protected minimum floor of ${formatPaise(minFloor)} for ${String.format(Locale.US, "%.1f", durationHours)} hrs of $skill work$overtimeNote."
            )
        }
    }

    data class PayoutBreakdown(
        val totalPaise: Long,
        val workerWagePaise: Long,
        val adminFeePaise: Long,
        val welfarePaise: Long,
        val adminFeePercent: Int
    )

    /**
     * Calculates transparent payout split:
     * - Admin fee = price * adminFeePercent / 100
     * - Worker wage = price - adminFee
     * - Welfare contribution = adminFee * 50%
     */
    fun calculatePayoutSplit(priceInPaise: Long, adminFeePercent: Int = 10): PayoutBreakdown {
        val adminFee = (priceInPaise * adminFeePercent.toLong()) / 100L
        val workerWage = priceInPaise - adminFee
        val welfare = adminFee / 2L // 50% of cooperative fee directed to welfare fund
        return PayoutBreakdown(
            totalPaise = priceInPaise,
            workerWagePaise = workerWage,
            adminFeePaise = adminFee,
            welfarePaise = welfare,
            adminFeePercent = adminFeePercent
        )
    }

    /**
     * Formats integer paise into Indian Rupee string, e.g., 25000L -> "₹250.00"
     */
    fun formatPaise(paise: Long): String {
        val rupees = paise / 100.0
        val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        return format.format(rupees)
    }

    /**
     * Formats integer paise to whole number representation if no fraction, e.g. "₹250"
     */
    fun formatPaiseCompact(paise: Long): String {
        val rupees = paise / 100
        val rem = paise % 100
        return if (rem == 0L) {
            "₹$rupees"
        } else {
            String.format(Locale.US, "₹%d.%02d", rupees, rem)
        }
    }
}
