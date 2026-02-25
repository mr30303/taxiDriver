package com.lnk.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lnk.app.R
import com.lnk.app.auth.AuthViewModel

@Composable
fun SignUpScreen(
    authViewModel: AuthViewModel,
    onLogin: () -> Unit,
    onSuccess: () -> Unit
) {
    val uiState by authViewModel.uiState.collectAsState()
    val context = LocalContext.current
    var email: String by rememberSaveable { mutableStateOf("") }
    var password: String by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(uiState.userId) {
        if (!uiState.userId.isNullOrBlank()) {
            onSuccess()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        val message = uiState.errorMessage ?: return@LaunchedEffect
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        authViewModel.clearErrorMessage()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 상단 로고
        Image(
            painter = painterResource(id = R.drawable.ic_logo),
            contentDescription = "Logo",
            modifier = Modifier.size(80.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "새로운 동료를 환영합니다",
            style = MaterialTheme.typography.headlineSmall,
            color = Color(0xFFFFC107),
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
        
        Text(
            text = "택시노트와 함께 스마트한 운행을 시작하세요",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // 이메일 입력 필드
        OutlinedTextField(
            value = email,
            onValueChange = { value -> email = value },
            label = { Text("이메일") },
            placeholder = { Text("사용할 이메일 주소") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFFFC107),
                focusedLabelColor = Color(0xFFFFC107)
            )
        )
        
        Spacer(modifier = Modifier.height(12.dp))

        // 비밀번호 입력 필드
        OutlinedTextField(
            value = password,
            onValueChange = { value -> password = value },
            label = { Text("비밀번호") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = PasswordVisualTransformation(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFFFC107),
                focusedLabelColor = Color(0xFFFFC107)
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 회원가입 버튼
        Button(
            onClick = { authViewModel.signUp(email, password) },
            enabled = email.isNotBlank() && password.isNotBlank() && !uiState.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFFC107),
                contentColor = Color.Black,
                disabledContainerColor = Color(0xFFFFE082)
            )
        ) {
            Text(
                text = if (uiState.isLoading) "가입 중..." else "회원가입 완료",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 로그인으로 돌아가기
        TextButton(onClick = onLogin) {
            Text(
                text = "이미 계정이 있으신가요? 로그인",
                color = Color.Gray
            )
        }
    }
}
