package com.example.data

import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SupabaseClient {
    private const val TAG = "SupabaseClient"
    private const val SUPABASE_URL = "https://dikmpybsherlbejsywrg.supabase.co/rest/v1"
    private const val API_KEY = "sb_publishable_KV9IHauca_fqiVsrh4b6lQ_-sGTic37"

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    // Status tracking for UI logging
    var lastSyncStatus: String = "Not initialized"
        private set
    var syncLog: List<String> = emptyList()
        private set

    private fun addLog(message: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        val formattedLog = "[$timestamp] $message"
        Log.d(TAG, formattedLog)
        syncLog = (syncLog + formattedLog).takeLast(25) // Keep last 25 logs
    }

    private fun buildBaseRequest(endpoint: String, method: String, jsonBody: String? = null): Request {
        val url = "$SUPABASE_URL/$endpoint"
        val builder = Request.Builder()
            .url(url)
            .addHeader("apikey", API_KEY)
            .addHeader("Authorization", "Bearer $API_KEY")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=minimal") // Minimal return size for efficiency

        if (jsonBody != null) {
            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val body = jsonBody.toRequestBody(mediaType)
            builder.method(method, body)
        } else {
            builder.method(method, null)
        }
        return builder.build()
    }

    suspend fun insertUser(user: UserEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("phone", user.phone)
                put("name", user.name)
                put("email", user.email ?: JSONObject.NULL)
                put("location", user.location)
                put("role", user.role)
                put("verified", user.verified)
                put("createdAt", user.createdAt)
            }.toString()

            addLog("POST User to Supabase for unique ID: ${user.phone}")
            val request = buildBaseRequest("users", "POST", json)
            
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    lastSyncStatus = "User registered on Supabase successfully"
                    addLog("Supabase registration success: Code ${response.code}")
                    true
                } else {
                    lastSyncStatus = "Supabase User POST failed: Code ${response.code}"
                    addLog("Supabase error: ${response.body?.string() ?: "Empty body"}")
                    false
                }
            }
        } catch (e: Exception) {
            lastSyncStatus = "Supabase User connection failed: ${e.message}"
            addLog("Supabase error during User insert: ${e.message}")
            false
        }
    }

    suspend fun insertListing(listing: ListingEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("sellerId", listing.sellerId)
                put("sellerName", listing.sellerName)
                put("category", listing.category)
                put("title", listing.title)
                put("description", listing.description)
                put("condition", listing.condition)
                put("price", listing.price)
                put("imageUri", listing.imageUri)
                put("location", listing.location)
                put("createdAt", listing.createdAt)
            }.toString()

            addLog("POST Listing to Supabase: ${listing.title} (${listing.category})")
            val request = buildBaseRequest("listings", "POST", json)

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    lastSyncStatus = "Listing published on Supabase successfully"
                    addLog("Supabase listing success: Code ${response.code}")
                    true
                } else {
                    lastSyncStatus = "Supabase Listing POST failed: Code ${response.code}"
                    addLog("Supabase listing push error: Code ${response.code}")
                    false
                }
            }
        } catch (e: Exception) {
            lastSyncStatus = "Supabase Listing sync offline: ${e.message}"
            addLog("Supabase Listing offline: ${e.message}")
            false
        }
    }

    suspend fun insertWantedRequest(wanted: WantedRequestEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("buyerId", wanted.buyerId)
                put("buyerName", wanted.buyerName)
                put("category", wanted.category)
                put("productWanted", wanted.productWanted)
                put("description", wanted.description)
                put("budget", wanted.budget)
                put("location", wanted.location)
                put("createdAt", wanted.createdAt)
            }.toString()

            addLog("POST WantedRequest to Supabase: ${wanted.productWanted}")
            val request = buildBaseRequest("wanted_requests", "POST", json)

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    lastSyncStatus = "WantedRequest uploaded to Supabase"
                    addLog("Supabase WantedRequest success: Code ${response.code}")
                    true
                } else {
                    lastSyncStatus = "Supabase Request upload failed: Code ${response.code}"
                    addLog("Supabase Request error: Code ${response.code}")
                    false
                }
            }
        } catch (e: Exception) {
            lastSyncStatus = "Supabase Request sync offline: ${e.message}"
            addLog("Supabase Request offline: ${e.message}")
            false
        }
    }

    suspend fun insertFeedback(feedback: FeedbackEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("userId", feedback.userId)
                put("userName", feedback.userName)
                put("rating", feedback.rating)
                put("comment", feedback.comment)
                put("createdAt", feedback.createdAt)
            }.toString()

            addLog("POST Feedback to Supabase: ${feedback.rating} stars")
            val request = buildBaseRequest("feedbacks", "POST", json)

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    lastSyncStatus = "Feedback posted to Supabase"
                    addLog("Supabase Feedback posted: Code ${response.code}")
                    true
                } else {
                    lastSyncStatus = "Supabase Feedback POST failed: Code ${response.code}"
                    addLog("Supabase Feedback error: Code ${response.code}")
                    false
                }
            }
        } catch (e: Exception) {
            lastSyncStatus = "Supabase Feedback offline: ${e.message}"
            addLog("Supabase Feedback offline: ${e.message}")
            false
        }
    }

    suspend fun insertReport(report: ReportEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("reporterId", report.reporterId)
                put("targetType", report.targetType)
                put("targetId", report.targetId)
                put("targetName", report.targetName)
                put("reason", report.reason)
                put("createdAt", report.createdAt)
            }.toString()

            addLog("POST Report on ${report.targetType} to Supabase")
            val request = buildBaseRequest("reports", "POST", json)

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    lastSyncStatus = "Report submitted to Supabase"
                    addLog("Supabase Report submitted: Code ${response.code}")
                    true
                } else {
                    lastSyncStatus = "Supabase Report submission failed: Code ${response.code}"
                    addLog("Supabase Report error: Code ${response.code}")
                    false
                }
            }
        } catch (e: Exception) {
            lastSyncStatus = "Supabase Report offline: ${e.message}"
            addLog("Supabase Report offline: ${e.message}")
            false
        }
    }
}
