package com.example.sokotz.ui.screens.ads

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.sokotz.data.model.AppLanguage
import com.example.sokotz.data.model.SavedAdItem
import com.example.sokotz.data.repository.AdCampaignRepository
import com.example.ui.theme.SokoBlueBlack
import com.example.ui.theme.SokoColors
import com.example.ui.theme.SokoDeepNavy
import com.example.ui.theme.SokoGold
import com.example.ui.theme.SokoNeonBlue
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@Composable
fun SavedAdsScreen(
    adRepo: AdCampaignRepository,
    currentUser: FirebaseUser?,
    language: AppLanguage,
    onBack: () -> Unit,
    onNavigateToListing: (String) -> Unit,
    onNavigateToSignIn: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val isSw = language == AppLanguage.SW

    if (currentUser == null) {
        onNavigateToSignIn()
        return
    }

    val savedAds by adRepo.savedAds.collectAsState()

    Scaffold(
        containerColor = SokoColors.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SokoDeepNavy)
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Filled.Bookmark,
                    contentDescription = null,
                    tint = SokoGold,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isSw) "Matangazo Yaliyohifadhiwa" else "Saved Video Ads",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(SokoColors.background)
        ) {
            if (savedAds.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.Bookmark,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (isSw) "Hujayahifadhi Tangazo Lolote Bado" else "No Saved Ads Yet",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isSw)
                                "Bofya alama ya alama-kitabu (bookmark) kwenye tangazo lolote la video ili kulihifadhi hapa."
                            else
                                "Tap the bookmark icon on any video ad in the Ads feed to view it later here.",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(savedAds, key = { it.id }) { item ->
                        SavedAdCard(
                            item = item,
                            isSw = isSw,
                            onClick = { onNavigateToListing(item.listingId) },
                            onRemove = {
                                scope.launch {
                                    // toggleSaveAd will delete document from saved_ads
                                    val dummyCampaign = com.example.sokotz.data.model.AdCampaign(
                                        id = item.campaignId,
                                        listingId = item.listingId
                                    )
                                    adRepo.toggleSaveAd(dummyCampaign, currentUser.uid)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedAdCard(
    item: SavedAdItem,
    isSw: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SokoBlueBlack),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (item.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.title,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SokoDeepNavy),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Bookmark,
                        contentDescription = null,
                        tint = SokoGold
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1
                )
                Text(
                    text = item.sellerName,
                    color = Color.LightGray,
                    fontSize = 12.sp
                )
                if (item.priceTzs > 0) {
                    val formattedPrice = NumberFormat.getNumberInstance(Locale.US).format(item.priceTzs)
                    Text(
                        text = "TSh $formattedPrice",
                        color = SokoNeonBlue,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                }
            }

            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Remove",
                    tint = Color.Gray
                )
            }
        }
    }
}
