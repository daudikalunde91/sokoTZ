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
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthManager(private val context: Context) {

    private val auth: FirebaseAuth = Firebase.auth
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
        val dbId = context.getString(R.string.firestore_database_id)
        return FirebaseFirestore.getInstance(dbId)
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
                    auth.signInWithCredential(authCredential).await()
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
                    authResult.user?.let { user ->
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

    private var pendingDisplayName: String? = null
    private var pendingPhoneNumber: String? = null
    private var pendingRole: String = "CUSTOMER"
    private var pendingAcceptedTerms: Boolean = true

    fun setPendingRegistration(name: String, phone: String, role: String, acceptedTerms: Boolean) {
        pendingDisplayName = name.trim().ifBlank { null }
        pendingPhoneNumber = phone.trim().ifBlank { null }
        pendingRole = if (role == "BUSINESS_OWNER") "BUSINESS_OWNER" else "CUSTOMER"
        pendingAcceptedTerms = acceptedTerms
    }

    private fun loadOrCreateUserProfile(user: FirebaseUser) {
        val db = getDb()
        val docRef = db.collection("users").document(user.uid)
        docRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val existing = snapshot.toObject(UserProfile::class.java)
                // If user registered with updated details, merge them
                if (pendingDisplayName != null || pendingPhoneNumber != null) {
                    val updatedName = pendingDisplayName ?: existing?.displayName ?: user.displayName ?: "Mwanachama SokoTZ"
                    val updatedPhone = pendingPhoneNumber ?: existing?.phoneNumber ?: user.phoneNumber ?: ""
                    val updatedRole = pendingRole
                    val updates = hashMapOf<String, Any>(
                        "displayName" to updatedName,
                        "phoneNumber" to updatedPhone,
                        "role" to updatedRole,
                        "hasAcceptedTerms" to true
                    )
                    docRef.update(updates)
                    _userProfile.value = existing?.copy(
                        displayName = updatedName,
                        phoneNumber = updatedPhone,
                        role = updatedRole,
                        hasAcceptedTerms = true
                    )
                    pendingDisplayName = null
                    pendingPhoneNumber = null
                } else {
                    _userProfile.value = existing
                }
            } else {
                val displayName = pendingDisplayName ?: user.displayName ?: "Mwanachama SokoTZ"
                val phoneNumber = pendingPhoneNumber ?: user.phoneNumber ?: ""
                val role = pendingRole
                val newProfile = UserProfile(
                    id = user.uid,
                    userId = user.uid,
                    displayName = displayName,
                    email = user.email ?: "",
                    phoneNumber = phoneNumber,
                    role = role,
                    photoUrl = user.photoUrl?.toString() ?: "",
                    hasAcceptedTerms = pendingAcceptedTerms
                )
                docRef.set(newProfile)
                _userProfile.value = newProfile
                pendingDisplayName = null
                pendingPhoneNumber = null
            }
        }.addOnFailureListener { e ->
            Log.e("AuthManager", "Failed to fetch user profile", e)
            _userProfile.value = UserProfile(
                id = user.uid,
                userId = user.uid,
                displayName = pendingDisplayName ?: user.displayName ?: "Mwanachama SokoTZ",
                email = user.email ?: "",
                phoneNumber = pendingPhoneNumber ?: user.phoneNumber ?: "",
                role = pendingRole,
                hasAcceptedTerms = pendingAcceptedTerms
            )
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
            return instance ?: synchronized(this) {
                instance ?: AuthManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
