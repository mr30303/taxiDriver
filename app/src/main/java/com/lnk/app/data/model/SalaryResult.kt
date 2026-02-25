package com.lnk.app.data.model

data class SalaryResult(
    val monthlyIncome: Long = 0L,
    val monthlyTollFee: Long = 0L,
    val recognitionTotal: Long = 0L,
    val annualDays: Int = 0,
    val holidayDays: Int = 0,
    val isFullAttendance: Boolean = false,
    val excessIncome: Long = 0L,
    val performanceBonus: Double = 0.0,
    val dailyDutyAllowance: Long = 0L,
    val recognizedDays: Double = 0.0,
    val dutyAllowance: Double = 0.0,
    val hasAccident: Boolean = false,
    val totalPretax: Long = 0L
)
