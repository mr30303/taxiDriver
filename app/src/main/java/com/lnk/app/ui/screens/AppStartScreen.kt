package com.lnk.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "택시노트", style = MaterialTheme.typography.headlineMedium)
        Text(
            text = if (uiState.isLoading) "로그인 상태 확인 중..." else "이동 중...",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
