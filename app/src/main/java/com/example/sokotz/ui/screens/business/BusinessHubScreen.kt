package com.example.sokotz.ui.screens.business

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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Visibility
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.withContext
import coil.compose.AsyncImage
import com.example.sokotz.data.auth.AuthManager
import com.example.sokotz.data.model.AppLanguage
import com.example.sokotz.data.model.Listing
import com.example.sokotz.data.remote.GeminiClient
import com.example.sokotz.data.repository.FirestoreMarketplaceRepository
import com.example.sokotz.data.util.ImageUtils
import com.example.sokotz.ui.components.ListingCard
import com.example.sokotz.ui.components.SokoListingImage
import com.example.ui.theme.SokoColors
import com.example.ui.theme.SokoBlueBlack
import com.example.ui.theme.SokoCardBorder
import com.example.ui.theme.SokoCardSurface
import com.example.ui.theme.SokoDeepNavy
import com.example.ui.theme.SokoDeepPurple
import com.example.ui.theme.SokoError
import com.example.ui.theme.SokoGradients
import com.example.ui.theme.SokoLightText
import com.example.ui.theme.SokoMidnightBlue
import com.example.ui.theme.SokoMutedText
import com.example.ui.theme.SokoNeonBlue
import com.example.ui.theme.SokoNeonViolet
import com.example.ui.theme.SokoSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun BusinessHubScreen(
    repository: FirestoreMarketplaceRepository,
    authManager: AuthManager,
    onNavigateToListing: (String) -> Unit,
    onNavigateToAssistant: () -> Unit,
    onNavigateToSignIn: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val language by repository.language.collectAsState()
    val currentUser by authManager.currentUser.collectAsState()
    val businesses by repository.businesses.collectAsState()
    val listings by repository.listings.collectAsState()
    val isSw = language == AppLanguage.SW

    if (currentUser == null) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(SokoColors.background)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(SokoGradients.AccentGradient),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Storefront, contentDescription = null, tint = SokoBlueBlack, modifier = Modifier.size(40.dp))
            }
            Text(
                text = if (isSw) "Kituo cha Wafanyabiashara SokoTZ" else "SokoTZ Business Seller Hub",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = SokoColors.textPrimary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Text(
                text = if (isSw)
                    "Kuuza kwenye SokoTZ kunahitaji akaunti ya mfanyabiashara ili uweze kusajili duka lako, kuweka muda wa kazi, kutuma matangazo, na kuwafikia wateja nchini Tanzania."
                else
                    "Selling on SokoTZ requires a seller account to register your store profile, specify working hours, publish listings, and connect with customers.",
                style = MaterialTheme.typography.bodyMedium,
                color = SokoColors.textMuted,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, SokoColors.cardBorder, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = SokoColors.cardSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isSw) "Faida za Kujiunga kama Muuzaji:" else "Seller Benefits:",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = SokoNeonBlue
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = SokoNeonBlue, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isSw) "Sajili wasifu wa duka lako na muda maalum wa kazi" else "Register store profile with working hours",
                            color = SokoColors.textPrimary,
                            fontSize = 12.sp
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = SokoNeonBlue, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isSw) "Weka picha kutoka kwenye kamera au galari ya simu" else "Upload photos using camera or gallery",
                            color = SokoColors.textPrimary,
                            fontSize = 12.sp
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = SokoNeonBlue, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isSw) "Tazama idadi halisi ya watazamaji (Unique Viewers)" else "Track unique viewers per user account",
                            color = SokoColors.textPrimary,
                            fontSize = 12.sp
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = SokoNeonBlue, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isSw) "Usaidizi wa sokoTZ AI kutunga maelezo ya kuvutia" else "Draft ad copy with sokoTZ AI assistance",
                            color = SokoColors.textPrimary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
            Button(
                onClick = onNavigateToSignIn,
                colors = ButtonDefaults.buttonColors(containerColor = SokoNeonBlue),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("seller_gate_signin_button"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Login, contentDescription = null, tint = SokoBlueBlack, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isSw) "Ingia au Fungua Akaunti ya Muuzaji" else "Sign In or Register as Seller",
                    color = SokoBlueBlack,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
        return
    }

    var currentTab by remember { mutableIntStateOf(0) } // 0: Dashboard, 1: Add Listing, 2: Onboard/Edit Business
    var editingListing by remember { mutableStateOf<Listing?>(null) }
    var listingToDelete by remember { mutableStateOf<Listing?>(null) }

    val userBusiness = businesses.firstOrNull { it.ownerId == (currentUser?.uid ?: "") }
    val businessListings = listings.filter { it.ownerId == (currentUser?.uid ?: "") }
    val totalViews = businessListings.sumOf { it.viewsCount }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SokoColors.background)
            .testTag("business_hub_screen")
    ) {
        // Hub Top Bar
        Surface(color = SokoColors.headerSurface) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isSw) "Kituo cha Biashara Yako" else "Seller Hub & Management",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = SokoLightText
                        )
                        Text(
                            text = userBusiness?.name ?: if (isSw) "Bado hujasajili biashara rasmi" else "No business registered yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SokoNeonBlue
                        )
                    }

                    if (userBusiness != null && userBusiness.isVerified) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(SokoSuccess.copy(alpha = 0.2f))
                                .border(1.dp, SokoSuccess, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Imethibitishwa", color = SokoSuccess, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tab Selector
                TabRow(
                    selectedTabIndex = currentTab,
                    containerColor = SokoColors.inputBackground,
                    contentColor = SokoNeonBlue,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[currentTab]),
                            color = SokoNeonBlue
                        )
                    }
                ) {
                    Tab(
                        selected = currentTab == 0,
                        onClick = { currentTab = 0 },
                        text = { Text(if (isSw) "Dashibodi" else "Dashboard", fontWeight = FontWeight.Bold, color = if (currentTab == 0) SokoNeonBlue else SokoMutedText) }
                    )
                    Tab(
                        selected = currentTab == 1,
                        onClick = { currentTab = 1 },
                        text = { Text(if (isSw) "+ Tangazo" else "+ New Listing", fontWeight = FontWeight.Bold, color = if (currentTab == 1) SokoNeonBlue else SokoMutedText) }
                    )
                    Tab(
                        selected = currentTab == 2,
                        onClick = { currentTab = 2 },
                        text = {
                            Text(
                                text = if (userBusiness != null) (if (isSw) "Hariri Biashara" else "Edit Biz") else (if (isSw) "Sajili Biashara" else "Onboard Biz"),
                                fontWeight = FontWeight.Bold,
                                color = if (currentTab == 2) SokoNeonBlue else SokoMutedText
                            )
                        }
                    )
                }
            }
        }

        // Tab Content
        when (currentTab) {
            0 -> {
                // Dashboard Tab
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Quick Stats Cards - with Impression/Views Tracker
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                StatCard(
                                    title = if (isSw) "Matangazo Yako Hai" else "Active Listings",
                                    value = "${businessListings.size}",
                                    icon = Icons.Default.Inventory2,
                                    iconColor = SokoNeonBlue,
                                    modifier = Modifier.weight(1f)
                                )
                                StatCard(
                                    title = if (isSw) "Watazamaji Wote (Views)" else "Total Views",
                                    value = "$totalViews",
                                    icon = Icons.Default.Visibility,
                                    iconColor = SokoNeonViolet,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            StatCard(
                                title = if (isSw) "Hali ya Duka / Biashara" else "Business Profile Status",
                                value = if (userBusiness != null) (if (isSw) "Ipo Hai: ${userBusiness.name}" else "Active: ${userBusiness.name}") else (if (isSw) "Bofya Tab ya 3 Kusajili" else "Tap Tab 3 to Register"),
                                icon = Icons.Default.Business,
                                iconColor = if (userBusiness != null) SokoSuccess else SokoNeonBlue,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // SokoTZ Assistant Drafting Banner
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, SokoNeonViolet, RoundedCornerShape(12.dp))
                                .clickable { onNavigateToAssistant() },
                            colors = CardDefaults.cardColors(containerColor = SokoDeepPurple.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(SokoGradients.PurpleNeonGradient),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = "AI Draft", tint = SokoLightText)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isSw) "sokoTZ Msaidizi wa Kuandika Matangazo" else "sokoTZ AI Copywriter Assistant",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = SokoLightText
                                    )
                                    Text(
                                        text = if (isSw) "Bofya hapa sokoTZ Msaidizi akusaidie kutunga maelezo ya mauzo" else "Chat with sokoTZ AI to draft sales copy and descriptions",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SokoMutedText
                                    )
                                }
                            }
                        }
                    }

                    // Listings Section Header
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isSw) "Matangazo Yako (${businessListings.size})" else "Your Published Listings (${businessListings.size})",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = SokoLightText
                            )
                            Button(
                                onClick = { currentTab = 1 },
                                colors = ButtonDefaults.buttonColors(containerColor = SokoNeonBlue),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add", tint = SokoBlueBlack, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isSw) "Ongeza" else "Add New", color = SokoBlueBlack, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }

                    if (businessListings.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                colors = CardDefaults.cardColors(containerColor = SokoColors.cardSurface)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = if (isSw) "Bado hujachapisha tangazo lolote." else "You haven't published any listings yet.",
                                        color = SokoColors.textMuted,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = { currentTab = 1 },
                                        colors = ButtonDefaults.buttonColors(containerColor = SokoNeonBlue)
                                    ) {
                                        Text(if (isSw) "Weka Tangazo la Kwanza" else "Create First Listing", color = SokoBlueBlack, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    } else {
                        items(businessListings) { listing ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(SokoColors.cardSurface)
                                    .border(1.dp, SokoColors.cardBorder, RoundedCornerShape(14.dp))
                                    .padding(12.dp)
                            ) {
                                ListingCard(
                                    listing = listing,
                                    language = language,
                                    onClick = { onNavigateToListing(listing.id) }
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Seller Performance & Action Bar (Views Tracker, Edit, Delete)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SokoColors.inputBackground)
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Visibility,
                                            contentDescription = "Views",
                                            tint = SokoNeonBlue,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isSw) "${listing.viewsCount} watazamaji" else "${listing.viewsCount} views",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = SokoNeonBlue
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = { editingListing = listing },
                                            shape = RoundedCornerShape(6.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, SokoNeonBlue),
                                            colors = ButtonDefaults.outlinedButtonColors(containerColor = if (SokoColors.isDark) SokoMidnightBlue else androidx.compose.ui.graphics.Color(0xFFE0F2FE)),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(32.dp).testTag("edit_listing_button_${listing.id}")
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = SokoNeonBlue, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(if (isSw) "Hariri" else "Edit", color = SokoNeonBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        OutlinedButton(
                                            onClick = { listingToDelete = listing },
                                            shape = RoundedCornerShape(6.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, SokoError.copy(alpha = 0.6f)),
                                            colors = ButtonDefaults.outlinedButtonColors(containerColor = if (SokoColors.isDark) SokoMidnightBlue else androidx.compose.ui.graphics.Color(0xFFE0F2FE)),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = SokoError, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(if (isSw) "Futa" else "Delete", color = SokoError, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                // Add New Listing Tab (with Product Photo Capture & Upload!)
                var listingTitle by remember { mutableStateOf("") }
                var listingPrice by remember { mutableStateOf("") }
                var listingCategory by remember { mutableStateOf("kilimo") }
                var listingType by remember { mutableStateOf("PRODUCT") }
                var listingCity by remember { mutableStateOf(userBusiness?.city ?: "Dar es Salaam") }
                var listingDescription by remember { mutableStateOf("") }
                var listingImageUrl by remember { mutableStateOf("") }
                var isProcessingImage by remember { mutableStateOf(false) }
                var isDraftingAi by remember { mutableStateOf(false) }
                var isPublishing by remember { mutableStateOf(false) }

                val photoPickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.PickVisualMedia()
                ) { uri: Uri? ->
                    if (uri != null) {
                        isProcessingImage = true
                        scope.launch(Dispatchers.IO) {
                            val base64 = ImageUtils.uriToBase64(context, uri)
                            withContext(Dispatchers.Main) {
                                isProcessingImage = false
                                if (base64 != null) {
                                    listingImageUrl = base64
                                    Toast.makeText(context, if (isSw) "Picha imewekwa kikamilifu!" else "Photo loaded successfully!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, if (isSw) "Hitilafu ya kusoma picha, tafadhali jaribu nyingine." else "Could not load image, please try another.", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                }

                val cameraLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.TakePicturePreview()
                ) { bitmap: Bitmap? ->
                    if (bitmap != null) {
                        isProcessingImage = true
                        scope.launch(Dispatchers.IO) {
                            val base64 = ImageUtils.bitmapToBase64(bitmap)
                            withContext(Dispatchers.Main) {
                                isProcessingImage = false
                                listingImageUrl = base64
                                Toast.makeText(context, if (isSw) "Picha imepigwa kikamilifu!" else "Photo captured successfully!", Toast.LENGTH_SHORT).show()
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
                            if (isSw) "Ruhusa ya kamera inahitajika kupiga picha ya bidhaa." else "Camera permission is required to capture product photo.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = if (isSw) "Chapisha Bidhaa au Huduma Mpya" else "Publish New Product or Service",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = SokoLightText
                    )

                    // Type toggle: Product vs Service
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { listingType = "PRODUCT" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (listingType == "PRODUCT") SokoNeonBlue else SokoDeepNavy
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (isSw) "Bidhaa (Product)" else "Product",
                                color = if (listingType == "PRODUCT") SokoBlueBlack else SokoLightText,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = { listingType = "SERVICE" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (listingType == "SERVICE") SokoNeonViolet else SokoDeepNavy
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (isSw) "Huduma (Service)" else "Service",
                                color = if (listingType == "SERVICE") SokoBlueBlack else SokoLightText,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Product Photo Picker & Camera Section
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SokoCardSurface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = if (isSw) "Picha ya Bidhaa / Huduma" else "Product / Service Photo",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = SokoLightText
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isSw) "Weka picha halisi kuvutia wateja zaidi." else "Add a real photo to attract more buyers.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SokoMutedText
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            if (isProcessingImage) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SokoMidnightBlue)
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = SokoNeonBlue, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = if (isSw) "Inachakata picha ya bidhaa..." else "Processing product photo...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SokoNeonBlue
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                            }

                            if (listingImageUrl.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(1.dp, SokoNeonBlue, RoundedCornerShape(8.dp))
                                ) {
                                    SokoListingImage(
                                        imageUrl = listingImageUrl,
                                        contentDescription = "Preview",
                                        modifier = Modifier.fillMaxSize(),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    IconButton(
                                        onClick = { listingImageUrl = "" },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(SokoBlueBlack.copy(alpha = 0.8f))
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Remove", tint = SokoError, modifier = Modifier.size(16.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, SokoNeonBlue),
                                    colors = ButtonDefaults.outlinedButtonColors(containerColor = SokoMidnightBlue),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Image, contentDescription = null, tint = SokoNeonBlue, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isSw) "Kivinjari / Galari" else "Pick Image", color = SokoNeonBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, SokoNeonViolet),
                                    colors = ButtonDefaults.outlinedButtonColors(containerColor = SokoMidnightBlue),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = SokoNeonViolet, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isSw) "Piga Picha" else "Camera", color = SokoNeonViolet, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Title
                    OutlinedTextField(
                        value = listingTitle,
                        onValueChange = { listingTitle = it },
                        label = { Text(if (isSw) "Jina la Bidhaa au Huduma" else "Listing Title", color = SokoMutedText) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_listing_title_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SokoNeonBlue,
                            unfocusedBorderColor = SokoCardBorder,
                            focusedTextColor = SokoLightText,
                            unfocusedTextColor = SokoLightText
                        )
                    )

                    // Price
                    OutlinedTextField(
                        value = listingPrice,
                        onValueChange = { listingPrice = it.filter { char -> char.isDigit() } },
                        label = { Text(if (isSw) "Bei kwa TZS (mf. 50000)" else "Price in TZS (e.g. 50000)", color = SokoMutedText) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_listing_price_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SokoNeonBlue,
                            unfocusedBorderColor = SokoCardBorder,
                            focusedTextColor = SokoLightText,
                            unfocusedTextColor = SokoLightText
                        )
                    )

                    // Location City
                    OutlinedTextField(
                        value = listingCity,
                        onValueChange = { listingCity = it },
                        label = { Text(if (isSw) "Mji / Mkoa (mf. Dar es Salaam, Mbeya)" else "City / Region (e.g. Dar es Salaam)", color = SokoMutedText) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SokoNeonBlue,
                            unfocusedBorderColor = SokoCardBorder,
                            focusedTextColor = SokoLightText,
                            unfocusedTextColor = SokoLightText
                        )
                    )

                    // sokoTZ AI description draft helper trigger
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SokoMidnightBlue)
                            .clickable(enabled = !isDraftingAi) {
                                if (listingTitle.isBlank()) {
                                    Toast.makeText(context, if (isSw) "Tafadhali weka jina la bidhaa kwanza!" else "Please enter item title first!", Toast.LENGTH_SHORT).show()
                                    return@clickable
                                }
                                isDraftingAi = true
                                scope.launch {
                                    try {
                                        val prompt = "Nisaidie kutunga maelezo mafupi ya kuvutia ya tangazo la SokoTZ la bidhaa hii: '$listingTitle' yenye bei ya TZS $listingPrice katika mji wa $listingCity. Andika maelezo ya ubora na kuwasiliana kwa simu/WhatsApp."
                                        val draft = GeminiClient.chat(emptyList(), prompt)
                                        listingDescription = draft
                                        Toast.makeText(context, if (isSw) "sokoTZ Msaidizi ametunga maelezo ya tangazo!" else "Draft generated by sokoTZ AI!", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "AI info: ${e.message}", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isDraftingAi = false
                                    }
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isDraftingAi) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = SokoNeonViolet, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "AI", tint = SokoNeonViolet, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isDraftingAi)
                                (if (isSw) "sokoTZ Msaidizi anatunga maelezo..." else "sokoTZ AI is generating copy...")
                            else
                                (if (isSw) "Tunga maelezo kwa sokoTZ Msaidizi" else "Auto-draft description using sokoTZ AI"),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = SokoNeonViolet
                        )
                    }

                    // Description text area
                    OutlinedTextField(
                        value = listingDescription,
                        onValueChange = { listingDescription = it },
                        label = { Text(if (isSw) "Maelezo Kamili ya Tangazo" else "Full Description", color = SokoMutedText) },
                        minLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_listing_desc_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SokoNeonBlue,
                            unfocusedBorderColor = SokoCardBorder,
                            focusedTextColor = SokoLightText,
                            unfocusedTextColor = SokoLightText
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            if (listingTitle.isBlank() || listingPrice.isBlank()) {
                                Toast.makeText(context, if (isSw) "Tafadhali jaza jina na bei!" else "Please fill title and price!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val priceLong = listingPrice.toLongOrNull() ?: 0L
                            isPublishing = true
                            scope.launch {
                                val result = repository.createListing(
                                    businessId = userBusiness?.id ?: "biz_direct_${currentUser?.uid ?: "user"}",
                                    businessName = userBusiness?.name ?: (currentUser?.displayName ?: "Muuzaji SokoTZ"),
                                    title = listingTitle,
                                    priceTzs = priceLong,
                                    categorySlug = listingCategory,
                                    description = listingDescription,
                                    type = listingType,
                                    city = listingCity,
                                    phoneNumber = userBusiness?.phoneNumber ?: (currentUser?.phoneNumber ?: "+255"),
                                    whatsappNumber = userBusiness?.whatsappNumber ?: (currentUser?.phoneNumber ?: "+255"),
                                    imageUrl = listingImageUrl
                                )
                                isPublishing = false
                                if (result.isSuccess) {
                                    Toast.makeText(context, if (isSw) "Tangazo limehifadhiwa Firestore!" else "Listing published to Firestore!", Toast.LENGTH_SHORT).show()
                                    currentTab = 0
                                } else {
                                    Toast.makeText(context, "Hitilafu: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        enabled = !isPublishing,
                        colors = ButtonDefaults.buttonColors(containerColor = SokoNeonBlue),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("submit_new_listing_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isPublishing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = SokoBlueBlack, strokeWidth = 2.dp)
                        } else {
                            Text(
                                text = if (isSw) "Chapisha Tangazo Kwenye Firestore" else "Publish Listing to Firestore",
                                color = SokoBlueBlack,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }

            2 -> {
                // Onboard or Edit Business Profile Tab
                var bizName by remember { mutableStateOf(userBusiness?.name ?: "") }
                var bizCategory by remember { mutableStateOf(userBusiness?.categorySlug ?: "elektroniki") }
                var bizCity by remember { mutableStateOf(userBusiness?.city ?: "Dar es Salaam") }
                var bizDistrict by remember { mutableStateOf(userBusiness?.district ?: "Ilala") }
                var bizPhone by remember { mutableStateOf(userBusiness?.phoneNumber ?: "+255") }
                var bizWhatsapp by remember { mutableStateOf(userBusiness?.whatsappNumber ?: "+255") }
                var bizOpeningHours by remember { mutableStateOf(userBusiness?.openingHours ?: "Jumatatu - Jumamosi: 08:00 - 18:00") }
                var bizDescription by remember { mutableStateOf(userBusiness?.descriptionSw ?: "") }
                var isSubmittingBiz by remember { mutableStateOf(false) }

                LaunchedEffect(userBusiness) {
                    userBusiness?.let {
                        bizName = it.name
                        bizCategory = it.categorySlug
                        bizCity = it.city
                        bizDistrict = it.district
                        bizPhone = it.phoneNumber
                        bizWhatsapp = it.whatsappNumber
                        bizOpeningHours = it.openingHours
                        bizDescription = it.descriptionSw
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = if (userBusiness != null)
                            (if (isSw) "Hariri Taarifa za Biashara Yako" else "Edit Your Business Profile")
                        else
                            (if (isSw) "Sajili Biashara Yako kwenye SokoTZ" else "Register Your Business on SokoTZ"),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = SokoLightText
                    )
                    Text(
                        text = if (isSw) "Taarifa zako zitahifadhiwa kwenye Firestore na kuonekana moja kwa moja nchini kote." else "Your business will be stored in Firestore and visible countrywide.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SokoMutedText
                    )

                    OutlinedTextField(
                        value = bizName,
                        onValueChange = { bizName = it },
                        label = { Text(if (isSw) "Jina Rasmi la Biashara / Duka" else "Official Business Name", color = SokoMutedText) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("onboard_biz_name_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SokoNeonBlue,
                            unfocusedBorderColor = SokoCardBorder,
                            focusedTextColor = SokoLightText,
                            unfocusedTextColor = SokoLightText
                        )
                    )

                    OutlinedTextField(
                        value = bizCity,
                        onValueChange = { bizCity = it },
                        label = { Text(if (isSw) "Mji / Mkoa (mf. Dar es Salaam)" else "City / Region (e.g. Dar es Salaam)", color = SokoMutedText) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SokoNeonBlue,
                            unfocusedBorderColor = SokoCardBorder,
                            focusedTextColor = SokoLightText,
                            unfocusedTextColor = SokoLightText
                        )
                    )

                    OutlinedTextField(
                        value = bizDistrict,
                        onValueChange = { bizDistrict = it },
                        label = { Text(if (isSw) "Wilaya / Mtaa (mf. Kariakoo, Ilala)" else "District / Street (e.g. Kariakoo)", color = SokoMutedText) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SokoNeonBlue,
                            unfocusedBorderColor = SokoCardBorder,
                            focusedTextColor = SokoLightText,
                            unfocusedTextColor = SokoLightText
                        )
                    )

                    OutlinedTextField(
                        value = bizPhone,
                        onValueChange = { bizPhone = it },
                        label = { Text(if (isSw) "Namba ya Simu ya Kupigiwa" else "Direct Phone Number", color = SokoMutedText) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SokoNeonBlue,
                            unfocusedBorderColor = SokoCardBorder,
                            focusedTextColor = SokoLightText,
                            unfocusedTextColor = SokoLightText
                        )
                    )

                    OutlinedTextField(
                        value = bizWhatsapp,
                        onValueChange = { bizWhatsapp = it },
                        label = { Text(if (isSw) "Namba ya WhatsApp ya Biashara" else "Business WhatsApp Number", color = SokoMutedText) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SokoNeonBlue,
                            unfocusedBorderColor = SokoCardBorder,
                            focusedTextColor = SokoLightText,
                            unfocusedTextColor = SokoLightText
                        )
                    )

                    OutlinedTextField(
                        value = bizOpeningHours,
                        onValueChange = { bizOpeningHours = it },
                        label = { Text(if (isSw) "Muda wa Kazi (mf. Jumatatu - Jumamosi: 8:00 Asubuhi - 2:00 Usiku)" else "Working Hours (e.g. Mon - Sat: 8:00 AM - 6:00 PM)", color = SokoMutedText) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("onboard_biz_hours_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SokoNeonBlue,
                            unfocusedBorderColor = SokoCardBorder,
                            focusedTextColor = SokoLightText,
                            unfocusedTextColor = SokoLightText
                        )
                    )

                    OutlinedTextField(
                        value = bizDescription,
                        onValueChange = { bizDescription = it },
                        label = { Text(if (isSw) "Maelezo ya Biashara & Huduma Unazotoa" else "Description & Services Offered", color = SokoMutedText) },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SokoNeonBlue,
                            unfocusedBorderColor = SokoCardBorder,
                            focusedTextColor = SokoLightText,
                            unfocusedTextColor = SokoLightText
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            if (bizName.isBlank() || bizPhone.isBlank()) {
                                Toast.makeText(context, if (isSw) "Tafadhali weka jina na simu!" else "Please fill name and phone!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isSubmittingBiz = true
                            scope.launch {
                                val result = if (userBusiness != null) {
                                    repository.updateBusinessProfile(
                                        businessId = userBusiness.id,
                                        name = bizName,
                                        categorySlug = bizCategory,
                                        city = bizCity,
                                        district = bizDistrict,
                                        phoneNumber = bizPhone,
                                        whatsappNumber = bizWhatsapp,
                                        description = bizDescription,
                                        openingHours = bizOpeningHours
                                    )
                                } else {
                                    repository.registerBusinessProfile(
                                        name = bizName,
                                        categorySlug = bizCategory,
                                        city = bizCity,
                                        district = bizDistrict,
                                        phoneNumber = bizPhone,
                                        whatsappNumber = bizWhatsapp,
                                        description = bizDescription,
                                        openingHours = bizOpeningHours
                                    )
                                }
                                isSubmittingBiz = false
                                if (result.isSuccess) {
                                    authManager.updateUserRole("BUSINESS_OWNER")
                                    Toast.makeText(
                                        context,
                                        if (userBusiness != null)
                                            (if (isSw) "Taarifa za biashara zimesasishwa!" else "Business updated successfully!")
                                        else
                                            (if (isSw) "Biashara imesajiliwa kwenye Firestore!" else "Business registered in Firestore!"),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    currentTab = 0
                                } else {
                                    Toast.makeText(context, "Hitilafu: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        enabled = !isSubmittingBiz,
                        colors = ButtonDefaults.buttonColors(containerColor = SokoNeonViolet),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("submit_onboard_biz_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isSubmittingBiz) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = SokoLightText, strokeWidth = 2.dp)
                        } else {
                            Text(
                                text = if (userBusiness != null)
                                    (if (isSw) "Hifadhi Mabadiliko ya Biashara" else "Update Business in Firestore")
                                else
                                    (if (isSw) "Hifadhi Biashara kwenye Firestore" else "Save Business to Firestore"),
                                color = SokoLightText,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }
    }

    // Edit Listing Dialog
    editingListing?.let { listing ->
        EditListingDialog(
            listing = listing,
            isSw = isSw,
            onDismiss = { editingListing = null },
            onSave = { updatedTitle, updatedPrice, updatedDesc, updatedCity, updatedType, updatedImage ->
                scope.launch {
                    val result = repository.updateListing(
                        listingId = listing.id,
                        title = updatedTitle,
                        priceTzs = updatedPrice,
                        categorySlug = listing.categorySlug,
                        description = updatedDesc,
                        type = updatedType,
                        city = updatedCity,
                        phoneNumber = listing.phoneNumber,
                        whatsappNumber = listing.whatsappNumber,
                        imageUrl = updatedImage
                    )
                    if (result.isSuccess) {
                        Toast.makeText(context, if (isSw) "Tangazo limesasishwa!" else "Listing updated successfully!", Toast.LENGTH_SHORT).show()
                        editingListing = null
                    } else {
                        Toast.makeText(context, "Hitilafu: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    // Delete Confirmation Dialog
    listingToDelete?.let { listing ->
        AlertDialog(
            onDismissRequest = { listingToDelete = null },
            title = { Text(if (isSw) "Kufuta Tangazo" else "Delete Listing", color = SokoLightText, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = if (isSw) "Je, una uhakika unataka kufuta tangazo '${listing.titleSw}'? Halitaonekana tena kwenye SokoTZ."
                    else "Are you sure you want to delete '${listing.titleSw}'? It will no longer appear on SokoTZ.",
                    color = SokoMutedText
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toDeleteId = listing.id
                        listingToDelete = null
                        scope.launch {
                            val res = repository.deleteListing(toDeleteId)
                            if (res.isSuccess) {
                                Toast.makeText(context, if (isSw) "Tangazo limefutwa" else "Listing deleted", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Hitilafu: ${res.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SokoError)
                ) {
                    Text(if (isSw) "Futa Kabisa" else "Delete Permanently", color = SokoLightText, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { listingToDelete = null }) {
                    Text(if (isSw) "Ghairi" else "Cancel", color = SokoMutedText)
                }
            },
            containerColor = SokoCardSurface
        )
    }
}

@Composable
fun EditListingDialog(
    listing: Listing,
    isSw: Boolean,
    onDismiss: () -> Unit,
    onSave: (title: String, price: Long, desc: String, city: String, type: String, image: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var editTitle by remember { mutableStateOf(listing.titleSw) }
    var editPrice by remember { mutableStateOf(listing.priceTzs.toString()) }
    var editCity by remember { mutableStateOf(listing.city) }
    var editType by remember { mutableStateOf(listing.type) }
    var editDescription by remember { mutableStateOf(listing.descriptionSw) }
    var editImageUrl by remember { mutableStateOf(listing.imageUrl) }
    var isProcessingEditImage by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            isProcessingEditImage = true
            scope.launch(Dispatchers.IO) {
                val base64 = ImageUtils.uriToBase64(context, uri)
                withContext(Dispatchers.Main) {
                    isProcessingEditImage = false
                    if (base64 != null) {
                        editImageUrl = base64
                        Toast.makeText(context, if (isSw) "Picha imewekwa!" else "Photo loaded!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, if (isSw) "Hitilafu ya kusoma picha!" else "Could not load image!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            isProcessingEditImage = true
            scope.launch(Dispatchers.IO) {
                val base64 = ImageUtils.bitmapToBase64(bitmap)
                withContext(Dispatchers.Main) {
                    isProcessingEditImage = false
                    editImageUrl = base64
                    Toast.makeText(context, if (isSw) "Picha imepigwa!" else "Photo captured!", Toast.LENGTH_SHORT).show()
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
                if (isSw) "Ruhusa ya kamera inahitajika kupiga picha." else "Camera permission is required to capture photo.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isSw) "Hariri Tangazo Lako" else "Edit Your Listing",
                color = SokoLightText,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isProcessingEditImage) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SokoMidnightBlue)
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = SokoNeonBlue, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isSw) "Inachakata picha mpya..." else "Processing new photo...",
                            style = MaterialTheme.typography.bodySmall,
                            color = SokoNeonBlue
                        )
                    }
                }

                // Photo preview & upload in edit mode
                if (editImageUrl.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, SokoNeonBlue, RoundedCornerShape(8.dp))
                    ) {
                        SokoListingImage(
                            imageUrl = editImageUrl,
                            contentDescription = "Edit photo",
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        IconButton(
                            onClick = { editImageUrl = "" },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(SokoBlueBlack.copy(alpha = 0.8f))
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = SokoError, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SokoNeonBlue),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = SokoNeonBlue, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isSw) "Badilisha Picha" else "Pick Photo", color = SokoNeonBlue, fontSize = 10.sp)
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
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SokoNeonViolet),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = SokoNeonViolet, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isSw) "Piga Picha" else "Camera", color = SokoNeonViolet, fontSize = 10.sp)
                    }
                }

                OutlinedTextField(
                    value = editTitle,
                    onValueChange = { editTitle = it },
                    label = { Text(if (isSw) "Jina la Bidhaa" else "Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = editPrice,
                    onValueChange = { editPrice = it.filter { c -> c.isDigit() } },
                    label = { Text(if (isSw) "Bei (TZS)" else "Price (TZS)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = editCity,
                    onValueChange = { editCity = it },
                    label = { Text(if (isSw) "Mji / Mkoa" else "City") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = editDescription,
                    onValueChange = { editDescription = it },
                    label = { Text(if (isSw) "Maelezo" else "Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val priceLong = editPrice.toLongOrNull() ?: listing.priceTzs
                    isSaving = true
                    onSave(editTitle, priceLong, editDescription, editCity, editType, editImageUrl)
                },
                enabled = !isSaving && editTitle.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = SokoNeonBlue)
            ) {
                Text(if (isSw) "Hifadhi Mabadiliko" else "Save Changes", color = SokoBlueBlack, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isSw) "Ghairi" else "Cancel", color = SokoMutedText)
            }
        },
        containerColor = SokoCardSurface
    )
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, SokoColors.cardBorder, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = SokoColors.cardSurface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.bodySmall, color = SokoColors.textMuted)
                Icon(icon, contentDescription = title, tint = iconColor, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black), color = SokoColors.textPrimary)
        }
    }
}
