package com.lnk.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lnk.app.data.model.SalarySetting
import com.lnk.app.salary.SalaryViewModel
import com.lnk.app.ui.format.CommaVisualTransformation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalarySettingScreen(
    salaryViewModel: SalaryViewModel,
    onBack: () -> Unit
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

    LaunchedEffect(uiState.errorMessage) {
        val message = uiState.errorMessage ?: return@LaunchedEffect
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        salaryViewModel.clearErrorMessage()
    }

    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Settings, contentDescription = null, tint = Color(0xFFFBC02D), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("정산 설정", fontWeight = FontWeight.ExtraBold)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.White
                    )
                )
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFEEEEEE)))
            }
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = Color.White
            ) {
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
                        Toast.makeText(context, "설정이 저장되었습니다.", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFC107),
                        contentColor = Color.Black
                    )
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("설정 저장하기", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F8F8))
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            SettingSection(title = "기본 급여 및 성과급", icon = Icons.Default.AttachMoney) {
                NumberField("기준금(사납금)", monthlyQuota) { monthlyQuota = it }
                DecimalField("성과급 비율 (예: 0.6)", bonusRatio) { bonusRatio = it }
                NumberField("기본급", baseSalary) { baseSalary = it }
            }

            SettingSection(title = "근태 및 수당", icon = Icons.Default.Event) {
                NumberField("만근 기준 일수", fullAttendanceDays) { fullAttendanceDays = it }
                NumberField("만근 기준 월 승무수당", fullAttendanceDutyAllowance) { fullAttendanceDutyAllowance = it }
                NumberField("상여금", bonusAmount) { bonusAmount = it }
            }

            SettingSection(title = "기타 수당", icon = Icons.Default.AddCircle) {
                NumberField("무사고 수당", safeAllowance) { safeAllowance = it }
                NumberField("가맹 수당", affiliateAllowance) { affiliateAllowance = it }
                NumberField("연차 인정금(1일)", annualAllowancePerDay) { annualAllowancePerDay = it }
                NumberField("법정공휴일 인정금(1일)", holidayAllowancePerDay) { holidayAllowancePerDay = it }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SettingSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFFFBC02D),
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF333333)
            )
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                content()
            }
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
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        visualTransformation = CommaVisualTransformation(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFFFFC107),
            focusedLabelColor = Color(0xFFFFC107)
        )
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
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        visualTransformation = VisualTransformation.None,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFFFFC107),
            focusedLabelColor = Color(0xFFFFC107)
        )
    )
}
