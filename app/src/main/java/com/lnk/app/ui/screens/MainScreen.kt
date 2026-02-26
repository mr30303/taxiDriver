package com.lnk.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lnk.app.navigation.Route
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class MainMenuItem(
    val title: String,
    val route: String,
    val icon: ImageVector,
    val color: Color,
    val description: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit
) {
    var isProfileMenuExpanded by remember { mutableStateOf(false) }

    val menuItems = listOf(
        MainMenuItem(
            title = "일 매출 입력",
            route = Route.DailySalesInput.route,
            icon = Icons.Default.Edit,
            color = Color(0xFFFBC02D),
            description = "오늘 수입 기록"
        ),
        MainMenuItem(
            title = "화장실 지도",
            route = Route.ToiletMap.route,
            icon = Icons.Default.Place,
            color = Color(0xFF43A047),
            description = "주변 화장실 찾기"
        ),
        MainMenuItem(
            title = "급여 분석",
            route = Route.SalaryResult.route,
            icon = Icons.Default.Assessment,
            color = Color(0xFF1E88E5),
            description = "이번 달 예상 급여 확인"
        ),
        MainMenuItem(
            title = "커뮤니티",
            route = Route.Comment.route,
            icon = Icons.Default.Chat,
            color = Color(0xFF8E24AA),
            description = "동료 기사님과 소통"
        ),
        MainMenuItem(
            title = "급여 설정",
            route = Route.SalarySetting.route,
            icon = Icons.Default.Settings,
            color = Color(0xFF757575),
            description = "정산 기준 설정"
        )
    )

    val today = LocalDate.now().format(DateTimeFormatter.ofPattern("M월 d일 (E)", Locale.KOREA))

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFFFFD54F), Color(0xFFFFB300))
                        ),
                        shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = today,
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.Black.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "안녕하세요\n기사님",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.Black,
                                lineHeight = 32.sp
                            )
                        }

                        Box {
                            Surface(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clickable { isProfileMenuExpanded = true },
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.3f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "계정 메뉴",
                                    modifier = Modifier.padding(8.dp),
                                    tint = Color.Black
                                )
                            }

                            DropdownMenu(
                                expanded = isProfileMenuExpanded,
                                onDismissRequest = { isProfileMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("로그아웃") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Logout,
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        isProfileMenuExpanded = false
                                        onLogout()
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF8F8F8))
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "주요 기능",
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.DarkGray
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(menuItems) { item ->
                    MenuCard(item = item) {
                        onNavigate(item.route)
                    }
                }
            }
        }
    }
}

@Composable
fun MenuCard(item: MainMenuItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(item.color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = item.color,
                    modifier = Modifier.size(28.dp)
                )
            }

            Column {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF333333)
                )
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    maxLines = 1
                )
            }
        }
    }
}
