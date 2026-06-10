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

    suspend fun insertOrder(
        customerName: String,
        phone: String,
        email: String,
        city: String,
        address: String,
        country: String,
        productName: String,
        productVariant: String,
        quantity: Int,
        notes: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("customer_name", customerName)
                put("phone", phone)
                put("email", if (email.isBlank()) JSONObject.NULL else email)
                put("city", city)
                put("address", address)
                put("country", country)
                put("product_name", productName)
                put("product_variant", if (productVariant.isBlank()) JSONObject.NULL else productVariant)
                put("quantity", quantity)
                put("notes", if (notes.isBlank()) JSONObject.NULL else notes)
                put("status", "pending")
            }.toString()

            val appointmentsUrl = "https://ycikjhfmwkzhtcstucyg.supabase.co/rest/v1/appointments"
            val appointmentsKey = "sb_publishable_PyvV2NWIBoQEF3hZHErHtA_65N53ybM"

            addLog("POST Appointment to Supabase: $productName for $customerName")
            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val body = json.toRequestBody(mediaType)
            val request = Request.Builder()
                .url(appointmentsUrl)
                .addHeader("apikey", appointmentsKey)
                .addHeader("Authorization", "Bearer $appointmentsKey")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    lastSyncStatus = "Appointment submitted to Supabase successfully"
                    addLog("Supabase Appointment success: Code ${response.code}")
                    true
                } else {
                    lastSyncStatus = "Supabase Appointment POST failed: Code ${response.code}"
                    addLog("Supabase Appointment error: Code ${response.code}")
                    false
                }
            }
        } catch (e: Exception) {
            lastSyncStatus = "Supabase Appointment connection failed: ${e.message}"
            addLog("Supabase Appointment error during insert: ${e.message}")
            false
        }
    }

    private fun buildRequestForOrders(endpoint: String, method: String, jsonBody: String? = null): Request {
        val url = "$SUPABASE_URL/$endpoint"
        val builder = Request.Builder()
            .url(url)
            .addHeader("apikey", API_KEY)
            .addHeader("Authorization", "Bearer $API_KEY")
            .addHeader("Content-Type", "application/json")
        
        if (jsonBody != null) {
            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val body = jsonBody.toRequestBody(mediaType)
            builder.method(method, body)
        } else {
            builder.method(method, null)
        }
        return builder.build()
    }

    suspend fun fetchOrders(): List<SupabaseOrder> = withContext(Dispatchers.IO) {
        try {
            val urlWithQuery = "orders?select=*&order=id.desc"
            val request = buildRequestForOrders(urlWithQuery, "GET")
            
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: "[]"
                    val jsonArray = JSONArray(bodyStr)
                    val list = mutableListOf<SupabaseOrder>()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val id = obj.optLong("id", 0L)
                        val customerName = obj.optString("customer_name", "")
                        val email = obj.optString("email", "")
                        val phone = obj.optString("phone", "")
                        val city = obj.optString("city", "")
                        val address = obj.optString("address", "")
                        val productName = obj.optString("product_name", "")
                        val productVariant = obj.optString("product_variant", "")
                        val quantity = obj.optInt("quantity", 1)
                        val status = obj.optString("status", "pending")
                        val createdAt = obj.optString("created_at", "")
                        val country = obj.optString("country", "Ethiopia")
                        val notes = obj.optString("notes", "")

                        list.add(
                            SupabaseOrder(
                                id = id,
                                customerName = customerName,
                                email = email,
                                phone = phone,
                                city = city,
                                address = address,
                                productName = productName,
                                productVariant = productVariant,
                                quantity = quantity,
                                status = status,
                                createdAt = createdAt,
                                country = country,
                                notes = notes
                            )
                        )
                    }
                    list
                } else {
                    addLog("Supabase GET Orders failed: Code ${response.code}")
                    emptyList()
                }
            }
        } catch (e: Exception) {
            addLog("Supabase GET Orders failed with error: ${e.message}")
            emptyList()
        }
    }

    suspend fun updateOrderStatus(orderId: Long, newStatus: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("status", newStatus)
            }.toString()

            val urlWithQuery = "orders?id=eq.$orderId"
            addLog("PATCH Order $orderId to $newStatus")
            
            val request = buildRequestForOrders(urlWithQuery, "PATCH", json)
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    addLog("Supabase Order status update success: $orderId -> $newStatus")
                    true
                } else {
                    addLog("Supabase Order PATCH failed: Code ${response.code}")
                    false
                }
            }
        } catch (e: Exception) {
            addLog("Supabase Order PATCH error: ${e.message}")
            false
        }
    }
}

