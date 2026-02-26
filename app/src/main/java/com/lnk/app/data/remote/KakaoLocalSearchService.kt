package com.lnk.app.data.remote

import com.lnk.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class KakaoLocationResult(
    val latitude: Double,
    val longitude: Double,
    val label: String
)

class KakaoLocalSearchService(
    private val restApiKey: String = BuildConfig.KAKAO_REST_API_KEY
) {
    fun isConfigured(): Boolean = restApiKey.isNotBlank()

    suspend fun search(query: String): Result<KakaoLocationResult> = withContext(Dispatchers.IO) {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("검색어를 입력해 주세요."))
        }
        if (!isConfigured()) {
            return@withContext Result.failure(
                IllegalStateException("KAKAO_REST_API_KEY 설정이 필요합니다.")
            )
        }

        runCatching {
            searchKeyword(normalizedQuery) ?: searchAddress(normalizedQuery)
            ?: throw IllegalStateException("검색 결과가 없습니다. 주소나 건물명을 더 구체적으로 입력해 주세요.")
        }
    }

    private fun searchKeyword(query: String): KakaoLocationResult? {
        val json = requestJson(
            endpoint = "https://dapi.kakao.com/v2/local/search/keyword.json",
            query = query
        )
        val documents = json.optJSONArray("documents") ?: return null
        for (index in 0 until documents.length()) {
            val document = documents.optJSONObject(index) ?: continue
            val longitude = document.optString("x").toDoubleOrNull() ?: continue
            val latitude = document.optString("y").toDoubleOrNull() ?: continue
            val placeName = document.optString("place_name").trim()
            val roadAddress = document.optString("road_address_name").trim()
            val address = document.optString("address_name").trim()
            val label = if (placeName.isNotBlank()) {
                placeName
            } else {
                roadAddress.ifBlank { address }
            }
            return KakaoLocationResult(
                latitude = latitude,
                longitude = longitude,
                label = label.ifBlank { query }
            )
        }
        return null
    }

    private fun searchAddress(query: String): KakaoLocationResult? {
        val json = requestJson(
            endpoint = "https://dapi.kakao.com/v2/local/search/address.json",
            query = query
        )
        val documents = json.optJSONArray("documents") ?: return null
        for (index in 0 until documents.length()) {
            val document = documents.optJSONObject(index) ?: continue
            val longitude = document.optString("x").toDoubleOrNull() ?: continue
            val latitude = document.optString("y").toDoubleOrNull() ?: continue

            val roadAddress = document
                .optJSONObject("road_address")
                ?.optString("address_name")
                .orEmpty()
                .trim()
            val jibunAddress = document
                .optJSONObject("address")
                ?.optString("address_name")
                .orEmpty()
                .trim()
            val label = roadAddress.ifBlank { jibunAddress }.ifBlank { query }

            return KakaoLocationResult(
                latitude = latitude,
                longitude = longitude,
                label = label
            )
        }
        return null
    }

    private fun requestJson(endpoint: String, query: String): JSONObject {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = URL("$endpoint?query=$encodedQuery&size=10")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 7000
            readTimeout = 7000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Authorization", "KakaoAK $restApiKey")
        }

        return try {
            val responseCode = connection.responseCode
            val responseBody = (if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            })?.bufferedReader()?.use { it.readText() }.orEmpty()

            if (responseCode !in 200..299) {
                val reason = runCatching {
                    JSONObject(responseBody).optString("msg")
                }.getOrNull().orEmpty()
                val message = if (reason.isBlank()) "HTTP $responseCode" else reason
                throw IllegalStateException("카카오 검색 실패: $message")
            }

            JSONObject(responseBody)
        } finally {
            connection.disconnect()
        }
    }
}
