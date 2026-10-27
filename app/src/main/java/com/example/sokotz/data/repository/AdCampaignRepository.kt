package com.example.sokotz.data.repository

import android.content.Context
import android.util.Log
import com.example.sokotz.data.model.AdCampaign
import com.example.sokotz.data.model.AdCampaignStatus
import com.example.sokotz.data.model.AdComment
import com.example.sokotz.data.model.AdInAppNotification
import com.example.sokotz.data.model.AdPackage
import com.example.sokotz.data.model.AdReport
import com.example.sokotz.data.model.Listing
import com.example.sokotz.data.model.PackagePriceAuditRecord
import com.example.sokotz.data.model.PaymentTransactionRecord
import com.example.sokotz.data.model.SavedAdItem
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID

class AdCampaignRepository private constructor(private val appContext: Context) {

    private val db = com.example.sokotz.data.util.FirebaseInitHelper.getFirestore(appContext)
    private val auth = FirebaseAuth.getInstance()
    private val scope = CoroutineScope(Dispatchers.IO)

    // Dynamic Admin-Controlled Advertising Packages
    private val _adPackages = MutableStateFlow<List<AdPackage>>(emptyList())
    val adPackages: StateFlow<List<AdPackage>> = _adPackages.asStateFlow()

    private val _allPackagesForAdmin = MutableStateFlow<List<AdPackage>>(emptyList())
    val allPackagesForAdmin: StateFlow<List<AdPackage>> = _allPackagesForAdmin.asStateFlow()

    private val _packagePriceAudits = MutableStateFlow<List<PackagePriceAuditRecord>>(emptyList())
    val packagePriceAudits: StateFlow<List<PackagePriceAuditRecord>> = _packagePriceAudits.asStateFlow()

    // Active Ads Feed (Only PAID, APPROVED, and ACTIVE ads)
    private val _activeAdsFeed = MutableStateFlow<List<AdCampaign>>(emptyList())
    val activeAdsFeed: StateFlow<List<AdCampaign>> = _activeAdsFeed.asStateFlow()

    // Seller Campaigns
    private val _sellerCampaigns = MutableStateFlow<List<AdCampaign>>(emptyList())
    val sellerCampaigns: StateFlow<List<AdCampaign>> = _sellerCampaigns.asStateFlow()

    // Buyer Saved Ads & Hidden Ad IDs
    private val _savedAds = MutableStateFlow<List<SavedAdItem>>(emptyList())
    val savedAds: StateFlow<List<SavedAdItem>> = _savedAds.asStateFlow()

    private val _hiddenAdIds = MutableStateFlow<Set<String>>(emptySet())
    val hiddenAdIds: StateFlow<Set<String>> = _hiddenAdIds.asStateFlow()

    // Admin Oversight Flows
    private val _adminAllCampaigns = MutableStateFlow<List<AdCampaign>>(emptyList())
    val adminAllCampaigns: StateFlow<List<AdCampaign>> = _adminAllCampaigns.asStateFlow()

    private val _adminPendingReports = MutableStateFlow<List<AdReport>>(emptyList())
    val adminPendingReports: StateFlow<List<AdReport>> = _adminPendingReports.asStateFlow()

    // In-App Notifications
    private val _notifications = MutableStateFlow<List<AdInAppNotification>>(emptyList())
    val notifications: StateFlow<List<AdInAppNotification>> = _notifications.asStateFlow()

    private var packagesListener: ListenerRegistration? = null
    private var feedListener: ListenerRegistration? = null
    private var sellerCampaignsListener: ListenerRegistration? = null
    private var adminCampaignsListener: ListenerRegistration? = null
    private var adminReportsListener: ListenerRegistration? = null
    private var savedAdsListener: ListenerRegistration? = null
    private var hiddenAdsListener: ListenerRegistration? = null
    private var notificationsListener: ListenerRegistration? = null
    private var priceAuditListener: ListenerRegistration? = null

    init {
        listenToPackages()
        listenToActiveFeed()
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                listenToSellerCampaigns(user.uid)
                listenToSavedAds(user.uid)
                listenToHiddenAds(user.uid)
                listenToNotifications(user.uid)
                listenToAdminData()
            } else {
                sellerCampaignsListener?.remove()
                adminCampaignsListener?.remove()
                adminReportsListener?.remove()
                savedAdsListener?.remove()
                hiddenAdsListener?.remove()
                notificationsListener?.remove()
                priceAuditListener?.remove()
                _sellerCampaigns.value = emptyList()
                _savedAds.value = emptyList()
                _hiddenAdIds.value = emptySet()
                _notifications.value = emptyList()
                _adminAllCampaigns.value = emptyList()
                _adminPendingReports.value = emptyList()
            }
        }
    }

    // -------------------------------------------------------------
    // 1. DYNAMIC ADMIN-CONTROLLED PACKAGES & PRICING
    // -------------------------------------------------------------

    private fun listenToPackages() {
        packagesListener?.remove()
        packagesListener = db.collection("ad_packages")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Listen to packages failed: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { it.toObject(AdPackage::class.java) }
                    _allPackagesForAdmin.value = list.sortedBy { it.sortOrder }
                    _adPackages.value = list.filter { it.isActive }.sortedBy { it.sortOrder }

                    if (list.isEmpty()) {
                        scope.launch { seedDefaultPackagesIfEmpty() }
                    }
                }
            }
    }

    private suspend fun seedDefaultPackagesIfEmpty() {
        try {
            val count = db.collection("ad_packages").get().await().size()
            if (count == 0) {
                val defaults = listOf(
                    AdPackage(
                        id = "pkg_1_day",
                        nameSw = "Kifurushi cha Siku 1",
                        nameEn = "1 Day Flash Ad",
                        durationDays = 1,
                        priceTzs = 2000L,
                        descriptionSw = "Tangazo lako la video litaonyeshwa kwa masaa 24 kwenye Ads Feed ya SokoTZ kote nchini.",
                        descriptionEn = "Your video ad will run for 24 hours on SokoTZ video ads feed across Tanzania.",
                        isActive = true,
                        sortOrder = 1,
                        features = listOf("Masaa 24 hewani", "Wateja 1,000+", "Kitufe cha moja kwa moja")
                    ),
                    AdPackage(
                        id = "pkg_3_days",
                        nameSw = "Kifurushi cha Siku 3 (Ofa)",
                        nameEn = "3 Days Standard Ad",
                        durationDays = 3,
                        priceTzs = 5000L,
                        descriptionSw = "Tangazo la video la siku tatu mfululizo lenye ufikiaji mpana kwa wateja wengi zaidi.",
                        descriptionEn = "Three consecutive days of video promotion with high visibility and engagement.",
                        isActive = true,
                        sortOrder = 2,
                        features = listOf("Siku 3 hewani", "Wateja 5,000+", "Analytics ya mauzo", "Kipaumbele cha mzunguko")
                    ),
                    AdPackage(
                        id = "pkg_7_days",
                        nameSw = "Kifurushi cha Wiki 1 (Wiki Nzima)",
                        nameEn = "7 Days Super Brand",
                        durationDays = 7,
                        priceTzs = 10000L,
                        descriptionSw = "Tangazo rasmi la wiki nzima (Siku 7) linaloweka biashara yako kileleni kwa wateja wa SokoTZ.",
                        descriptionEn = "Full 7 days premium placement ensuring maximum customer conversion and reach.",
                        isActive = true,
                        sortOrder = 3,
                        features = listOf("Wiki nzima (siku 7)", "Wateja 15,000+", "Msaada maalum", "Kipaumbele kikuu")
                    )
                )
                for (pkg in defaults) {
                    db.collection("ad_packages").document(pkg.id).set(pkg).await()
                }
                Log.i(TAG, "Default ad packages seeded successfully.")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to seed default packages: ${e.message}")
        }
    }

    suspend fun createAdPackage(
        nameSw: String,
        nameEn: String,
        durationDays: Int,
        priceTzs: Long,
        descSw: String,
        descEn: String,
        adminId: String,
        adminName: String,
        features: List<String> = emptyList()
    ): Result<String> {
        return try {
            val docRef = db.collection("ad_packages").document()
            val newPkg = AdPackage(
                id = docRef.id,
                nameSw = nameSw.trim(),
                nameEn = nameEn.trim(),
                durationDays = durationDays,
                priceTzs = priceTzs,
                descriptionSw = descSw.trim(),
                descriptionEn = descEn.trim(),
                isActive = true,
                sortOrder = (_allPackagesForAdmin.value.maxOfOrNull { it.sortOrder } ?: 0) + 1,
                features = features,
                updatedByAdminId = adminId
            )
            docRef.set(newPkg).await()

            // Record initial audit entry
            recordPriceAudit(
                packageId = docRef.id,
                packageName = nameSw.trim(),
                adminId = adminId,
                adminName = adminName,
                oldPriceTzs = 0L,
                newPriceTzs = priceTzs,
                reason = "Uundaji mpya wa kifurushi (New package creation)"
            )
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "createAdPackage failed", e)
            Result.failure(e)
        }
    }

    suspend fun updateAdPackagePrice(
        packageId: String,
        newPriceTzs: Long,
        reason: String,
        adminId: String,
        adminName: String
    ): Result<Unit> {
        return try {
            val existing = _allPackagesForAdmin.value.find { it.id == packageId }
                ?: return Result.failure(Exception("Kifurushi hakijapatikana (Package not found)"))

            val oldPrice = existing.priceTzs
            db.collection("ad_packages").document(packageId).update(
                mapOf(
                    "priceTzs" to newPriceTzs,
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "updatedByAdminId" to adminId
                )
            ).await()

            recordPriceAudit(
                packageId = packageId,
                packageName = existing.nameSw,
                adminId = adminId,
                adminName = adminName,
                oldPriceTzs = oldPrice,
                newPriceTzs = newPriceTzs,
                reason = reason.ifBlank { "Mabadiliko ya bei ya utawala (Admin price update)" }
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "updateAdPackagePrice failed", e)
            Result.failure(e)
        }
    }

    suspend fun togglePackageActive(
        packageId: String,
        isActive: Boolean,
        adminId: String
    ): Result<Unit> {
        return try {
            db.collection("ad_packages").document(packageId).update(
                mapOf(
                    "isActive" to isActive,
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "updatedByAdminId" to adminId
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "togglePackageActive failed", e)
            Result.failure(e)
        }
    }

    private suspend fun recordPriceAudit(
        packageId: String,
        packageName: String,
        adminId: String,
        adminName: String,
        oldPriceTzs: Long,
        newPriceTzs: Long,
        reason: String
    ) {
        try {
            val docRef = db.collection("package_price_audits").document()
            val audit = PackagePriceAuditRecord(
                id = docRef.id,
                packageId = packageId,
                packageName = packageName,
                adminId = adminId,
                adminName = adminName,
                oldPriceTzs = oldPriceTzs,
                newPriceTzs = newPriceTzs,
                reason = reason
            )
            docRef.set(audit).await()
        } catch (e: Exception) {
            Log.w(TAG, "recordPriceAudit failed: ${e.message}")
        }
    }

    // -------------------------------------------------------------
    // 2. TIKTOK-STYLE ACTIVE ADS FEED (PUBLIC BUYERS)
    // -------------------------------------------------------------

    private fun listenToActiveFeed() {
        feedListener?.remove()
        feedListener = db.collection("ad_campaigns")
            .whereEqualTo("status", AdCampaignStatus.ACTIVE.name)
            .whereEqualTo("paymentStatus", "PAID")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Listen to active feed error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val now = System.currentTimeMillis()
                    val campaigns = snapshot.documents.mapNotNull { it.toObject(AdCampaign::class.java) }
                        .filter { camp ->
                            // Check campaign duration expiry
                            val end = camp.endDate?.toDate()?.time
                            end == null || end > now
                        }

                    // Apply fair rotation & deduplication algorithm
                    val rotated = applyFairRotationAlgorithm(campaigns)
                    _activeAdsFeed.value = rotated
                }
            }
    }

    /**
     * Fair rotation ranking algorithm:
     * - Blends advertiser rotation to prevent showing the same seller repeatedly
     * - Randomizes seed rotation across viewing sessions
     */
    private fun applyFairRotationAlgorithm(campaigns: List<AdCampaign>): List<AdCampaign> {
        if (campaigns.size <= 1) return campaigns
        // Group by seller to interleave ads fairly
        val bySeller = campaigns.groupBy { it.sellerId }
        val result = mutableListOf<AdCampaign>()
        var index = 0
        var hasMore = true
        val sellers = bySeller.keys.shuffled()

        while (hasMore) {
            hasMore = false
            for (seller in sellers) {
                val sellerAds = bySeller[seller] ?: emptyList()
                if (index < sellerAds.size) {
                    result.add(sellerAds[index])
                    hasMore = true
                }
            }
            index++
        }
        return result
    }

    // -------------------------------------------------------------
    // 3. SELLER CAMPAIGN CREATION, PAYMENT & LIFECYCLE
    // -------------------------------------------------------------

    private fun listenToSellerCampaigns(sellerId: String) {
        sellerCampaignsListener?.remove()
        sellerCampaignsListener = db.collection("ad_campaigns")
            .whereEqualTo("sellerId", sellerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Listen to seller campaigns error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { it.toObject(AdCampaign::class.java) }
                    _sellerCampaigns.value = list.sortedByDescending { it.createdAt?.toDate() ?: Date(0) }
                }
            }
    }

    suspend fun createCampaign(
        sellerId: String,
        sellerName: String,
        businessId: String,
        businessName: String,
        listing: Listing,
        adTitle: String,
        adDescription: String,
        videoUrl: String,
        selectedPackage: AdPackage,
        callToAction: String
    ): Result<String> {
        return try {
            val docRef = db.collection("ad_campaigns").document()
            val campaign = AdCampaign(
                id = docRef.id,
                sellerId = sellerId,
                sellerName = sellerName,
                businessId = businessId,
                businessName = businessName,
                listingId = listing.id,
                listingTitle = listing.titleSw.ifBlank { listing.titleEn },
                listingPriceTzs = listing.priceTzs,
                listingImageUrl = listing.imageUrl,
                listingCategorySlug = listing.categorySlug,
                listingType = listing.type,
                listingCity = listing.city,
                sellerPhone = listing.phoneNumber,
                sellerWhatsapp = listing.whatsappNumber,
                videoUrl = videoUrl.trim(),
                videoAspectRatio = "9:16",
                adTitle = adTitle.trim(),
                adDescription = adDescription.trim(),
                callToActionType = callToAction,
                packageId = selectedPackage.id,
                packageDurationDays = selectedPackage.durationDays,
                packagePriceTzs = selectedPackage.priceTzs, // authoritative snapshot of price at purchase time
                paymentStatus = "UNPAID",
                status = AdCampaignStatus.AWAITING_PAYMENT.name
            )
            docRef.set(campaign).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "createCampaign failed", e)
            Result.failure(e)
        }
    }

    suspend fun processPaymentAndSubmitForReview(
        campaignId: String,
        sellerId: String,
        provider: String, // "MPESA", "AIRTEL_MONEY", "HALOPESA", "MIXX_YAS"
        phoneNumber: String
    ): Result<String> {
        return try {
            val campaignDoc = db.collection("ad_campaigns").document(campaignId).get().await()
            val campaign = campaignDoc.toObject(AdCampaign::class.java)
                ?: return Result.failure(Exception("Kampeni haikupatikana (Campaign not found)"))

            val ref = "TZ_${provider.take(3)}_${System.currentTimeMillis().toString().takeLast(8)}"
            val idempotencyKey = UUID.randomUUID().toString()

            // 1. Create immutable audit transaction record
            val txnDoc = db.collection("payment_transactions").document()
            val txn = PaymentTransactionRecord(
                id = txnDoc.id,
                campaignId = campaignId,
                sellerId = sellerId,
                provider = provider,
                reference = ref,
                amountTzs = campaign.packagePriceTzs,
                status = "SUBMITTED",
                idempotencyKey = idempotencyKey
            )
            txnDoc.set(txn).await()

            // 2. Update campaign to PENDING_VERIFICATION and submit to AWAITING_REVIEW
            db.collection("ad_campaigns").document(campaignId).update(
                mapOf(
                    "paymentStatus" to "PENDING_VERIFICATION",
                    "paymentReference" to ref,
                    "paymentMethod" to provider,
                    "status" to AdCampaignStatus.AWAITING_REVIEW.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            // 3. Create notification for seller
            createNotification(
                recipientUserId = sellerId,
                titleSw = "Malipo Yamewasilishwa: ${campaign.adTitle}",
                titleEn = "Payment Submitted: ${campaign.adTitle}",
                messageSw = "Ombi la malipo ya TZS ${txn.formattedAmount} limewasilishwa kwa kumbukumbu $ref. Kampeni yako sasa ipo kwenye ukaguzi wa wasimamizi wa SokoTZ kabla ya kuwekwa hewani.",
                messageEn = "Payment submission of TZS ${txn.formattedAmount} recorded with ref $ref. Your campaign is awaiting SokoTZ admin review and verification before going live.",
                type = "PAYMENT_SUBMITTED",
                campaignId = campaignId
            )

            Result.success(ref)
        } catch (e: Exception) {
            Log.e(TAG, "processPaymentAndSubmitForReview failed", e)
            Result.failure(e)
        }
    }

    suspend fun requestReinstatement(
        campaignId: String,
        reason: String
    ): Result<Unit> {
        return try {
            db.collection("ad_campaigns").document(campaignId).update(
                mapOf(
                    "reinstatementRequested" to true,
                    "reinstatementReason" to reason.trim(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "requestReinstatement failed", e)
            Result.failure(e)
        }
    }

    suspend fun permanentlyCloseCampaign(campaignId: String): Result<Unit> {
        return try {
            db.collection("ad_campaigns").document(campaignId).update(
                mapOf(
                    "status" to AdCampaignStatus.PERMANENTLY_CLOSED.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "permanentlyCloseCampaign failed", e)
            Result.failure(e)
        }
    }

    /**
     * Automatically pauses campaign if linked product is unavailable or deleted
     */
    suspend fun checkProductAvailabilityAndPauseIfNeeded(
        campaign: AdCampaign,
        listing: Listing?
    ) {
        if (campaign.status != AdCampaignStatus.ACTIVE.name) return
        val isUnavailable = listing == null || listing.status != "PUBLISHED"
        if (isUnavailable) {
            try {
                db.collection("ad_campaigns").document(campaign.id).update(
                    mapOf(
                        "status" to AdCampaignStatus.PAUSED_PRODUCT_UNAVAILABLE.name,
                        "productUnavailableSince" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

                createNotification(
                    recipientUserId = campaign.sellerId,
                    titleSw = "Tangazo Limesitishwa (Bidhaa Haipo): ${campaign.adTitle}",
                    titleEn = "Ad Paused (Product Unavailable): ${campaign.adTitle}",
                    messageSw = "Tangazo lako la video limesitishwa kwa sababu bidhaa '${campaign.listingTitle}' haipatikani. Muda wa tangazo hautapungua wakati wa kusitishwa. Una siku 7 kurejesha bidhaa na kuomba tangazo lianze tena.",
                    messageEn = "Your video ad was paused because product '${campaign.listingTitle}' is unavailable. Ad time is stopped during pause. You have 7 days to restore the product and request reinstatement.",
                    type = "PRODUCT_PAUSED",
                    campaignId = campaign.id
                )
            } catch (e: Exception) {
                Log.w(TAG, "checkProductAvailabilityAndPauseIfNeeded failed: ${e.message}")
            }
        }
    }

    // -------------------------------------------------------------
    // 4. ADMIN REVIEW, APPROVAL, MODERATION & RESOLUTIONS
    // -------------------------------------------------------------

    private fun listenToAdminData() {
        adminCampaignsListener?.remove()
        adminCampaignsListener = db.collection("ad_campaigns")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Listen to admin campaigns failed: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { it.toObject(AdCampaign::class.java) }
                    _adminAllCampaigns.value = list.sortedByDescending { it.createdAt?.toDate() ?: Date(0) }
                }
            }

        adminReportsListener?.remove()
        adminReportsListener = db.collection("ad_reports")
            .whereEqualTo("status", "PENDING")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Listen to admin reports failed: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { it.toObject(AdReport::class.java) }
                    _adminPendingReports.value = list.sortedByDescending { it.createdAt?.toDate() ?: Date(0) }
                }
            }

        priceAuditListener?.remove()
        priceAuditListener = db.collection("package_price_audits")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Listen to price audits failed: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { it.toObject(PackagePriceAuditRecord::class.java) }
                    _packagePriceAudits.value = list.sortedByDescending { it.timestamp?.toDate() ?: Date(0) }
                }
            }
    }

    suspend fun approveCampaign(
        campaignId: String,
        adminId: String,
        notes: String = ""
    ): Result<Unit> {
        return try {
            val doc = db.collection("ad_campaigns").document(campaignId).get().await()
            val campaign = doc.toObject(AdCampaign::class.java)
                ?: return Result.failure(Exception("Campaign not found"))

            val now = Date()
            val durationMs = campaign.packageDurationDays.toLong() * 24 * 60 * 60 * 1000
            val endDate = Date(now.time + durationMs)

            db.collection("ad_campaigns").document(campaignId).update(
                mapOf(
                    "status" to AdCampaignStatus.ACTIVE.name,
                    "paymentStatus" to "PAID",
                    "moderatedByAdminId" to adminId,
                    "moderationNotes" to notes.ifBlank { "Imeidhinishwa na Msimamizi (Approved by SokoTZ Admin)" },
                    "startDate" to Timestamp(now),
                    "endDate" to Timestamp(endDate),
                    "reinstatementRequested" to false,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            // Also verify any associated payment transaction record
            try {
                val txns = db.collection("payment_transactions")
                    .whereEqualTo("campaignId", campaignId)
                    .get().await()
                for (tDoc in txns.documents) {
                    tDoc.reference.update(
                        mapOf(
                            "status" to "VERIFIED",
                            "verifiedByAdminId" to adminId,
                            "verifiedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed updating payment_transactions for campaign: ${e.message}")
            }

            createNotification(
                recipientUserId = campaign.sellerId,
                titleSw = "Tangazo Lako Liko Hewani! 🎉: ${campaign.adTitle}",
                titleEn = "Your Ad is Now Live! 🎉: ${campaign.adTitle}",
                messageSw = "Hongera! Kampeni yako ya video ya '${campaign.adTitle}' imeidhinishwa na sasa inaonekana kote Tanzania kwenye SokoTZ Ads Feed.",
                messageEn = "Congratulations! Your video ad '${campaign.adTitle}' has been approved and is now live on SokoTZ Ads Feed across Tanzania.",
                type = "CAMPAIGN_APPROVED",
                campaignId = campaignId
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "approveCampaign failed", e)
            Result.failure(e)
        }
    }

    suspend fun rejectCampaign(
        campaignId: String,
        adminId: String,
        rejectionReason: String,
        resolutionChoice: String = "CREDIT" // "CREDIT" or "REFUND"
    ): Result<Unit> {
        return try {
            val doc = db.collection("ad_campaigns").document(campaignId).get().await()
            val campaign = doc.toObject(AdCampaign::class.java)
                ?: return Result.failure(Exception("Campaign not found"))

            db.collection("ad_campaigns").document(campaignId).update(
                mapOf(
                    "status" to AdCampaignStatus.REJECTED.name,
                    "moderatedByAdminId" to adminId,
                    "rejectionReason" to rejectionReason.trim(),
                    "paymentStatus" to if (resolutionChoice == "REFUND") "REFUNDED" else "CREDITED",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            createNotification(
                recipientUserId = campaign.sellerId,
                titleSw = "Taarifa ya Kampeni Yako: ${campaign.adTitle}",
                titleEn = "Campaign Update: ${campaign.adTitle}",
                messageSw = "Kampeni yako ya video haijaidhinishwa kwa sababu: $rejectionReason. Fedha zako za TZS ${campaign.formattedPrice} zimehifadhiwa kama salio la matangazo (Ad Credit).",
                messageEn = "Your video campaign was not approved due to: $rejectionReason. Your payment of TZS ${campaign.formattedPrice} has been credited to your SokoTZ ad balance.",
                type = "CAMPAIGN_REJECTED",
                campaignId = campaignId
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "rejectCampaign failed", e)
            Result.failure(e)
        }
    }

    suspend fun suspendCampaignForPolicy(
        campaignId: String,
        adminId: String,
        reason: String
    ): Result<Unit> {
        return try {
            val doc = db.collection("ad_campaigns").document(campaignId).get().await()
            val campaign = doc.toObject(AdCampaign::class.java)
                ?: return Result.failure(Exception("Campaign not found"))

            db.collection("ad_campaigns").document(campaignId).update(
                mapOf(
                    "status" to AdCampaignStatus.PAUSED_POLICY_REVIEW.name,
                    "moderatedByAdminId" to adminId,
                    "moderationNotes" to reason.trim(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            createNotification(
                recipientUserId = campaign.sellerId,
                titleSw = "Tangazo Limesitishwa (Ukaguzi wa Sheria): ${campaign.adTitle}",
                titleEn = "Ad Suspended for Policy Review: ${campaign.adTitle}",
                messageSw = "Tangazo lako limesitishwa kwa ukaguzi wa kisheria: $reason. Tafadhali wasiliana na huduma kwa wateja ya SokoTZ.",
                messageEn = "Your ad has been suspended for policy review: $reason. Please contact SokoTZ support.",
                type = "CAMPAIGN_SUSPENDED",
                campaignId = campaignId
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "suspendCampaignForPolicy failed", e)
            Result.failure(e)
        }
    }

    suspend fun resolveReport(
        reportId: String,
        actionTaken: String,
        adminId: String,
        campaignId: String? = null
    ): Result<Unit> {
        return try {
            db.collection("ad_reports").document(reportId).update(
                mapOf(
                    "status" to "ACTION_TAKEN",
                    "adminActionTaken" to actionTaken,
                    "reviewedByAdminId" to adminId,
                    "reviewedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            if (actionTaken == "TAKE_DOWN_AD" && campaignId != null) {
                suspendCampaignForPolicy(campaignId, adminId, "Imetolewa baada ya ukaguzi wa ripoti ya jamii (Removed following moderation review)")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "resolveReport failed", e)
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------
    // 5. BUYER INTERACTIONS (LIKES, COMMENTS, REPLIES, SAVES, HIDES, SHARES)
    // -------------------------------------------------------------

    suspend fun toggleLike(campaignId: String, userId: String): Result<Boolean> {
        return try {
            val likeRef = db.collection("ad_campaigns").document(campaignId)
                .collection("likes").document(userId)

            val likeDoc = likeRef.get().await()
            val nowLiked: Boolean
            if (likeDoc.exists()) {
                // Unlike
                likeRef.delete().await()
                db.collection("ad_campaigns").document(campaignId)
                    .update("likesCount", FieldValue.increment(-1))
                nowLiked = false
            } else {
                // Like
                likeRef.set(mapOf("likedAt" to FieldValue.serverTimestamp())).await()
                db.collection("ad_campaigns").document(campaignId)
                    .update("likesCount", FieldValue.increment(1))
                nowLiked = true
            }
            Result.success(nowLiked)
        } catch (e: Exception) {
            Log.e(TAG, "toggleLike failed", e)
            Result.failure(e)
        }
    }

    suspend fun hasUserLiked(campaignId: String, userId: String): Boolean {
        return try {
            db.collection("ad_campaigns").document(campaignId)
                .collection("likes").document(userId).get().await().exists()
        } catch (_: Exception) {
            false
        }
    }

    suspend fun postComment(
        campaignId: String,
        userId: String,
        userName: String,
        userPhotoUrl: String,
        text: String,
        isSellerReply: Boolean = false,
        parentCommentId: String? = null
    ): Result<String> {
        return try {
            val docRef = db.collection("ad_campaigns").document(campaignId)
                .collection("comments").document()

            val comment = AdComment(
                id = docRef.id,
                campaignId = campaignId,
                userId = userId,
                userName = userName.trim(),
                userPhotoUrl = userPhotoUrl,
                isSellerReply = isSellerReply,
                parentCommentId = parentCommentId,
                text = text.trim()
            )
            docRef.set(comment).await()

            db.collection("ad_campaigns").document(campaignId)
                .update("commentsCount", FieldValue.increment(1))

            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "postComment failed", e)
            Result.failure(e)
        }
    }

    fun listenToCampaignComments(
        campaignId: String,
        onComments: (List<AdComment>) -> Unit
    ): ListenerRegistration {
        return db.collection("ad_campaigns").document(campaignId)
            .collection("comments")
            .whereEqualTo("isHiddenByMod", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Comments listener failed: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val comments = snapshot.documents.mapNotNull { it.toObject(AdComment::class.java) }
                        .sortedBy { it.createdAt?.toDate() ?: Date(0) }
                    onComments(comments)
                }
            }
    }

    suspend fun toggleSaveAd(campaign: AdCampaign, userId: String): Result<Boolean> {
        return try {
            val docRef = db.collection("saved_ads").document(userId)
                .collection("items").document(campaign.id)

            val exists = docRef.get().await().exists()
            val nowSaved: Boolean
            if (exists) {
                docRef.delete().await()
                db.collection("ad_campaigns").document(campaign.id)
                    .update("savesCount", FieldValue.increment(-1))
                nowSaved = false
            } else {
                val item = SavedAdItem(
                    id = campaign.id,
                    campaignId = campaign.id,
                    listingId = campaign.listingId,
                    title = campaign.adTitle,
                    sellerName = campaign.businessName,
                    priceTzs = campaign.listingPriceTzs,
                    videoUrl = campaign.videoUrl,
                    imageUrl = campaign.listingImageUrl
                )
                docRef.set(item).await()
                db.collection("ad_campaigns").document(campaign.id)
                    .update("savesCount", FieldValue.increment(1))
                nowSaved = true
            }
            Result.success(nowSaved)
        } catch (e: Exception) {
            Log.e(TAG, "toggleSaveAd failed", e)
            Result.failure(e)
        }
    }

    private fun listenToSavedAds(userId: String) {
        savedAdsListener?.remove()
        savedAdsListener = db.collection("saved_ads").document(userId)
            .collection("items")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Listen to saved ads error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    _savedAds.value = snapshot.documents.mapNotNull { it.toObject(SavedAdItem::class.java) }
                }
            }
    }

    suspend fun hideAd(campaignId: String, userId: String): Result<Unit> {
        return try {
            db.collection("hidden_ads").document(userId)
                .collection("items").document(campaignId)
                .set(mapOf("hiddenAt" to FieldValue.serverTimestamp())).await()
            _hiddenAdIds.value = _hiddenAdIds.value + campaignId
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "hideAd failed", e)
            Result.failure(e)
        }
    }

    private fun listenToHiddenAds(userId: String) {
        hiddenAdsListener?.remove()
        hiddenAdsListener = db.collection("hidden_ads").document(userId)
            .collection("items")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Listen to hidden ads error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    _hiddenAdIds.value = snapshot.documents.map { it.id }.toSet()
                }
            }
    }

    suspend fun submitReport(
        targetType: String,
        targetId: String,
        campaignId: String,
        reporterUserId: String,
        reporterName: String,
        reason: String,
        details: String,
        severity: String
    ): Result<String> {
        return try {
            val docRef = db.collection("ad_reports").document()
            val report = AdReport(
                id = docRef.id,
                targetType = targetType,
                targetId = targetId,
                campaignId = campaignId,
                reporterUserId = reporterUserId,
                reporterName = reporterName.trim(),
                reason = reason.trim(),
                details = details.trim(),
                severity = severity,
                status = "PENDING"
            )
            docRef.set(report).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "submitReport failed", e)
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------
    // 6. METRICS & TELEMETRY RECORDING
    // -------------------------------------------------------------

    fun recordImpression(campaignId: String, userId: String?) {
        scope.launch {
            try {
                db.collection("ad_campaigns").document(campaignId)
                    .update("impressionsCount", FieldValue.increment(1))

                // Deduplicate unique viewers
                if (!userId.isNullOrBlank()) {
                    val viewerRef = db.collection("ad_campaigns").document(campaignId)
                        .collection("unique_viewers").document(userId)
                    if (!viewerRef.get().await().exists()) {
                        viewerRef.set(mapOf("viewedAt" to FieldValue.serverTimestamp()))
                        db.collection("ad_campaigns").document(campaignId)
                            .update("uniqueViewsCount", FieldValue.increment(1))
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "recordImpression error: ${e.message}")
            }
        }
    }

    fun recordClick(campaignId: String) {
        scope.launch {
            try {
                db.collection("ad_campaigns").document(campaignId)
                    .update("clicksCount", FieldValue.increment(1))
            } catch (_: Exception) {}
        }
    }

    fun recordShare(campaignId: String) {
        scope.launch {
            try {
                db.collection("ad_campaigns").document(campaignId)
                    .update("sharesCount", FieldValue.increment(1))
            } catch (_: Exception) {}
        }
    }

    // -------------------------------------------------------------
    // 7. IN-APP NOTIFICATIONS
    // -------------------------------------------------------------

    private fun listenToNotifications(userId: String) {
        notificationsListener?.remove()
        notificationsListener = db.collection("ad_notifications")
            .whereEqualTo("recipientUserId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Listen to notifications failed: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { it.toObject(AdInAppNotification::class.java) }
                    _notifications.value = list.sortedByDescending { it.createdAt?.toDate() ?: Date(0) }
                }
            }
    }

    suspend fun createNotification(
        recipientUserId: String,
        titleSw: String,
        titleEn: String,
        messageSw: String,
        messageEn: String,
        type: String,
        campaignId: String? = null
    ) {
        try {
            val docRef = db.collection("ad_notifications").document()
            val notif = AdInAppNotification(
                id = docRef.id,
                recipientUserId = recipientUserId,
                titleSw = titleSw,
                titleEn = titleEn,
                messageSw = messageSw,
                messageEn = messageEn,
                type = type,
                campaignId = campaignId,
                isRead = false
            )
            docRef.set(notif).await()
        } catch (e: Exception) {
            Log.w(TAG, "createNotification failed: ${e.message}")
        }
    }

    suspend fun markNotificationAsRead(notificationId: String) {
        try {
            db.collection("ad_notifications").document(notificationId)
                .update("isRead", true).await()
        } catch (_: Exception) {}
    }

    companion object {
        private const val TAG = "AdCampaignRepo"

        @Volatile
        private var instance: AdCampaignRepository? = null

        fun getInstance(context: Context): AdCampaignRepository {
            return instance ?: synchronized(this) {
                instance ?: AdCampaignRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
