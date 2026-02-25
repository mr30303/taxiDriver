package com.lnk.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lnk.app.navigation.Route

@Composable
fun MainScreen(
    onNavigate: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "메인", style = MaterialTheme.typography.headlineSmall)
        Button(onClick = { onNavigate(Route.SalarySetting.route) }) {
            Text(text = "급여 설정")
        }
        Button(onClick = { onNavigate(Route.DailySalesInput.route) }) {
            Text(text = "일 매출 입력")
        }
        Button(onClick = { onNavigate(Route.SalaryResult.route) }) {
            Text(text = "급여 결과")
        }
        Button(onClick = { onNavigate(Route.ToiletMap.route) }) {
            Text(text = "화장실 지도")
        }
        Button(onClick = { onNavigate(Route.Comment.route) }) {
            Text(text = "댓글")
        }
    }
}
