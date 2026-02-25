package com.lnk.app.data.repository

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AuthRepositoryImpl(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : AuthRepository {
    override fun getCurrentUserId(): String? = auth.currentUser?.uid

    override suspend fun signIn(email: String, password: String): Result<String> {
        return runCatching {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            result.user?.uid ?: error("UserId not found")
        }
    }

    override suspend fun signUp(email: String, password: String): Result<String> {
        return runCatching {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            result.user?.uid ?: error("UserId not found")
        }
    }

    override suspend fun signOut() {
        auth.signOut()
    }

    private suspend fun <T> Task<T>.await(): T {
        return suspendCancellableCoroutine { cont ->
            addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    cont.resume(task.result)
                } else {
                    cont.resumeWithException(task.exception ?: IllegalStateException("Task failed"))
                }
            }
        }
    }
}
