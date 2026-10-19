package com.example.sokotz.ui.screens.search

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import com.example.sokotz.data.model.AppLanguage
import com.example.sokotz.data.repository.FirestoreMarketplaceRepository
import com.example.sokotz.data.repository.MarketplaceData
import com.example.sokotz.ui.components.ListingCard
import com.example.ui.theme.SokoBlueBlack
import com.example.ui.theme.SokoCardBorder
import com.example.ui.theme.SokoDeepNavy
import com.example.ui.theme.SokoLightText
import com.example.ui.theme.SokoMidnightBlue
import com.example.ui.theme.SokoMutedText
import com.example.ui.theme.SokoNeonBlue
import com.example.ui.theme.SokoNeonViolet

@Composable
fun SearchScreen(
    repository: FirestoreMarketplaceRepository,
    initialQuery: String? = null,
    initialCategory: String? = null,
    onNavigateToListing: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val language by repository.language.collectAsState()
    val categories by repository.categories.collectAsState()
    val listings by repository.listings.collectAsState()
    val isSw = language == AppLanguage.SW

    var searchQuery by remember { mutableStateOf(initialQuery ?: "") }
    var selectedCategorySlug by remember { mutableStateOf(initialCategory ?: "all") }
    var selectedCity by remember { mutableStateOf("Tanzania Nzima") }
    var selectedType by remember { mutableStateOf<String?>(null) } // "PRODUCT" or "SERVICE"

    val filteredListings = remember(listings, searchQuery, selectedCategorySlug, selectedCity, selectedType) {
        repository.filterListings(
            query = searchQuery,
            categorySlug = if (selectedCategorySlug == "all") null else selectedCategorySlug,
            city = selectedCity,
            type = selectedType
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SokoBlueBlack)
            .testTag("search_screen")
    ) {
        // Search Input Header
        Surface(color = SokoMidnightBlue, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = if (isSw) "Tafuta bidhaa, huduma, maduka..." else "Search products, services, shops...",
                            color = SokoMutedText
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = SokoNeonBlue)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = SokoMutedText)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SokoDeepNavy)
                        .testTag("search_screen_query_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SokoNeonBlue,
                        unfocusedBorderColor = SokoCardBorder,
                        focusedTextColor = SokoLightText,
                        unfocusedTextColor = SokoLightText
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Type filter buttons: All, Products, Services
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterTypePill(
                        text = if (isSw) "Zote" else "All",
                        isSelected = selectedType == null,
                        onClick = { selectedType = null },
                        modifier = Modifier.weight(1f)
                    )
                    FilterTypePill(
                        text = if (isSw) "Bidhaa" else "Products",
                        isSelected = selectedType == "PRODUCT",
                        onClick = { selectedType = "PRODUCT" },
                        modifier = Modifier.weight(1f)
                    )
                    FilterTypePill(
                        text = if (isSw) "Huduma" else "Services",
                        isSelected = selectedType == "SERVICE",
                        onClick = { selectedType = "SERVICE" },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Categories horizontally scrollable filter
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        text = if (isSw) "Makundi Yote" else "All Categories",
                        isSelected = selectedCategorySlug == "all",
                        onClick = { selectedCategorySlug = "all" }
                    )
                    categories.forEach { category ->
                        FilterChip(
                            text = category.localizedName(language),
                            isSelected = selectedCategorySlug == category.slug,
                            onClick = { selectedCategorySlug = category.slug }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Region filter row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MarketplaceData.regions.forEach { region ->
                        FilterChip(
                            text = region,
                            isSelected = selectedCity == region,
                            onClick = { selectedCity = region }
                        )
                    }
                }
            }
        }

        // Result count header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isSw) "Matokeo yaliyopatikana (${filteredListings.size})" else "Found (${filteredListings.size}) results",
                style = MaterialTheme.typography.titleMedium,
                color = SokoLightText
            )
            if (selectedCategorySlug != "all" || selectedCity != "Tanzania Nzima" || selectedType != null || searchQuery.isNotBlank()) {
                Text(
                    text = if (isSw) "Futa Vichujio" else "Reset Filters",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = SokoNeonViolet,
                    modifier = Modifier.clickable {
                        searchQuery = ""
                        selectedCategorySlug = "all"
                        selectedCity = "Tanzania Nzima"
                        selectedType = null
                    }
                )
            }
        }

        // Results List
        if (filteredListings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.SearchOff,
                        contentDescription = "No results",
                        tint = SokoMutedText,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (isSw) "Hakuna matokeo yanayolingana" else "No matching results found",
                        style = MaterialTheme.typography.titleLarge,
                        color = SokoLightText
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isSw) "Bado hakuna matangazo kwenye kategoria hii au mji huu." else "No listings yet in this category or region.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SokoMutedText
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = {
                            searchQuery = ""
                            selectedCategorySlug = "all"
                            selectedCity = "Tanzania Nzima"
                            selectedType = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SokoNeonBlue)
                    ) {
                        Text(
                            text = if (isSw) "Onyesha Matangazo Yote" else "Show All Listings",
                            color = SokoBlueBlack,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredListings) { listing ->
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
fun FilterTypePill(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) SokoNeonBlue else SokoDeepNavy)
            .border(1.dp, if (isSelected) SokoNeonBlue else SokoCardBorder, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
            color = if (isSelected) SokoBlueBlack else SokoLightText
        )
    }
}

@Composable
fun FilterChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) SokoNeonViolet else SokoDeepNavy)
            .border(1.dp, if (isSelected) SokoNeonViolet else SokoCardBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
            color = if (isSelected) SokoLightText else SokoMutedText
        )
    }
}
