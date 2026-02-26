package com.lnk.app.data.model

data class Comment(
    val id: String = "",
    val toiletId: String = "",
    val userId: String = "",
    val userNickname: String = "",
    val content: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)
