package com.lnk.app.data.remote

import com.lnk.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class GeocodingResult(
    val latitude: Double,
    val longitude: Double,
    val roadAddress: String,
    val jibunAddress: String
)

class NaverGeocodingService(
    private val keyId: String = BuildConfig.NAVER_MAP_NCP_KEY_ID,
    private val key: String = BuildConfig.NAVER_MAP_NCP_KEY
) {
    fun isConfigured(): Boolean = keyId.isNotBlank() && key.isNotBlank()

    suspend fun geocode(query: String): Result<GeocodingResult> = withContext(Dispatchers.IO) {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("검색어를 입력해 주세요."))
        }
        if (!isConfigured()) {
            return@withContext Result.failure(
                IllegalStateException("NAVER_MAP_NCP_KEY_ID / NAVER_MAP_NCP_KEY 설정이 필요합니다.")
            )
        }

        val candidates = buildQueryCandidates(normalizedQuery)
        for (candidate in candidates) {
            when (val response = requestGeocode(candidate)) {
                is GeocodeResponse.Success -> {
                    if (response.result != null) {
                        return@withContext Result.success(response.result)
                    }
                }

                is GeocodeResponse.Failure -> {
                    return@withContext Result.failure(response.error)
                }
            }
        }

        Result.failure(
            IllegalStateException("검색 결과가 없습니다. 주소나 명칭을 조금 더 구체적으로 입력해 주세요.")
        )
    }

    private fun requestGeocode(query: String): GeocodeResponse {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val endpoint =
            "https://maps.apigw.ntruss.com/map-geocode/v2/geocode?query=$encodedQuery&count=1"
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 7000
            readTimeout = 7000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("x-ncp-apigw-api-key-id", keyId)
            setRequestProperty("x-ncp-apigw-api-key", key)
        }

        return try {
            val responseCode = connection.responseCode
            val responseBody = (if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            })?.bufferedReader()?.use { it.readText() }.orEmpty()

            if (responseCode !in 200..299) {
                val message = runCatching { JSONObject(responseBody).optString("errorMessage") }
                    .getOrNull()
                val reason = if (message.isNullOrBlank()) "HTTP $responseCode" else message
                return GeocodeResponse.Failure(
                    IllegalStateException("주소 검색 실패: $reason")
                )
            }

            val json = JSONObject(responseBody)
            val addresses = json.optJSONArray("addresses") ?: return GeocodeResponse.Success(null)
            if (addresses.length() == 0) return GeocodeResponse.Success(null)

            val address = addresses.getJSONObject(0)
            val longitude = address.optString("x").toDoubleOrNull()
            val latitude = address.optString("y").toDoubleOrNull()
            if (latitude == null || longitude == null) {
                return GeocodeResponse.Failure(
                    IllegalStateException("검색 결과에서 좌표를 읽지 못했습니다.")
                )
            }

            GeocodeResponse.Success(
                GeocodingResult(
                    latitude = latitude,
                    longitude = longitude,
                    roadAddress = address.optString("roadAddress"),
                    jibunAddress = address.optString("jibunAddress")
                )
            )
        } catch (error: Exception) {
            GeocodeResponse.Failure(error)
        } finally {
            connection.disconnect()
        }
    }

    private fun buildQueryCandidates(query: String): List<String> {
        val normalizedSpace = query.replace("\\s+".toRegex(), " ").trim()
        val noDash = normalizedSpace.replace("-", " ")
        val noSpace = normalizedSpace.replace(" ", "")
        return listOf(
            normalizedSpace,
            noDash,
            noSpace,
            "$normalizedSpace 대한민국",
            "$normalizedSpace 서울특별시",
            "$noDash 서울특별시",
            "$noSpace 서울특별시"
        ).map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    private sealed interface GeocodeResponse {
        data class Success(val result: GeocodingResult?) : GeocodeResponse
        data class Failure(val error: Throwable) : GeocodeResponse
    }
}
