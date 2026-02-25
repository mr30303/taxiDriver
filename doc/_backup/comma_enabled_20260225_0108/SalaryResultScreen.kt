package com.lnk.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lnk.app.salary.SalaryViewModel
import com.lnk.app.ui.format.formatWithComma

@Composable
fun SalaryResultScreen(
    salaryViewModel: SalaryViewModel
) {
    val uiState by salaryViewModel.uiState.collectAsState()
    val result = uiState.result

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "급여 결과", style = MaterialTheme.typography.headlineSmall)
        if (result == null) {
            Text(text = "입력 데이터가 없습니다.")
            return
        }

        Text(text = "월 실입금 합계: ${formatWithComma(result.monthlyIncome)}원")
        Text(text = "월 톨게이트비 합계: ${formatWithComma(result.monthlyTollFee)}원")
        Text(text = "세전 월급: ${formatWithComma(result.totalPretax)}원")
    }
}
