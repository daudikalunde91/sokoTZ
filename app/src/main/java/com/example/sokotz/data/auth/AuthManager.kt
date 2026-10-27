package com.example.sokotz.data.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.sokotz.data.model.UserProfile
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthManager(private val context: Context) {

    private val auth: FirebaseAuth = run {
        com.example.sokotz.data.util.FirebaseInitHelper.ensureInitialized(context)
        Firebase.auth
    }
    private val credentialManager: CredentialManager = CredentialManager.create(context)

    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            _currentUser.value = user
            if (user != null) {
                loadOrCreateUserProfile(user)
            } else {
                _userProfile.value = null
            }
        }
    }

    private fun getDb(): FirebaseFirestore {
        return com.example.sokotz.data.util.FirebaseInitHelper.getFirestore(context)
    }

    fun clearError() {
        _authError.value = null
    }

    fun attemptAutoSignIn(scope: CoroutineScope) {
        if (auth.currentUser != null) {
            return
        }
        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            return
        }

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        scope.launch {
            try {
                val result = credentialManager.getCredential(context, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    val authResult = auth.signInWithCredential(authCredential).await()
                    authResult.user?.let { user ->
                        _currentUser.value = user
                        loadOrCreateUserProfile(user)
                    }
                }
            } catch (e: Exception) {
                // Auto sign-in is silent; proceed unauthenticated
            }
        }
    }

    fun signInWithGoogle(activity: Activity, scope: CoroutineScope) {
        _isLoading.value = true
        _authError.value = null

        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            _isLoading.value = false
            _authError.value = "Google Sign-In configuration missing (default_web_client_id)"
            return
        }

        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        scope.launch {
            try {
                val result = credentialManager.getCredential(activity, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    val authResult = auth.signInWithCredential(authCredential).await()
                    val user = authResult.user
                    if (user != null) {
                        _currentUser.value = user
                        loadOrCreateUserProfile(user)
                    }
                } else {
                    _authError.value = "Aina ya kitambulisho isiyotarajiwa (Unexpected credential)"
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w("AuthManager", "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
                // User dismissed account chooser
            } catch (e: Exception) {
                Log.e("AuthManager", "Google Sign-In failed", e)
                _authError.value = e.localizedMessage ?: "Google Sign-In failed"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Genuine email/password sign-in using FirebaseAuth.signInWithEmailAndPassword
     */
    fun signInWithEmailPassword(
        email: String,
        password: String,
        scope: CoroutineScope,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val trimmedEmail = email.trim()
        val trimmedPassword = password.trim()
        if (trimmedEmail.isBlank() || trimmedPassword.isBlank()) {
            val err = "Tafadhali weka barua pepe na nenosiri (Please enter email and password)"
            _authError.value = err
            onError(err)
            return
        }

        _isLoading.value = true
        _authError.value = null
        scope.launch {
            try {
                val result = auth.signInWithEmailAndPassword(trimmedEmail, trimmedPassword).await()
                val user = result.user
                if (user != null) {
                    _currentUser.value = user
                    loadOrCreateUserProfile(user)
                    onSuccess()
                } else {
                    val err = "Imeshindikana kuingia (Authentication failed)"
                    _authError.value = err
                    onError(err)
                }
            } catch (e: Exception) {
                Log.e("AuthManager", "Email/Password sign-in failed", e)
                val message = when {
                    e.message?.contains("user-not-found", ignoreCase = true) == true ||
                    e.message?.contains("no user record", ignoreCase = true) == true ->
                        "Akaunti hii haipo. Tafadhali jisajili kwanza (Account not found. Please register)"
                    e.message?.contains("wrong-password", ignoreCase = true) == true ||
                    e.message?.contains("invalid-credential", ignoreCase = true) == true ->
                        "Nenosiri sio sahihi au akaunti haikupatikana (Invalid credentials)"
                    e.message?.contains("network", ignoreCase = true) == true ->
                        "Hitilafu ya mtandao. Tafadhali angalia intaneti yako (Network failure)"
                    else -> e.localizedMessage ?: "Imeshindikana kuingia (Sign in failed)"
                }
                _authError.value = message
                onError(message)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Alias for signInWithEmailPassword for backward compatibility
     */
    fun signInStandard(
        email: String,
        password: String,
        scope: CoroutineScope,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        signInWithEmailPassword(email, password, scope, onSuccess, onError)
    }

    /**
     * Genuine email/password registration using FirebaseAuth.createUserWithEmailAndPassword
     */
    fun registerWithEmailPassword(
        email: String,
        password: String,
        displayName: String,
        phone: String,
        role: String,
        acceptedTerms: Boolean,
        scope: CoroutineScope,
        aiPreferredName: String = "",
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val trimmedEmail = email.trim()
        val trimmedPassword = password.trim()
        val trimmedName = displayName.trim()

        if (trimmedEmail.isBlank() || trimmedPassword.isBlank()) {
            val err = "Tafadhali weka barua pepe na nenosiri (Email and password required)"
            _authError.value = err
            onError(err)
            return
        }
        if (trimmedPassword.length < 6) {
            val err = "Nenosiri lazima liwe na angalau herufi 6 (Password must be at least 6 characters)"
            _authError.value = err
            onError(err)
            return
        }
        if (!acceptedTerms) {
            val err = "Tafadhali kubali Vigezo na Masharti (Please accept terms and conditions)"
            _authError.value = err
            onError(err)
            return
        }

        setPendingRegistration(
            name = trimmedName.ifBlank { trimmedEmail.substringBefore("@") },
            phone = phone,
            role = role,
            acceptedTerms = acceptedTerms,
            aiPreferredName = aiPreferredName
        )

        _isLoading.value = true
        _authError.value = null
        scope.launch {
            try {
                val result = auth.createUserWithEmailAndPassword(trimmedEmail, trimmedPassword).await()
                val user = result.user
                if (user != null) {
                    // Update FirebaseUser display name if provided
                    if (trimmedName.isNotBlank()) {
                        try {
                            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                .setDisplayName(trimmedName)
                                .build()
                            user.updateProfile(profileUpdates).await()
                        } catch (e: Exception) {
                            Log.w("AuthManager", "Failed to update Firebase profile display name", e)
                        }
                    }
                    _currentUser.value = user
                    loadOrCreateUserProfile(user)
                    onSuccess()
                } else {
                    val err = "Usajili umeshindwa (Registration failed)"
                    _authError.value = err
                    onError(err)
                }
            } catch (e: Exception) {
                Log.e("AuthManager", "Email/Password registration failed", e)
                val message = when {
                    e.message?.contains("email-already-in-use", ignoreCase = true) == true ||
                    e.message?.contains("already in use", ignoreCase = true) == true ->
                        "Barua pepe hii tayari imesajiliwa. Tafadhali ingia (Email already in use. Please sign in)"
                    e.message?.contains("weak-password", ignoreCase = true) == true ->
                        "Nenosiri ni dhaifu. Tumia angalau herufi 6 (Weak password. Use at least 6 characters)"
                    e.message?.contains("network", ignoreCase = true) == true ->
                        "Hitilafu ya mtandao. Angalia muunganisho wako (Network failure)"
                    else -> e.localizedMessage ?: "Usajili umeshindwa (Registration failed)"
                }
                _authError.value = message
                onError(message)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Alias for registerWithEmailPassword for backward compatibility
     */
    fun registerStandard(
        email: String,
        password: String,
        displayName: String,
        phone: String,
        role: String,
        acceptedTerms: Boolean,
        scope: CoroutineScope,
        aiPreferredName: String = "",
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        registerWithEmailPassword(
            email, password, displayName, phone, role, acceptedTerms, scope, aiPreferredName, onSuccess, onError
        )
    }

    /**
     * Password reset via FirebaseAuth.sendPasswordResetEmail
     */
    fun sendPasswordReset(
        email: String,
        scope: CoroutineScope,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank()) {
            val err = "Tafadhali weka barua pepe yako (Please enter your email)"
            _authError.value = err
            onError(err)
            return
        }

        _isLoading.value = true
        _authError.value = null
        scope.launch {
            try {
                auth.sendPasswordResetEmail(trimmedEmail).await()
                onSuccess()
            } catch (e: Exception) {
                Log.e("AuthManager", "Password reset failed", e)
                val message = when {
                    e.message?.contains("user-not-found", ignoreCase = true) == true ->
                        "Hakuna akaunti yenye barua pepe hii (No account found for this email)"
                    e.message?.contains("invalid-email", ignoreCase = true) == true ->
                        "Barua pepe sio sahihi (Invalid email format)"
                    else -> e.localizedMessage ?: "Imeshindikana kutuma barua pepe ya kurejesha nenosiri"
                }
                _authError.value = message
                onError(message)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private var pendingDisplayName: String? = null
    private var pendingPhoneNumber: String? = null
    private var pendingRole: String = "CUSTOMER"
    private var pendingAcceptedTerms: Boolean = true
    private var pendingAiPreferredName: String? = null

    fun setPendingRegistration(name: String, phone: String, role: String, acceptedTerms: Boolean, aiPreferredName: String = "") {
        pendingDisplayName = name.trim().ifBlank { null }
        pendingPhoneNumber = phone.trim().ifBlank { null }
        pendingRole = if (role == "BUSINESS_OWNER") "BUSINESS_OWNER" else "CUSTOMER"
        pendingAcceptedTerms = acceptedTerms
        pendingAiPreferredName = aiPreferredName.trim().ifBlank { null }
    }

    private fun loadOrCreateUserProfile(user: FirebaseUser) {
        val db = getDb()
        val docRef = db.collection("users").document(user.uid)
        docRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val existing = snapshot.toObject(UserProfile::class.java)
                // If user registered with updated details, merge them
                if (pendingDisplayName != null || pendingPhoneNumber != null || pendingAiPreferredName != null) {
                    val updatedName = (pendingDisplayName ?: existing?.displayName ?: user.displayName ?: "").trim().ifBlank {
                        user.email?.substringBefore("@") ?: "Mwanachama SokoTZ"
                    }
                    val updatedPhone = pendingPhoneNumber ?: existing?.phoneNumber ?: user.phoneNumber ?: ""
                    val updatedAiName = pendingAiPreferredName ?: existing?.aiPreferredName ?: ""
                    val updatedRole = pendingRole
                    val updates = hashMapOf<String, Any>(
                        "displayName" to updatedName,
                        "phoneNumber" to updatedPhone,
                        "role" to updatedRole,
                        "aiPreferredName" to updatedAiName,
                        "hasAcceptedTerms" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    docRef.update(updates).addOnSuccessListener {
                        Log.i("AuthManager", "User profile updated in Firestore: ${user.uid}")
                    }.addOnFailureListener { e ->
                        Log.e("AuthManager", "Failed to update profile: ${e.message}", e)
                    }
                    _userProfile.value = existing?.copy(
                        displayName = updatedName,
                        phoneNumber = updatedPhone,
                        role = updatedRole,
                        aiPreferredName = updatedAiName,
                        hasAcceptedTerms = true
                    )
                    pendingDisplayName = null
                    pendingPhoneNumber = null
                    pendingAiPreferredName = null
                } else {
                    _userProfile.value = existing
                }
            } else {
                val displayName = (pendingDisplayName ?: user.displayName ?: "").trim().ifBlank {
                    user.email?.substringBefore("@") ?: "Mwanachama SokoTZ"
                }
                val phoneNumber = pendingPhoneNumber ?: user.phoneNumber ?: ""
                val aiPreferred = pendingAiPreferredName ?: ""
                val role = pendingRole
                val newProfile = UserProfile(
                    id = user.uid,
                    userId = user.uid,
                    displayName = displayName,
                    email = user.email ?: "",
                    phoneNumber = phoneNumber,
                    role = role,
                    photoUrl = user.photoUrl?.toString() ?: "",
                    aiPreferredName = aiPreferred,
                    hasAcceptedTerms = pendingAcceptedTerms
                )
                docRef.set(newProfile)
                    .addOnSuccessListener {
                        Log.i("AuthManager", "User profile created in Firestore: ${user.uid}")
                    }
                    .addOnFailureListener { e ->
                        Log.e("AuthManager", "Failed to set user profile in Firestore: ${e.message}", e)
                    }
                _userProfile.value = newProfile
                pendingDisplayName = null
                pendingPhoneNumber = null
                pendingAiPreferredName = null
            }
        }.addOnFailureListener { e ->
            Log.e("AuthManager", "Failed to fetch user profile, creating new doc", e)
            val displayName = (pendingDisplayName ?: user.displayName ?: "").trim().ifBlank {
                user.email?.substringBefore("@") ?: "Mwanachama SokoTZ"
            }
            val newProfile = UserProfile(
                id = user.uid,
                userId = user.uid,
                displayName = displayName,
                email = user.email ?: "",
                phoneNumber = pendingPhoneNumber ?: user.phoneNumber ?: "",
                role = pendingRole,
                photoUrl = user.photoUrl?.toString() ?: "",
                aiPreferredName = pendingAiPreferredName ?: "",
                hasAcceptedTerms = pendingAcceptedTerms
            )
            docRef.set(newProfile)
                .addOnSuccessListener {
                    Log.i("AuthManager", "User profile created after fetch failure: ${user.uid}")
                }
                .addOnFailureListener { setErr ->
                    Log.e("AuthManager", "Failed to write user profile after fetch failure: ${setErr.message}", setErr)
                }
            _userProfile.value = newProfile
        }
    }

    suspend fun updateUserProfile(
        displayName: String,
        phoneNumber: String,
        photoUrl: String,
        aiPreferredName: String
    ): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(Exception("Not signed in"))
        val db = getDb()
        val updates = hashMapOf<String, Any>(
            "displayName" to displayName.trim(),
            "phoneNumber" to phoneNumber.trim(),
            "photoUrl" to photoUrl.trim(),
            "aiPreferredName" to aiPreferredName.trim(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            db.collection("users").document(user.uid).update(updates).await()
            _userProfile.value = _userProfile.value?.copy(
                displayName = displayName.trim(),
                phoneNumber = phoneNumber.trim(),
                photoUrl = photoUrl.trim(),
                aiPreferredName = aiPreferredName.trim()
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun updateUserRole(newRole: String) {
        val user = auth.currentUser ?: return
        val db = getDb()
        db.collection("users").document(user.uid).update("role", newRole)
        _userProfile.value = _userProfile.value?.copy(role = newRole)
    }

    fun signOut(scope: CoroutineScope) {
        auth.signOut()
        _currentUser.value = null
        _userProfile.value = null
        scope.launch {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.e("AuthManager", "Failed to clear credential state", e)
            }
        }
    }

    companion object {
        @Volatile
        private var instance: AuthManager? = null

        fun getInstance(context: Context): AuthManager {
            val appContext = context.applicationContext ?: context
            com.example.sokotz.data.util.FirebaseInitHelper.ensureInitialized(appContext)
            return instance ?: synchronized(this) {
                instance ?: AuthManager(appContext).also { instance = it }
            }
        }
    }
}
