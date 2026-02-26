package com.lnk.app.comment

data class CommentUiState(
    val isLoading: Boolean = false,
    val currentUserId: String? = null,
    val summaries: List<CommentToiletSummary> = emptyList(),
    val errorMessage: String? = null
)
