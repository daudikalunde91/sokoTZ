package com.example.sokotz.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.sokotz.data.model.Listing
import com.example.ui.theme.SokoBlueBlack
import com.example.ui.theme.SokoCardBorder
import com.example.ui.theme.SokoCardSurface
import com.example.ui.theme.SokoColors
import com.example.ui.theme.SokoDeepNavy
import com.example.ui.theme.SokoElectricBlue
import com.example.ui.theme.SokoLightText
import com.example.ui.theme.SokoMutedText
import com.example.ui.theme.SokoNeonBlue
import com.example.ui.theme.SokoSuccess
import com.example.ui.theme.SokoWarning
import com.example.ui.theme.SokoWhatsApp
import java.text.NumberFormat
import java.util.Locale

@Composable
fun MeetupLocationDialog(
    listing: Listing,
    isSw: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    var isLocating by remember { mutableStateOf(false) }
    var location by remember { mutableStateOf<Location?>(null) }
    var locationError by remember { mutableStateOf<String?>(null) }

    fun fetchCurrentLocation() {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (lm == null) {
            locationError = if (isSw) "Huduma ya eneo haipatikani kwenye simu yako" else "Location service not available"
            return
        }

        isLocating = true
        locationError = null

        try {
            val isGpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            if (!isGpsEnabled && !isNetworkEnabled) {
                isLocating = false
                locationError = if (isSw) "Tafadhali washa GPS/Eneo kwenye mipangilio ya simu yako" else "Please enable GPS/Location in device settings"
                return
            }

            // Attempt best last known location first for immediate responsiveness
            var bestLoc: Location? = null
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            ) {
                val gpsLoc = if (isGpsEnabled) lm.getLastKnownLocation(LocationManager.GPS_PROVIDER) else null
                val netLoc = if (isNetworkEnabled) lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) else null

                bestLoc = when {
                    gpsLoc != null && netLoc != null -> if (gpsLoc.accuracy < netLoc.accuracy) gpsLoc else netLoc
                    gpsLoc != null -> gpsLoc
                    else -> netLoc
                }
            }

            if (bestLoc != null) {
                location = bestLoc
                isLocating = false
            }

            // Also register a single-shot location listener to refine the position
            val provider = if (isGpsEnabled) LocationManager.GPS_PROVIDER else LocationManager.NETWORK_PROVIDER
            val listener = object : LocationListener {
                override fun onLocationChanged(loc: Location) {
                    location = loc
                    isLocating = false
                    try {
                        lm.removeUpdates(this)
                    } catch (_: Exception) {}
                }
                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }

            @SuppressLint("MissingPermission")
            lm.requestSingleUpdate(provider, listener, Looper.getMainLooper())

        } catch (e: Exception) {
            isLocating = false
            locationError = e.localizedMessage ?: "Hitilafu ya kupata eneo"
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            hasPermission = true
            fetchCurrentLocation()
        } else {
            hasPermission = false
            locationError = if (isSw)
                "Ruhusa ya eneo (Location) imekataliwa. Ruhusu ili ushiriki eneo lako na muuzaji."
            else
                "Location permission denied. Please allow it to share your location with the seller."
        }
    }

    LaunchedEffect(Unit) {
        if (hasPermission) {
            fetchCurrentLocation()
        }
    }

    val mapsUrl = location?.let { "https://maps.google.com/?q=${it.latitude},${it.longitude}" }
    val formattedPrice = NumberFormat.getNumberInstance(Locale.US).format(listing.priceTzs)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.NearMe,
                    contentDescription = null,
                    tint = SokoNeonBlue,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isSw) "Shiriki Eneo la Kukutana (Live Location)" else "Share Meetup Location",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = SokoColors.textPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("meetup_location_dialog")
            ) {
                Text(
                    text = if (isSw)
                        "Mmekubaliana na muuzaji '${listing.businessName}'? Mpe eneo lako la sasa (GPS) ili muweze kuonana na kukabidhiana '${listing.titleSw}' kwa haraka na usalama."
                    else
                        "Agreed with seller '${listing.businessName}'? Share your current GPS live pin so you can meet up and inspect '${listing.titleEn}' safely.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SokoMutedText
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Location Details Card
                Surface(
                    color = SokoDeepNavy,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SokoCardBorder, RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        if (!hasPermission) {
                            Text(
                                text = if (isSw)
                                    "Ili kupata eneo lako kiotomatiki, sokoTZ inahitaji ruhusa ya GPS/Location ya simu yako."
                                else
                                    "To acquire your coordinates accurately, sokoTZ needs device Location permission.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SokoLightText
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SokoNeonBlue),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("request_location_permission_button")
                            ) {
                                Icon(Icons.Default.MyLocation, contentDescription = null, tint = SokoBlueBlack, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isSw) "Ruhusu na Chukua Eneo Langu" else "Grant & Get Location",
                                    color = SokoBlueBlack,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else if (isLocating) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                            ) {
                                CircularProgressIndicator(color = SokoNeonBlue, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (isSw) "Inatafuta eneo lako halisi (GPS)..." else "Acquiring accurate GPS location...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SokoLightText
                                )
                            }
                        } else if (location != null) {
                            val loc = location!!
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SokoSuccess, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isSw) "Eneo Lako Lipo Tayari" else "Location Acquired",
                                    fontWeight = FontWeight.Bold,
                                    color = SokoSuccess,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Lat: %.5f, Long: %.5f".format(Locale.US, loc.latitude, loc.longitude),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = SokoLightText
                            )
                            if (loc.hasAccuracy()) {
                                Text(
                                    text = if (isSw) "Usahihi wa GPS: takriban +/- %.0fm".format(Locale.US, loc.accuracy) else "Accuracy: +/- %.0fm".format(Locale.US, loc.accuracy),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SokoMutedText
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = mapsUrl ?: "",
                                style = MaterialTheme.typography.labelSmall,
                                color = SokoNeonBlue,
                                maxLines = 1
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("SokoTZ Meetup Location", mapsUrl)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, if (isSw) "Kiungo kimenakiliwa!" else "Link copied!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f).testTag("copy_location_link_button"),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = SokoLightText, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isSw) "Nakili" else "Copy", color = SokoLightText, fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        mapsUrl?.let { url ->
                                            val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                            context.startActivity(mapIntent)
                                        }
                                    },
                                    modifier = Modifier.weight(1f).testTag("view_maps_button"),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Map, contentDescription = null, tint = SokoNeonBlue, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isSw) "Tazama" else "View", color = SokoNeonBlue, fontSize = 12.sp)
                                }
                            }
                        } else {
                            Text(
                                text = locationError ?: if (isSw) "Eneo halikupatikana" else "Could not get location",
                                style = MaterialTheme.typography.bodySmall,
                                color = SokoWarning
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { fetchCurrentLocation() },
                                colors = ButtonDefaults.buttonColors(containerColor = SokoNeonBlue),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(if (isSw) "Jaribu Tena" else "Retry", color = SokoBlueBlack, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Safety Tip Note
                Surface(
                    color = SokoColors.cardSurface,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = SokoWarning,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isSw)
                                "Ushauri wa Usalama: Kwa manufaa yako na ya muuzaji, chagua kukutana maeneo ya wazi yenye watu (madukani, benki, vituo vya mwendokasi au maeneo ya umma)."
                            else
                                "Safety Reminder: Always arrange to meet in open, well-lit public spots like shopping malls, transit hubs, or near established stores.",
                            style = MaterialTheme.typography.labelSmall,
                            color = SokoMutedText,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (location != null && mapsUrl != null) {
                Button(
                    onClick = {
                        val loc = location!!
                        val cleanPhone = listing.whatsappNumber.replace("+", "").replace(" ", "").replace("-", "").let {
                            if (it.startsWith("0")) "255" + it.substring(1) else it
                        }

                        val accuracyText = if (loc.hasAccuracy()) " (Usahihi: takriban +/- %.0fm)".format(Locale.US, loc.accuracy) else ""
                        val text = if (isSw) {
                            "Habari ${listing.businessName}! Nimependa na tumekubaliana kuhusu bidhaa yako '${listing.titleSw}' (TZS $formattedPrice).\n\n" +
                            "Hili ndilo eneo langu nilipo sasa (Live Meetup Location) ili tuonane na kukabidhiana:\n" +
                            "📍 $mapsUrl$accuracyText\n\n" +
                            "Tafadhali nijulishe utakapofika karibu!"
                        } else {
                            "Hello ${listing.businessName}! I am interested in '${listing.titleEn}' (TZS $formattedPrice).\n\n" +
                            "Here is my current live meetup location so we can meet:\n" +
                            "📍 $mapsUrl$accuracyText\n\n" +
                            "Please let me know once you are nearby!"
                        }

                        val uri = Uri.parse("https://wa.me/$cleanPhone?text=${Uri.encode(text)}")
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        try {
                            context.startActivity(intent)
                            onDismiss()
                        } catch (e: Exception) {
                            Toast.makeText(context, "WhatsApp haikupatikana: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SokoWhatsApp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("send_location_whatsapp_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Chat,
                        contentDescription = "WhatsApp",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isSw) "Tuma Eneo kwa WhatsApp" else "Send via WhatsApp",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isSw) "Funga" else "Close", color = SokoMutedText)
            }
        },
        containerColor = SokoCardSurface
    )
}
