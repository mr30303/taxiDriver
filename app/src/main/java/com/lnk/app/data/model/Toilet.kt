package com.lnk.app.data.model

data class Toilet(
    val id: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val type: String = "",
    val description: String = "",
    val createdBy: String = "",
    val likeCount: Int = 0,
    val dislikeCount: Int = 0,
    val likedUserIds: List<String> = emptyList(),
    val dislikedUserIds: List<String> = emptyList(),
    val source: String = SOURCE_USER
)

const val SOURCE_USER = "user"
const val SOURCE_MASTER = "master"
