package com.lnk.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import com.lnk.app.data.model.DailySales
import com.lnk.app.data.model.WorkType
import com.lnk.app.salary.SalaryViewModel
import com.lnk.app.ui.format.CommaVisualTransformation
import com.lnk.app.ui.format.formatWithComma
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailySalesInputScreen(
    salaryViewModel: SalaryViewModel,
    onBack: () -> Unit
) {
    val uiState by salaryViewModel.uiState.collectAsState()
    val dailySales = uiState.dailySales
    val context = LocalContext.current

    val today = remember { LocalDate.now() }

    var date by rememberSaveable { mutableStateOf(today.toString()) }
    var amount by rememberSaveable { mutableStateOf("") }
    var tollFee by rememberSaveable { mutableStateOf("") }
    var workType by remember { mutableStateOf(WorkType.NORMAL) }
    var hasAccident by rememberSaveable { mutableStateOf(false) }
    val selectedDate = remember(date) { date.toLocalDateOrNull() }
    val selectedMonth = remember(selectedDate, today) {
        YearMonth.from(selectedDate ?: today)
    }

    val selectedMonthSales = remember(dailySales, selectedMonth) {
        dailySales.mapIndexedNotNull { index, item ->
            val parsedDate = item.date.toLocalDateOrNull() ?: return@mapIndexedNotNull null
            if (YearMonth.from(parsedDate) == selectedMonth) index to item else null
        }.sortedByDescending { (_, item) -> item.date }
    }
    val selectedMonthIncome = remember(selectedMonthSales) {
        selectedMonthSales.sumOf { (_, item) -> item.amount }
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
                            Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("매출 관리", fontWeight = FontWeight.ExtraBold)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
                        }
                    },
                    actions = {
                        Column(
                            modifier = Modifier.padding(end = 16.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text("이번 달 합계", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text("${formatWithComma(selectedMonthIncome)}원", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFFEEEEEE))
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F8F8))
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(4.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color(0xFFFFF9C4),
                            shape = CircleShape,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFFFBC02D), modifier = Modifier.padding(8.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Text("운행 기록하기", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }

                    DateSelectorField(
                        date = date,
                        onDateSelected = { date = it },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = amount,
                            onValueChange = { amount = it.filter(Char::isDigit) },
                            label = { Text("실입금액") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            visualTransformation = CommaVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFFFC107),
                                focusedLabelColor = Color(0xFFFFC107)
                            )
                        )
                        OutlinedTextField(
                            value = tollFee,
                            onValueChange = { tollFee = it.filter(Char::isDigit) },
                            label = { Text("톨게이트") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            visualTransformation = CommaVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFFFC107),
                                focusedLabelColor = Color(0xFFFFC107)
                            )
                        )
                    }

                    Column {
                        Text("근무 형태", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                        Row(
                            modifier = Modifier.padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            WorkType.values().forEach { type ->
                                FilterChip(
                                    selected = workType == type,
                                    onClick = { workType = type },
                                    label = { Text(type.label) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFFFC107),
                                        selectedLabelColor = Color.Black
                                    )
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { hasAccident = !hasAccident }
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = hasAccident,
                            onCheckedChange = { hasAccident = it },
                            colors = CheckboxDefaults.colors(checkedColor = Color.Red)
                        )
                        Text("운행 중 사고 발생", style = MaterialTheme.typography.bodyMedium, color = if (hasAccident) Color.Red else Color.Black)
                    }

                    Button(
                        onClick = {
                            val inputDate = date.toLocalDateOrNull() ?: return@Button
                            if (inputDate.isAfter(LocalDate.now())) return@Button
                            val entry = DailySales(
                                date = inputDate.toString(),
                                amount = amount.toLongOrNull() ?: 0L,
                                tollFee = tollFee.toLongOrNull() ?: 0L,
                                workType = workType,
                                hasAccident = hasAccident
                            )
                            val added = salaryViewModel.addDailySales(entry)
                            if (!added) return@Button
                            date = LocalDate.now().toString()
                            amount = ""
                            tollFee = ""
                            workType = WorkType.NORMAL
                            hasAccident = false
                        },
                        enabled = amount.isNotBlank() && selectedDate != null && !selectedDate.isAfter(LocalDate.now()),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFC107),
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("기록 저장하기", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    }
                }
            }

            // 내역 섹션 헤더
            Row(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${selectedMonth.monthValue}월 운행 내역",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = Color.DarkGray
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = Color.LightGray.copy(alpha = 0.2f),
                    shape = CircleShape
                ) {
                    Text(
                        text = "${selectedMonthSales.size}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (selectedMonthSales.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "등록된 운행 내역이 없습니다.",
                        color = Color.LightGray,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    itemsIndexed(
                        items = selectedMonthSales,
                        key = { _, entry -> entry.first }
                    ) { index, entry ->
                        val (originalIndex, item) = entry
                        DailySalesCard(
                            index = index,
                            item = item,
                            onDelete = { salaryViewModel.removeDailySales(originalIndex) }
                        )
                    }
                }
            }

        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateSelectorField(
    date: String,
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCalendar by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = date,
            onValueChange = {},
            readOnly = true,
            label = { Text("날짜") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            trailingIcon = {
                Icon(Icons.Default.DateRange, contentDescription = "날짜 선택")
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFFFC107),
                focusedLabelColor = Color(0xFFFFC107)
            )
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(12.dp))
                .clickable { showCalendar = true }
        )
    }

    if (showCalendar) {
        CalendarDatePickerDialog(
            initialDate = date.toLocalDateOrNull() ?: LocalDate.now(),
            onDismiss = { showCalendar = false },
            onDateSelected = {
                onDateSelected(it.toString())
                showCalendar = false
            }
        )
    }
}

@Composable
private fun CalendarDatePickerDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    var selectedDate by remember(initialDate) { mutableStateOf(initialDate) }
    val today = remember { LocalDate.now() }
    val initialMonth = remember(initialDate, today) {
        val month = YearMonth.from(initialDate)
        val current = YearMonth.from(today)
        if (month.isAfter(current)) current else month
    }
    val endMonth = remember(today) { YearMonth.from(today) }
    val startMonth = remember(endMonth) { endMonth.minusMonths(24) }
    val daysOfWeek = remember { daysOfWeek(firstDayOfWeekFromLocale()) }
    val coroutineScope = rememberCoroutineScope()
    val state = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = initialMonth,
        firstDayOfWeek = daysOfWeek.first()
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        },
        title = { Text("날짜 선택") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HorizontalCalendar(
                    state = state,
                    monthHeader = { month ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                        ) {
                            Text(
                                text = "${month.yearMonth.year}년 ${month.yearMonth.monthValue}월",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                TextButton(
                                    onClick = {
                                        val target = month.yearMonth.minusYears(1)
                                        if (!target.isBefore(startMonth)) {
                                            coroutineScope.launch { state.animateScrollToMonth(target) }
                                        }
                                    }
                                ) { Text("<<") }
                                TextButton(
                                    onClick = {
                                        val target = month.yearMonth.minusMonths(1)
                                        if (!target.isBefore(startMonth)) {
                                            coroutineScope.launch { state.animateScrollToMonth(target) }
                                        }
                                    }
                                ) { Text("<") }
                                TextButton(
                                    onClick = {
                                        val target = month.yearMonth.plusMonths(1)
                                        if (!target.isAfter(endMonth)) {
                                            coroutineScope.launch { state.animateScrollToMonth(target) }
                                        }
                                    }
                                ) { Text(">") }
                                TextButton(
                                    onClick = {
                                        val target = month.yearMonth.plusYears(1)
                                        if (!target.isAfter(endMonth)) {
                                            coroutineScope.launch { state.animateScrollToMonth(target) }
                                        }
                                    }
                                ) { Text(">>") }
                            }
                            Row(modifier = Modifier.fillMaxWidth()) {
                                daysOfWeek.forEach { dayOfWeek ->
                                    Text(
                                        text = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREA),
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    },
                    dayContent = { day ->
                        CalendarDayCell(
                            day = day,
                            today = today,
                            selectedDate = selectedDate,
                            onClick = {
                                selectedDate = it
                                onDateSelected(it)
                            }
                        )
                    }
                )
            }
        }
    )
}

@Composable
private fun CalendarDayCell(
    day: CalendarDay,
    today: LocalDate,
    selectedDate: LocalDate,
    onClick: (LocalDate) -> Unit
) {
    val isFuture = day.date.isAfter(today)
    val isSelectable = day.position == DayPosition.MonthDate && !isFuture
    val isSelected = isSelectable && day.date == selectedDate
    val isToday = isSelectable && day.date == today

    val backgroundColor = when {
        isSelected -> Color(0xFFFFC107)
        isToday -> Color(0xFFFFF3CD)
        else -> Color.Transparent
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .clickable(enabled = isSelectable) { onClick(day.date) },
        contentAlignment = Alignment.Center
    ) {
        if (isSelectable) {
            Text(
                text = day.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isSelected -> Color.Black
                    isFuture -> Color.LightGray
                    else -> Color.DarkGray
                }
            )
        }
    }
}

@Composable
private fun DailySalesCard(
    index: Int,
    item: DailySales,
    onDelete: () -> Unit
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF5F5F5)),
                contentAlignment = Alignment.Center
            ) {
                Text("${index + 1}", fontWeight = FontWeight.Bold, color = Color.Gray)
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.date, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color(0xFF333333))
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = Color(0xFFE3F2FD),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            item.workType.label,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF1976D2),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "실입금 ${formatWithComma(item.amount)}원",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                    Text(
                        text = " · 톨게이트 ${formatWithComma(item.tollFee)}원",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                if (item.hasAccident) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Color.Red, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("사고 발생 기록됨", color = Color.Red, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFF1F1))
            ) {
                Icon(Icons.Default.Delete, contentDescription = "삭제", tint = Color(0xFFFF5252), modifier = Modifier.size(18.dp))
            }
        }
    }
}

private fun String.toLocalDateOrNull(): LocalDate? = runCatching { LocalDate.parse(this) }.getOrNull()
