package com.example.sokotz.ui.screens.account

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.sokotz.data.auth.AuthManager
import com.example.sokotz.data.model.AppLanguage
import com.example.sokotz.data.model.AppThemeMode
import com.example.sokotz.data.model.UserProfile
import com.example.sokotz.data.repository.FirestoreMarketplaceRepository
import com.example.sokotz.data.util.ImageUtils
import com.example.sokotz.ui.components.SokoListingImage
import com.example.ui.theme.SokoColors
import com.example.ui.theme.SokoBlueBlack
import com.example.ui.theme.SokoCardBorder
import com.example.ui.theme.SokoCardSurface
import com.example.ui.theme.SokoDeepNavy
import com.example.ui.theme.SokoError
import com.example.ui.theme.SokoGradients
import com.example.ui.theme.SokoLightText
import com.example.ui.theme.SokoMidnightBlue
import com.example.ui.theme.SokoMutedText
import com.example.ui.theme.SokoNeonBlue
import com.example.ui.theme.SokoNeonViolet
import com.example.ui.theme.ThemeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AccountScreen(
    authManager: AuthManager,
    repository: FirestoreMarketplaceRepository,
    onNavigateToBusinessHub: () -> Unit,
    onNavigateToSignIn: () -> Unit = {},
    onNavigateToSellerAdsManager: () -> Unit = {},
    onNavigateToSavedAds: () -> Unit = {},
    onNavigateToAdminDashboard: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val language by repository.language.collectAsState()
    val currentUser by authManager.currentUser.collectAsState()
    val userProfile by authManager.userProfile.collectAsState()
    val themeMode by ThemeManager.themeMode.collectAsState()
    val isSw = language == AppLanguage.SW

    var showEditProfileDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SokoColors.background)
            .verticalScroll(rememberScrollState())
            .testTag("account_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top App Bar
        Surface(color = SokoColors.headerSurface) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.AccountCircle, contentDescription = "Account", tint = SokoNeonBlue, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (isSw) "Akaunti ya SokoTZ" else "SokoTZ Account",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = SokoColors.textPrimary
                )
            }
        }

        if (currentUser != null) {
            // Logged-in User Profile Card (from Firestore)
            val user = currentUser!!
            val profile = userProfile
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, SokoColors.cardBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = SokoColors.cardSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // User Profile Photo
                        val photoUrl = profile?.photoUrl ?: user.photoUrl?.toString() ?: ""
                        if (photoUrl.isNotBlank()) {
                            SokoListingImage(
                                imageUrl = photoUrl,
                                contentDescription = "Profile Photo",
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, SokoNeonBlue, CircleShape),
                                shape = CircleShape
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(SokoGradients.AccentGradient),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = (user.displayName ?: user.email ?: "U").take(1).uppercase(),
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Black,
                                    color = SokoBlueBlack
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = profile?.displayName?.ifBlank { null } ?: user.displayName ?: (if (isSw) "Mwanachama SokoTZ" else "SokoTZ Member"),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = SokoColors.textPrimary
                            )
                            Text(
                                text = profile?.email?.ifBlank { null } ?: user.email ?: profile?.phoneNumber ?: user.phoneNumber ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SokoColors.textMuted
                            )

                            // AI Preferred Name indicator
                            val aiName = profile?.aiPreferredName?.ifBlank { null }
                            if (aiName != null) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isSw) "sokoTZ AI anakuita: '$aiName'" else "sokoTZ AI calls you: '$aiName'",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SokoNeonViolet,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(SokoColors.inputBackground)
                                        .border(1.dp, SokoNeonBlue.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (profile?.role == "BUSINESS_OWNER") (if (isSw) "Mwenye Biashara" else "Business Owner") else (if (isSw) "Mteja / Mnunuzi" else "Customer"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SokoNeonBlue
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.Default.CloudDone, contentDescription = "Synced", tint = SokoNeonBlue, modifier = Modifier.size(14.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Edit Profile Action Button (Requirement 4)
                    OutlinedButton(
                        onClick = { showEditProfileDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .testTag("edit_profile_button"),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SokoNeonBlue.copy(alpha = 0.7f)),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = if (SokoColors.isDark) SokoMidnightBlue else androidx.compose.ui.graphics.Color(0xFFE0F2FE))
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = SokoNeonBlue, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isSw) "Hariri Wasifu & Picha Yako" else "Edit Profile & Photo",
                            color = SokoNeonBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Switch Role Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isCustomer = profile?.role != "BUSINESS_OWNER"
                        OutlinedButton(
                            onClick = { authManager.updateUserRole("CUSTOMER") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isCustomer) SokoNeonBlue else SokoCardBorder
                            )
                        ) {
                            Text(
                                text = if (isSw) "Hali ya Mteja" else "Customer View",
                                color = if (isCustomer) SokoNeonBlue else SokoMutedText,
                                fontSize = 12.sp
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                authManager.updateUserRole("BUSINESS_OWNER")
                                onNavigateToBusinessHub()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (!isCustomer) SokoNeonViolet else SokoCardBorder
                            )
                        ) {
                            Text(
                                text = if (isSw) "Mwenye Biashara" else "Seller View",
                                color = if (!isCustomer) SokoNeonViolet else SokoMutedText,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        } else {
            // Guest User Card (Requirement 6)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, SokoNeonBlue.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = SokoColors.cardSurface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(SokoGradients.AccentGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AccountCircle, contentDescription = null, tint = SokoBlueBlack, modifier = Modifier.size(36.dp))
                    }

                    Text(
                        text = if (isSw) "Mgeni / Guest User" else "Guest User",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = SokoColors.textPrimary
                    )

                    Text(
                        text = if (isSw)
                            "Unaweza kuangalia na kutafuta bidhaa na maduka nchini kote bure. Kuanza kuuza au kuongea na sokoTZ AI, tafadhali ingia au jisajili."
                        else
                            "You can browse and search items countrywide as a guest. To post ads or chat with sokoTZ AI, please sign in or register.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SokoColors.textMuted,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Button(
                        onClick = onNavigateToSignIn,
                        colors = ButtonDefaults.buttonColors(containerColor = SokoNeonBlue),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("guest_signin_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Login, contentDescription = null, tint = SokoBlueBlack, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isSw) "Ingia / Fungua Akaunti Bure" else "Sign In / Register Free",
                            color = SokoBlueBlack,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Settings Section
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Theme Mode Selector Card (Requirement 3: Dark / Light Mode)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, SokoColors.cardBorder, RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = SokoColors.cardSurface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (themeMode == AppThemeMode.DARK) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = "Theme",
                            tint = SokoNeonBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isSw) "Muonekano (Mode ya Mandhari)" else "Appearance (Theme Mode)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = SokoColors.textPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppThemeMode.values().forEach { mode ->
                            val isSelected = themeMode == mode
                            val label = if (isSw) mode.titleSw else mode.titleEn
                            val icon = when (mode) {
                                AppThemeMode.DARK -> Icons.Default.DarkMode
                                AppThemeMode.LIGHT -> Icons.Default.LightMode
                                AppThemeMode.SYSTEM -> Icons.Default.SettingsBrightness
                            }

                            OutlinedButton(
                                onClick = { ThemeManager.setThemeMode(context, mode) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("theme_btn_${mode.name.lowercase()}"),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) SokoNeonBlue else SokoColors.cardBorder
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) (if (SokoColors.isDark) SokoMidnightBlue else androidx.compose.ui.graphics.Color(0xFFE0F2FE)) else Color.Transparent
                                ),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSelected) SokoNeonBlue else SokoColors.textMuted,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) SokoNeonBlue else SokoColors.textMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Language Selection Card
            AccountSettingItem(
                title = if (isSw) "Lugha / Language" else "Language / Lugha",
                subtitle = language.displayName,
                icon = Icons.Default.Language,
                onClick = { repository.toggleLanguage() }
            )

            // Admin Portal Link (Visible only to authorized Admins)
            if (userProfile?.role == "ADMIN") {
                AccountSettingItem(
                    title = if (isSw) "Dashibodi ya Msimamizi (Admin)" else "Admin Operations Portal",
                    subtitle = if (isSw) "Ukaguzi wa kampeni, ripoti, na usimamizi wa bei" else "Review campaigns, reports, and pricing",
                    icon = Icons.Default.AdminPanelSettings,
                    onClick = onNavigateToAdminDashboard
                )
            }

            // Video Ads Campaign Manager
            AccountSettingItem(
                title = if (isSw) "Kampeni Zangu za Video (Ads Feed)" else "My Video Ad Campaigns",
                subtitle = if (isSw) "Dhibiti na fuatilia matangazo yako ya video ya sokoTZ" else "Manage and track your video ads",
                icon = Icons.Default.Campaign,
                onClick = onNavigateToSellerAdsManager
            )

            // Saved Video Ads
            AccountSettingItem(
                title = if (isSw) "Matangazo Yaliyohifadhiwa" else "Saved Video Ads",
                subtitle = if (isSw) "Tazama video na ofa ulizozihifadhi" else "View saved ads and bookmarked offers",
                icon = Icons.Default.Bookmark,
                onClick = onNavigateToSavedAds
            )

            // Seller Hub Link
            AccountSettingItem(
                title = if (isSw) "Kituo cha Biashara Yangu" else "My Business Hub",
                subtitle = if (isSw) "Dhibiti matangazo na taarifa za duka lako" else "Manage listings and store info",
                icon = Icons.Default.Store,
                onClick = onNavigateToBusinessHub
            )

            // Platform Security Info
            AccountSettingItem(
                title = if (isSw) "Usalama & Hifadhi ya Wingu" else "Security & Cloud Persistence",
                subtitle = "Firebase Firestore • Google Identity Services",
                icon = Icons.Default.Security,
                onClick = {
                    Toast.makeText(
                        context,
                        "SokoTZ - Imehifadhiwa kwenye Firebase Firestore kwa usalama.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            )

            // Logout Button (Only if logged in)
            if (currentUser != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = { authManager.signOut(scope) },
                    colors = ButtonDefaults.buttonColors(containerColor = SokoDeepNavy),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .border(1.dp, SokoError.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .testTag("logout_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout", tint = SokoError, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isSw) "Ondoka kwenye Akaunti" else "Sign Out", color = SokoError, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Edit Profile Dialog (Requirement 4 & 5)
    if (showEditProfileDialog && currentUser != null) {
        val profile = userProfile
        EditProfileDialog(
            currentProfile = profile,
            isSw = isSw,
            onDismiss = { showEditProfileDialog = false },
            onSave = { newName, newPhone, newPhoto, newAiName ->
                scope.launch {
                    val result = authManager.updateUserProfile(
                        displayName = newName,
                        phoneNumber = newPhone,
                        photoUrl = newPhoto,
                        aiPreferredName = newAiName
                    )
                    if (result.isSuccess) {
                        Toast.makeText(
                            context,
                            if (isSw) "Wasifu umesasishwa kikamilifu!" else "Profile updated successfully!",
                            Toast.LENGTH_SHORT
                        ).show()
                        showEditProfileDialog = false
                    } else {
                        Toast.makeText(
                            context,
                            "Hitilafu: ${result.exceptionOrNull()?.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        )
    }
}

@Composable
fun EditProfileDialog(
    currentProfile: UserProfile?,
    isSw: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String, photo: String, aiName: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var editName by remember { mutableStateOf(currentProfile?.displayName ?: "") }
    var editPhone by remember { mutableStateOf(currentProfile?.phoneNumber ?: "") }
    var editAiName by remember { mutableStateOf(currentProfile?.aiPreferredName ?: "") }
    var editPhotoUrl by remember { mutableStateOf(currentProfile?.photoUrl ?: "") }
    var isProcessingPhoto by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            isProcessingPhoto = true
            scope.launch(Dispatchers.IO) {
                val base64 = ImageUtils.uriToBase64(context, uri, maxDimension = 400, quality = 80)
                withContext(Dispatchers.Main) {
                    isProcessingPhoto = false
                    if (base64 != null) {
                        editPhotoUrl = base64
                    }
                }
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            isProcessingPhoto = true
            scope.launch(Dispatchers.IO) {
                val base64 = ImageUtils.bitmapToBase64(bitmap, maxDimension = 400, quality = 80)
                withContext(Dispatchers.Main) {
                    isProcessingPhoto = false
                    editPhotoUrl = base64
                }
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch(null)
        } else {
            Toast.makeText(
                context,
                if (isSw) "Ruhusa ya kamera inahitajika kupiga picha." else "Camera permission is required.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isSw) "Hariri Wasifu Wako" else "Edit Your Profile",
                color = SokoColors.textPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Profile Photo Preview & Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (editPhotoUrl.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape)
                                .border(2.dp, SokoNeonBlue, CircleShape)
                        ) {
                            SokoListingImage(
                                imageUrl = editPhotoUrl,
                                contentDescription = "Avatar",
                                modifier = Modifier.fillMaxSize(),
                                shape = CircleShape
                            )
                            IconButton(
                                onClick = { editPhotoUrl = "" },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(SokoBlueBlack.copy(alpha = 0.8f))
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = SokoError, modifier = Modifier.size(14.dp))
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(SokoMidnightBlue)
                                .border(1.dp, SokoCardBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AccountCircle, contentDescription = null, tint = SokoMutedText, modifier = Modifier.size(48.dp))
                        }
                    }

                    if (isProcessingPhoto) {
                        Spacer(modifier = Modifier.height(6.dp))
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = SokoNeonBlue, strokeWidth = 2.dp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SokoNeonBlue),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, tint = SokoNeonBlue, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isSw) "Picha (Galari)" else "Gallery", color = SokoNeonBlue, fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                val hasPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasPermission) {
                                    cameraLauncher.launch(null)
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SokoNeonViolet),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = SokoNeonViolet, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isSw) "Piga Picha" else "Camera", color = SokoNeonViolet, fontSize = 11.sp)
                        }
                    }
                }

                // Name field
                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = { Text(if (isSw) "Jina Kamili" else "Full Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_profile_name_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SokoNeonBlue,
                        unfocusedBorderColor = SokoCardBorder,
                        focusedTextColor = SokoColors.textPrimary,
                        unfocusedTextColor = SokoColors.textPrimary
                    )
                )

                // AI Preferred Name field (Requirement 5)
                OutlinedTextField(
                    value = editAiName,
                    onValueChange = { editAiName = it },
                    label = { Text(if (isSw) "Jina la Kuitwa na sokoTZ AI" else "Name sokoTZ AI Calls You") },
                    leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = SokoNeonViolet) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_profile_ai_name_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SokoNeonViolet,
                        unfocusedBorderColor = SokoCardBorder,
                        focusedTextColor = SokoColors.textPrimary,
                        unfocusedTextColor = SokoColors.textPrimary
                    )
                )

                // Phone field
                OutlinedTextField(
                    value = editPhone,
                    onValueChange = { editPhone = it },
                    label = { Text(if (isSw) "Namba ya Simu" else "Phone Number") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = SokoNeonBlue) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SokoNeonBlue,
                        unfocusedBorderColor = SokoCardBorder,
                        focusedTextColor = SokoColors.textPrimary,
                        unfocusedTextColor = SokoColors.textPrimary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(editName, editPhone, editPhotoUrl, editAiName)
                },
                colors = ButtonDefaults.buttonColors(containerColor = SokoNeonBlue),
                modifier = Modifier.testTag("save_profile_button")
            ) {
                Text(if (isSw) "Hifadhi Mabadiliko" else "Save Changes", color = SokoBlueBlack, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isSw) "Ghairi" else "Cancel", color = SokoColors.textMuted)
            }
        },
        containerColor = SokoColors.cardSurface
    )
}

@Composable
fun AccountSettingItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, SokoColors.cardBorder, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = SokoColors.cardSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (SokoColors.isDark) SokoMidnightBlue else MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = SokoNeonBlue, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(text = title, style = MaterialTheme.typography.titleMedium, color = SokoColors.textPrimary, fontWeight = FontWeight.SemiBold)
                    Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = SokoColors.textMuted)
                }
            }
            Icon(Icons.Default.ChevronRight, contentDescription = "Navigate", tint = SokoColors.textMuted)
        }
    }
}
