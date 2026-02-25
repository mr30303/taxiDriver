package com.lnk.app.domain

import com.lnk.app.data.model.DailySales
import com.lnk.app.data.model.SalaryResult
import com.lnk.app.data.model.SalarySetting
import com.lnk.app.data.model.WorkType
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

object SalaryCalculator {
    fun calculate(setting: SalarySetting, dailySales: List<DailySales>): SalaryResult {
        val monthlyIncome = dailySales.sumOf { it.amount }
        val monthlyTollFee = dailySales.sumOf { it.tollFee }
        val annualDays = dailySales.count { it.workType == WorkType.ANNUAL }
        val holidayDays = dailySales.count { it.workType == WorkType.HOLIDAY }
        val recognitionTotal =
            annualDays * setting.annualAllowancePerDay + holidayDays * setting.holidayAllowancePerDay

        val totalForQuota = monthlyIncome + recognitionTotal
        val isFullAttendance = totalForQuota >= setting.monthlyQuota

        val excessIncome = totalForQuota - setting.monthlyQuota
        val performanceBonus = if (excessIncome > 0) {
            excessIncome.toDouble() * setting.bonusRatio
        } else {
            0.0
        }

        val dailyDutyAllowance = if (setting.fullAttendanceDays > 0) {
            roundToHundred(setting.fullAttendanceDutyAllowance.toDouble() / setting.fullAttendanceDays)
        } else {
            0L
        }

        val recognizedDaysRaw = if (setting.monthlyQuota > 0) {
            setting.fullAttendanceDays * (totalForQuota.toDouble() / setting.monthlyQuota)
        } else {
            0.0
        }
        val recognizedDays = min(setting.fullAttendanceDays.toDouble(), max(0.0, recognizedDaysRaw))
        val dutyAllowance = dailyDutyAllowance * recognizedDays

        val hasAccident = dailySales.any { it.hasAccident }
        val fixedAllowances = setting.baseSalary +
            if (isFullAttendance) setting.bonusAmount + setting.affiliateAllowance else 0L +
            if (isFullAttendance && !hasAccident) setting.safeAllowance else 0L

        val totalPretaxRaw = fixedAllowances + performanceBonus + dutyAllowance
        val totalPretax = roundToHundred(totalPretaxRaw)

        return SalaryResult(
            monthlyIncome = monthlyIncome,
            monthlyTollFee = monthlyTollFee,
            recognitionTotal = recognitionTotal,
            annualDays = annualDays,
            holidayDays = holidayDays,
            isFullAttendance = isFullAttendance,
            excessIncome = max(0L, excessIncome),
            performanceBonus = performanceBonus,
            dailyDutyAllowance = dailyDutyAllowance,
            recognizedDays = recognizedDays,
            dutyAllowance = dutyAllowance,
            hasAccident = hasAccident,
            totalPretax = totalPretax
        )
    }

    fun roundToHundred(value: Double): Long {
        return (round(value / 100.0) * 100).toLong()
    }
}
