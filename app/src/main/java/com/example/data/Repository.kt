package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DelalaRepository(
    private val database: AppDatabase,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val userDao = database.userDao()
    private val listingDao = database.listingDao()
    private val wantedRequestDao = database.wantedRequestDao()
    private val feedbackDao = database.feedbackDao()
    private val reportDao = database.reportDao()

    // Flow listings and requests
    val allListings: Flow<List<ListingEntity>> = listingDao.getAllListings()
    val allRequests: Flow<List<WantedRequestEntity>> = wantedRequestDao.getAllRequests()
    val allFeedback: Flow<List<FeedbackEntity>> = feedbackDao.getAllFeedback()
    val allReports: Flow<List<ReportEntity>> = reportDao.getAllReports()
    val allUsers: Flow<List<UserEntity>> = userDao.getAllUsers()

    init {
        // Pre-populate with realistic mock listings if DB is empty,
        // so first-time users can immediately experience filtering, searching, and calling brokers.
        scope.launch {
            try {
                val currentListings = allListings.first()
                if (currentListings.isEmpty()) {
                    createDefaultListings()
                }
                
                // Add Admin account if not exists
                val adminUser = userDao.getUserByPhone("0953348822")
                if (adminUser == null) {
                    val defaultAdmin = UserEntity(
                        phone = "0953348822",
                        name = "Ephraim (Delala Admin)",
                        email = "ephraim@delala.app",
                        location = "Dire Dawa",
                        role = "Admin",
                        verified = true
                    )
                    userDao.insertUser(defaultAdmin)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun getUserByPhone(phone: String): UserEntity? {
        return userDao.getUserByPhone(phone)
    }

    suspend fun registerUser(user: UserEntity) {
        userDao.insertUser(user)
        scope.launch {
            SupabaseClient.insertUser(user)
        }
    }

    suspend fun updateUserVerification(phone: String, verified: Boolean) {
        userDao.updateUserVerification(phone, verified)
    }

    suspend fun deleteUser(phone: String) {
        userDao.deleteUser(phone)
    }

    suspend fun addListing(listing: ListingEntity) {
        listingDao.insertListing(listing)
        scope.launch {
            SupabaseClient.insertListing(listing)
        }
    }

    suspend fun deleteListing(id: Int) {
        listingDao.deleteListing(id)
    }

    suspend fun addWantedRequest(request: WantedRequestEntity) {
        wantedRequestDao.insertRequest(request)
        scope.launch {
            SupabaseClient.insertWantedRequest(request)
        }
    }

    suspend fun deleteWantedRequest(id: Int) {
        wantedRequestDao.deleteRequest(id)
    }

    suspend fun addFeedback(feedback: FeedbackEntity) {
        feedbackDao.insertFeedback(feedback)
        scope.launch {
            SupabaseClient.insertFeedback(feedback)
        }
    }

    suspend fun addReport(report: ReportEntity) {
        reportDao.insertReport(report)
        scope.launch {
            SupabaseClient.insertReport(report)
        }
    }

    suspend fun deleteReport(id: Int) {
        reportDao.deleteReport(id)
    }

    private suspend fun createDefaultListings() {
        val defaultListings = listOf(
            ListingEntity(
                sellerId = "0944112233",
                sellerName = "Yared Solomon",
                category = "Electronics",
                title = "iPhone 15 Pro",
                description = "Brand new, sealed imports. Space Gray color, 256GB storage capacity. Perfect for student traders! In-person meetups available near university gates.",
                condition = "New",
                price = 105000.0,
                imageUri = "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=450&auto=format&fit=crop",
                location = "Dire Dawa"
            ),
            ListingEntity(
                sellerId = "0955667788",
                sellerName = "Salem Kebede",
                category = "Wearables",
                title = "Adidas Campus Shoes",
                description = "Classic campus edition sneakers, size 42. Only worn twice, looks clean as new. Selling quick because I need money for university books.",
                condition = "Medium Used",
                price = 4500.0,
                imageUri = "https://images.unsplash.com/photo-1549298916-b41d501d3772?w=450&auto=format&fit=crop",
                location = "Moyale"
            ),
            ListingEntity(
                sellerId = "0911223344",
                sellerName = "Dawit Broker (Delala)",
                category = "Jewelry",
                title = "Pure Silver Ethiopian Cross Ring",
                description = "Premium traditional handcarved Ethiopian silver cross ring. Excellent craftsmanship. I am acting as a middle-man broker for a local shop, direct deals accepted.",
                condition = "New",
                price = 3200.0,
                imageUri = "https://images.unsplash.com/photo-1605100804763-247f67b3557e?w=450&auto=format&fit=crop",
                location = "Other"
            ),
            ListingEntity(
                sellerId = "0988776655",
                sellerName = "Aster Abera",
                category = "Perfume",
                title = "Sauvage Dior Cologne",
                description = "Original import cologne. 100ml remaining. Distinct luxury wood fragrance. High verification and highly requested item.",
                condition = "New",
                price = 8500.0,
                imageUri = "https://images.unsplash.com/photo-1541643600914-78b084683601?w=450&auto=format&fit=crop",
                location = "Dire Dawa"
            ),
            ListingEntity(
                sellerId = "0944112233",
                sellerName = "Yared Solomon",
                category = "Electronics",
                title = "Air Fryer & Juicer Bundle",
                description = "Double kit, includes a 4L air fryer and high speed nutrient extractor juicer. Purchased last semester, moving out from campus hence the heavy discount price.",
                condition = "Medium Used",
                price = 9000.0,
                imageUri = "https://images.unsplash.com/photo-1585238342024-78d387f4a707?w=450&auto=format&fit=crop",
                location = "Dire Dawa"
            )
        )

        for (listing in defaultListings) {
            listingDao.insertListing(listing)
        }
    }
}
