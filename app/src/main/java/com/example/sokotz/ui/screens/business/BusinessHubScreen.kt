package com.example.sokotz.ui.screens.business

import android.widget.Toast
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sokotz.data.auth.AuthManager
import com.example.sokotz.data.model.AppLanguage
import com.example.sokotz.data.remote.GeminiClient
import com.example.sokotz.data.repository.FirestoreMarketplaceRepository
import com.example.sokotz.ui.components.ListingCard
import com.example.ui.theme.SokoBlueBlack
import com.example.ui.theme.SokoCardBorder
import com.example.ui.theme.SokoCardSurface
import com.example.ui.theme.SokoDeepNavy
import com.example.ui.theme.SokoDeepPurple
import com.example.ui.theme.SokoGradients
import com.example.ui.theme.SokoLightText
import com.example.ui.theme.SokoMidnightBlue
import com.example.ui.theme.SokoMutedText
import com.example.ui.theme.SokoNeonBlue
import com.example.ui.theme.SokoNeonViolet
import com.example.ui.theme.SokoSuccess
import kotlinx.coroutines.launch

@Composable
fun BusinessHubScreen(
    repository: FirestoreMarketplaceRepository,
    authManager: AuthManager,
    onNavigateToListing: (String) -> Unit,
    onNavigateToAssistant: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val language by repository.language.collectAsState()
    val currentUser by authManager.currentUser.collectAsState()
    val businesses by repository.businesses.collectAsState()
    val listings by repository.listings.collectAsState()
    val isSw = language == AppLanguage.SW

    var currentTab by remember { mutableStateOf(0) } // 0: Dashboard, 1: Add Listing, 2: Onboard Business

    val userBusiness = businesses.firstOrNull { it.ownerId == (currentUser?.uid ?: "") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SokoBlueBlack)
            .testTag("business_hub_screen")
    ) {
        // Hub Top Bar
        Surface(color = SokoMidnightBlue) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isSw) "Kituo cha Biashara Yako" else "Seller Hub & Management",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = SokoLightText
                        )
                        Text(
                            text = userBusiness?.name ?: if (isSw) "Bado hujasajili biashara" else "No business registered yet",
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
                    containerColor = SokoDeepNavy,
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
                        text = { Text(if (isSw) "Sajili Biashara" else "Onboard Biz", fontWeight = FontWeight.Bold, color = if (currentTab == 2) SokoNeonBlue else SokoMutedText) }
                    )
                }
            }
        }

        // Tab Content
        when (currentTab) {
            0 -> {
                // Dashboard Tab
                val businessListings = listings.filter { it.ownerId == (currentUser?.uid ?: "") }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Quick Stats Cards
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatCard(
                                title = if (isSw) "Matangazo Yako Hai" else "Your Active Listings",
                                value = "${businessListings.size}",
                                icon = Icons.Default.Inventory2,
                                iconColor = SokoNeonBlue,
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                title = if (isSw) "Biashara Yako" else "Business Status",
                                value = if (userBusiness != null) (if (isSw) "Ipo Hai" else "Active") else (if (isSw) "Haijasajiliwa" else "None"),
                                icon = Icons.Default.Business,
                                iconColor = SokoNeonViolet,
                                modifier = Modifier.weight(1f)
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
                                        text = if (isSw) "Msaidizi wa Gemini wa Kuandika Matangazo" else "Gemini AI Copywriter Assistant",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = SokoLightText
                                    )
                                    Text(
                                        text = if (isSw) "Bofya hapa kuongea na AI ikusaidie kutunga maelezo ya mauzo" else "Chat with Gemini to draft sales copy and descriptions",
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
                                colors = CardDefaults.cardColors(containerColor = SokoCardSurface)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = if (isSw) "Bado hujachapisha tangazo lolote." else "You haven't published any listings yet.",
                                        color = SokoMutedText,
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
                            ListingCard(
                                listing = listing,
                                language = language,
                                onClick = { onNavigateToListing(listing.id) }
                            )
                        }
                    }
                }
            }

            1 -> {
                // Add New Listing Tab
                var listingTitle by remember { mutableStateOf("") }
                var listingPrice by remember { mutableStateOf("") }
                var listingCategory by remember { mutableStateOf("kilimo") }
                var listingType by remember { mutableStateOf("PRODUCT") }
                var listingCity by remember { mutableStateOf("Dar es Salaam") }
                var listingDescription by remember { mutableStateOf("") }
                var isDraftingAi by remember { mutableStateOf(false) }
                var isPublishing by remember { mutableStateOf(false) }

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

                    // Gemini AI description draft helper trigger
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
                                        Toast.makeText(context, if (isSw) "Gemini imetunga maelezo ya tangazo!" else "Draft generated by Gemini!", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "AI error: ${e.message}", Toast.LENGTH_SHORT).show()
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
                                (if (isSw) "Gemini inatunga maelezo..." else "Gemini is generating copy...")
                            else
                                (if (isSw) "Tunga maelezo kwa Gemini AI" else "Auto-draft description using Gemini AI"),
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
                                    whatsappNumber = userBusiness?.whatsappNumber ?: (currentUser?.phoneNumber ?: "+255")
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
                // Onboard Business Profile Tab
                var bizName by remember { mutableStateOf("") }
                var bizCategory by remember { mutableStateOf("elektroniki") }
                var bizCity by remember { mutableStateOf("Dar es Salaam") }
                var bizDistrict by remember { mutableStateOf("Ilala") }
                var bizPhone by remember { mutableStateOf("+255") }
                var bizWhatsapp by remember { mutableStateOf("+255") }
                var bizDescription by remember { mutableStateOf("") }
                var isSubmittingBiz by remember { mutableStateOf(false) }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = if (isSw) "Sajili Biashara Yako kwenye SokoTZ" else "Register Your Business on SokoTZ",
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
                                val result = repository.registerBusinessProfile(
                                    name = bizName,
                                    categorySlug = bizCategory,
                                    city = bizCity,
                                    district = bizDistrict,
                                    phoneNumber = bizPhone,
                                    whatsappNumber = bizWhatsapp,
                                    description = bizDescription
                                )
                                isSubmittingBiz = false
                                if (result.isSuccess) {
                                    authManager.updateUserRole("BUSINESS_OWNER")
                                    Toast.makeText(context, if (isSw) "Biashara imesajiliwa kwenye Firestore!" else "Business registered in Firestore!", Toast.LENGTH_SHORT).show()
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
                                text = if (isSw) "Hifadhi Biashara kwenye Firestore" else "Save Business to Firestore",
                                color = SokoLightText,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }
    }
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
            .border(1.dp, SokoCardBorder, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = SokoCardSurface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.bodySmall, color = SokoMutedText)
                Icon(icon, contentDescription = title, tint = iconColor, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black), color = SokoLightText)
        }
    }
}
