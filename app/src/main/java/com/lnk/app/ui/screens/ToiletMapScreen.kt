package com.lnk.app.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.lnk.app.data.model.SOURCE_MASTER
import com.lnk.app.data.model.Toilet
import com.lnk.app.toilet.ToiletViewModel
import com.naver.maps.geometry.LatLng
import com.naver.maps.geometry.LatLngBounds
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.LocationTrackingMode
import com.naver.maps.map.MapView
import com.naver.maps.map.NaverMap
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.OverlayImage
import com.naver.maps.map.util.FusedLocationSource

private val DefaultMapCenter = LatLng(37.5665, 126.9780)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToiletMapScreen(
    toiletViewModel: ToiletViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by toiletViewModel.uiState.collectAsState()
    val locationSource = remember { FusedLocationSource(context as Activity, 1000) }
    var visibleBounds by remember { mutableStateOf<VisibleBounds?>(null) }
    var showHiddenManager by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val visibleTypeCounts = remember(uiState.visibleMasterToilets, visibleBounds) {
        countToiletsByTypeInBounds(uiState.visibleMasterToilets, visibleBounds)
    }
    val hiddenItems = remember(uiState.hiddenToiletIds, uiState.hiddenToiletLabels) {
        uiState.hiddenToiletIds
            .map { hiddenId ->
                HiddenToiletItem(
                    toiletId = hiddenId,
                    label = uiState.hiddenToiletLabels[hiddenId].orEmpty().ifBlank { hiddenId }
                )
            }
            .sortedBy { item -> item.label }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            toiletViewModel.clearErrorMessage()
        }
    }

    LaunchedEffect(uiState.lastHiddenToiletId) {
        val hiddenId = uiState.lastHiddenToiletId ?: return@LaunchedEffect
        val hiddenLabel = uiState.hiddenToiletLabels[hiddenId].orEmpty().ifBlank { "화장실" }
        val result = snackbarHostState.showSnackbar(
            message = "\"$hiddenLabel\" 숨김 처리됨",
            actionLabel = "되돌리기",
            withDismissAction = true
        )
        if (result == SnackbarResult.ActionPerformed) {
            toiletViewModel.unhideToilet(hiddenId)
        }
        toiletViewModel.consumeLastHiddenToilet()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "화장실 지도",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showHiddenManager = true }) {
                        Icon(
                            imageVector = Icons.Default.VisibilityOff,
                            contentDescription = "숨긴 화장실 관리"
                        )
                    }
                    IconButton(onClick = toiletViewModel::loadToilets) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "새로고침"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                )
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            var naverMapInstance by remember { mutableStateOf<NaverMap?>(null) }

            LaunchedEffect(
                uiState.pendingFocusToiletId,
                uiState.pendingFocusLatitude,
                uiState.pendingFocusLongitude,
                uiState.masterToilets,
                uiState.userToilets,
                naverMapInstance
            ) {
                val focusToiletId = uiState.pendingFocusToiletId ?: return@LaunchedEffect
                val map = naverMapInstance
                val loadedToilet = (uiState.masterToilets + uiState.userToilets)
                    .firstOrNull { toilet -> toilet.id == focusToiletId }

                if (loadedToilet != null) {
                    val target = LatLng(loadedToilet.lat, loadedToilet.lng)
                    map?.moveCamera(CameraUpdate.scrollTo(target))
                    map?.moveCamera(CameraUpdate.zoomTo(16.0))
                    toiletViewModel.selectToilet(loadedToilet)
                    toiletViewModel.consumePendingFocusToilet()
                    return@LaunchedEffect
                }

                val lat = uiState.pendingFocusLatitude ?: return@LaunchedEffect
                val lng = uiState.pendingFocusLongitude ?: return@LaunchedEffect
                map?.moveCamera(CameraUpdate.scrollTo(LatLng(lat, lng)))
                map?.moveCamera(CameraUpdate.zoomTo(16.0))
            }

            ToiletMapView(
                toilets = uiState.visibleMasterToilets,
                onMarkerClick = toiletViewModel::selectToilet,
                onVisibleBoundsChanged = { minLat, maxLat, minLng, maxLng ->
                    visibleBounds = VisibleBounds(
                        minLat = minOf(minLat, maxLat),
                        maxLat = maxOf(minLat, maxLat),
                        minLng = minOf(minLng, maxLng),
                        maxLng = maxOf(minLng, maxLng)
                    )
                    toiletViewModel.onMapBoundsChanged(minLat, maxLat, minLng, maxLng)
                },
                locationSource = locationSource,
                onMapReady = { naverMapInstance = it },
                modifier = Modifier.fillMaxSize()
            )

            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                    .fillMaxWidth(0.9f),
                shape = MaterialTheme.shapes.extraLarge,
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "현재 화면 화장실",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "개방 ${visibleTypeCounts.openCount} | 공중 ${visibleTypeCounts.publicCount} | 사설 ${visibleTypeCounts.privateCount} | 기타 ${visibleTypeCounts.otherCount}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                    if (uiState.isMasterLoading) {
                        Text(
                            text = "지도 데이터 갱신 중...",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 84.dp),
                horizontalAlignment = Alignment.End
            ) {
                FloatingActionButton(
                    onClick = {
                        naverMapInstance?.locationTrackingMode = LocationTrackingMode.Follow
                    },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = "내 위치")
                }
            }

            MapLegend(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 16.dp)
            )

            if (uiState.isLoading && uiState.visibleMasterToilets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }

    uiState.selectedToilet?.let { toilet ->
        ToiletDetailSheet(
            toilet = toilet,
            currentUserId = uiState.currentUserId,
            comments = uiState.comments,
            isCommentLoading = uiState.isCommentLoading,
            isCommentSaving = uiState.isCommentSaving,
            onDismiss = { toiletViewModel.selectToilet(null) },
            onToggleLike = toiletViewModel::toggleLike,
            onToggleDislike = toiletViewModel::toggleDislike,
            onAddComment = toiletViewModel::addComment,
            onUpdateComment = toiletViewModel::updateComment,
            onDeleteComment = toiletViewModel::deleteComment,
            onHideToilet = toiletViewModel::hideSelectedToilet
        )
    }

    if (showHiddenManager) {
        HiddenToiletManagerSheet(
            hiddenItems = hiddenItems,
            onDismiss = { showHiddenManager = false },
            onUnhide = toiletViewModel::unhideToilet,
            onUnhideAll = toiletViewModel::unhideAllToilets
        )
    }
}

@Composable
private fun MapLegend(modifier: Modifier = Modifier) {
    val legendItems = listOf(
        "open" to "개방",
        "public" to "공중",
        "private" to "사설",
        "other" to "기타"
    )
    val typeColors = mapOf(
        "open" to Color(0xFF2196F3),
        "public" to Color(0xFF4CAF50),
        "private" to Color(0xFF9C27B0),
        "other" to Color(0xFF9E9E9E)
    )

    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(
                text = "범례",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            legendItems.forEach { (type, label) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(typeColors[type]!!, androidx.compose.foundation.shape.CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HiddenToiletManagerSheet(
    hiddenItems: List<HiddenToiletItem>,
    onDismiss: () -> Unit,
    onUnhide: (String) -> Unit,
    onUnhideAll: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .padding(bottom = 20.dp)
        ) {
            Text(
                text = "숨긴 화장실 관리",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "총 ${hiddenItems.size}건",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
            )

            if (hiddenItems.isEmpty()) {
                Text(
                    text = "숨긴 화장실이 없습니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    hiddenItems.forEach { hiddenItem ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = hiddenItem.label,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { onUnhide(hiddenItem.toiletId) }) {
                                Text("복원")
                            }
                        }
                        HorizontalDivider()
                    }
                }
                Button(
                    onClick = onUnhideAll,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Text("모두 복원")
                }
            }
        }
    }
}

@Composable
private fun ToiletMapView(
    toilets: List<Toilet>,
    onMarkerClick: (Toilet) -> Unit,
    onVisibleBoundsChanged: (Double, Double, Double, Double) -> Unit,
    locationSource: FusedLocationSource,
    onMapReady: (NaverMap) -> Unit,
    modifier: Modifier = Modifier
) {
    val mapView = rememberMapViewWithLifecycle()
    var naverMap by remember { mutableStateOf<NaverMap?>(null) }
    val markerMap = remember { mutableStateMapOf<String, Marker>() }
    val validToilets = remember(toilets) {
        toilets.filter { toilet ->
            toilet.lat in -90.0..90.0 && toilet.lng in -180.0..180.0
        }
    }

    AndroidView(
        modifier = modifier,
        factory = {
            mapView.apply {
                getMapAsync { map ->
                    naverMap = map
                    map.locationSource = locationSource
                    map.locationTrackingMode = LocationTrackingMode.NoFollow

                    map.uiSettings.isLocationButtonEnabled = false
                    map.uiSettings.isZoomControlEnabled = true
                    map.uiSettings.isZoomGesturesEnabled = true
                    map.uiSettings.isScrollGesturesEnabled = true
                    map.uiSettings.isLogoClickEnabled = false

                    val firstPosition = validToilets.firstOrNull()?.let { LatLng(it.lat, it.lng) }
                        ?: DefaultMapCenter
                    val zoom = if (validToilets.isEmpty()) 14.0 else 15.0
                    map.moveCamera(CameraUpdate.scrollTo(firstPosition))
                    map.moveCamera(CameraUpdate.zoomTo(zoom))

                    map.addOnCameraIdleListener {
                        notifyVisibleBounds(map, onVisibleBoundsChanged)
                    }
                    notifyVisibleBounds(map, onVisibleBoundsChanged)
                    onMapReady(map)
                }
            }
        }
    )

    LaunchedEffect(naverMap, validToilets) {
        val map = naverMap ?: return@LaunchedEffect
        val toiletsById = validToilets.associateBy { toilet -> toilet.id }
        val idsToRemove = markerMap.keys.filter { markerId ->
            !toiletsById.containsKey(markerId)
        }

        idsToRemove.forEach { markerId ->
            markerMap.remove(markerId)?.let { marker ->
                marker.map = null
            }
        }

        validToilets.forEach { toilet ->
            val markerId = toilet.id
            val tintColor = when (toilet.type.lowercase()) {
                "open" -> Color(0xFF2196F3).toArgb()
                "public" -> Color(0xFF4CAF50).toArgb()
                "private" -> Color(0xFF9C27B0).toArgb()
                else -> Color(0xFF9E9E9E).toArgb()
            }

            val existing = markerMap[markerId]
            if (existing != null) {
                existing.position = LatLng(toilet.lat, toilet.lng)
                existing.captionText = toiletMarkerLabel(toilet)
                existing.iconTintColor = tintColor
                existing.tag = toilet
                if (existing.map != map) {
                    existing.map = map
                }
            } else {
                val marker = Marker().apply {
                    position = LatLng(toilet.lat, toilet.lng)
                    captionText = toiletMarkerLabel(toilet)
                    icon = OverlayImage.fromResource(com.lnk.app.R.drawable.ic_restroom_marker)
                    iconTintColor = tintColor
                    width = 80
                    height = 110
                    tag = toilet
                    this.map = map
                    setOnClickListener { overlay ->
                        val currentToilet = (overlay.tag as? Toilet) ?: toilet
                        onMarkerClick(currentToilet)
                        true
                    }
                }
                markerMap[markerId] = marker
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            markerMap.values.forEach { marker -> marker.map = null }
            markerMap.clear()
        }
    }
}

private fun notifyVisibleBounds(
    map: NaverMap,
    onVisibleBoundsChanged: (Double, Double, Double, Double) -> Unit
) {
    val bounds: LatLngBounds = map.contentBounds
    onVisibleBoundsChanged(
        bounds.southWest.latitude,
        bounds.northEast.latitude,
        bounds.southWest.longitude,
        bounds.northEast.longitude
    )
}

@Composable
private fun rememberMapViewWithLifecycle(): MapView {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val mapView = remember { MapView(context).apply { onCreate(null) } }

    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }

        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }

    return mapView
}

private fun toiletMarkerLabel(toilet: Toilet): String {
    val descriptionParts = toilet.description
        .split("|")
        .map { part -> part.trim() }
        .filter { part -> part.isNotEmpty() && !part.startsWith("open:", ignoreCase = true) }

    val fallback = when (toilet.type.lowercase()) {
        "open" -> "개방 화장실"
        "public" -> "공중 화장실"
        "private" -> "사설 화장실"
        else -> if (toilet.source == SOURCE_MASTER) "공공 화장실" else "등록 화장실"
    }

    val baseLabel = descriptionParts.firstOrNull().orEmpty().ifBlank { fallback }
    return if (baseLabel.length > 12) "${baseLabel.take(12)}..." else baseLabel
}

private data class VisibleBounds(
    val minLat: Double,
    val maxLat: Double,
    val minLng: Double,
    val maxLng: Double
)

private data class VisibleTypeCounts(
    val openCount: Int = 0,
    val publicCount: Int = 0,
    val privateCount: Int = 0,
    val otherCount: Int = 0
)

private data class HiddenToiletItem(
    val toiletId: String,
    val label: String
)

private fun countToiletsByTypeInBounds(
    toilets: List<Toilet>,
    bounds: VisibleBounds?
): VisibleTypeCounts {
    val visibleToilets = if (bounds == null) {
        toilets
    } else {
        toilets.filter { toilet ->
            toilet.lat in bounds.minLat..bounds.maxLat &&
                toilet.lng in bounds.minLng..bounds.maxLng
        }
    }

    var openCount = 0
    var publicCount = 0
    var privateCount = 0
    var otherCount = 0

    visibleToilets.forEach { toilet ->
        when (toilet.type.lowercase()) {
            "open" -> openCount += 1
            "public" -> publicCount += 1
            "private" -> privateCount += 1
            else -> otherCount += 1
        }
    }

    return VisibleTypeCounts(
        openCount = openCount,
        publicCount = publicCount,
        privateCount = privateCount,
        otherCount = otherCount
    )
}
