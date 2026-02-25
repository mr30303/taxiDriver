package com.lnk.app.salary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.lnk.app.data.repository.AuthRepository
import com.lnk.app.data.repository.FirestoreRepository

class SalaryViewModelFactory(
    private val authRepository: AuthRepository,
    private val firestoreRepository: FirestoreRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SalaryViewModel::class.java)) {
            return SalaryViewModel(authRepository, firestoreRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
