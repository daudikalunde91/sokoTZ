package com.example.sokotz.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.sokotz.data.model.AppLanguage
import com.example.sokotz.data.model.AssistantMessage
import com.example.sokotz.data.model.BusinessProfile
import com.example.sokotz.data.model.Category
import com.example.sokotz.data.model.Listing
import com.example.sokotz.data.remote.GeminiClient
import com.example.sokotz.data.remote.OperationType
import com.example.sokotz.data.remote.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirestoreMarketplaceRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        Firebase.auth
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private val _categories = MutableStateFlow<List<Category>>(MarketplaceData.categories)
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _businesses = MutableStateFlow<List<BusinessProfile>>(emptyList())
    val businesses: StateFlow<List<BusinessProfile>> = _businesses.asStateFlow()

    private val _listings = MutableStateFlow<List<Listing>>(emptyList())
    val listings: StateFlow<List<Listing>> = _listings.asStateFlow()

    private val _language = MutableStateFlow(AppLanguage.SW)
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    private val userConversations = mutableMapOf<String, List<AssistantMessage>>()

    private fun getWelcomeMessage(userName: String = ""): AssistantMessage {
        val isSw = _language.value == AppLanguage.SW
        val greeting = if (userName.isNotBlank()) " $userName" else ""
        return AssistantMessage(
            id = "msg_init_${System.currentTimeMillis()}",
            isUser = false,
            text = if (isSw)
                "Habari$greeting! Mimi ni sokoTZ Msaidizi (sokoTZ AI). Nawezaje kukusaidia leo kupata bidhaa, wauzaji, au kutunga maelezo ya tangazo lako?"
            else
                "Hello$greeting! I am sokoTZ AI (sokoTZ Assistant). How can I assist you today with products, verified Tanzanian businesses, or drafting ad descriptions?"
        )
    }

    private val _assistantMessages = MutableStateFlow<List<AssistantMessage>>(listOf(getWelcomeMessage()))
    val assistantMessages: StateFlow<List<AssistantMessage>> = _assistantMessages.asStateFlow()

    private val _isAssistantThinking = MutableStateFlow(false)
    val isAssistantThinking: StateFlow<Boolean> = _isAssistantThinking.asStateFlow()

    private var businessesListener: ListenerRegistration? = null
    private var listingsListener: ListenerRegistration? = null
    private var activeUserId: String? = auth.currentUser?.uid

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        val newUid = user?.uid
        if (newUid != activeUserId) {
            // Save previous user conversation
            activeUserId?.let { uid ->
                userConversations[uid] = _assistantMessages.value
            }
            activeUserId = newUid
            // Load new user's conversation or fresh welcome message
            if (newUid != null) {
                val greetingName = currentAiUserName.ifBlank { user.displayName ?: "" }
                _assistantMessages.value = userConversations[newUid] ?: listOf(getWelcomeMessage(greetingName))
            } else {
                _assistantMessages.value = listOf(getWelcomeMessage(currentAiUserName))
            }
        }
        startRealtimeListeners()
    }

    init {
        auth.addAuthStateListener(authStateListener)
        startRealtimeListeners()
    }

    private var currentAiUserName: String = ""

    fun setAssistantUserName(name: String) {
        val trimmed = name.trim()
        if (trimmed == currentAiUserName) return
        currentAiUserName = trimmed
        if (_assistantMessages.value.size <= 1) {
            _assistantMessages.value = listOf(getWelcomeMessage(trimmed))
        }
    }

    fun setLanguage(lang: AppLanguage) {
        _language.value = lang
    }

    fun toggleLanguage() {
        _language.value = if (_language.value == AppLanguage.SW) AppLanguage.EN else AppLanguage.SW
    }

    private fun startRealtimeListeners() {
        if (businessesListener != null && listingsListener != null) return

        businessesListener?.remove()
        businessesListener = db.collection("businesses")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, "businesses")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(BusinessProfile::class.java)?.copy(id = doc.id)
                    }
                    _businesses.value = list
                }
            }

        listingsListener?.remove()
        listingsListener = db.collection("listings")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, "listings")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Listing::class.java)?.copy(id = doc.id)
                    }
                    _listings.value = list
                }
            }
    }

    private fun stopRealtimeListeners() {
        businessesListener?.remove()
        businessesListener = null
        listingsListener?.remove()
        listingsListener = null
        _businesses.value = emptyList()
        _listings.value = emptyList()
    }

    fun observeListings(): Flow<List<Listing>> = _listings

    fun observeBusinesses(): Flow<List<BusinessProfile>> = _businesses

    suspend fun getBusiness(businessId: String): BusinessProfile? {
        if (auth.currentUser == null) return null
        val path = "businesses/$businessId"
        return try {
            val doc = db.collection("businesses").document(businessId).get().await()
            doc.toObject(BusinessProfile::class.java)?.copy(id = doc.id)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            throw e
        }
    }

    suspend fun getListing(listingId: String): Listing? {
        if (auth.currentUser == null) return null
        val path = "listings/$listingId"
        return try {
            val doc = db.collection("listings").document(listingId).get().await()
            doc.toObject(Listing::class.java)?.copy(id = doc.id)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            throw e
        }
    }

    fun getBusinessById(businessId: String): BusinessProfile? {
        return _businesses.value.firstOrNull { it.id == businessId }
    }

    fun getListingById(listingId: String): Listing? {
        return _listings.value.firstOrNull { it.id == listingId }
    }

    fun getListingsForBusiness(businessId: String): List<Listing> {
        return _listings.value.filter { it.businessId == businessId }
    }

    fun filterListings(
        query: String = "",
        categorySlug: String? = null,
        city: String? = null,
        type: String? = null
    ): List<Listing> {
        val q = query.trim().lowercase()
        return _listings.value.filter { listing ->
            val matchesQuery = q.isEmpty() ||
                    listing.titleSw.lowercase().contains(q) ||
                    listing.titleEn.lowercase().contains(q) ||
                    listing.descriptionSw.lowercase().contains(q) ||
                    listing.descriptionEn.lowercase().contains(q) ||
                    listing.businessName.lowercase().contains(q)

            val matchesCategory = categorySlug == null || categorySlug == "all" || listing.categorySlug == categorySlug
            val matchesCity = city == null || city == "Tanzania Nzima" || listing.city.equals(city, ignoreCase = true)
            val matchesType = type == null || listing.type.equals(type, ignoreCase = true)

            matchesQuery && matchesCategory && matchesCity && matchesType
        }
    }

    suspend fun registerBusinessProfile(
        name: String,
        categorySlug: String,
        city: String,
        district: String,
        phoneNumber: String,
        whatsappNumber: String,
        description: String,
        openingHours: String = "Jumatatu - Jumamosi: 08:00 - 18:00"
    ): Result<String> {
        val currentUserId = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }

        val categoryName = _categories.value.firstOrNull { it.slug == categorySlug }?.nameSw ?: "Biashara"
        val docRef = db.collection("businesses").document()

        val payload = hashMapOf(
            "ownerId" to currentUserId,
            "name" to name.trim(),
            "categorySlug" to categorySlug,
            "categoryName" to categoryName,
            "descriptionSw" to description.trim(),
            "descriptionEn" to description.trim(),
            "city" to city.trim(),
            "district" to district.trim(),
            "phoneNumber" to phoneNumber.trim(),
            "whatsappNumber" to whatsappNumber.trim(),
            "isVerified" to false,
            "rating" to 5.0,
            "reviewCount" to 0,
            "openingHours" to openingHours.trim().ifBlank { "Jumatatu - Jumamosi: 08:00 - 18:00" },
            "address" to "$district, $city",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun createListing(
        businessId: String,
        businessName: String,
        title: String,
        priceTzs: Long,
        categorySlug: String,
        description: String,
        type: String,
        city: String,
        phoneNumber: String,
        whatsappNumber: String,
        imageUrl: String = ""
    ): Result<String> {
        val currentUserId = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }

        val docRef = db.collection("listings").document()

        val payload = hashMapOf(
            "businessId" to businessId,
            "ownerId" to currentUserId,
            "businessName" to businessName,
            "titleSw" to title.trim(),
            "titleEn" to title.trim(),
            "descriptionSw" to description.trim(),
            "descriptionEn" to description.trim(),
            "priceTzs" to priceTzs,
            "categorySlug" to categorySlug,
            "type" to type,
            "status" to "PUBLISHED",
            "city" to city.trim(),
            "isFeatured" to false,
            "imageUrl" to imageUrl.trim(),
            "viewsCount" to 0L,
            "phoneNumber" to phoneNumber.trim(),
            "whatsappNumber" to whatsappNumber.trim(),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateListing(
        listingId: String,
        title: String,
        priceTzs: Long,
        categorySlug: String,
        description: String,
        type: String,
        city: String,
        phoneNumber: String,
        whatsappNumber: String,
        imageUrl: String = ""
    ): Result<Unit> {
        try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }

        val docRef = db.collection("listings").document(listingId)
        val updates = hashMapOf<String, Any>(
            "titleSw" to title.trim(),
            "titleEn" to title.trim(),
            "priceTzs" to priceTzs,
            "categorySlug" to categorySlug,
            "descriptionSw" to description.trim(),
            "descriptionEn" to description.trim(),
            "type" to type,
            "city" to city.trim(),
            "phoneNumber" to phoneNumber.trim(),
            "whatsappNumber" to whatsappNumber.trim(),
            "imageUrl" to imageUrl.trim(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteListing(listingId: String): Result<Unit> {
        try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("listings").document(listingId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateBusinessProfile(
        businessId: String,
        name: String,
        categorySlug: String,
        city: String,
        district: String,
        phoneNumber: String,
        whatsappNumber: String,
        description: String,
        openingHours: String = "Jumatatu - Jumamosi: 08:00 - 18:00"
    ): Result<Unit> {
        try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }

        val categoryName = _categories.value.firstOrNull { it.slug == categorySlug }?.nameSw ?: "Biashara"
        val docRef = db.collection("businesses").document(businessId)
        val updates = hashMapOf<String, Any>(
            "name" to name.trim(),
            "categorySlug" to categorySlug,
            "categoryName" to categoryName,
            "city" to city.trim(),
            "district" to district.trim(),
            "phoneNumber" to phoneNumber.trim(),
            "whatsappNumber" to whatsappNumber.trim(),
            "descriptionSw" to description.trim(),
            "descriptionEn" to description.trim(),
            "openingHours" to openingHours.trim().ifBlank { "Jumatatu - Jumamosi: 08:00 - 18:00" },
            "address" to "$district, $city",
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    private val viewedListingsPerUser = mutableMapOf<String, MutableSet<String>>()

    suspend fun incrementListingView(listingId: String) {
        val currentUser = auth.currentUser
        val uid = currentUser?.uid ?: "guest"
        val userViewed = viewedListingsPerUser.getOrPut(uid) { mutableSetOf() }
        if (userViewed.contains(listingId)) {
            return
        }

        // For signed-in users, record in Firestore subcollection to prevent repeat counts across app restarts
        if (currentUser != null) {
            val viewerDoc = db.collection("listings").document(listingId)
                .collection("viewers").document(currentUser.uid)
            try {
                val snapshot = viewerDoc.get().await()
                if (snapshot.exists()) {
                    userViewed.add(listingId)
                    return
                }
                viewerDoc.set(mapOf("viewedAt" to FieldValue.serverTimestamp())).await()
            } catch (e: Exception) {
                Log.w("FirestoreRepo", "Could not check viewer record", e)
            }
        }

        userViewed.add(listingId)

        try {
            db.collection("listings").document(listingId).update(
                mapOf(
                    "viewsCount" to FieldValue.increment(1),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            Log.w("FirestoreRepo", "Could not increment view count for $listingId", e)
        }
    }

    fun sendAssistantMessage(userText: String, scope: CoroutineScope) {
        val trimmed = userText.trim()
        if (trimmed.isEmpty()) return

        val userMsg = AssistantMessage(
            id = "user_${UUID.randomUUID()}",
            isUser = true,
            text = trimmed
        )
        _assistantMessages.value = _assistantMessages.value + userMsg
        _isAssistantThinking.value = true

        scope.launch(Dispatchers.IO) {
            try {
                val history = _assistantMessages.value.dropLast(1).map {
                    (if (it.isUser) "user" else "model") to it.text
                }

                val activeListings = _listings.value.take(15)
                val activeBusinesses = _businesses.value.take(10)
                val marketplaceContext = buildString {
                    appendLine("Orodha ya Biashara zilizosajiliwa SokoTZ:")
                    activeBusinesses.forEach {
                        appendLine("- ${it.name} (${it.categoryName}, ${it.city}). Simu: ${it.phoneNumber}")
                    }
                    appendLine("Orodha ya Bidhaa na Huduma za sasa:")
                    activeListings.forEach {
                        appendLine("- ${it.titleSw} | Bei: TZS ${it.priceTzs} | Muuzaji: ${it.businessName} (${it.city})")
                    }
                }

                val currentUserName = if (currentAiUserName.isNotBlank()) currentAiUserName else (auth.currentUser?.displayName ?: "")
                val aiResponse = GeminiClient.chat(
                    conversationHistory = history,
                    userMessage = trimmed,
                    marketplaceContext = marketplaceContext,
                    userName = currentUserName
                )

                val matchedListings = _listings.value.filter { listing ->
                    val queryTerms = trimmed.lowercase().split(" ").filter { it.length > 2 }
                    val itemText = "${listing.titleSw} ${listing.businessName} ${listing.categorySlug}".lowercase()
                    queryTerms.any { itemText.contains(it) }
                }.take(3)

                val isDraft = trimmed.lowercase().contains("andika") ||
                        trimmed.lowercase().contains("tunga") ||
                        trimmed.lowercase().contains("draft") ||
                        trimmed.lowercase().contains("maelezo")

                val assistantMsg = AssistantMessage(
                    id = "asst_${UUID.randomUUID()}",
                    isUser = false,
                    text = aiResponse,
                    referencedListings = matchedListings,
                    isAiDraft = isDraft
                )

                val updated = _assistantMessages.value + assistantMsg
                _assistantMessages.value = updated
                activeUserId?.let { uid -> userConversations[uid] = updated }
            } catch (e: Exception) {
                Log.e("FirestoreRepo", "Assistant error", e)
                val errorMsg = AssistantMessage(
                    id = "asst_err_${UUID.randomUUID()}",
                    isUser = false,
                    text = if (_language.value == AppLanguage.SW)
                        "Samahani, kumekuwa na changamoto ya mtandao kufikia sokoTZ Msaidizi. Tafadhali jaribu tena baada ya muda mfupi."
                    else
                        "Sorry, there was a temporary network issue connecting to sokoTZ AI. Please try again shortly."
                )
                val updated = _assistantMessages.value + errorMsg
                _assistantMessages.value = updated
                activeUserId?.let { uid -> userConversations[uid] = updated }
            } finally {
                _isAssistantThinking.value = false
            }
        }
    }

    fun clearAssistantConversation() {
        val welcome = getWelcomeMessage(auth.currentUser?.displayName ?: "")
        val freshList = listOf(welcome)
        _assistantMessages.value = freshList
        activeUserId?.let { uid -> userConversations[uid] = freshList }
    }

    companion object {
        @Volatile
        private var instance: FirestoreMarketplaceRepository? = null

        fun getInstance(context: Context): FirestoreMarketplaceRepository {
            return instance ?: synchronized(this) {
                instance ?: FirestoreMarketplaceRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
