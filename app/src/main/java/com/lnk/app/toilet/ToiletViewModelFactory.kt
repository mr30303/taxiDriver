package com.lnk.app.toilet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.lnk.app.data.repository.AuthRepository
import com.lnk.app.data.repository.FirestoreRepository

class ToiletViewModelFactory(
    private val authRepository: AuthRepository,
    private val firestoreRepository: FirestoreRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ToiletViewModel::class.java)) {
            return ToiletViewModel(authRepository, firestoreRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
