package com.example.sokotz.ui.screens.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sokotz.data.model.AppLanguage
import com.example.sokotz.data.model.Category
import com.example.sokotz.data.repository.FirestoreMarketplaceRepository
import com.example.sokotz.data.repository.MarketplaceData
import com.example.sokotz.ui.components.BusinessCard
import com.example.sokotz.ui.components.ListingCard
import com.example.ui.theme.SokoBlueBlack
import com.example.ui.theme.SokoCardBorder
import com.example.ui.theme.SokoCardSurface
import com.example.ui.theme.SokoDeepNavy
import com.example.ui.theme.SokoGradients
import com.example.ui.theme.SokoLightText
import com.example.ui.theme.SokoMidnightBlue
import com.example.ui.theme.SokoMutedText
import com.example.ui.theme.SokoNeonBlue
import com.example.ui.theme.SokoNeonViolet

@Composable
fun DiscoverScreen(
    repository: FirestoreMarketplaceRepository,
    onNavigateToSearch: (String?, String?) -> Unit,
    onNavigateToBusiness: (String) -> Unit,
    onNavigateToListing: (String) -> Unit,
    onNavigateToAssistant: () -> Unit,
    onNavigateToBusinessHub: () -> Unit,
    modifier: Modifier = Modifier
) {
    val language by repository.language.collectAsState()
    val categories by repository.categories.collectAsState()
    val businesses by repository.businesses.collectAsState()
    val listings by repository.listings.collectAsState()

    var selectedRegion by remember { mutableStateOf("Tanzania Nzima") }
    var searchInput by remember { mutableStateOf("") }

    val isSw = language == AppLanguage.SW

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SokoBlueBlack)
            .testTag("discover_screen"),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Hero Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SokoGradients.HeroGradient)
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isSw) "Karibu SokoTZ" else "Welcome to SokoTZ",
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = SokoLightText
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isSw) "Soko halisi la kidijitali Tanzania" else "Live Tanzania marketplace powered by Firestore",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SokoNeonBlue
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Search input trigger
                    OutlinedTextField(
                        value = searchInput,
                        onValueChange = { searchInput = it },
                        placeholder = {
                            Text(
                                text = if (isSw) "Tafuta bidhaa, huduma, maduka..." else "Search products, services, stores...",
                                color = SokoMutedText,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = SokoNeonBlue)
                        },
                        trailingIcon = {
                            if (searchInput.isNotBlank()) {
                                Button(
                                    onClick = { onNavigateToSearch(searchInput, null) },
                                    colors = ButtonDefaults.buttonColors(containerColor = SokoNeonBlue),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.padding(end = 4.dp)
                                ) {
                                    Text("Go", color = SokoBlueBlack, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SokoDeepNavy)
                            .testTag("home_search_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SokoNeonBlue,
                            unfocusedBorderColor = SokoCardBorder,
                            focusedTextColor = SokoLightText,
                            unfocusedTextColor = SokoLightText
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // SokoTZ Assistant Prompt Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, SokoNeonViolet.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .clickable { onNavigateToAssistant() }
                            .testTag("assistant_banner_card"),
                        colors = CardDefaults.cardColors(containerColor = SokoMidnightBlue)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SokoGradients.PurpleNeonGradient),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = "AI", tint = SokoLightText, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isSw) "sokoTZ Msaidizi" else "sokoTZ AI",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = SokoLightText
                                )
                                Text(
                                    text = if (isSw) "Ongea na sokoTZ Msaidizi kwa Kiswahili au Kiingereza" else "Intelligent sokoTZ AI chat for Tanzanian business discovery",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SokoMutedText
                                )
                            }
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Go", tint = SokoNeonViolet)
                        }
                    }
                }
            }
        }

        // Location Filters
        item {
            Column(modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = "Region", tint = SokoNeonBlue, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isSw) "Chagua Eneo / Mkoa:" else "Filter by Region:",
                        style = MaterialTheme.typography.labelLarge,
                        color = SokoLightText
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MarketplaceData.regions.forEach { region ->
                        val isSelected = region == selectedRegion
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) SokoNeonBlue else SokoDeepNavy)
                                .border(1.dp, if (isSelected) SokoNeonBlue else SokoCardBorder, RoundedCornerShape(20.dp))
                                .clickable { selectedRegion = region }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                .testTag("region_chip_$region")
                        ) {
                            Text(
                                text = region,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                                color = if (isSelected) SokoBlueBlack else SokoLightText
                            )
                        }
                    }
                }
            }
        }

        // Categories section
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isSw) "Makundi ya Biashara" else "Marketplace Categories",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = SokoLightText
                    )
                    Text(
                        text = if (isSw) "Yote (${categories.size})" else "All (${categories.size})",
                        style = MaterialTheme.typography.labelMedium,
                        color = SokoNeonBlue,
                        modifier = Modifier.clickable { onNavigateToSearch(null, null) }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(categories) { category ->
                        CategoryCard(
                            category = category,
                            language = language,
                            onClick = { onNavigateToSearch(null, category.slug) }
                        )
                    }
                }
            }
        }

        // Live Businesses from Firestore
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isSw) "Biashara Zilizosajiliwa (${businesses.size})" else "Registered Businesses (${businesses.size})",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = SokoLightText
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        val filteredBusinesses = businesses.filter {
            selectedRegion == "Tanzania Nzima" || it.city.equals(selectedRegion, ignoreCase = true)
        }

        if (filteredBusinesses.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .border(1.dp, SokoCardBorder, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = SokoCardSurface)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.AddBusiness, contentDescription = "Add", tint = SokoNeonBlue, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isSw) "Bado hakuna biashara iliyosajiliwa hapa." else "No live businesses registered in this area yet.",
                            color = SokoLightText,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isSw) "Kuwa wa kwanza kusajili biashara yako leo bure!" else "Be the first to register your business on SokoTZ!",
                            color = SokoMutedText,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onNavigateToBusinessHub,
                            colors = ButtonDefaults.buttonColors(containerColor = SokoNeonBlue)
                        ) {
                            Text(if (isSw) "Sajili Biashara Yako" else "Register Business", color = SokoBlueBlack, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            items(filteredBusinesses.take(5)) { business ->
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    BusinessCard(
                        business = business,
                        language = language,
                        onClick = { onNavigateToBusiness(business.id) }
                    )
                }
            }
        }

        // Live Listings from Firestore
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isSw) "Bidhaa & Huduma Hai (${listings.size})" else "Active Marketplace Listings (${listings.size})",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = SokoLightText
                    )
                    Text(
                        text = if (isSw) "Tazama Zote" else "View All",
                        style = MaterialTheme.typography.labelMedium,
                        color = SokoNeonBlue,
                        modifier = Modifier.clickable { onNavigateToSearch(null, null) }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        val filteredListings = listings.filter {
            selectedRegion == "Tanzania Nzima" || it.city.equals(selectedRegion, ignoreCase = true)
        }

        if (filteredListings.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .border(1.dp, SokoCardBorder, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = SokoCardSurface)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isSw) "Bado hakuna matangazo yaliyochapishwa." else "No live listings published yet.",
                            color = SokoMutedText,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = onNavigateToBusinessHub,
                            colors = ButtonDefaults.buttonColors(containerColor = SokoNeonViolet)
                        ) {
                            Text(if (isSw) "Chapisha Tangazo la Kwanza" else "Publish First Listing", color = SokoLightText, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            items(filteredListings) { listing ->
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    ListingCard(
                        listing = listing,
                        language = language,
                        onClick = { onNavigateToListing(listing.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryCard(
    category: Category,
    language: AppLanguage,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(135.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, SokoCardBorder, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag("category_card_${category.slug}"),
        colors = CardDefaults.cardColors(containerColor = SokoCardSurface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SokoMidnightBlue),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Category,
                    contentDescription = category.localizedName(language),
                    tint = SokoNeonBlue,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = category.localizedName(language),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = SokoLightText,
                maxLines = 2
            )
        }
    }
}
