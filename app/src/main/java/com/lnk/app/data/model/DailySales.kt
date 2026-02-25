package com.lnk.app.data.model

data class DailySales(
    val userId: String = "",
    val date: String = "",
    val amount: Long = 0L,
    val tollFee: Long = 0L,
    val workType: WorkType = WorkType.NORMAL,
    val hasAccident: Boolean = false
)
