package com.lnk.app.data.repository

interface AuthRepository {
    fun getCurrentUserId(): String?
    fun getCurrentUserNickname(): String?
    suspend fun signIn(email: String, password: String): Result<String>
    suspend fun signUp(email: String, password: String, nickname: String): Result<String>
    suspend fun signOut()
}
