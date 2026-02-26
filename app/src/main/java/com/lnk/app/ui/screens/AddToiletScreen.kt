package com.lnk.app.ui.screens

import android.view.MotionEvent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.lnk.app.data.remote.KakaoLocalSearchService
import com.lnk.app.toilet.ToiletViewModel
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.MapView
import com.naver.maps.map.NaverMap
import com.naver.maps.map.overlay.Marker
import kotlinx.coroutines.launch

private val DefaultPickLocation = LatLng(37.5665, 126.9780)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToiletScreen(
    toiletViewModel: ToiletViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val uiState by toiletViewModel.uiState.collectAsState()
    val kakaoSearchService = remember { KakaoLocalSearchService() }
    val scope = rememberCoroutineScope()

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var searchedAddressLabel by rememberSaveable { mutableStateOf("") }
    var isSearching by rememberSaveable { mutableStateOf(false) }
    var selectedType by rememberSaveable { mutableStateOf("public") }
    var description by rememberSaveable { mutableStateOf("") }
    var typeExpanded by rememberSaveable { mutableStateOf(false) }
    var selectedLat by rememberSaveable { mutableStateOf(DefaultPickLocation.latitude) }
    var selectedLng by rememberSaveable { mutableStateOf(DefaultPickLocation.longitude) }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            toiletViewModel.clearErrorMessage()
        }
    }

    LaunchedEffect(uiState.lastAddedToiletId) {
        if (!uiState.lastAddedToiletId.isNullOrBlank()) {
            Toast.makeText(context, "화장실이 등록되었습니다.", Toast.LENGTH_SHORT).show()
            toiletViewModel.consumeLastAddedToilet()
            onSaved()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("화장실 추가") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors()
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "주소 또는 명칭(예: 강동구청)을 검색하거나 지도에서 위치를 선택해 주세요.",
                style = MaterialTheme.typography.bodyMedium
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("주소/명칭 검색") },
                singleLine = true
            )

            Button(
                onClick = {
                    scope.launch {
                        val query = searchQuery.trim()
                        if (query.isBlank()) {
                            Toast.makeText(context, "검색어를 입력해 주세요.", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        isSearching = true
                        kakaoSearchService.search(query)
                            .onSuccess { location ->
                                selectedLat = location.latitude
                                selectedLng = location.longitude
                                searchedAddressLabel = location.label
                                Toast.makeText(context, "위치를 찾았습니다.", Toast.LENGTH_SHORT).show()
                            }
                            .onFailure { error ->
                                Toast.makeText(
                                    context,
                                    error.message ?: "검색에 실패했습니다.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        isSearching = false
                    }
                },
                enabled = !isSearching,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isSearching) "검색 중..." else "검색")
            }

            if (searchedAddressLabel.isNotBlank()) {
                Text(
                    text = "검색 결과: $searchedAddressLabel",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            AddToiletMapPicker(
                selectedLat = selectedLat,
                selectedLng = selectedLng,
                onLocationSelected = { location ->
                    selectedLat = location.latitude
                    selectedLng = location.longitude
                }
            )

            Column {
                Text(
                    text = "유형",
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(modifier = Modifier.height(6.dp))
                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = !typeExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedTypeLabel(selectedType),
                        onValueChange = {},
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        readOnly = true,
                        singleLine = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded)
                        }
                    )
                    DropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        listOf("open", "public", "private").forEach { type ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = selectedTypeLabel(type),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                onClick = {
                                    selectedType = type
                                    typeExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                label = { Text("설명") },
                minLines = 4
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    toiletViewModel.addToilet(
                        latitudeInput = selectedLat.toString(),
                        longitudeInput = selectedLng.toString(),
                        type = selectedType,
                        description = description
                    )
                },
                enabled = !uiState.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (uiState.isSaving) "저장 중..." else "저장")
            }
        }
    }
}

@Composable
private fun AddToiletMapPicker(
    selectedLat: Double,
    selectedLng: Double,
    onLocationSelected: (LatLng) -> Unit
) {
    val mapView = rememberMapViewWithLifecycle()
    var naverMap by remember { mutableStateOf<NaverMap?>(null) }
    val marker = remember { Marker() }
    val selectedLocation = remember(selectedLat, selectedLng) { LatLng(selectedLat, selectedLng) }

    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp),
        factory = {
            mapView.apply {
                setOnTouchListener { view, motionEvent ->
                    when (motionEvent.actionMasked) {
                        MotionEvent.ACTION_DOWN,
                        MotionEvent.ACTION_MOVE,
                        MotionEvent.ACTION_POINTER_DOWN -> {
                            view.parent?.requestDisallowInterceptTouchEvent(true)
                        }

                        MotionEvent.ACTION_UP,
                        MotionEvent.ACTION_CANCEL -> {
                            view.parent?.requestDisallowInterceptTouchEvent(false)
                        }
                    }
                    false
                }
                getMapAsync { map ->
                    naverMap = map
                    map.uiSettings.isZoomControlEnabled = true
                    map.uiSettings.isZoomGesturesEnabled = true
                    map.uiSettings.isScrollGesturesEnabled = true
                    map.moveCamera(CameraUpdate.scrollTo(selectedLocation))
                    map.moveCamera(CameraUpdate.zoomTo(14.0))
                    map.setOnMapClickListener { _, latLng ->
                        onLocationSelected(latLng)
                    }
                }
            }
        }
    )

    LaunchedEffect(naverMap, selectedLocation) {
        val map = naverMap ?: return@LaunchedEffect
        map.moveCamera(CameraUpdate.scrollTo(selectedLocation))
        marker.position = selectedLocation
        marker.captionText = "선택 위치"
        marker.map = map
    }

    DisposableEffect(Unit) {
        onDispose {
            marker.map = null
        }
    }
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
        onDispose { lifecycle.removeObserver(observer) }
    }

    return mapView
}

private fun selectedTypeLabel(type: String): String {
    return when (type.lowercase()) {
        "open" -> "개방"
        "public" -> "공중"
        "private" -> "사설"
        else -> type
    }
}
