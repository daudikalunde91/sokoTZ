package com.example.sokotz.ui.screens.listing

import android.content.Intent
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sokotz.data.model.AppLanguage
import com.example.sokotz.data.repository.FirestoreMarketplaceRepository
import com.example.sokotz.ui.components.ContactActionRow
import com.example.sokotz.ui.components.SokoListingImage
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
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ListingDetailScreen(
    listingId: String,
    repository: FirestoreMarketplaceRepository,
    onBack: () -> Unit,
    onNavigateToBusiness: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val language by repository.language.collectAsState()
    val isSw = language == AppLanguage.SW

    val listing = repository.getListingById(listingId)

    LaunchedEffect(listing?.id) {
        listing?.id?.let { id ->
            repository.incrementListingView(id)
        }
    }

    if (listing == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(SokoBlueBlack),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isSw) "Tangazo halikupatikana" else "Listing not found",
                color = SokoLightText,
                style = MaterialTheme.typography.titleLarge
            )
        }
        return
    }

    val formattedPrice = NumberFormat.getNumberInstance(Locale.US).format(listing.priceTzs)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SokoBlueBlack)
            .testTag("listing_detail_screen")
    ) {
        // App bar
        Surface(color = SokoMidnightBlue) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("listing_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = SokoLightText
                        )
                    }
                    Text(
                        text = if (isSw) "Maelezo ya Tangazo" else "Listing Details",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = SokoLightText
                    )
                }

                IconButton(onClick = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "${listing.localizedTitle(language)} - TZS $formattedPrice kwenye SokoTZ: https://sokotz.co.tz/listing/${listing.id}"
                        )
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Listing"))
                }) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = SokoNeonBlue
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Optional Product Image
            if (listing.imageUrl.isNotBlank()) {
                SokoListingImage(
                    imageUrl = listing.imageUrl,
                    contentDescription = listing.localizedTitle(language),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    shape = RoundedCornerShape(16.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Type & Location badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (listing.isProduct) SokoMidnightBlue else SokoNeonViolet.copy(alpha = 0.2f))
                        .border(1.dp, if (listing.isProduct) SokoNeonBlue else SokoNeonViolet, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (listing.isProduct) "Bidhaa / Product" else "Huduma / Service",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (listing.isProduct) SokoNeonBlue else SokoNeonViolet
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = "Location", tint = SokoMutedText, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(listing.city, style = MaterialTheme.typography.bodySmall, color = SokoMutedText)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Title
            Text(
                text = listing.localizedTitle(language),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = SokoLightText
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Views Tracker Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(SokoDeepNavy)
                    .border(1.dp, SokoCardBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = "Views",
                    tint = SokoNeonBlue,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isSw)
                        "Watazamaji ${listing.viewsCount} wameona tangazo hili"
                    else
                        "${listing.viewsCount} people have viewed this listing",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = SokoLightText
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Price banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, SokoNeonBlue.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = SokoMidnightBlue)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isSw) "Bei Iliyotangazwa" else "Listed Price",
                            style = MaterialTheme.typography.labelSmall,
                            color = SokoMutedText
                        )
                        Text(
                            text = "TZS $formattedPrice",
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black),
                            color = SokoNeonBlue
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Business link card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, SokoCardBorder, RoundedCornerShape(12.dp))
                    .clickable { onNavigateToBusiness(listing.businessId) }
                    .testTag("seller_business_link_card"),
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
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SokoGradients.AccentGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Store, contentDescription = "Store", tint = SokoBlueBlack)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isSw) "Muuzaji / Mtoa Huduma" else "Seller / Service Provider",
                            style = MaterialTheme.typography.bodySmall,
                            color = SokoMutedText
                        )
                        Text(
                            text = listing.businessName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = SokoLightText
                        )
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = "View", tint = SokoNeonBlue)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Description
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, SokoCardBorder, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = SokoCardSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isSw) "Maelezo Kamili" else "Full Description",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = SokoLightText
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = listing.localizedDesc(language),
                        style = MaterialTheme.typography.bodyLarge,
                        color = SokoLightText,
                        lineHeight = 22.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Fixed Bottom Contact Bar
        Surface(
            color = SokoDeepNavy,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SokoCardBorder, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                ContactActionRow(
                    phoneNumber = listing.phoneNumber,
                    whatsappNumber = listing.whatsappNumber,
                    title = listing.localizedTitle(language)
                )
            }
        }
    }
}
