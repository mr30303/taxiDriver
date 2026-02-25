package com.lnk.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lnk.app.data.model.WorkType
import com.lnk.app.domain.SalaryCalculator
import com.lnk.app.salary.SalaryViewModel
import com.lnk.app.ui.format.formatWithComma
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalaryResultScreen(
    salaryViewModel: SalaryViewModel
) {
    val uiState by salaryViewModel.uiState.collectAsState()
    val currentMonth = remember { YearMonth.now() }

    val availableMonths = remember(uiState.dailySales, currentMonth) {
        val months = uiState.dailySales
            .mapNotNull { it.date.toLocalDateOrNull() }
            .map { YearMonth.from(it) }
            .distinct()
            .sortedDescending()
        if (months.isEmpty()) listOf(currentMonth)
        else if (currentMonth in months) months else listOf(currentMonth) + months
    }

    var selectedMonthValue by rememberSaveable { mutableStateOf(currentMonth.toString()) }
    val selectedMonth = selectedMonthValue.toYearMonthOrNull() ?: availableMonths.first()

    LaunchedEffect(availableMonths) {
        if (availableMonths.none { it.toString() == selectedMonthValue }) {
            selectedMonthValue = availableMonths.first().toString()
        }
    }

    val monthlySales = remember(uiState.dailySales, selectedMonth) {
        uiState.dailySales.filter { sale ->
            sale.date.toLocalDateOrNull()?.let { YearMonth.from(it) == selectedMonth } == true
        }
    }

    val monthlyResult = remember(uiState.setting, monthlySales) {
        if (monthlySales.isEmpty()) null else SalaryCalculator.calculate(uiState.setting, monthlySales)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("급여 분석 리포트", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F8F8))
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            MonthSelector(
                selectedMonth = selectedMonth,
                months = availableMonths,
                onMonthSelected = { selectedMonthValue = it.toString() }
            )

            if (monthlyResult == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.LightGray
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "${selectedMonth.year}년 ${selectedMonth.monthValue}월 운행 데이터가 없습니다.",
                            color = Color.Gray
                        )
                    }
                }
                return@Scaffold
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${selectedMonth.year}년 ${selectedMonth.monthValue}월 예상 월급 (세전)",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "${formatWithComma(monthlyResult.totalPretax)}원",
                        color = Color(0xFFFFC107),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ResultSummaryItem(
                            label = "운행 일수",
                            value = "${monthlySales.size}일",
                            icon = Icons.Default.DateRange
                        )
                        ResultSummaryItem(
                            label = "사고 건수",
                            value = "${monthlySales.count { it.hasAccident }}건",
                            icon = Icons.Default.Warning
                        )
                    }
                }
            }

            Text(
                text = "상세 내역",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(start = 4.dp)
            )

            DetailResultCard(
                title = "월 실입금 합계",
                amount = monthlyResult.monthlyIncome,
                icon = Icons.Default.ShoppingCart,
                iconColor = Color(0xFF4CAF50)
            )

            DetailResultCard(
                title = "월 톨게이트비 합계",
                amount = monthlyResult.monthlyTollFee,
                icon = Icons.Default.Place,
                iconColor = Color(0xFF2196F3)
            )

            AchievementRateCard(
                monthlyQuota = uiState.setting.monthlyQuota,
                monthlyIncome = monthlyResult.monthlyIncome,
                attendanceDays = monthlySales.count {
                    it.workType == WorkType.NORMAL || it.workType == WorkType.HOLIDAY
                },
                fullAttendanceDays = uiState.setting.fullAttendanceDays,
                isFullAttendance = monthlyResult.isFullAttendance
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9C4))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFBC02D))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "수고하셨습니다! 다음 달도 안전운행 하세요.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF5D4037),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MonthSelector(
    selectedMonth: YearMonth,
    months: List<YearMonth>,
    onMonthSelected: (YearMonth) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DateRange, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("조회 월 선택", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            }
            
            Box {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { expanded = true }
                        .border(
                            width = 1.dp,
                            color = if (expanded) Color(0xFFFFC107) else Color(0xFFE0E0E0),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    color = Color.Transparent
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${selectedMonth.year}년 ${selectedMonth.monthValue}월",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "월 선택",
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.rotate(if (expanded) 180f else 0f)
                        )
                    }
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier
                        .fillMaxWidth(0.9f) // 화면 너비에 맞춤
                        .background(Color.White)
                        .padding(vertical = 8.dp)
                ) {
                    months.forEach { month ->
                        val isSelected = month == selectedMonth
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = null,
                                        tint = if (isSelected) Color(0xFFFFC107) else Color.LightGray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = "${month.year}년 ${month.monthValue}월",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.Black else Color.DarkGray
                                    )
                                }
                            },
                            trailingIcon = {
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = "선택됨", tint = Color(0xFFFFC107))
                                }
                            },
                            onClick = {
                                onMonthSelected(month)
                                expanded = false
                            },
                            modifier = Modifier
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFFFFF9C4) else Color.Transparent)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ResultSummaryItem(label: String, value: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(text = label, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
        Spacer(Modifier.width(8.dp))
        Text(text = value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
fun DetailResultCard(
    title: String,
    amount: Long,
    icon: ImageVector,
    iconColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text(text = title, fontWeight = FontWeight.Medium, color = Color.DarkGray)
            }
            Text(
                text = "${formatWithComma(amount)}원",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.Black
            )
        }
    }
}

@Composable
private fun AchievementRateCard(
    monthlyQuota: Long,
    monthlyIncome: Long,
    attendanceDays: Int,
    fullAttendanceDays: Int,
    isFullAttendance: Boolean
) {
    val quotaRate = if (monthlyQuota > 0) {
        (monthlyIncome.toDouble() / monthlyQuota).toFloat()
    } else {
        0f
    }
    val fullAttendanceRate = if (fullAttendanceDays > 0) {
        attendanceDays.toFloat() / fullAttendanceDays
    } else {
        0f
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "목표 달성 현황",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (monthlyQuota <= 0L) {
                Text(
                    text = "기준금(사납금) 설정이 필요합니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                return@Column
            }

            AchievementGraph(
                label = "사납금 달성률",
                progress = quotaRate,
                color = Color(0xFFFFC107),
                currentValue = "${formatWithComma(monthlyIncome)}원",
                targetValue = "${formatWithComma(monthlyQuota)}원"
            )

            AchievementGraph(
                label = "만근 달성률",
                progress = fullAttendanceRate,
                color = Color(0xFF2196F3),
                currentValue = "${attendanceDays}일",
                targetValue = "${fullAttendanceDays}일"
            )

            Text(
                text = "기준금 만근 판정: ${if (isFullAttendance) "달성" else "미달"} (실입금+인정금 기준)",
                style = MaterialTheme.typography.bodySmall,
                color = if (isFullAttendance) Color(0xFF2E7D32) else Color.Gray,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun AchievementGraph(
    label: String,
    progress: Float,
    color: Color,
    currentValue: String,
    targetValue: String
) {
    val animatedProgress by animateFloatAsState(
        targetValue = kotlin.math.min(1f, progress),
        animationSpec = tween(durationMillis = 1000)
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.DarkGray)
            Text(
                text = String.format(Locale.KOREA, "%.1f%%", progress * 100),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = color
            )
        }
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(CircleShape)
                .background(Color(0xFFF0F0F0))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(
                        androidx.compose.ui.graphics.Brush.horizontalGradient(
                            colors = listOf(color.copy(alpha = 0.7f), color)
                        )
                    )
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "현재: $currentValue", fontSize = 11.sp, color = Color.Gray)
            Text(text = "목표: $targetValue", fontSize = 11.sp, color = Color.Gray)
        }
    }
}

private fun String.toLocalDateOrNull(): LocalDate? = runCatching { LocalDate.parse(this) }.getOrNull()
private fun String.toYearMonthOrNull(): YearMonth? = runCatching { YearMonth.parse(this) }.getOrNull()
private fun formatPercent(value: Double): String = String.format(Locale.KOREA, "%.1f%%", value)
