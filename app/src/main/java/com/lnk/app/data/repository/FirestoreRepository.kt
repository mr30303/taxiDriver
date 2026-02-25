package com.lnk.app.data.repository

import com.lnk.app.data.model.Comment
import com.lnk.app.data.model.DailySales
import com.lnk.app.data.model.SalarySetting
import com.lnk.app.data.model.Toilet

interface FirestoreRepository {
    suspend fun getSalarySetting(userId: String): SalarySetting?
    suspend fun saveSalarySetting(userId: String, setting: SalarySetting)
    suspend fun addDailySales(sales: DailySales)
    suspend fun getDailySales(userId: String): List<DailySales>

    suspend fun addToilet(toilet: Toilet): String
    suspend fun getToilets(): List<Toilet>
    suspend fun getToilet(toiletId: String): Toilet?
    suspend fun updateToiletReactions(
        toiletId: String,
        likeCount: Int,
        dislikeCount: Int
    )

    suspend fun addComment(comment: Comment): String
    suspend fun getComments(toiletId: String): List<Comment>
}
