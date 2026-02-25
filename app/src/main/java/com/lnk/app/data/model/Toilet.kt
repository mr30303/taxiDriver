package com.lnk.app.data.model

data class Toilet(
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val type: String = "",
    val description: String = "",
    val createdBy: String = "",
    val likeCount: Int = 0,
    val dislikeCount: Int = 0
)
