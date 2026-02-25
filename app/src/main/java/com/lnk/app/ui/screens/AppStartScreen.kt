package com.lnk.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lnk.app.R
import com.lnk.app.auth.AuthUiState

@Composable
fun AppStartScreen(
    uiState: AuthUiState,
    onNavigateToLogin: () -> Unit,
    onNavigateToMain: () -> Unit
) {
    LaunchedEffect(uiState.isLoading, uiState.userId) {
        if (!uiState.isLoading) {
            if (uiState.userId.isNullOrBlank()) {
                onNavigateToLogin()
            } else {
                onNavigateToMain()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A)) // 어두운 배경으로 로고 강조
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 앱 로고
        Image(
            painter = painterResource(id = R.drawable.ic_logo),
            contentDescription = "Taxi App Logo",
            modifier = Modifier.size(140.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "택시노트",
            style = MaterialTheme.typography.headlineLarge,
            color = Color(0xFFFFC107), // 택시 옐로우
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
        
        Text(
            text = "당신의 운행 파트너",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(64.dp))

        if (uiState.isLoading) {
            CircularProgressIndicator(
                color = Color(0xFFFFC107),
                strokeWidth = 3.dp,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = "로그인 상태 확인 중...",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}
