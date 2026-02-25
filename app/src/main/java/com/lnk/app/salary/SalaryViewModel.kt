package com.lnk.app.salary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lnk.app.data.model.DailySales
import com.lnk.app.data.model.SalarySetting
import com.lnk.app.data.repository.AuthRepository
import com.lnk.app.data.repository.FirestoreRepository
import com.lnk.app.domain.SalaryCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SalaryViewModel(
    private val authRepository: AuthRepository,
    private val firestoreRepository: FirestoreRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(SalaryUiState())
    val uiState: StateFlow<SalaryUiState> = _uiState.asStateFlow()

    private val userId: String? = authRepository.getCurrentUserId()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        if (userId == null) {
            recalculate(_uiState.value.setting, _uiState.value.dailySales)
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                val setting = firestoreRepository.getSalarySetting(userId) ?: SalarySetting()
                val dailySales = firestoreRepository.getDailySales(userId)
                _uiState.update { it.copy(setting = setting, dailySales = dailySales) }
                recalculate(setting, dailySales)
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
            }
        }
    }

    fun saveSetting(setting: SalarySetting) {
        _uiState.update { it.copy(setting = setting, errorMessage = null) }
        recalculate(setting, _uiState.value.dailySales)
        val currentUserId = userId ?: return
        viewModelScope.launch {
            runCatching {
                firestoreRepository.saveSalarySetting(currentUserId, setting)
            }.onFailure { error ->
                _uiState.update { it.copy(errorMessage = error.message) }
            }
        }
    }

    fun addDailySales(entry: DailySales) {
        val entryWithUser = if (entry.userId.isBlank() && !userId.isNullOrBlank()) {
            entry.copy(userId = userId)
        } else {
            entry
        }
        val updated = _uiState.value.dailySales + entryWithUser
        _uiState.update { it.copy(dailySales = updated, errorMessage = null) }
        recalculate(_uiState.value.setting, updated)

        val currentUserId = userId ?: return
        viewModelScope.launch {
            runCatching {
                firestoreRepository.addDailySales(entryWithUser.copy(userId = currentUserId))
            }.onFailure { error ->
                _uiState.update { it.copy(errorMessage = error.message) }
            }
        }
    }

    fun removeDailySales(index: Int) {
        val updated = _uiState.value.dailySales.toMutableList().also {
            if (index in it.indices) it.removeAt(index)
        }
        _uiState.update { it.copy(dailySales = updated, errorMessage = null) }
        recalculate(_uiState.value.setting, updated)
    }

    private fun recalculate(setting: SalarySetting, dailySales: List<DailySales>) {
        val result = SalaryCalculator.calculate(setting, dailySales)
        _uiState.update { it.copy(result = result, isLoading = false) }
    }
}
