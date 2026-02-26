package com.lnk.app.comment

data class CommentToiletSummary(
    val toiletId: String,
    val toiletName: String,
    val commentCount: Int,
    val latestComment: String,
    val latestCommentAt: Long,
    val latitude: Double? = null,
    val longitude: Double? = null
)
