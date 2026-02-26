package com.lnk.app.comment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.lnk.app.data.repository.AuthRepository
import com.lnk.app.data.repository.FirestoreRepository

class CommentViewModelFactory(
    private val authRepository: AuthRepository,
    private val firestoreRepository: FirestoreRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CommentViewModel::class.java)) {
            return CommentViewModel(authRepository, firestoreRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
