package com.lnk.app.comment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lnk.app.data.model.Comment
import com.lnk.app.data.model.Toilet
import com.lnk.app.data.repository.AuthRepository
import com.lnk.app.data.repository.FirestoreRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

private const val RECENT_COMMENT_FETCH_LIMIT = 120
private const val MAX_TOILET_CARD_COUNT = 24
private const val COMMENT_FETCH_CONCURRENCY = 4

class CommentViewModel(
    private val authRepository: AuthRepository,
    private val firestoreRepository: FirestoreRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        CommentUiState(currentUserId = authRepository.getCurrentUserId())
    )
    val uiState: StateFlow<CommentUiState> = _uiState.asStateFlow()

    init {
        loadRecentComments()
    }

    fun loadRecentComments() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                buildToiletSummaries()
            }.onSuccess { summaries ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        summaries = summaries,
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message
                    )
                }
            }
        }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun onAuthUserChanged(userId: String?) {
        val normalizedUserId = userId?.trim().takeUnless { it.isNullOrBlank() }
        _uiState.update { state ->
            if (state.currentUserId == normalizedUserId) state
            else state.copy(currentUserId = normalizedUserId)
        }
    }

    private suspend fun buildToiletSummaries(): List<CommentToiletSummary> {
        val recentComments = firestoreRepository.getRecentComments(RECENT_COMMENT_FETCH_LIMIT)
        val recentGroupsByToiletId = recentComments.groupBy { comment -> comment.toiletId.trim() }
        val orderedToiletIds = recentComments
            .map { comment -> comment.toiletId.trim() }
            .filter { toiletId -> toiletId.isNotBlank() }
            .distinct()
            .take(MAX_TOILET_CARD_COUNT)

        if (orderedToiletIds.isEmpty()) return emptyList()

        val toiletsById = firestoreRepository.getToiletsByIds(orderedToiletIds)
        val semaphore = Semaphore(COMMENT_FETCH_CONCURRENCY)

        return coroutineScope {
            orderedToiletIds.map { toiletId ->
                async {
                    semaphore.withPermit {
                        val recentForToilet = recentGroupsByToiletId[toiletId].orEmpty()
                        val comments = runCatching {
                            firestoreRepository.getComments(toiletId)
                        }.getOrElse {
                            recentForToilet
                        }

                        val latestComment = comments.maxByOrNull { comment -> comment.sortKey() }
                            ?: recentForToilet.maxByOrNull { comment -> comment.sortKey() }
                        val toilet = toiletsById[toiletId]

                        CommentToiletSummary(
                            toiletId = toiletId,
                            toiletName = resolveToiletName(toilet, toiletId),
                            commentCount = comments.size.coerceAtLeast(recentForToilet.size),
                            latestComment = latestComment?.content.orEmpty(),
                            latestCommentAt = latestComment?.sortKey() ?: 0L,
                            latitude = toilet?.lat,
                            longitude = toilet?.lng
                        )
                    }
                }
            }.awaitAll()
                .sortedByDescending { summary -> summary.latestCommentAt }
        }
    }

    private fun resolveToiletName(toilet: Toilet?, toiletId: String): String {
        val description = toilet?.description.orEmpty()
        val nameFromDescription = description
            .split("|")
            .map { part -> part.trim() }
            .firstOrNull { part ->
                part.isNotBlank() && !part.startsWith("open:", ignoreCase = true)
            }
            .orEmpty()

        if (nameFromDescription.isNotBlank()) return nameFromDescription
        val tail = if (toiletId.length > 8) toiletId.takeLast(8) else toiletId
        return "화장실($tail)"
    }

    private fun Comment.sortKey(): Long {
        return if (updatedAt > 0L) updatedAt else createdAt
    }
}
