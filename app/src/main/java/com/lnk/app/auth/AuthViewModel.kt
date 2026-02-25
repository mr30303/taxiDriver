package com.lnk.app.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lnk.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import android.util.Log

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState(isLoading = true))
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        refreshAuthState()
    }

    fun refreshAuthState() {
        val userId = authRepository.getCurrentUserId()
        _uiState.update { it.copy(isLoading = false, userId = userId, errorMessage = null) }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authRepository.signIn(email, password)
            _uiState.update {
                if (result.isSuccess) {
                    it.copy(isLoading = false, userId = result.getOrNull(), errorMessage = null)
                } else {
                    Log.e("AuthViewModel", "signIn failed", result.exceptionOrNull())
                    it.copy(isLoading = false, errorMessage = result.exceptionOrNull()?.message)
                }
            }
        }
    }

    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authRepository.signUp(email, password)
            _uiState.update {
                if (result.isSuccess) {
                    it.copy(isLoading = false, userId = result.getOrNull(), errorMessage = null)
                } else {
                    Log.e("AuthViewModel", "signUp failed", result.exceptionOrNull())
                    it.copy(isLoading = false, errorMessage = result.exceptionOrNull()?.message)
                }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _uiState.update { it.copy(userId = null, errorMessage = null) }
        }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
