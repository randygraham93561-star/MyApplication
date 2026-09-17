package com.google.refereeschedule.domain.model

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName
import java.util.Calendar
import java.util.Date
import java.util.UUID

data class Organization(
    val id: String = "",
    val name: String = "",
    val contactEmail: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isTeamAppSyncEnabled: Boolean = false,
    @get:PropertyName("tier") @set:PropertyName("tier")
    var tier: String = "Free",
    val subscriptionExpiresAt: java.util.Date? = null,
    val printerIp: String = "",
    val printerMacAddress: String = "",
    val printerModel: String = "Generic Thermal",
    val currentGameDayCode: String = "",
    val printerStatus: String = "IDLE",
    val timeZone: String = "UTC",
    val themeColor: String = "Default", // Default, Red, Blue, Yellow
    val logoUrl: String? = null,
    val printRequirements: PrintStationRequirements = PrintStationRequirements(),
    val youthRefereeRequirements: YouthRefereeRequirements = YouthRefereeRequirements(),
    val printerSettings: Map<String, String> = mapOf(
        "paper_width" to "80mm",
        "print_density" to "Normal",
        "auto_cut" to "true",
        "voucher_header" to "League Lunch Voucher",
        "character_set" to "UTF-8"
    )
) {
    @get:Exclude
    val subscriptionTier: SubscriptionTier
        get() = when (tier.lowercase()) {
            "base" -> SubscriptionTier.Base
            "standard" -> SubscriptionTier.Standard
            "pro" -> SubscriptionTier.Pro
            else -> SubscriptionTier.Free
        }

    @get:Exclude
    val isCurrentlyExpired: Boolean
        get() {
            // Rule: All tiers expire July 31st.
            // If subscriptionExpiresAt is null, it's expired (Free tier).
            val expiry = subscriptionExpiresAt ?: return true
            val now = Date()
            
            // If the specific expiry date has passed, it's definitely expired.
            if (expiry.before(now)) return true
            
            // Additional check: Does it belong to the current season?
            // Every year expires on July 31.
            val cal = Calendar.getInstance()
            cal.time = now
            val currentYear = cal.get(Calendar.YEAR)
            val currentMonth = cal.get(Calendar.MONTH) // 0-indexed, July is 6
            
            val seasonEnd = Calendar.getInstance()
            if (currentMonth <= 6) { // Jan - July
                seasonEnd.set(currentYear, 6, 31, 23, 59, 59)
            } else { // Aug - Dec
                seasonEnd.set(currentYear + 1, 6, 31, 23, 59, 59)
            }
            
            // If the stored expiry is before the end of the current season's July 31st, 
            // it means it hasn't been renewed for the current/next cycle.
            // But usually, we just trust the subscriptionExpiresAt date if it's in the future.
            
            return expiry.before(now)
        }
}

data class LunchVoucherRequirement(
    val id: String = UUID.randomUUID().toString(),
    val label: String = "Voucher",
    val assignedGamesRequired: Int = 0,
    val completedGamesRequired: Int = 0,
    val maxPrints: Int = 1
)

data class MatchScheduleRequirement(
    val maxPrints: Int = 3
)

data class MatchReportRequirement(
    val headRefereeOnly: Boolean = true,
    val includeReferenceSheet: Boolean = false,
    val referenceSheetMode: String = "EveryTime" // "Once", "EveryTime"
)

data class YouthRefereeRequirements(
    val minDivisionGapHead: Int = 2,
    val minDivisionGapAR: Int = 1
)

data class PrintStationRequirements(
    val lunchVouchers: List<LunchVoucherRequirement> = listOf(LunchVoucherRequirement(label = "Primary Voucher")),
    val matchSchedule: MatchScheduleRequirement = MatchScheduleRequirement(),
    val matchReport: MatchReportRequirement = MatchReportRequirement()
)
