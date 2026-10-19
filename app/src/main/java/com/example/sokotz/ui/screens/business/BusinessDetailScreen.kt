package com.example.sokotz.ui.screens.business

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sokotz.data.model.AppLanguage
import com.example.sokotz.data.repository.FirestoreMarketplaceRepository
import com.example.sokotz.ui.components.ContactActionRow
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
fun BusinessDetailScreen(
    businessId: String,
    repository: FirestoreMarketplaceRepository,
    onBack: () -> Unit,
    onNavigateToListing: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val language by repository.language.collectAsState()
    val isSw = language == AppLanguage.SW

    val business = repository.getBusinessById(businessId)
    val listings = repository.getListingsForBusiness(businessId)

    if (business == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(SokoBlueBlack),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isSw) "Biashara haikupatikana" else "Business not found",
                color = SokoLightText,
                style = MaterialTheme.typography.titleLarge
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SokoBlueBlack)
            .testTag("business_detail_screen"),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // App bar
        item {
            Surface(color = SokoMidnightBlue) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = SokoLightText
                        )
                    }
                    Text(
                        text = if (isSw) "Taarifa za Biashara" else "Business Profile",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = SokoLightText
                    )
                }
            }
        }

        // Hero Card
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
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SokoGradients.AccentGradient),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = business.name.take(2).uppercase(),
                                fontWeight = FontWeight.Black,
                                fontSize = 24.sp,
                                color = SokoBlueBlack
                            )
                        }

                        if (business.isVerified) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(SokoNeonBlue.copy(alpha = 0.2f))
                                    .border(1.dp, SokoNeonBlue, RoundedCornerShape(20.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Verified",
                                        tint = SokoNeonBlue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isSw) "Imethibitishwa" else "Verified",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = SokoNeonBlue
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = business.name,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = SokoLightText
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = business.categoryName,
                        style = MaterialTheme.typography.titleMedium,
                        color = SokoNeonViolet
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = "Location", tint = SokoMutedText, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${business.city}, ${business.district}", style = MaterialTheme.typography.bodySmall, color = SokoMutedText)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = "Rating", tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${business.rating} (${business.reviewCount} ${if (isSw) "maoni" else "reviews"})", style = MaterialTheme.typography.bodySmall, color = SokoLightText)
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Contact Action Buttons
                    ContactActionRow(
                        phoneNumber = business.phoneNumber,
                        whatsappNumber = business.whatsappNumber,
                        title = business.name
                    )
                }
            }
        }

        // Details Section
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, SokoCardBorder, RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = SokoCardSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isSw) "Kuhusu Biashara Hii" else "About This Business",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = SokoLightText
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = business.localizedDesc(language),
                        style = MaterialTheme.typography.bodyLarge,
                        color = SokoLightText
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccessTime, contentDescription = "Hours", tint = SokoNeonBlue, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (isSw) "Masaa ya Kazi" else "Operating Hours",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = SokoLightText
                            )
                            Text(business.openingHours, style = MaterialTheme.typography.bodyMedium, color = SokoMutedText)
                        }
                    }

                    if (business.address.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = "Address", tint = SokoNeonViolet, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (isSw) "Anwani Halisi" else "Physical Address",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = SokoLightText
                                )
                                Text(business.address, style = MaterialTheme.typography.bodyMedium, color = SokoMutedText)
                            }
                        }
                    }
                }
            }
        }

        // Business Listings Section
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = if (isSw) "Matangazo ya Biashara Hii (${listings.size})" else "Listings by this Business (${listings.size})",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = SokoLightText
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        if (listings.isEmpty()) {
            item {
                Text(
                    text = if (isSw) "Bado hakuna bidhaa zilizopakiwa na biashara hii." else "No listings uploaded yet by this business.",
                    color = SokoMutedText,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            items(listings) { listing ->
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
