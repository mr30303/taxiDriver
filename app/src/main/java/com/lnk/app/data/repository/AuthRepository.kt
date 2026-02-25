package com.lnk.app.data.repository

interface AuthRepository {
    fun getCurrentUserId(): String?
    suspend fun signIn(email: String, password: String): Result<String>
    suspend fun signUp(email: String, password: String): Result<String>
    suspend fun signOut()
}
