package com.lnk.app.data.model

data class SalarySetting(
    val monthlyQuota: Long = 4_550_000L,
    val bonusRatio: Double = 0.6,
    val baseSalary: Long = 0L,
    val fullAttendanceDays: Int = 26,
    val fullAttendanceDutyAllowance: Long = 0L,
    val bonusAmount: Long = 0L,
    val safeAllowance: Long = 0L,
    val affiliateAllowance: Long = 0L,
    val annualAllowancePerDay: Long = 115_000L,
    val holidayAllowancePerDay: Long = 87_000L
)
