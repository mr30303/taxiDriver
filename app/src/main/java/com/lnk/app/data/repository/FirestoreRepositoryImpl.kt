package com.lnk.app.data.repository

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.lnk.app.data.model.Comment
import com.lnk.app.data.model.DailySales
import com.lnk.app.data.model.HiddenToiletPreference
import com.lnk.app.data.model.SOURCE_MASTER
import com.lnk.app.data.model.SOURCE_USER
import com.lnk.app.data.model.SalarySetting
import com.lnk.app.data.model.Toilet
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirestoreRepositoryImpl(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
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
        val docRef = firestore.collection("toilets").document()
        docRef.set(toilet.copy(id = docRef.id)).await()
        return docRef.id
    }

    override suspend fun getToilets(): List<Toilet> {
        val snapshot = firestore.collection("toilets")
            .get()
            .await()
        return snapshot.documents.mapNotNull { document ->
            document.toObject(Toilet::class.java)?.copy(id = document.id)
        }
    }

    override suspend fun getHiddenToiletPreference(userId: String): HiddenToiletPreference {
        val snapshot = firestore.collection("userPreferences")
            .document(userId)
            .get()
            .await()

        val hiddenToiletIds = (snapshot.get("hiddenToiletIds") as? List<*>)
            .orEmpty()
            .mapNotNull { value -> value as? String }
            .toSet()

        val hiddenToiletLabels = (snapshot.get("hiddenToiletLabels") as? Map<*, *>)
            .orEmpty()
            .mapNotNull { (key, value) ->
                val safeKey = key as? String ?: return@mapNotNull null
                val safeValue = value as? String ?: return@mapNotNull null
                safeKey to safeValue
            }
            .toMap()

        return HiddenToiletPreference(
            hiddenToiletIds = hiddenToiletIds,
            hiddenToiletLabels = hiddenToiletLabels
        )
    }

    override suspend fun saveHiddenToiletPreference(
        userId: String,
        preference: HiddenToiletPreference
    ) {
        firestore.collection("userPreferences")
            .document(userId)
            .set(
                mapOf(
                    "userId" to userId,
                    "hiddenToiletIds" to preference.hiddenToiletIds.toList(),
                    "hiddenToiletLabels" to preference.hiddenToiletLabels,
                    "updatedAt" to System.currentTimeMillis()
                ),
                SetOptions.merge()
            )
            .await()
    }

    override suspend fun getMasterRestroomsByLatRange(
        minLat: Double,
        maxLat: Double,
        limit: Int
    ): List<Toilet> {
        val totalLimit = limit.coerceIn(1, 2000)
        val pageSize = totalLimit.coerceAtMost(500)
        val toilets = mutableListOf<Toilet>()
        var lastDocument: DocumentSnapshot? = null

        while (toilets.size < totalLimit) {
            val remaining = totalLimit - toilets.size
            val requestSize = remaining.coerceAtMost(pageSize)
            var query = firestore.collection("restrooms_master")
                .whereGreaterThanOrEqualTo("lat", minLat)
                .whereLessThanOrEqualTo("lat", maxLat)
                .orderBy("lat")
                .limit(requestSize.toLong())

            lastDocument?.let { cursor ->
                query = query.startAfter(cursor)
            }

            val snapshot = query.get().await()
            if (snapshot.isEmpty) break

            toilets += snapshot.documents.mapNotNull { document ->
                document.toObject(Toilet::class.java)
                    ?.copy(id = document.id, source = SOURCE_MASTER)
            }

            if (snapshot.size() < requestSize) break
            lastDocument = snapshot.documents.lastOrNull() ?: break
        }

        return if (toilets.size > totalLimit) {
            toilets.take(totalLimit)
        } else {
            toilets
        }
    }

    override suspend fun getToiletsByIds(toiletIds: List<String>): Map<String, Toilet> {
        val ids = toiletIds.map { id -> id.trim() }
            .filter { id -> id.isNotBlank() }
            .distinct()
        if (ids.isEmpty()) return emptyMap()

        val result = mutableMapOf<String, Toilet>()
        val chunkSize = 30

        ids.chunked(chunkSize).forEach { chunk ->
            val userSnapshot = firestore.collection("toilets")
                .whereIn(FieldPath.documentId(), chunk)
                .get()
                .await()

            userSnapshot.documents.forEach { document ->
                document.toObject(Toilet::class.java)?.let { toilet ->
                    result[document.id] = toilet.copy(id = document.id, source = SOURCE_USER)
                }
            }

            val missingIds = chunk.filterNot { id -> result.containsKey(id) }
            if (missingIds.isEmpty()) return@forEach

            val masterSnapshot = firestore.collection("restrooms_master")
                .whereIn(FieldPath.documentId(), missingIds)
                .get()
                .await()

            masterSnapshot.documents.forEach { document ->
                document.toObject(Toilet::class.java)?.let { toilet ->
                    result[document.id] = toilet.copy(id = document.id, source = SOURCE_MASTER)
                }
            }
        }

        return result
    }

    override suspend fun getToilet(toiletId: String): Toilet? {
        val snapshot = firestore.collection("toilets")
            .document(toiletId)
            .get()
            .await()
        return snapshot.toObject(Toilet::class.java)?.copy(id = snapshot.id)
    }

    override suspend fun updateToiletReactions(
        toiletId: String,
        likeCount: Int,
        dislikeCount: Int,
        likedUserIds: List<String>,
        dislikedUserIds: List<String>,
        source: String
    ) {
        val collection = if (source == SOURCE_MASTER) "restrooms_master" else "toilets"
        firestore.collection(collection)
            .document(toiletId)
            .update(
                mapOf(
                    "likeCount" to likeCount,
                    "dislikeCount" to dislikeCount,
                    "likedUserIds" to likedUserIds,
                    "dislikedUserIds" to dislikedUserIds
                )
            )
            .await()
    }

    override suspend fun addComment(comment: Comment): String {
        val currentUserId = requireCurrentUserId()
        if (comment.userId != currentUserId) {
            throw IllegalStateException("Invalid comment owner.")
        }
        val docRef = firestore.collection("comments").document()
        docRef.set(comment.copy(id = docRef.id)).await()
        return docRef.id
    }

    override suspend fun getRecentComments(limit: Int): List<Comment> {
        val safeLimit = limit.coerceIn(1, 200)
        val snapshot = firestore.collection("comments")
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .limit(safeLimit.toLong())
            .get()
            .await()
        return snapshot.documents.mapNotNull { document ->
            document.toObject(Comment::class.java)?.copy(id = document.id)
        }.sortedByDescending { comment ->
            if (comment.updatedAt > 0L) comment.updatedAt else comment.createdAt
        }
    }

    override suspend fun getComments(toiletId: String): List<Comment> {
        val snapshot = firestore.collection("comments")
            .whereEqualTo("toiletId", toiletId)
            .get()
            .await()
        return snapshot.documents.mapNotNull { document ->
            document.toObject(Comment::class.java)?.copy(id = document.id)
        }.sortedByDescending { comment ->
            if (comment.updatedAt > 0L) comment.updatedAt else comment.createdAt
        }
    }

    override suspend fun updateComment(commentId: String, content: String, updatedAt: Long) {
        val currentUserId = requireCurrentUserId()
        val snapshot = firestore.collection("comments")
            .document(commentId)
            .get()
            .await()
        val commentOwnerId = snapshot.getString("userId").orEmpty()
        if (commentOwnerId != currentUserId) {
            throw IllegalStateException("You can only edit your own comment.")
        }

        firestore.collection("comments")
            .document(commentId)
            .update(
                mapOf(
                    "content" to content,
                    "updatedAt" to updatedAt
                )
            )
            .await()
    }

    override suspend fun deleteComment(commentId: String) {
        val currentUserId = requireCurrentUserId()
        val snapshot = firestore.collection("comments")
            .document(commentId)
            .get()
            .await()
        val commentOwnerId = snapshot.getString("userId").orEmpty()
        if (commentOwnerId != currentUserId) {
            throw IllegalStateException("You can only delete your own comment.")
        }

        firestore.collection("comments")
            .document(commentId)
            .delete()
            .await()
    }

    private fun requireCurrentUserId(): String {
        val currentUserId = auth.currentUser?.uid?.trim().orEmpty()
        if (currentUserId.isBlank()) {
            throw IllegalStateException("User is not authenticated.")
        }
        return currentUserId
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
