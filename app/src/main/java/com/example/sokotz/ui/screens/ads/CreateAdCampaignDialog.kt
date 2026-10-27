package com.example.sokotz.ui.screens.ads

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.sokotz.data.model.AdPackage
import com.example.sokotz.data.model.AppLanguage
import com.example.sokotz.data.model.Listing
import com.example.sokotz.data.model.UserProfile
import com.example.sokotz.data.repository.AdCampaignRepository
import com.example.sokotz.data.repository.FirestoreMarketplaceRepository
import com.example.ui.theme.SokoBlueBlack
import com.example.ui.theme.SokoColors
import com.example.ui.theme.SokoDeepNavy
import com.example.ui.theme.SokoGold
import com.example.ui.theme.SokoGreen
import com.example.ui.theme.SokoNeonBlue
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAdCampaignDialog(
    adRepo: AdCampaignRepository,
    marketplaceRepo: FirestoreMarketplaceRepository,
    currentUser: FirebaseUser,
    userProfile: UserProfile?,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onCampaignCreated: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isSw = language == AppLanguage.SW
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Dynamic packages from Firestore
    val packages by adRepo.adPackages.collectAsState()
    val allListings by marketplaceRepo.listings.collectAsState()

    // Filter to only products/services owned by this seller
    val sellerListings = remember(allListings, currentUser.uid) {
        allListings.filter { listing ->
            listing.ownerId == currentUser.uid
        }
    }

    // Wizard Step: 1 = Select Listing, 2 = Video & Copy, 3 = Choose Package, 4 = Payment & Confirmation
    var currentStep by remember { mutableIntStateOf(1) }

    // Form State
    var selectedListing by remember { mutableStateOf<Listing?>(null) }
    var videoUrl by remember { mutableStateOf("") }
    var adTitle by remember { mutableStateOf("") }
    var adDescription by remember { mutableStateOf("") }
    var ctaType by remember { mutableStateOf("VIEW_PRODUCT") }
    var selectedPackage by remember { mutableStateOf<AdPackage?>(null) }
    var paymentMethod by remember { mutableStateOf("MPESA") }
    var phoneNumber by remember { mutableStateOf(userProfile?.phoneNumber ?: "") }

    var isSubmitting by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }
    var createdCampaignId by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SokoDeepNavy,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header with Back and Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentStep > 1 && !isSuccess) {
                    IconButton(onClick = { currentStep-- }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }

                Text(
                    text = if (isSw) "Unda Tangazo la Video" else "Create Video Ad",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }

            // Step Progress Indicator
            if (!isSuccess) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    (1..4).forEach { step ->
                        Box(
                            modifier = Modifier
                                .size(width = 40.dp, height = 4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    if (step <= currentStep) SokoNeonBlue else Color.Gray.copy(alpha = 0.3f)
                                )
                        )
                        if (step < 4) Spacer(modifier = Modifier.width(8.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Step Contents
            when {
                isSuccess -> {
                    // Success View
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = SokoGreen,
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (isSw) "Malipo Yamethibitishwa!" else "Payment Verified!",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isSw)
                                "Kampeni yako imetumwa na inasubiri ukaguzi wa admin. Itaanza kuonekana kwenye Ads Feed mara moja itakapoidhinishwa."
                            else
                                "Your campaign is now submitted for admin review. It will start streaming in the Ads Feed once approved.",
                            color = Color.LightGray,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                onCampaignCreated()
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SokoNeonBlue),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Text(
                                text = if (isSw) "Angalia Kampeni Zangu" else "View My Campaigns",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                currentStep == 1 -> {
                    // Step 1: Select Existing Owned Product/Service
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isSw) "Hatua ya 1: Chagua Bidhaa au Huduma" else "Step 1: Choose Product or Service",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = if (isSw)
                                "Tangazo lazima liunganishwe na bidhaa/huduma iliyosajiliwa kwenye akaunti yako."
                            else
                                "Every ad must be linked to an approved listing owned by your account.",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                        )

                        if (sellerListings.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isSw)
                                        "Huna bidhaa au huduma iliyosajiliwa bado. Tafadhali ongeza bidhaa kwanza kwenye Biashara yako."
                                    else
                                        "No listings found. Please add a product or service first in your Business hub.",
                                    color = Color.LightGray,
                                    fontSize = 13.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(sellerListings, key = { it.id }) { listing ->
                                    val isSelected = selectedListing?.id == listing.id
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) SokoNeonBlue.copy(alpha = 0.15f) else SokoBlueBlack
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isSelected) SokoNeonBlue else Color.Gray.copy(alpha = 0.3f)
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedListing = listing }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (listing.imageUrl.isNotBlank()) {
                                                AsyncImage(
                                                    model = listing.imageUrl,
                                                    contentDescription = listing.localizedTitle(language),
                                                    modifier = Modifier
                                                        .size(50.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .size(50.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(Color.Gray.copy(alpha = 0.3f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Campaign,
                                                        contentDescription = null,
                                                        tint = Color.White
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = listing.localizedTitle(language),
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                                val priceFormatted = java.text.NumberFormat.getNumberInstance(java.util.Locale.US).format(listing.priceTzs)
                                                Text(
                                                    text = "TSh $priceFormatted",
                                                    color = SokoNeonBlue,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 12.sp
                                                )
                                            }

                                            RadioButton(
                                                selected = isSelected,
                                                onClick = { selectedListing = listing },
                                                colors = RadioButtonDefaults.colors(
                                                    selectedColor = SokoNeonBlue,
                                                    unselectedColor = Color.Gray
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = {
                                selectedListing?.let {
                                    if (adTitle.isBlank()) adTitle = it.localizedTitle(language)
                                    if (adDescription.isBlank()) adDescription = it.localizedDesc(language)
                                    ctaType = if (it.type == "SERVICE") "VIEW_SERVICE" else "VIEW_PRODUCT"
                                }
                                currentStep = 2
                            },
                            enabled = selectedListing != null,
                            colors = ButtonDefaults.buttonColors(containerColor = SokoNeonBlue),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Text(
                                text = if (isSw) "Endelea na Video" else "Continue to Video",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                currentStep == 2 -> {
                    // Step 2: Video URL & Copy
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = if (isSw) "Hatua ya 2: Video na Maelezo" else "Step 2: Video & Ad Details",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )

                        // Video URL
                        OutlinedTextField(
                            value = videoUrl,
                            onValueChange = { videoUrl = it },
                            label = { Text(if (isSw) "Kiungo cha Video (MP4/HLS)" else "Video URL (MP4/HLS)") },
                            placeholder = { Text("https://example.com/video.mp4") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = SokoNeonBlue,
                                unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_video_url")
                        )

                        Text(
                            text = if (isSw)
                                "Kidokezo: Video ya wima (9:16) huvutia wateja zaidi kwenye Ads Feed."
                            else
                                "Tip: Vertical 9:16 aspect ratio video yields maximum engagement in the feed.",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )

                        // Ad Title
                        OutlinedTextField(
                            value = adTitle,
                            onValueChange = { adTitle = it },
                            label = { Text(if (isSw) "Kichwa cha Tangazo" else "Ad Headline") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = SokoNeonBlue,
                                unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Ad Description
                        OutlinedTextField(
                            value = adDescription,
                            onValueChange = { adDescription = it },
                            label = { Text(if (isSw) "Maelezo ya Tangazo" else "Ad Description") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = SokoNeonBlue,
                                unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            maxLines = 4,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Call to Action
                        Text(
                            text = if (isSw) "Kitufe cha Kuchukua Hatua (CTA):" else "Call-To-Action (CTA) Button:",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )

                        val ctaOptions = listOf(
                            "VIEW_PRODUCT" to (if (isSw) "Angalia Bidhaa" else "View Product"),
                            "VIEW_SERVICE" to (if (isSw) "Angalia Huduma" else "View Service"),
                            "CONTACT_SELLER" to (if (isSw) "Wasiliana na Muuzaji" else "Contact Seller"),
                            "BUY_NOW" to (if (isSw) "Nunua Sasa" else "Buy Now")
                        )

                        ctaOptions.forEach { (key, label) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { ctaType = key }
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(
                                    selected = ctaType == key,
                                    onClick = { ctaType = key },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = SokoNeonBlue,
                                        unselectedColor = Color.Gray
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = label, color = Color.White, fontSize = 13.sp)
                            }
                        }

                        Button(
                            onClick = {
                                if (videoUrl.isBlank()) {
                                    // If seller doesn't have a direct video link, use sample demo video
                                    videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"
                                }
                                currentStep = 3
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SokoNeonBlue),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Text(
                                text = if (isSw) "Chagua Kifurushi cha Bei" else "Choose Ad Package",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                currentStep == 3 -> {
                    // Step 3: Choose Dynamic Duration Package from Firestore
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isSw) "Hatua ya 3: Chagua Kifurushi cha Matangazo" else "Step 3: Choose Advertising Package",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = if (isSw)
                                "Bei hizi zinasimamiwa na sokoTZ moja kwa moja kulingana na muda unaotaka tangazo lionekane."
                            else
                                "Official published rates for your campaign duration.",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                        )

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(packages, key = { it.id }) { pkg ->
                                val isSelected = selectedPackage?.id == pkg.id
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) SokoNeonBlue.copy(alpha = 0.15f) else SokoBlueBlack
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.5.dp,
                                        if (isSelected) SokoNeonBlue else Color.Gray.copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedPackage = pkg }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = pkg.localizedName(language),
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            )
                                            Text(
                                                text = "${pkg.durationDays} ${if (isSw) "siku za maonyesho" else "days of continuous feed"}",
                                                color = Color.LightGray,
                                                fontSize = 12.sp
                                            )
                                            if (pkg.localizedDesc(language).isNotBlank()) {
                                                Text(
                                                    text = pkg.localizedDesc(language),
                                                    color = Color.Gray,
                                                    fontSize = 11.sp,
                                                    modifier = Modifier.padding(top = 4.dp)
                                                )
                                            }
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "TSh ${pkg.formattedPrice}",
                                                color = SokoNeonBlue,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 16.sp
                                            )
                                            RadioButton(
                                                selected = isSelected,
                                                onClick = { selectedPackage = pkg },
                                                colors = RadioButtonDefaults.colors(
                                                    selectedColor = SokoNeonBlue,
                                                    unselectedColor = Color.Gray
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = { currentStep = 4 },
                            enabled = selectedPackage != null,
                            colors = ButtonDefaults.buttonColors(containerColor = SokoNeonBlue),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Text(
                                text = if (isSw) "Endelea na Malipo" else "Proceed to Payment",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                currentStep == 4 -> {
                    // Step 4: Tanzanian Mobile Money Payment
                    val pkg = selectedPackage!!
                    val listing = selectedListing!!

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = if (isSw) "Hatua ya 4: Lipia Tangazo Lako" else "Step 4: Payment Verification",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )

                        // Order Summary Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SokoBlueBlack),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = if (isSw) "Muhtasari wa Tangazo" else "Campaign Summary",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = listing.localizedTitle(language), color = Color.LightGray, fontSize = 12.sp)
                                    Text(text = pkg.localizedName(language), color = Color.White, fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = if (isSw) "Muda" else "Duration", color = Color.LightGray, fontSize = 12.sp)
                                    Text(text = "${pkg.durationDays} ${if (isSw) "Siku" else "Days"}", color = Color.White, fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(Color.Gray.copy(alpha = 0.3f))
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (isSw) "Jumla ya Malipo:" else "Total Amount:",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "TSh ${pkg.formattedPrice}",
                                        color = SokoNeonBlue,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }

                        // Mobile Payment Methods (Tanzania)
                        Text(
                            text = if (isSw) "Chagua Njia ya Malipo ya Simu:" else "Select Mobile Money Provider:",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )

                        val providers = listOf(
                            "MPESA" to "Vodacom M-Pesa",
                            "AIRTEL_MONEY" to "Airtel Money",
                            "MIXX_YAS" to "Mixx by Yas (Tigo)",
                            "HALOPESA" to "HaloPesa"
                        )

                        providers.forEach { (key, name) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { paymentMethod = key }
                                    .background(
                                        if (paymentMethod == key) SokoNeonBlue.copy(alpha = 0.1f) else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                RadioButton(
                                    selected = paymentMethod == key,
                                    onClick = { paymentMethod = key },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = SokoNeonBlue,
                                        unselectedColor = Color.Gray
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Filled.PhoneAndroid,
                                    contentDescription = null,
                                    tint = SokoNeonBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = name, color = Color.White, fontSize = 13.sp)
                            }
                        }

                        // Phone Number
                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { phoneNumber = it },
                            label = { Text(if (isSw) "Nambari ya Simu ya Malipo" else "Payment Phone Number") },
                            placeholder = { Text("07XXXXXXXX") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = SokoNeonBlue,
                                unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = if (isSw)
                                "Mfumo utathibitisha malipo mara moja na kuwasilisha tangazo kwenye foleni ya ukaguzi ya admin."
                            else
                                "Payment will be verified and submitted securely to the admin review queue.",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )

                        // Submit Button
                        Button(
                            onClick = {
                                if (phoneNumber.isBlank()) {
                                    Toast.makeText(context, if (isSw) "Weka nambari ya simu" else "Enter phone number", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                isSubmitting = true
                                scope.launch {
                                    // 1. Create draft campaign
                                    val createResult = adRepo.createCampaign(
                                        sellerId = currentUser.uid,
                                        sellerName = currentUser.displayName ?: userProfile?.displayName ?: "Muuzaji",
                                        businessId = listing.businessId,
                                        businessName = listing.businessName,
                                        listing = listing,
                                        adTitle = adTitle.ifBlank { listing.localizedTitle(language) },
                                        adDescription = adDescription.ifBlank { listing.localizedDesc(language) },
                                        videoUrl = videoUrl,
                                        selectedPackage = pkg,
                                        callToAction = ctaType
                                    )

                                    if (createResult.isSuccess) {
                                        val campaignId = createResult.getOrThrow()
                                        createdCampaignId = campaignId

                                        // 2. Process simulated mobile payment and submit for review
                                        val payResult = adRepo.processPaymentAndSubmitForReview(
                                            campaignId = campaignId,
                                            sellerId = currentUser.uid,
                                            provider = paymentMethod,
                                            phoneNumber = phoneNumber
                                        )

                                        isSubmitting = false
                                        if (payResult.isSuccess) {
                                            isSuccess = true
                                        } else {
                                            Toast.makeText(context, "Payment verification error: ${payResult.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                        }
                                    } else {
                                        isSubmitting = false
                                        Toast.makeText(context, "Campaign creation error: ${createResult.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            enabled = !isSubmitting,
                            colors = ButtonDefaults.buttonColors(containerColor = SokoNeonBlue),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_submit_ad_payment")
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black)
                            } else {
                                Icon(imageVector = Icons.Filled.Payment, contentDescription = null, tint = Color.Black)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isSw) "Kamilisha Malipo na Tuma" else "Complete Payment & Submit",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
