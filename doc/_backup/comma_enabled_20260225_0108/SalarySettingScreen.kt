package com.lnk.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import com.lnk.app.data.model.SalarySetting
import com.lnk.app.salary.SalaryViewModel
import com.lnk.app.ui.format.CommaVisualTransformation

@Composable
fun SalarySettingScreen(
    salaryViewModel: SalaryViewModel
) {
    val uiState by salaryViewModel.uiState.collectAsState()
    val setting = uiState.setting
    val context = LocalContext.current

    var monthlyQuota by rememberSaveable { mutableStateOf(setting.monthlyQuota.toString()) }
    var bonusRatio by rememberSaveable { mutableStateOf(setting.bonusRatio.toString()) }
    var baseSalary by rememberSaveable { mutableStateOf(setting.baseSalary.toString()) }
    var fullAttendanceDays by rememberSaveable { mutableStateOf(setting.fullAttendanceDays.toString()) }
    var fullAttendanceDutyAllowance by rememberSaveable {
        mutableStateOf(setting.fullAttendanceDutyAllowance.toString())
    }
    var bonusAmount by rememberSaveable { mutableStateOf(setting.bonusAmount.toString()) }
    var safeAllowance by rememberSaveable { mutableStateOf(setting.safeAllowance.toString()) }
    var affiliateAllowance by rememberSaveable { mutableStateOf(setting.affiliateAllowance.toString()) }
    var annualAllowancePerDay by rememberSaveable { mutableStateOf(setting.annualAllowancePerDay.toString()) }
    var holidayAllowancePerDay by rememberSaveable { mutableStateOf(setting.holidayAllowancePerDay.toString()) }

    LaunchedEffect(setting) {
        monthlyQuota = setting.monthlyQuota.toString()
        bonusRatio = setting.bonusRatio.toString()
        baseSalary = setting.baseSalary.toString()
        fullAttendanceDays = setting.fullAttendanceDays.toString()
        fullAttendanceDutyAllowance = setting.fullAttendanceDutyAllowance.toString()
        bonusAmount = setting.bonusAmount.toString()
        safeAllowance = setting.safeAllowance.toString()
        affiliateAllowance = setting.affiliateAllowance.toString()
        annualAllowancePerDay = setting.annualAllowancePerDay.toString()
        holidayAllowancePerDay = setting.holidayAllowancePerDay.toString()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "급여 설정", style = MaterialTheme.typography.headlineSmall)
        Text(text = "기본값을 수정한 뒤 저장하세요.", style = MaterialTheme.typography.bodySmall)

        NumberField("기준금(사납금)", monthlyQuota) { monthlyQuota = it }
        DecimalField("성과급 비율 (예: 0.6)", bonusRatio) { bonusRatio = it }
        NumberField("기본급", baseSalary) { baseSalary = it }
        NumberField("만근 기준 일수", fullAttendanceDays) { fullAttendanceDays = it }
        NumberField("만근 기준 월 승무수당", fullAttendanceDutyAllowance) { fullAttendanceDutyAllowance = it }
        NumberField("상여금", bonusAmount) { bonusAmount = it }
        NumberField("무사고 수당", safeAllowance) { safeAllowance = it }
        NumberField("가맹 수당", affiliateAllowance) { affiliateAllowance = it }
        NumberField("연차 인정금(1일)", annualAllowancePerDay) { annualAllowancePerDay = it }
        NumberField("법정공휴일 인정금(1일)", holidayAllowancePerDay) { holidayAllowancePerDay = it }

        Button(
            onClick = {
                val updated = SalarySetting(
                    monthlyQuota = monthlyQuota.toLongOrNull() ?: 0L,
                    bonusRatio = bonusRatio.toDoubleOrNull() ?: 0.0,
                    baseSalary = baseSalary.toLongOrNull() ?: 0L,
                    fullAttendanceDays = fullAttendanceDays.toIntOrNull() ?: 0,
                    fullAttendanceDutyAllowance = fullAttendanceDutyAllowance.toLongOrNull() ?: 0L,
                    bonusAmount = bonusAmount.toLongOrNull() ?: 0L,
                    safeAllowance = safeAllowance.toLongOrNull() ?: 0L,
                    affiliateAllowance = affiliateAllowance.toLongOrNull() ?: 0L,
                    annualAllowancePerDay = annualAllowancePerDay.toLongOrNull() ?: 0L,
                    holidayAllowancePerDay = holidayAllowancePerDay.toLongOrNull() ?: 0L
                )
                salaryViewModel.saveSetting(updated)
                Toast.makeText(context, "저장되었습니다.", Toast.LENGTH_SHORT).show()
            },
            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 20.dp)
        ) {
            Text(text = "저장")
        }
        if (!uiState.errorMessage.isNullOrBlank()) {
            Text(
                text = uiState.errorMessage ?: "",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun NumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = { text -> onValueChange(text.filter(Char::isDigit)) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        visualTransformation = CommaVisualTransformation()
    )
}

@Composable
private fun DecimalField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = { text -> onValueChange(text) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        visualTransformation = VisualTransformation.None
    )
}
