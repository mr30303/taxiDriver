package com.lnk.app.data.repository

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.lnk.app.data.model.Comment
import com.lnk.app.data.model.DailySales
import com.lnk.app.data.model.SalarySetting
import com.lnk.app.data.model.Toilet
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirestoreRepositoryImpl(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : FirestoreRepository {
    override suspend fun getSalarySetting(userId: String): SalarySetting? {
        val snapshot = firestore.collection("salarySettings")
            .document(userId)
            .get()
            .await()
        return snapshot.toObject(SalarySetting::class.java)
    }

    override suspend fun saveSalarySetting(userId: String, setting: SalarySetting) {
        firestore.collection("salarySettings")
            .document(userId)
            .set(setting)
            .await()
    }

    override suspend fun addDailySales(sales: DailySales) {
        firestore.collection("dailySales")
            .add(sales)
            .await()
    }

    override suspend fun getDailySales(userId: String): List<DailySales> {
        val snapshot = firestore.collection("dailySales")
            .whereEqualTo("userId", userId)
            .get()
            .await()
        return snapshot.documents.mapNotNull { it.toObject(DailySales::class.java) }
    }

    override suspend fun addToilet(toilet: Toilet): String {
        val docRef = firestore.collection("toilets")
            .add(toilet)
            .await()
        return docRef.id
    }

    override suspend fun getToilets(): List<Toilet> {
        val snapshot = firestore.collection("toilets")
            .get()
            .await()
        return snapshot.documents.mapNotNull { it.toObject(Toilet::class.java) }
    }

    override suspend fun getToilet(toiletId: String): Toilet? {
        val snapshot = firestore.collection("toilets")
            .document(toiletId)
            .get()
            .await()
        return snapshot.toObject(Toilet::class.java)
    }

    override suspend fun updateToiletReactions(
        toiletId: String,
        likeCount: Int,
        dislikeCount: Int
    ) {
        firestore.collection("toilets")
            .document(toiletId)
            .update(mapOf("likeCount" to likeCount, "dislikeCount" to dislikeCount))
            .await()
    }

    override suspend fun addComment(comment: Comment): String {
        val docRef = firestore.collection("comments")
            .add(comment)
            .await()
        return docRef.id
    }

    override suspend fun getComments(toiletId: String): List<Comment> {
        val snapshot = firestore.collection("comments")
            .whereEqualTo("toiletId", toiletId)
            .get()
            .await()
        return snapshot.documents.mapNotNull { it.toObject(Comment::class.java) }
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
