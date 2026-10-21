package com.example.sokotz.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.foundation.Image
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sokotz.data.model.AppLanguage
import com.example.sokotz.data.model.BusinessProfile
import com.example.sokotz.data.model.Listing
import com.example.sokotz.data.model.ListingType
import com.example.sokotz.data.util.ImageUtils
import com.example.ui.theme.SokoBlueBlack
import com.example.ui.theme.SokoCardBorder
import com.example.ui.theme.SokoCardSurface
import com.example.ui.theme.SokoDeepNavy
import com.example.ui.theme.SokoElectricBlue
import com.example.ui.theme.SokoGradients
import com.example.ui.theme.SokoLightText
import com.example.ui.theme.SokoMidnightBlue
import com.example.ui.theme.SokoMutedText
import com.example.ui.theme.SokoNeonBlue
import com.example.ui.theme.SokoNeonViolet
import com.example.ui.theme.SokoPhoneCall
import com.example.ui.theme.SokoSuccess
import com.example.ui.theme.SokoWhatsApp
import java.text.NumberFormat
import java.util.Locale

@Composable
fun SokoTzHeader(
    currentLanguage: AppLanguage,
    onToggleLanguage: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = SokoMidnightBlue,
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SokoGradients.AccentGradient),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "SZ",
                        fontWeight = FontWeight.Black,
                        color = SokoBlueBlack,
                        fontSize = 16.sp
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "SokoTZ",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = SokoLightText
                    )
                    Text(
                        text = if (currentLanguage == AppLanguage.SW) "Soko Lako la Kidijitali" else "Tanzania Marketplace",
                        style = MaterialTheme.typography.bodySmall,
                        color = SokoNeonBlue
                    )
                }
            }

            // Language Toggle
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, SokoCardBorder, RoundedCornerShape(20.dp))
                    .background(SokoDeepNavy)
                    .clickable { onToggleLanguage() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("language_toggle_button"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = "Language",
                    tint = SokoNeonBlue,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = currentLanguage.name,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = SokoLightText
                )
            }
        }
    }
}

@Composable
fun ListingCard(
    listing: Listing,
    language: AppLanguage,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, SokoCardBorder, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .testTag("listing_card_${listing.id}"),
        colors = CardDefaults.cardColors(containerColor = SokoCardSurface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Type Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (listing.isProduct) SokoMidnightBlue else SokoNeonViolet.copy(alpha = 0.2f))
                        .border(1.dp, if (listing.isProduct) SokoNeonBlue.copy(alpha = 0.5f) else SokoNeonViolet.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (listing.isProduct) "Bidhaa / Product" else "Huduma / Service",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (listing.isProduct) SokoNeonBlue else SokoNeonViolet
                    )
                }

                // Views count & City
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "Views",
                        tint = SokoNeonBlue.copy(alpha = 0.8f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${listing.viewsCount}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = SokoNeonBlue.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location",
                        tint = SokoMutedText,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = listing.city,
                        style = MaterialTheme.typography.bodySmall,
                        color = SokoMutedText
                    )
                }
            }

            // Optional product photo display
            if (listing.imageUrl.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                SokoListingImage(
                    imageUrl = listing.imageUrl,
                    contentDescription = listing.localizedTitle(language),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title
            Text(
                text = listing.localizedTitle(language),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = SokoLightText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Business Name
            Text(
                text = listing.businessName,
                style = MaterialTheme.typography.bodyMedium,
                color = SokoMutedText
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Price in TZS
            val formattedPrice = NumberFormat.getNumberInstance(Locale.US).format(listing.priceTzs)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TZS $formattedPrice",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = SokoNeonBlue
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (listing.whatsappNumber.isNotBlank()) {
                        IconButton(
                            onClick = { openWhatsApp(context, listing.whatsappNumber, listing.localizedTitle(language)) },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SokoWhatsApp.copy(alpha = 0.2f))
                                .border(1.dp, SokoWhatsApp.copy(alpha = 0.6f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Chat,
                                contentDescription = "WhatsApp",
                                tint = SokoWhatsApp,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    if (listing.phoneNumber.isNotBlank()) {
                        IconButton(
                            onClick = { openPhoneDialer(context, listing.phoneNumber) },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SokoPhoneCall.copy(alpha = 0.2f))
                                .border(1.dp, SokoPhoneCall.copy(alpha = 0.6f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Call",
                                tint = SokoNeonBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BusinessCard(
    business: BusinessProfile,
    language: AppLanguage,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, SokoCardBorder, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .testTag("business_card_${business.id}"),
        colors = CardDefaults.cardColors(containerColor = SokoCardSurface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = business.name,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = SokoLightText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (business.isVerified) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Verified",
                            tint = SokoNeonBlue,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Rating",
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${business.rating} (${business.reviewCount})",
                        style = MaterialTheme.typography.bodySmall,
                        color = SokoLightText
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${business.categoryName} • ${business.city}, ${business.district}",
                style = MaterialTheme.typography.bodySmall,
                color = SokoNeonViolet
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = business.localizedDesc(language),
                style = MaterialTheme.typography.bodyMedium,
                color = SokoMutedText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ContactActionRow(
    phoneNumber: String,
    whatsappNumber: String,
    title: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (phoneNumber.isNotBlank()) {
            Button(
                onClick = { openPhoneDialer(context, phoneNumber) },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("call_button"),
                colors = ButtonDefaults.buttonColors(containerColor = SokoElectricBlue),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Call, contentDescription = "Call", tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Piga Simu", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        if (whatsappNumber.isNotBlank()) {
            Button(
                onClick = { openWhatsApp(context, whatsappNumber, title) },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("whatsapp_button"),
                colors = ButtonDefaults.buttonColors(containerColor = SokoWhatsApp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.AutoMirrored.Outlined.Chat, contentDescription = "WhatsApp", tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text("WhatsApp", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

fun openPhoneDialer(context: Context, phoneNumber: String) {
    try {
        val cleanPhone = phoneNumber.replace(" ", "").replace("-", "")
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanPhone"))
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Cannot open dialer: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

fun openWhatsApp(context: Context, whatsappNumber: String, itemTitle: String = "") {
    try {
        var cleanPhone = whatsappNumber.replace("+", "").replace(" ", "").replace("-", "")
        if (cleanPhone.startsWith("0")) {
            cleanPhone = "255" + cleanPhone.substring(1)
        }
        val encodedMsg = Uri.encode("Habari! Nimeona tangazo lako SokoTZ: $itemTitle. Ningependa kufahamu zaidi.")
        val uri = Uri.parse("https://wa.me/$cleanPhone?text=$encodedMsg")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Cannot open WhatsApp: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

/**
 * Universal image display component that cleanly and reliably renders Base64 images,
 * Data URIs, local device Uris, and remote URLs with memory caching and zero blank screens.
 */
@Composable
fun SokoListingImage(
    imageUrl: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(10.dp)
) {
    if (imageUrl.isBlank()) return

    val isBase64 = remember(imageUrl) { ImageUtils.isBase64OrDataUri(imageUrl) }
    val bitmap = remember(imageUrl) {
        if (isBase64) ImageUtils.decodeBase64ToBitmap(imageUrl) else null
    }

    Box(
        modifier = modifier
            .clip(shape)
            .border(1.dp, SokoCardBorder, shape)
            .background(SokoMidnightBlue),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            AsyncImage(
                model = imageUrl,
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

