package com.example.sokotz.ui.screens.account

import android.app.Activity
import android.widget.Toast
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sokotz.data.auth.AuthManager
import com.example.sokotz.data.model.AppLanguage
import com.example.sokotz.data.repository.FirestoreMarketplaceRepository
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

@Composable
fun AccountScreen(
    authManager: AuthManager,
    repository: FirestoreMarketplaceRepository,
    onNavigateToBusinessHub: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val language by repository.language.collectAsState()
    val currentUser by authManager.currentUser.collectAsState()
    val userProfile by authManager.userProfile.collectAsState()
    val isLoading by authManager.isLoading.collectAsState()
    val authError by authManager.authError.collectAsState()
    val isSw = language == AppLanguage.SW

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SokoBlueBlack)
            .verticalScroll(rememberScrollState())
            .testTag("account_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top App Bar
        Surface(color = SokoMidnightBlue) {
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
                    color = SokoLightText
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
                    .border(1.dp, SokoCardBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = SokoCardSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(SokoGradients.AccentGradient),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (user.displayName ?: user.email ?: "U").take(1).uppercase(),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = SokoBlueBlack
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = user.displayName ?: if (isSw) "Mwanachama SokoTZ" else "SokoTZ Member",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = SokoLightText
                            )
                            Text(
                                text = user.email ?: user.phoneNumber ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SokoMutedText
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(SokoDeepNavy)
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

                    Spacer(modifier = Modifier.height(16.dp))

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

            // Quick Actions
            Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Language Selection Card
                AccountSettingItem(
                    title = if (isSw) "Lugha / Language" else "Language / Lugha",
                    subtitle = language.displayName,
                    icon = Icons.Default.Language,
                    onClick = { repository.toggleLanguage() }
                )

                // Seller Hub Link
                AccountSettingItem(
                    title = if (isSw) "Kituo cha Biashara Yangu" else "My Business Hub",
                    subtitle = if (isSw) "Dhibiti matangazo na taarifa za duka lako" else "Manage listings and store info",
                    icon = Icons.Default.Store,
                    onClick = onNavigateToBusinessHub
                )

                // Platform Roadmap & Security Info
                AccountSettingItem(
                    title = if (isSw) "Usalama & Hifadhi ya Wingu" else "Security & Cloud Persistence",
                    subtitle = "Firebase Firestore • Google Identity Services",
                    icon = Icons.Default.Security,
                    onClick = {
                        Toast.makeText(
                            context,
                            "SokoTZ - Imehifadhiwa kwenye Firebase Firestore kwa usalama na kuunganishwa na Google.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Logout Button
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
        } else {
            // Google Sign-In Card (Real Google Authentication)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, SokoCardBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = SokoCardSurface)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(SokoGradients.AccentGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("SZ", fontWeight = FontWeight.Black, fontSize = 24.sp, color = SokoBlueBlack)
                    }

                    Text(
                        text = if (isSw) "Ingia kwenye SokoTZ" else "Sign in to SokoTZ",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = SokoLightText
                    )

                    Text(
                        text = if (isSw)
                            "Unganisha akaunti yako ya Google kupata huduma zote za soko, kusajili biashara, na kuweka matangazo halisi mtandaoni."
                        else
                            "Sign in with your Google account to discover businesses, onboard your store, and post live verified listings.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SokoMutedText,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    if (authError != null) {
                        Text(
                            text = authError!!,
                            color = SokoError,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }

                    // Interactive Google Sign-In Button
                    Button(
                        onClick = {
                            if (context is Activity) {
                                authManager.signInWithGoogle(context, scope)
                            }
                        },
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("google_signin_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = SokoBlueBlack, strokeWidth = 2.dp)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "G",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 20.sp,
                                    color = Color(0xFF4285F4)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = if (isSw) "Ingia kwa Google" else "Sign in with Google",
                                    color = Color(0xFF1F1F1F),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }

                    Text(
                        text = if (isSw) "Usajili na ulinzi unathibitishwa na Google Identity" else "Secured by Google Identity Services",
                        style = MaterialTheme.typography.bodySmall,
                        color = SokoMutedText
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun AccountSettingItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, SokoCardBorder, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = SokoCardSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SokoMidnightBlue),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = SokoNeonBlue, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = SokoLightText)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = SokoMutedText)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = "Go", tint = SokoMutedText)
        }
    }
}
