package com.lnk.app.toilet

import com.lnk.app.data.model.Toilet
import com.lnk.app.data.model.Comment

data class ToiletUiState(
    val isLoading: Boolean = false,
    val isMasterLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isCommentLoading: Boolean = false,
    val isCommentSaving: Boolean = false,
    val currentUserId: String? = null,
    val userToilets: List<Toilet> = emptyList(),
    val masterToilets: List<Toilet> = emptyList(),
    val hiddenToiletIds: Set<String> = emptySet(),
    val hiddenToiletLabels: Map<String, String> = emptyMap(),
    val selectedToilet: Toilet? = null,
    val comments: List<Comment> = emptyList(),
    val lastAddedToiletId: String? = null,
    val lastHiddenToiletId: String? = null,
    val pendingFocusToiletId: String? = null,
    val pendingFocusLatitude: Double? = null,
    val pendingFocusLongitude: Double? = null,
    val errorMessage: String? = null
) {
    val visibleMasterToilets: List<Toilet>
        get() = masterToilets.filterNot { toilet -> hiddenToiletIds.contains(toilet.id) }

    val toilets: List<Toilet>
        get() = visibleMasterToilets + userToilets.filterNot { toilet ->
            hiddenToiletIds.contains(toilet.id)
        }
}
