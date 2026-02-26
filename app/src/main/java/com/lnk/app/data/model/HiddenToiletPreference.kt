package com.lnk.app.data.model

data class HiddenToiletPreference(
    val hiddenToiletIds: Set<String> = emptySet(),
    val hiddenToiletLabels: Map<String, String> = emptyMap()
)
