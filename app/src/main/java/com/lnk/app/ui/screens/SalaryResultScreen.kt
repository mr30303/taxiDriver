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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
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
    salaryViewModel: SalaryViewModel,
    onBack: () -> Unit
) {
    val uiState by salaryViewModel.uiState.collectAsState()
    val currentMonth = remember { YearMonth.now() }

    val availableMonths = remember(uiState.dailySales, currentMonth) {
        val months: List<YearMonth> = uiState.dailySales
            .mapNotNull { it.date.toLocalDateOrNull() }
            .map { YearMonth.from(it) }
            .distinct()
            .sortedDescending()
        
        if (months.isEmpty()) {
            listOf(currentMonth)
        } else if (months.contains(currentMonth)) {
            months
        } else {
            listOf(currentMonth) + months
        }
    }

    var selectedMonthValue by rememberSaveable { mutableStateOf(currentMonth.toString()) }
    val selectedMonth = remember(selectedMonthValue, availableMonths) {
        selectedMonthValue.toYearMonthOrNull() ?: availableMonths.first()
    }

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
            Column {
                CenterAlignedTopAppBar(
                    title = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Assessment, contentDescription = null, tint = Color(0xFF1E88E5), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("급여 분석", fontWeight = FontWeight.ExtraBold)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
                )
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFEEEEEE)))
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
                            "${selectedMonth.year}년 ${selectedMonth.monthValue}월 데이터가 없습니다.",
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                return@Scaffold
            }

            // 하이라이트 결과 카드
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${selectedMonth.monthValue}월 예상 월급 (세전)",
                        color = Color.White.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "${formatWithComma(monthlyResult.totalPretax)}원",
                        color = Color(0xFFFFC107),
                        fontSize = 36.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(Modifier.height(20.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    Spacer(Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ResultSummaryItem(
                            label = "운행",
                            value = "${monthlySales.size}일",
                            icon = Icons.Default.DateRange
                        )
                        Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.White.copy(alpha = 0.1f)))
                        ResultSummaryItem(
                            label = "사고",
                            value = "${monthlySales.count { it.hasAccident }}건",
                            icon = Icons.Default.Warning,
                            iconColor = if (monthlySales.any { it.hasAccident }) Color(0xFFFF5252) else Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            Text(
                text = "세부 항목",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = Color(0xFF333333),
                modifier = Modifier.padding(start = 4.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DetailResultCard(
                    title = "월 실입금 합계",
                    amount = monthlyResult.monthlyIncome,
                    icon = Icons.Default.AccountBalance,
                    iconColor = Color(0xFF4CAF50)
                )

                DetailResultCard(
                    title = "월 톨게이트비 합계",
                    amount = monthlyResult.monthlyTollFee,
                    icon = Icons.Default.Place,
                    iconColor = Color(0xFF2196F3)
                )
            }

            AchievementRateCard(
                monthlyQuota = uiState.setting.monthlyQuota,
                monthlyIncome = monthlyResult.monthlyIncome,
                attendanceDays = monthlySales.count { sale ->
                    sale.workType == WorkType.NORMAL || sale.workType == WorkType.HOLIDAY
                },
                fullAttendanceDays = uiState.setting.fullAttendanceDays,
                isFullAttendance = monthlyResult.isFullAttendance
            )

            // 격려 문구 카드
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF43A047))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "이달의 운행 완료! 항상 안전운행 하세요.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF2E7D32),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
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
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DateRange, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("정산 월 선택", style = MaterialTheme.typography.labelMedium, color = Color.Gray, fontWeight = FontWeight.Bold)
            }
            
            Box {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { expanded = true }
                        .border(
                            width = 1.dp,
                            color = if (expanded) Color(0xFFFFC107) else Color(0xFFEEEEEE),
                            shape = RoundedCornerShape(14.dp)
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
                            fontWeight = FontWeight.ExtraBold,
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
                        .fillMaxWidth(0.85f)
                        .background(Color.White)
                        .padding(vertical = 4.dp)
                ) {
                    months.forEach { month ->
                        val isSelected = month == selectedMonth
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "${month.year}년 ${month.monthValue}월",
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                onMonthSelected(month)
                                expanded = false
                            },
                            modifier = Modifier.background(if (isSelected) Color(0xFFFFF9C4) else Color.Transparent)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ResultSummaryItem(
    label: String, 
    value: String, 
    icon: ImageVector,
    iconColor: Color = Color.White.copy(alpha = 0.5f)
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(text = label, color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
        Spacer(Modifier.width(6.dp))
        Text(text = value, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
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
        shape = RoundedCornerShape(20.dp),
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
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(16.dp))
                Text(text = title, fontWeight = FontWeight.Bold, color = Color(0xFF444444))
            }
            Text(
                text = "${formatWithComma(amount)}원",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
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
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "목표 및 달성률",
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            AchievementGraph(
                label = "사납금 달성",
                progress = quotaRate,
                color = Color(0xFFFFC107),
                currentValue = "${formatWithComma(monthlyIncome)}원",
                targetValue = "${formatWithComma(monthlyQuota)}원"
            )

            AchievementGraph(
                label = "만근 달성",
                progress = fullAttendanceRate,
                color = Color(0xFF2196F3),
                currentValue = "${attendanceDays}일",
                targetValue = "${fullAttendanceDays}일"
            )

            Surface(
                color = if (isFullAttendance) Color(0xFFE8F5E9) else Color(0xFFF5F5F5),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "판정: ${if (isFullAttendance) "만근 달성 (수당 지급 대상)" else "만근 미달"}",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isFullAttendance) Color(0xFF2E7D32) else Color.Gray,
                    fontWeight = FontWeight.Bold
                )
            }
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
        targetValue = kotlin.math.min(1.2f, progress), 
        animationSpec = tween(durationMillis = 1000),
        label = "AchievementProgress"
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF666666))
            Text(
                text = String.format(Locale.KOREA, "%.1f%%", progress * 100),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = color
            )
        }
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(CircleShape)
                .background(Color(0xFFF0F0F0))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(kotlin.math.min(1f, animatedProgress))
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(color.copy(alpha = 0.6f), color)
                        )
                    )
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "현재 $currentValue", fontSize = 11.sp, color = Color.Gray)
            Text(text = "목표 $targetValue", fontSize = 11.sp, color = Color.Gray)
        }
    }
}

private fun String.toLocalDateOrNull(): LocalDate? = runCatching { LocalDate.parse(this) }.getOrNull()
private fun String.toYearMonthOrNull(): YearMonth? = runCatching { YearMonth.parse(this) }.getOrNull()
