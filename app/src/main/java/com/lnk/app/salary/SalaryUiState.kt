package com.lnk.app.salary

import com.lnk.app.data.model.DailySales
import com.lnk.app.data.model.SalaryResult
import com.lnk.app.data.model.SalarySetting

data class SalaryUiState(
    val setting: SalarySetting = SalarySetting(),
    val dailySales: List<DailySales> = emptyList(),
    val result: SalaryResult? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
