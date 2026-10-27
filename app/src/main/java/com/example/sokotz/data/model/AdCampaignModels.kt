package com.example.sokotz.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.text.NumberFormat
import java.util.Locale

enum class AdCampaignStatus(val titleSw: String, val titleEn: String) {
    DRAFT("Rasimu", "Draft"),
    AWAITING_PAYMENT("Inasubiri Malipo", "Awaiting Payment"),
    AWAITING_REVIEW("Inasubiri Ukaguzi", "Awaiting Review"),
    APPROVED("Imeidhinishwa", "Approved"),
    ACTIVE("Inaonyeshwa (Hai)", "Active"),
    PAUSED_PRODUCT_UNAVAILABLE("Imesitishwa (Bidhaa Haipo)", "Paused (Product Unavailable)"),
    PAUSED_POLICY_REVIEW("Imesitishwa (Ukaguzi wa Sheria)", "Paused (Policy Review)"),
    ENDED("Imekwisha", "Ended"),
    REJECTED("Imekataliwa", "Rejected"),
    PERMANENTLY_CLOSED("Imefungwa Kabisa", "Permanently Closed"),
    REFUND_CREDIT_PROCESSING("Inashughulikiwa Marejesho", "Refund/Credit Processing")
}

@IgnoreExtraProperties
data class AdPackage(
    @DocumentId val id: String = "",
    val nameSw: String = "",
    val nameEn: String = "",
    val durationDays: Int = 1,
    val priceTzs: Long = 2000L,
    val descriptionSw: String = "",
    val descriptionEn: String = "",
    val isActive: Boolean = true,
    val sortOrder: Int = 0,
    val features: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
    val updatedByAdminId: String? = null
) {
    fun localizedName(lang: AppLanguage): String = if (lang == AppLanguage.SW) nameSw else (nameEn.ifBlank { nameSw })
    fun localizedDesc(lang: AppLanguage): String = if (lang == AppLanguage.SW) descriptionSw else (descriptionEn.ifBlank { descriptionSw })
    val formattedPrice: String get() = NumberFormat.getNumberInstance(Locale.US).format(priceTzs)
}

@IgnoreExtraProperties
data class PackagePriceAuditRecord(
    @DocumentId val id: String = "",
    val packageId: String = "",
    val packageName: String = "",
    val adminId: String = "",
    val adminName: String = "",
    val oldPriceTzs: Long = 0L,
    val newPriceTzs: Long = 0L,
    val reason: String = "",
    @ServerTimestamp val timestamp: Timestamp? = null
) {
    val formattedOldPrice: String get() = NumberFormat.getNumberInstance(Locale.US).format(oldPriceTzs)
    val formattedNewPrice: String get() = NumberFormat.getNumberInstance(Locale.US).format(newPriceTzs)
}

@IgnoreExtraProperties
data class AdCampaign(
    @DocumentId val id: String = "",
    val sellerId: String = "",
    val sellerName: String = "",
    val businessId: String = "",
    val businessName: String = "",
    val listingId: String = "",
    val listingTitle: String = "",
    val listingPriceTzs: Long = 0L,
    val listingImageUrl: String = "",
    val listingCategorySlug: String = "",
    val listingType: String = "PRODUCT", // "PRODUCT" or "SERVICE"
    val listingCity: String = "Dar es Salaam",
    val sellerPhone: String = "",
    val sellerWhatsapp: String = "",
    val videoUrl: String = "",
    val videoAspectRatio: String = "9:16",
    val adTitle: String = "",
    val adDescription: String = "",
    val callToActionType: String = "VIEW_PRODUCT", // "VIEW_PRODUCT", "VIEW_SERVICE", "CONTACT_SELLER", "BUY_NOW"
    val packageId: String = "",
    val packageDurationDays: Int = 1,
    val packagePriceTzs: Long = 0L,
    val paymentStatus: String = "UNPAID", // "UNPAID", "PAID", "REFUNDED", "CREDITED"
    val paymentReference: String = "",
    val paymentMethod: String = "MPESA", // "MPESA", "AIRTEL_MONEY", "HALOPESA", "MIXX_YAS"
    val status: String = "DRAFT", // matches AdCampaignStatus name
    val rejectionReason: String? = null,
    val productUnavailableSince: Timestamp? = null,
    val reinstatementRequested: Boolean = false,
    val reinstatementReason: String? = null,
    val moderationNotes: String? = null,
    val moderatedByAdminId: String? = null,
    val impressionsCount: Long = 0L,
    val uniqueViewsCount: Long = 0L,
    val clicksCount: Long = 0L,
    val likesCount: Long = 0L,
    val commentsCount: Long = 0L,
    val sharesCount: Long = 0L,
    val savesCount: Long = 0L,
    val startDate: Timestamp? = null,
    val endDate: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val isEligibleForFeed: Boolean
        get() = status == "ACTIVE" && paymentStatus == "PAID"

    val formattedPrice: String
        get() = NumberFormat.getNumberInstance(Locale.US).format(listingPriceTzs)

    fun statusEnum(): AdCampaignStatus = try {
        AdCampaignStatus.valueOf(status)
    } catch (_: Exception) {
        AdCampaignStatus.DRAFT
    }
}

@IgnoreExtraProperties
data class AdComment(
    @DocumentId val id: String = "",
    val campaignId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userPhotoUrl: String = "",
    val isSellerReply: Boolean = false,
    val parentCommentId: String? = null,
    val text: String = "",
    val isReported: Boolean = false,
    val isHiddenByMod: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AdReport(
    @DocumentId val id: String = "",
    val targetType: String = "AD", // "AD" or "COMMENT"
    val targetId: String = "",
    val campaignId: String = "",
    val reporterUserId: String = "",
    val reporterName: String = "",
    val reason: String = "",
    val details: String = "",
    val severity: String = "MISLEADING_PRICE", // "URGENT_SAFETY", "FRAUD_SCAM", "PROHIBITED_ITEM", "MISLEADING_PRICE", "SPAM"
    val status: String = "PENDING", // "PENDING", "ACTION_TAKEN", "REJECTED_NO_ACTION"
    val adminActionTaken: String? = null,
    val reviewedByAdminId: String? = null,
    @ServerTimestamp val reviewedAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PaymentTransactionRecord(
    @DocumentId val id: String = "",
    val campaignId: String = "",
    val sellerId: String = "",
    val provider: String = "MPESA",
    val reference: String = "",
    val amountTzs: Long = 0L,
    val status: String = "VERIFIED", // "PENDING", "VERIFIED", "FAILED", "REFUNDED"
    val idempotencyKey: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val verifiedAt: Timestamp? = null
) {
    val formattedAmount: String get() = NumberFormat.getNumberInstance(Locale.US).format(amountTzs)
}

@IgnoreExtraProperties
data class AdInAppNotification(
    @DocumentId val id: String = "",
    val recipientUserId: String = "",
    val titleSw: String = "",
    val titleEn: String = "",
    val messageSw: String = "",
    val messageEn: String = "",
    val type: String = "CAMPAIGN_UPDATE", // "CAMPAIGN_APPROVED", "CAMPAIGN_REJECTED", "PRODUCT_PAUSED", "REPLY_RECEIVED", "PAYMENT_CONFIRMED"
    val campaignId: String? = null,
    val isRead: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun localizedTitle(lang: AppLanguage): String = if (lang == AppLanguage.SW) titleSw else (titleEn.ifBlank { titleSw })
    fun localizedMessage(lang: AppLanguage): String = if (lang == AppLanguage.SW) messageSw else (messageEn.ifBlank { messageSw })
}

@IgnoreExtraProperties
data class SavedAdItem(
    @DocumentId val id: String = "",
    val campaignId: String = "",
    val listingId: String = "",
    val title: String = "",
    val sellerName: String = "",
    val priceTzs: Long = 0L,
    val videoUrl: String = "",
    val imageUrl: String = "",
    @ServerTimestamp val savedAt: Timestamp? = null
)
