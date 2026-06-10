package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val phone: String, // We use Phone as primary key / unique login identifier
    val name: String,
    val email: String?,
    val location: String, // Dire Dawa, Moyale, Other
    val role: String, // Buyer, Seller, Admin
    val verified: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "listings")
data class ListingEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sellerId: String, // Seller's phone
    val sellerName: String,
    val category: String, // Electronics, Wearables, Jewelry, etc.
    val title: String,
    val description: String,
    val condition: String, // New, Medium Used, Old
    val price: Double,
    val imageUri: String, // Sample image ID or local drawable label
    val location: String, // Dire Dawa, Moyale, Other
    val createdAt: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "wanted_requests")
data class WantedRequestEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val buyerId: String, // Buyer's phone
    val buyerName: String,
    val category: String,
    val productWanted: String,
    val description: String,
    val budget: Double,
    val location: String,
    val createdAt: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "feedbacks")
data class FeedbackEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String,
    val userName: String,
    val rating: Int,
    val comment: String,
    val createdAt: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "reports")
data class ReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val reporterId: String,
    val targetType: String, // "Listing" or "User"
    val targetId: String, // Listing ID or User Phone
    val targetName: String,
    val reason: String,
    val createdAt: Long = System.currentTimeMillis()
) : Serializable

data class SupabaseOrder(
    val id: Long,
    val customerName: String,
    val email: String,
    val phone: String,
    val city: String,
    val address: String,
    val productName: String,
    val productVariant: String,
    val quantity: Int,
    val status: String,
    val createdAt: String,
    val country: String = "Ethiopia",
    val notes: String = ""
) : Serializable

