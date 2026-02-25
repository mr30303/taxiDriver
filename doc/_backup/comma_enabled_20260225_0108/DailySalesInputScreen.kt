package com.lnk.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.lnk.app.data.model.DailySales
import com.lnk.app.data.model.WorkType
import com.lnk.app.salary.SalaryViewModel
import com.lnk.app.ui.format.CommaVisualTransformation
import com.lnk.app.ui.format.formatWithComma

@Composable
fun DailySalesInputScreen(
    salaryViewModel: SalaryViewModel
) {
    val uiState by salaryViewModel.uiState.collectAsState()
    val dailySales = uiState.dailySales

    var date by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    var tollFee by rememberSaveable { mutableStateOf("") }
    var workType by remember { mutableStateOf(WorkType.NORMAL) }
    var hasAccident by rememberSaveable { mutableStateOf(false) }
    var workTypeExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "일 매출 입력", style = MaterialTheme.typography.headlineSmall)
        Text(text = "톨게이트비는 월 합계만 표시됩니다.", style = MaterialTheme.typography.bodySmall)

        OutlinedTextField(
            value = date,
            onValueChange = { value -> date = value },
            label = { Text("날짜 (YYYY-MM-DD)") },
            singleLine = true
        )
        OutlinedTextField(
            value = amount,
            onValueChange = { value -> amount = value.filter(Char::isDigit) },
            label = { Text("일 실입금") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            visualTransformation = CommaVisualTransformation()
        )
        OutlinedTextField(
            value = tollFee,
            onValueChange = { value -> tollFee = value.filter(Char::isDigit) },
            label = { Text("톨게이트비") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            visualTransformation = CommaVisualTransformation()
        )

        Column {
            Text(text = "근무 형태", style = MaterialTheme.typography.labelMedium)
            TextButton(onClick = { workTypeExpanded = true }) {
                Text(text = workType.label)
            }
            DropdownMenu(
                expanded = workTypeExpanded,
                onDismissRequest = { workTypeExpanded = false }
            ) {
                WorkType.values().forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type.label) },
                        onClick = {
                            workType = type
                            workTypeExpanded = false
                        }
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Checkbox(
                checked = hasAccident,
                onCheckedChange = { hasAccident = it }
            )
            Text(text = "사고 있음")
        }

        Button(
            onClick = {
                val entry = DailySales(
                    date = date,
                    amount = amount.toLongOrNull() ?: 0L,
                    tollFee = tollFee.toLongOrNull() ?: 0L,
                    workType = workType,
                    hasAccident = hasAccident
                )
                salaryViewModel.addDailySales(entry)
                date = ""
                amount = ""
                tollFee = ""
                workType = WorkType.NORMAL
                hasAccident = false
            },
            enabled = amount.isNotBlank()
        ) {
            Text(text = "추가")
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(dailySales) { index, item ->
                DailySalesRow(
                    index = index,
                    item = item,
                    onDelete = { salaryViewModel.removeDailySales(index) }
                )
            }
        }

        if (uiState.result != null) {
            Text(
                text = "월 실입금 합계: ${formatWithComma(uiState.result!!.monthlyIncome)}원",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "월 톨게이트비 합계: ${formatWithComma(uiState.result!!.monthlyTollFee)}원",
                style = MaterialTheme.typography.bodyMedium
            )
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
private fun DailySalesRow(
    index: Int,
    item: DailySales,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "(${index + 1}) ${item.date} / ${item.workType.label}")
            Text(text = "실입금 ${formatWithComma(item.amount)}원, 톨 ${formatWithComma(item.tollFee)}원")
            Text(text = if (item.hasAccident) "사고 있음" else "사고 없음")
        }
        TextButton(onClick = onDelete, modifier = Modifier.size(64.dp)) {
            Text(text = "삭제")
        }
    }
}
