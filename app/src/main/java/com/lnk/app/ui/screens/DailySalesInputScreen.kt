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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
    salaryViewModel: SalaryViewModel
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
            CenterAlignedTopAppBar(
                title = { Text("일 매출 입력", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F8F8))
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFFFFC107))
                        Spacer(Modifier.width(8.dp))
                        Text("오늘의 운행 기록", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }

                    DateSelectorField(
                        date = date,
                        onDateSelected = { date = it },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                            modifier = Modifier.padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            WorkType.values().forEach { type ->
                                FilterChip(
                                    selected = workType == type,
                                    onClick = { workType = type },
                                    label = { Text(type.label) },
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
                            .clickable { hasAccident = !hasAccident }
                    ) {
                        Checkbox(
                            checked = hasAccident,
                            onCheckedChange = { hasAccident = it },
                            colors = CheckboxDefaults.colors(checkedColor = Color.Red)
                        )
                        Text("운행 중 사고 발생", color = if (hasAccident) Color.Red else Color.Black)
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
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFC107),
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("기록 추가하기", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "${selectedMonth.year}년 ${selectedMonth.monthValue}월 운행 내역",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    "합계: ${formatWithComma(selectedMonthIncome)}원",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )
            }

            if (selectedMonthSales.isEmpty()) {
                Text(
                    text = "${selectedMonth.year}년 ${selectedMonth.monthValue}월 운행 내역이 없습니다.",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
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
        shape = RoundedCornerShape(16.dp),
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
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF5F5F5)),
                contentAlignment = Alignment.Center
            ) {
                Text("${index + 1}", fontWeight = FontWeight.Bold, color = Color.Gray)
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.date, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = Color(0xFFE3F2FD),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            item.workType.label,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF1976D2)
                        )
                    }
                }
                Text(
                    text = "실입금 ${formatWithComma(item.amount)}원 · 톨 ${formatWithComma(item.tollFee)}원",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.DarkGray
                )
                if (item.hasAccident) {
                    Text("사고 발생", color = Color.Red, style = MaterialTheme.typography.labelSmall)
                }
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "삭제", tint = Color.LightGray)
            }
        }
    }
}

private fun String.toLocalDateOrNull(): LocalDate? = runCatching { LocalDate.parse(this) }.getOrNull()
