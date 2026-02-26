package com.lnk.app.toilet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestoreException
import com.lnk.app.data.model.Comment
import com.lnk.app.data.model.HiddenToiletPreference
import com.lnk.app.data.model.SOURCE_MASTER
import com.lnk.app.data.model.Toilet
import com.lnk.app.data.repository.AuthRepository
import com.lnk.app.data.repository.FirestoreRepository
import kotlin.math.ceil
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

private const val MASTER_CHUNK_RESULT_LIMIT = 1000
private const val MASTER_CONCURRENT_CHUNK_REQUESTS = 4
private const val MASTER_LAT_CHUNK_SPAN = 0.08
private const val MASTER_MAX_CHUNKS = 12
private const val MASTER_BOUNDS_DEBOUNCE_MS = 350L
private const val MASTER_BOUNDS_PADDING_RATIO = 0.25
private const val MASTER_CACHE_SIZE = 12
private const val HIDDEN_LABEL_MAX_LENGTH = 24

class ToiletViewModel(
    private val authRepository: AuthRepository,
    private val firestoreRepository: FirestoreRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        ToiletUiState(currentUserId = normalizeUserId(authRepository.getCurrentUserId()))
    )
    val uiState: StateFlow<ToiletUiState> = _uiState.asStateFlow()

    private var latestBounds: MapBounds? = null
    private var latestNetworkBoundsKey: String? = null
    private var debounceJob: Job? = null
    private var masterLoadJob: Job? = null
    private val masterBoundsCache = mutableListOf<MasterCacheEntry>()

    init {
        loadHiddenToiletPreference(_uiState.value.currentUserId)
        loadToilets()
    }

    fun onAuthUserChanged(userId: String?) {
        val normalizedUserId = normalizeUserId(userId)
        val previousUserId = _uiState.value.currentUserId
        if (previousUserId == normalizedUserId) return

        _uiState.update { state ->
            state.copy(
                currentUserId = normalizedUserId,
                selectedToilet = null,
                comments = emptyList(),
                hiddenToiletIds = if (normalizedUserId == null) emptySet() else state.hiddenToiletIds,
                hiddenToiletLabels = if (normalizedUserId == null) emptyMap() else state.hiddenToiletLabels
            )
        }

        if (normalizedUserId.isNullOrBlank()) return
        loadHiddenToiletPreference(normalizedUserId)
    }

    fun loadToilets() {
        loadUserToilets()
        latestBounds?.let { bounds ->
            scheduleMasterLoad(bounds, force = true)
        }
    }

    fun onMapBoundsChanged(
        minLat: Double,
        maxLat: Double,
        minLng: Double,
        maxLng: Double
    ) {
        if (!minLat.isFinite() || !maxLat.isFinite() || !minLng.isFinite() || !maxLng.isFinite()) {
            return
        }
        val normalized = MapBounds(
            minLat = minOf(minLat, maxLat),
            maxLat = maxOf(minLat, maxLat),
            minLng = minOf(minLng, maxLng),
            maxLng = maxOf(minLng, maxLng)
        )
        latestBounds = normalized
        scheduleMasterLoad(normalized, force = false)
    }

    fun selectToilet(toilet: Toilet?) {
        if (toilet != null && _uiState.value.hiddenToiletIds.contains(toilet.id)) {
            return
        }
        _uiState.update {
            it.copy(
                selectedToilet = toilet,
                comments = if (toilet == null) emptyList() else it.comments
            )
        }
        if (toilet != null) {
            loadComments(toilet.id)
        }
    }

    fun prepareToiletFocus(
        toiletId: String,
        latitude: Double? = null,
        longitude: Double? = null
    ) {
        val targetId = toiletId.trim()
        if (targetId.isBlank()) return

        val loadedToilet = findLoadedToiletById(targetId)
        val initialLat = loadedToilet?.lat ?: latitude
        val initialLng = loadedToilet?.lng ?: longitude

        _uiState.update {
            it.copy(
                pendingFocusToiletId = targetId,
                pendingFocusLatitude = initialLat,
                pendingFocusLongitude = initialLng
            )
        }

        if (loadedToilet != null || (initialLat != null && initialLng != null)) return

        viewModelScope.launch {
            runCatching {
                firestoreRepository.getToiletsByIds(listOf(targetId))[targetId]
            }.onSuccess { toilet ->
                if (toilet == null) return@onSuccess
                _uiState.update { state ->
                    if (state.pendingFocusToiletId != targetId) {
                        state
                    } else {
                        state.copy(
                            pendingFocusLatitude = toilet.lat,
                            pendingFocusLongitude = toilet.lng
                        )
                    }
                }
            }
        }
    }

    fun consumePendingFocusToilet() {
        _uiState.update {
            it.copy(
                pendingFocusToiletId = null,
                pendingFocusLatitude = null,
                pendingFocusLongitude = null
            )
        }
    }

    fun addToilet(
        latitudeInput: String,
        longitudeInput: String,
        type: String,
        description: String
    ) {
        val latitude = latitudeInput.trim().toDoubleOrNull()
        val longitude = longitudeInput.trim().toDoubleOrNull()
        val normalizedType = type.trim()
        val normalizedDescription = description.trim()
        val userId = resolveCurrentUserId()

        when {
            latitude == null || latitude !in -90.0..90.0 -> {
                _uiState.update { it.copy(errorMessage = "Invalid latitude.") }
                return
            }

            longitude == null || longitude !in -180.0..180.0 -> {
                _uiState.update { it.copy(errorMessage = "Invalid longitude.") }
                return
            }

            normalizedType.isBlank() -> {
                _uiState.update { it.copy(errorMessage = "Please select restroom type.") }
                return
            }

            normalizedDescription.isBlank() -> {
                _uiState.update { it.copy(errorMessage = "Please enter description.") }
                return
            }

            userId.isNullOrBlank() -> {
                _uiState.update { it.copy(errorMessage = "User is not authenticated.") }
                return
            }
        }

        val validLatitude = latitude ?: return
        val validLongitude = longitude ?: return
        val validUserId = userId ?: return

        val toilet = Toilet(
            lat = validLatitude,
            lng = validLongitude,
            type = normalizedType,
            description = normalizedDescription,
            createdBy = validUserId
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            runCatching {
                val toiletId = firestoreRepository.addToilet(toilet)
                toilet.copy(id = toiletId)
            }.onSuccess { savedToilet ->
                _uiState.update { state ->
                    state.copy(
                        isSaving = false,
                        userToilets = listOf(savedToilet) + state.userToilets,
                        selectedToilet = savedToilet,
                        lastAddedToiletId = savedToilet.id,
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                _uiState.update { it.copy(isSaving = false, errorMessage = error.message) }
            }
        }
    }

    fun toggleLike() {
        updateReaction(target = ReactionType.LIKE)
    }

    fun toggleDislike() {
        updateReaction(target = ReactionType.DISLIKE)
    }

    fun loadComments(toiletId: String) {
        if (toiletId.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isCommentLoading = true, errorMessage = null) }
            runCatching {
                firestoreRepository.getComments(toiletId)
            }.onSuccess { comments ->
                _uiState.update { state ->
                    state.copy(
                        isCommentLoading = false,
                        comments = comments,
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                _uiState.update { it.copy(isCommentLoading = false, errorMessage = error.message) }
            }
        }
    }

    fun addComment(content: String) {
        val selectedToilet = _uiState.value.selectedToilet ?: return
        val currentUserId = resolveCurrentUserId()
        val normalizedContent = content.trim()
        if (normalizedContent.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Comment cannot be empty.") }
            return
        }
        if (currentUserId.isNullOrBlank()) {
            _uiState.update { it.copy(errorMessage = "User is not authenticated.") }
            return
        }

        val now = System.currentTimeMillis()
        val commentNickname = resolveCurrentCommentNickname(currentUserId)
        val newComment = Comment(
            toiletId = selectedToilet.id,
            userId = currentUserId,
            userNickname = commentNickname,
            content = normalizedContent,
            createdAt = now,
            updatedAt = now
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isCommentSaving = true, errorMessage = null) }
            runCatching {
                val commentId = firestoreRepository.addComment(newComment)
                newComment.copy(id = commentId)
            }.onSuccess { savedComment ->
                _uiState.update { state ->
                    state.copy(
                        isCommentSaving = false,
                        comments = sortComments(listOf(savedComment) + state.comments)
                    )
                }
            }.onFailure { error ->
                _uiState.update { it.copy(isCommentSaving = false, errorMessage = error.message) }
            }
        }
    }

    fun updateComment(commentId: String, content: String) {
        val currentUserId = resolveCurrentUserId()
        val normalizedContent = content.trim()
        if (normalizedContent.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Comment cannot be empty.") }
            return
        }
        if (currentUserId.isNullOrBlank()) {
            _uiState.update { it.copy(errorMessage = "User is not authenticated.") }
            return
        }

        val targetComment = _uiState.value.comments.firstOrNull { it.id == commentId }
        if (targetComment == null) {
            _uiState.update { it.copy(errorMessage = "Comment not found.") }
            return
        }
        if (targetComment.userId != currentUserId) {
            _uiState.update { it.copy(errorMessage = "You can only edit your own comment.") }
            return
        }

        val now = System.currentTimeMillis()
        val updatedComment = targetComment.copy(content = normalizedContent, updatedAt = now)
        val previousComments = _uiState.value.comments
        _uiState.update { state ->
            state.copy(
                comments = sortComments(
                    state.comments.map { comment ->
                        if (comment.id == commentId) updatedComment else comment
                    }
                ),
                isCommentSaving = true,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            runCatching {
                firestoreRepository.updateComment(commentId, normalizedContent, now)
            }.onSuccess {
                _uiState.update { it.copy(isCommentSaving = false) }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isCommentSaving = false,
                        comments = previousComments,
                        errorMessage = error.message
                    )
                }
            }
        }
    }

    fun deleteComment(commentId: String) {
        val currentUserId = resolveCurrentUserId()
        if (currentUserId.isNullOrBlank()) {
            _uiState.update { it.copy(errorMessage = "User is not authenticated.") }
            return
        }

        val targetComment = _uiState.value.comments.firstOrNull { it.id == commentId }
        if (targetComment == null) {
            _uiState.update { it.copy(errorMessage = "Comment not found.") }
            return
        }
        if (targetComment.userId != currentUserId) {
            _uiState.update { it.copy(errorMessage = "You can only delete your own comment.") }
            return
        }

        val previousComments = _uiState.value.comments
        _uiState.update { state ->
            state.copy(
                comments = state.comments.filterNot { it.id == commentId },
                isCommentSaving = true,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            runCatching {
                firestoreRepository.deleteComment(commentId)
            }.onSuccess {
                _uiState.update { it.copy(isCommentSaving = false) }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isCommentSaving = false,
                        comments = previousComments,
                        errorMessage = error.message
                    )
                }
            }
        }
    }

    fun consumeLastAddedToilet() {
        _uiState.update { it.copy(lastAddedToiletId = null) }
    }

    fun hideSelectedToilet() {
        val state = _uiState.value
        val currentUserId = resolveCurrentUserId()
        val selectedToilet = state.selectedToilet ?: return

        if (currentUserId.isNullOrBlank()) {
            _uiState.update { it.copy(errorMessage = "로그인이 필요합니다.") }
            return
        }
        if (selectedToilet.id.isBlank()) {
            _uiState.update { it.copy(errorMessage = "유효한 화장실 정보가 아닙니다.") }
            return
        }
        if (state.hiddenToiletIds.contains(selectedToilet.id)) {
            _uiState.update {
                it.copy(
                    selectedToilet = null,
                    comments = emptyList()
                )
            }
            return
        }

        val previousIds = state.hiddenToiletIds
        val previousLabels = state.hiddenToiletLabels
        val label = buildHiddenLabel(selectedToilet)
        val updatedIds = previousIds + selectedToilet.id
        val updatedLabels = previousLabels + (selectedToilet.id to label)

        _uiState.update {
            it.copy(
                hiddenToiletIds = updatedIds,
                hiddenToiletLabels = updatedLabels,
                selectedToilet = null,
                comments = emptyList(),
                lastHiddenToiletId = selectedToilet.id,
                errorMessage = null
            )
        }

        persistHiddenToiletPreference(
            userId = currentUserId,
            hiddenToiletIds = updatedIds,
            hiddenToiletLabels = updatedLabels,
            onFailure = { error ->
                _uiState.update {
                    it.copy(
                        hiddenToiletIds = previousIds,
                        hiddenToiletLabels = previousLabels,
                        errorMessage = error.message
                    )
                }
            }
        )
    }

    fun unhideToilet(toiletId: String) {
        val state = _uiState.value
        val currentUserId = resolveCurrentUserId()
        if (toiletId.isBlank() || currentUserId.isNullOrBlank()) return
        if (!state.hiddenToiletIds.contains(toiletId)) return

        val previousIds = state.hiddenToiletIds
        val previousLabels = state.hiddenToiletLabels
        val updatedIds = previousIds - toiletId
        val updatedLabels = previousLabels - toiletId

        _uiState.update {
            it.copy(
                hiddenToiletIds = updatedIds,
                hiddenToiletLabels = updatedLabels,
                errorMessage = null
            )
        }

        persistHiddenToiletPreference(
            userId = currentUserId,
            hiddenToiletIds = updatedIds,
            hiddenToiletLabels = updatedLabels,
            onFailure = { error ->
                _uiState.update {
                    it.copy(
                        hiddenToiletIds = previousIds,
                        hiddenToiletLabels = previousLabels,
                        errorMessage = error.message
                    )
                }
            }
        )
    }

    fun unhideAllToilets() {
        val state = _uiState.value
        val currentUserId = resolveCurrentUserId()
        if (currentUserId.isNullOrBlank()) return
        if (state.hiddenToiletIds.isEmpty()) return

        val previousIds = state.hiddenToiletIds
        val previousLabels = state.hiddenToiletLabels

        _uiState.update {
            it.copy(
                hiddenToiletIds = emptySet(),
                hiddenToiletLabels = emptyMap(),
                errorMessage = null
            )
        }

        persistHiddenToiletPreference(
            userId = currentUserId,
            hiddenToiletIds = emptySet(),
            hiddenToiletLabels = emptyMap(),
            onFailure = { error ->
                _uiState.update {
                    it.copy(
                        hiddenToiletIds = previousIds,
                        hiddenToiletLabels = previousLabels,
                        errorMessage = error.message
                    )
                }
            }
        )
    }

    fun consumeLastHiddenToilet() {
        _uiState.update { it.copy(lastHiddenToiletId = null) }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun loadHiddenToiletPreference(userId: String?) {
        val currentUserId = normalizeUserId(userId)
        if (currentUserId.isNullOrBlank()) return

        viewModelScope.launch {
            runCatching {
                firestoreRepository.getHiddenToiletPreference(currentUserId)
            }.onSuccess { preference ->
                _uiState.update {
                    it.copy(
                        hiddenToiletIds = preference.hiddenToiletIds,
                        hiddenToiletLabels = preference.hiddenToiletLabels
                    )
                }
            }.onFailure { error ->
                if (!error.isPermissionDenied()) {
                    _uiState.update { it.copy(errorMessage = error.message) }
                }
            }
        }
    }

    private fun persistHiddenToiletPreference(
        userId: String,
        hiddenToiletIds: Set<String>,
        hiddenToiletLabels: Map<String, String>,
        onFailure: (Throwable) -> Unit
    ) {
        viewModelScope.launch {
            runCatching {
                firestoreRepository.saveHiddenToiletPreference(
                    userId = userId,
                    preference = HiddenToiletPreference(
                        hiddenToiletIds = hiddenToiletIds,
                        hiddenToiletLabels = hiddenToiletLabels
                    )
                )
            }.onFailure { error ->
                if (error.isPermissionDenied()) {
                    _uiState.update {
                        it.copy(
                            errorMessage = "Server permission denied. Hidden state is applied locally only."
                        )
                    }
                } else {
                    onFailure(error)
                }
            }
        }
    }

    private fun Throwable.isPermissionDenied(): Boolean {
        val firestoreError = this as? FirebaseFirestoreException
        if (firestoreError?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
            return true
        }
        return message?.contains("PERMISSION_DENIED", ignoreCase = true) == true
    }

    private fun buildHiddenLabel(toilet: Toilet): String {
        val base = toilet.description
            .split("|")
            .map { part -> part.trim() }
            .firstOrNull { part -> part.isNotBlank() && !part.startsWith("open:", ignoreCase = true) }
            .orEmpty()
            .ifBlank { toilet.type.ifBlank { "화장실" } }

        return if (base.length > HIDDEN_LABEL_MAX_LENGTH) {
            "${base.take(HIDDEN_LABEL_MAX_LENGTH)}..."
        } else {
            base
        }
    }

    private fun loadUserToilets() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                firestoreRepository.getToilets()
            }.onSuccess { toilets ->
                _uiState.update { state ->
                    val selectedId = state.selectedToilet?.id
                    val refreshedSelected = if (state.selectedToilet?.source == SOURCE_MASTER) {
                        state.selectedToilet
                    } else {
                        toilets.firstOrNull { it.id == selectedId } ?: state.selectedToilet
                    }
                    state.copy(
                        isLoading = false,
                        userToilets = toilets,
                        selectedToilet = refreshedSelected,
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
            }
        }
    }

    private fun loadMasterToiletsForBounds(bounds: MapBounds, force: Boolean) {
        if (!force) {
            findCachedMasterToilets(bounds)?.let { cachedToilets ->
                applyMasterToilets(cachedToilets)
                return
            }
        }

        val expandedBounds = bounds.withPadding()
        val networkQueryKey = expandedBounds.toQueryKey()
        if (!force && networkQueryKey == latestNetworkBoundsKey) {
            return
        }
        latestNetworkBoundsKey = networkQueryKey

        masterLoadJob?.cancel()
        masterLoadJob = viewModelScope.launch {
            _uiState.update { it.copy(isMasterLoading = true, errorMessage = null) }
            runCatching {
                val chunks = expandedBounds.splitByLatitude()
                val semaphore = Semaphore(MASTER_CONCURRENT_CHUNK_REQUESTS)
                val chunkResults = coroutineScope {
                    chunks.map { chunk ->
                        async {
                            semaphore.withPermit {
                                firestoreRepository.getMasterRestroomsByLatRange(
                                    minLat = chunk.minLat,
                                    maxLat = chunk.maxLat,
                                    limit = MASTER_CHUNK_RESULT_LIMIT
                                )
                            }
                        }
                    }.awaitAll()
                }

                val resultMap = linkedMapOf<String, Toilet>()
                for (chunkToilets in chunkResults) {
                    for (toilet in chunkToilets) {
                        if (toilet.lng !in expandedBounds.minLng..expandedBounds.maxLng) continue
                        if (resultMap.containsKey(toilet.id)) continue

                        resultMap[toilet.id] = toilet.copy(source = SOURCE_MASTER)
                    }
                }
                resultMap.values.toList()
            }.onSuccess { masterToilets ->
                putMasterCache(expandedBounds, masterToilets)
                _uiState.update { state ->
                    val selectedId = state.selectedToilet?.id
                    val visibleToilets = filterToBounds(masterToilets, bounds)
                    val refreshedSelected = if (state.selectedToilet?.source == SOURCE_MASTER) {
                        visibleToilets.firstOrNull { it.id == selectedId } ?: state.selectedToilet
                    } else {
                        state.selectedToilet
                    }
                    state.copy(
                        isMasterLoading = false,
                        masterToilets = visibleToilets,
                        selectedToilet = refreshedSelected,
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                if (error is CancellationException) return@onFailure
                _uiState.update { it.copy(isMasterLoading = false, errorMessage = error.message) }
            }
        }
    }

    private fun scheduleMasterLoad(bounds: MapBounds, force: Boolean) {
        debounceJob?.cancel()
        if (force) {
            loadMasterToiletsForBounds(bounds, force = true)
            return
        }

        debounceJob = viewModelScope.launch {
            delay(MASTER_BOUNDS_DEBOUNCE_MS)
            loadMasterToiletsForBounds(bounds, force = false)
        }
    }

    private fun findCachedMasterToilets(bounds: MapBounds): List<Toilet>? {
        val cacheEntry = masterBoundsCache.lastOrNull { entry ->
            entry.bounds.contains(bounds)
        } ?: return null
        return filterToBounds(cacheEntry.toilets, bounds)
    }

    private fun putMasterCache(bounds: MapBounds, toilets: List<Toilet>) {
        masterBoundsCache.removeAll { entry ->
            entry.bounds.toQueryKey() == bounds.toQueryKey()
        }
        masterBoundsCache += MasterCacheEntry(bounds = bounds, toilets = toilets)
        if (masterBoundsCache.size > MASTER_CACHE_SIZE) {
            masterBoundsCache.removeAt(0)
        }
    }

    private fun applyMasterToilets(toilets: List<Toilet>) {
        _uiState.update { state ->
            val selectedId = state.selectedToilet?.id
            val refreshedSelected = if (state.selectedToilet?.source == SOURCE_MASTER) {
                toilets.firstOrNull { it.id == selectedId } ?: state.selectedToilet
            } else {
                state.selectedToilet
            }
            state.copy(
                isMasterLoading = false,
                masterToilets = toilets,
                selectedToilet = refreshedSelected,
                errorMessage = null
            )
        }
    }

    private fun filterToBounds(toilets: List<Toilet>, bounds: MapBounds): List<Toilet> {
        return toilets.filter { toilet ->
            toilet.lat in bounds.minLat..bounds.maxLat &&
                toilet.lng in bounds.minLng..bounds.maxLng
        }
    }

    private fun findLoadedToiletById(toiletId: String): Toilet? {
        val state = _uiState.value
        return (state.masterToilets + state.userToilets)
            .firstOrNull { toilet -> toilet.id == toiletId }
    }

    private fun updateReaction(target: ReactionType) {
        val state = _uiState.value
        val selected = state.selectedToilet ?: return
        val currentUserId = resolveCurrentUserId()
        if (selected.id.isBlank()) return
        if (currentUserId.isNullOrBlank()) {
            _uiState.update { it.copy(errorMessage = "User is not authenticated.") }
            return
        }

        val liked = selected.likedUserIds.toMutableSet()
        val disliked = selected.dislikedUserIds.toMutableSet()

        when (target) {
            ReactionType.LIKE -> {
                if (liked.contains(currentUserId)) {
                    liked.remove(currentUserId)
                } else {
                    liked.add(currentUserId)
                    disliked.remove(currentUserId)
                }
            }

            ReactionType.DISLIKE -> {
                if (disliked.contains(currentUserId)) {
                    disliked.remove(currentUserId)
                } else {
                    disliked.add(currentUserId)
                    liked.remove(currentUserId)
                }
            }
        }

        val updated = selected.copy(
            likedUserIds = liked.toList(),
            dislikedUserIds = disliked.toList(),
            likeCount = liked.size,
            dislikeCount = disliked.size
        )

        val previousSelected = selected
        val previousUserToilets = state.userToilets
        val previousMasterToilets = state.masterToilets
        val updatedUserToilets = state.userToilets.map { toilet ->
            if (selected.source != SOURCE_MASTER && toilet.id == selected.id) updated else toilet
        }
        val updatedMasterToilets = state.masterToilets.map { toilet ->
            if (selected.source == SOURCE_MASTER && toilet.id == selected.id) updated else toilet
        }

        _uiState.update {
            it.copy(
                userToilets = updatedUserToilets,
                masterToilets = updatedMasterToilets,
                selectedToilet = updated
            )
        }

        viewModelScope.launch {
            runCatching {
                firestoreRepository.updateToiletReactions(
                    toiletId = selected.id,
                    likeCount = updated.likeCount,
                    dislikeCount = updated.dislikeCount,
                    likedUserIds = updated.likedUserIds,
                    dislikedUserIds = updated.dislikedUserIds,
                    source = selected.source
                )
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        userToilets = previousUserToilets,
                        masterToilets = previousMasterToilets,
                        selectedToilet = previousSelected,
                        errorMessage = error.message
                    )
                }
            }
        }
    }

    private fun sortComments(comments: List<Comment>): List<Comment> {
        return comments.sortedByDescending { comment ->
            if (comment.updatedAt > 0L) comment.updatedAt else comment.createdAt
        }
    }

    private fun resolveCurrentCommentNickname(currentUserId: String): String {
        val nickname = authRepository.getCurrentUserNickname().orEmpty().trim()
        if (nickname.isNotBlank()) return nickname
        val tail = if (currentUserId.length > 4) currentUserId.takeLast(4) else currentUserId
        return "기사$tail"
    }

    private fun resolveCurrentUserId(): String? {
        val currentUserId = normalizeUserId(authRepository.getCurrentUserId())
        if (_uiState.value.currentUserId != currentUserId) {
            _uiState.update { state ->
                state.copy(currentUserId = currentUserId)
            }
        }
        return currentUserId
    }

    private fun normalizeUserId(userId: String?): String? {
        val normalized = userId?.trim()
        return if (normalized.isNullOrBlank()) null else normalized
    }

    private enum class ReactionType {
        LIKE,
        DISLIKE
    }

    private data class MapBounds(
        val minLat: Double,
        val maxLat: Double,
        val minLng: Double,
        val maxLng: Double
    ) {
        fun toQueryKey(): String {
            return "${round(minLat)}:${round(maxLat)}:${round(minLng)}:${round(maxLng)}"
        }

        private fun round(value: Double): String {
            return String.format("%.3f", value)
        }

        fun contains(other: MapBounds): Boolean {
            return other.minLat >= minLat &&
                other.maxLat <= maxLat &&
                other.minLng >= minLng &&
                other.maxLng <= maxLng
        }

        fun withPadding(
            ratio: Double = MASTER_BOUNDS_PADDING_RATIO
        ): MapBounds {
            val latSpan = (maxLat - minLat).coerceAtLeast(0.01)
            val lngSpan = (maxLng - minLng).coerceAtLeast(0.01)
            val latPadding = latSpan * ratio
            val lngPadding = lngSpan * ratio

            return MapBounds(
                minLat = (minLat - latPadding).coerceAtLeast(-90.0),
                maxLat = (maxLat + latPadding).coerceAtMost(90.0),
                minLng = (minLng - lngPadding).coerceAtLeast(-180.0),
                maxLng = (maxLng + lngPadding).coerceAtMost(180.0)
            )
        }

        fun splitByLatitude(
            chunkSpan: Double = MASTER_LAT_CHUNK_SPAN,
            maxChunks: Int = MASTER_MAX_CHUNKS
        ): List<MapBounds> {
            val totalSpan = (maxLat - minLat).coerceAtLeast(0.0)
            if (totalSpan <= chunkSpan) return listOf(this)

            val chunkCount = ceil(totalSpan / chunkSpan)
                .toInt()
                .coerceIn(1, maxChunks)
            val perChunkSpan = totalSpan / chunkCount
            val ranges = mutableListOf<MapBounds>()
            var start = minLat

            repeat(chunkCount) { index ->
                val end = if (index == chunkCount - 1) maxLat else (start + perChunkSpan)
                ranges += MapBounds(
                    minLat = start,
                    maxLat = end,
                    minLng = minLng,
                    maxLng = maxLng
                )
                start = end
            }

            return ranges
        }
    }

    private data class MasterCacheEntry(
        val bounds: MapBounds,
        val toilets: List<Toilet>
    )
}
