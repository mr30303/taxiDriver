package com.lnk.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lnk.app.navigation.Route

data class MainMenuItem(
    val title: String,
    val route: String,
    val icon: ImageVector,
    val color: Color
)

@Composable
fun MainScreen(
    onNavigate: (String) -> Unit
) {
    val menuItems = listOf(
        MainMenuItem("일 매출 입력", Route.DailySalesInput.route, Icons.Default.Edit, Color(0xFF4CAF50)),
        MainMenuItem("화장실 지도", Route.ToiletMap.route, Icons.Default.Place, Color(0xFFFF5722)),
        MainMenuItem("급여 분석 리포트", Route.SalaryResult.route, Icons.Default.DateRange, Color(0xFF2196F3)),
        MainMenuItem("커뮤니티", Route.Comment.route, Icons.Default.Send, Color(0xFF9C27B0)),
        MainMenuItem("급여 설정", Route.SalarySetting.route, Icons.Default.Settings, Color(0xFFFFC107))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .padding(20.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "반갑습니다!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        
        Text(
            text = "오늘도 안전운행 하세요.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(32.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(menuItems) { item ->
                MenuCard(item) { onNavigate(item.route) }
            }
        }
    }
}

@Composable
fun MenuCard(item: MainMenuItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(item.color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = item.color,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.DarkGray
            )
        }
    }
}
