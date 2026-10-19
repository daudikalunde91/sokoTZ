package com.example.sokotz.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class UserRole {
    CUSTOMER,
    BUSINESS_OWNER,
    ADMIN
}

enum class ListingType {
    PRODUCT,
    SERVICE
}

enum class ListingStatus {
    PUBLISHED,
    PENDING_REVIEW,
    DRAFT
}

enum class AppLanguage(val code: String, val displayName: String) {
    SW("sw", "Kiswahili"),
    EN("en", "English")
}

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val role: String = "CUSTOMER",
    val photoUrl: String = "",
    val hasAcceptedTerms: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class Category(
    val id: String = "",
    val slug: String = "",
    val nameSw: String = "",
    val nameEn: String = "",
    val iconName: String = "",
    val listingCount: Int = 0
) {
    fun localizedName(lang: AppLanguage): String = if (lang == AppLanguage.SW) nameSw else nameEn
}

@IgnoreExtraProperties
data class BusinessProfile(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val name: String = "",
    val categorySlug: String = "",
    val categoryName: String = "",
    val descriptionSw: String = "",
    val descriptionEn: String = "",
    val city: String = "Dar es Salaam",
    val district: String = "",
    val phoneNumber: String = "",
    val whatsappNumber: String = "",
    val isVerified: Boolean = false,
    val rating: Double = 5.0,
    val reviewCount: Long = 0L,
    val openingHours: String = "Jumatatu - Jumamosi: 08:00 - 18:00",
    val address: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun localizedDesc(lang: AppLanguage): String = if (lang == AppLanguage.SW) descriptionSw else (descriptionEn.ifBlank { descriptionSw })
}

@IgnoreExtraProperties
data class Listing(
    @DocumentId val id: String = "",
    val businessId: String = "",
    val ownerId: String = "",
    val businessName: String = "",
    val titleSw: String = "",
    val titleEn: String = "",
    val descriptionSw: String = "",
    val descriptionEn: String = "",
    val priceTzs: Long = 0L,
    val categorySlug: String = "",
    val type: String = "PRODUCT", // "PRODUCT" or "SERVICE"
    val status: String = "PUBLISHED",
    val city: String = "Dar es Salaam",
    val isFeatured: Boolean = false,
    val imageUrl: String = "",
    val viewsCount: Long = 0L,
    val phoneNumber: String = "",
    val whatsappNumber: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun localizedTitle(lang: AppLanguage): String = if (lang == AppLanguage.SW) titleSw else (titleEn.ifBlank { titleSw })
    fun localizedDesc(lang: AppLanguage): String = if (lang == AppLanguage.SW) descriptionSw else (descriptionEn.ifBlank { descriptionSw })
    val isProduct: Boolean get() = type == "PRODUCT"
}

data class AssistantMessage(
    val id: String = "",
    val isUser: Boolean = false,
    val text: String = "",
    val referencedListings: List<Listing> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
    val isAiDraft: Boolean = false
)
