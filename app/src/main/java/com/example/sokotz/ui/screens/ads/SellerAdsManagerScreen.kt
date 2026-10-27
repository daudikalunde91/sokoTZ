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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.sokotz.data.model.AdCampaign
import com.example.sokotz.data.model.AdCampaignStatus
import com.example.sokotz.data.model.AppLanguage
import com.example.sokotz.data.model.UserProfile
import com.example.sokotz.data.repository.AdCampaignRepository
import com.example.sokotz.data.repository.FirestoreMarketplaceRepository
import com.example.ui.theme.SokoBlueBlack
import com.example.ui.theme.SokoColors
import com.example.ui.theme.SokoDeepNavy
import com.example.ui.theme.SokoGreen
import com.example.ui.theme.SokoLightText
import com.example.ui.theme.SokoNeonBlue
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

@Composable
fun SellerAdsManagerScreen(
    adRepo: AdCampaignRepository,
    marketplaceRepo: FirestoreMarketplaceRepository,
    currentUser: FirebaseUser?,
    userProfile: UserProfile?,
    language: AppLanguage,
    onBack: () -> Unit,
    onNavigateToSignIn: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isSw = language == AppLanguage.SW

    if (currentUser == null) {
        onNavigateToSignIn()
        return
    }

    val campaigns by adRepo.sellerCampaigns.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showCreateDialog by remember { mutableStateOf(false) }

    // Permanent close dialog state
    var campaignToClose by remember { mutableStateOf<AdCampaign?>(null) }
    var isClosing by remember { mutableStateOf(false) }

    // Reinstatement dialog state
    var campaignToReinstate by remember { mutableStateOf<AdCampaign?>(null) }
    var reinstatementNote by remember { mutableStateOf("") }
    var isReinstating by remember { mutableStateOf(false) }

    val tabs = if (isSw) {
        listOf("Zote", "Zinazoonyeshwa", "Ukaguzi", "Zilizofungwa")
    } else {
        listOf("All", "Active", "In Review", "Closed")
    }

    val filteredCampaigns = remember(campaigns, selectedTab) {
        when (selectedTab) {
            1 -> campaigns.filter { it.status == "ACTIVE" }
            2 -> campaigns.filter { it.status == "AWAITING_REVIEW" || it.status == "AWAITING_PAYMENT" }
            3 -> campaigns.filter { it.status == "PERMANENTLY_CLOSED" || it.status == "ENDED" || it.status == "REJECTED" }
            else -> campaigns
        }
    }

    Scaffold(
        containerColor = SokoColors.background,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SokoDeepNavy)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
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
                    Text(
                        text = if (isSw) "Usimamizi wa Matangazo ya Video" else "Video Ads Manager",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }

                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = SokoDeepNavy,
                    contentColor = SokoNeonBlue,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = SokoNeonBlue
                        )
                    },
                    edgePadding = 16.dp
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 13.sp,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == index) SokoNeonBlue else Color.LightGray
                                )
                            }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = SokoNeonBlue,
                contentColor = Color.Black,
                shape = CircleShape,
                modifier = Modifier.testTag("fab_create_ad_campaign")
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "Create Ad")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(SokoColors.background)
        ) {
            if (filteredCampaigns.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.Campaign,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (isSw) "Hakuna Kampeni zilizopatikana" else "No Campaigns Found",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isSw)
                                "Bofya kitufe cha '+' hapa chini kuanzisha tangazo lako la kwanza la video."
                            else
                                "Tap the '+' button below to launch your first vertical video campaign.",
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
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredCampaigns, key = { it.id }) { campaign ->
                        SellerCampaignCard(
                            campaign = campaign,
                            isSw = isSw,
                            onClosePermanently = { campaignToClose = campaign },
                            onRequestReinstatement = {
                                campaignToReinstate = campaign
                                reinstatementNote = ""
                            }
                        )
                    }
                }
            }
        }
    }

    // Create Campaign Dialog
    if (showCreateDialog) {
        CreateAdCampaignDialog(
            adRepo = adRepo,
            marketplaceRepo = marketplaceRepo,
            currentUser = currentUser,
            userProfile = userProfile,
            language = language,
            onDismiss = { showCreateDialog = false },
            onCampaignCreated = {
                showCreateDialog = false
                selectedTab = 0
            }
        )
    }

    // Permanent Voluntary Closure Dialog
    campaignToClose?.let { campaign ->
        AlertDialog(
            onDismissRequest = { if (!isClosing) campaignToClose = null },
            containerColor = SokoDeepNavy,
            title = {
                Text(
                    text = if (isSw) "Funga Tangazo Kabisa?" else "Permanently Close Campaign?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (isSw)
                        "ONYO: Ukifunga tangazo hili la '${campaign.listingTitle}', halitaweza kuanzishwa au kuendelezwa tena. Takwimu na rekodi za malipo zitahifadhiwa kwenye kumbukumbu zako. Je, una uhakika?"
                    else
                        "WARNING: If you permanently close this campaign for '${campaign.listingTitle}', it cannot be resumed. Historical metrics and financial records will be preserved. Are you sure?",
                    color = Color.LightGray,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        isClosing = true
                        scope.launch {
                            val result = adRepo.permanentlyCloseCampaign(campaign.id)
                            isClosing = false
                            campaignToClose = null
                            if (result.isSuccess) {
                                Toast.makeText(
                                    context,
                                    if (isSw) "Tangazo limefungwa kabisa" else "Campaign permanently closed",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                    enabled = !isClosing,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4D4F))
                ) {
                    if (isClosing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                    } else {
                        Text(
                            text = if (isSw) "Funga Kabisa" else "Close Permanently",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            dismissButton = {
                if (!isClosing) {
                    TextButton(onClick = { campaignToClose = null }) {
                        Text(if (isSw) "Ghairi" else "Cancel", color = Color.LightGray)
                    }
                }
            }
        )
    }

    // Reinstatement Request Dialog
    campaignToReinstate?.let { campaign ->
        AlertDialog(
            onDismissRequest = { if (!isReinstating) campaignToReinstate = null },
            containerColor = SokoDeepNavy,
            title = {
                Text(
                    text = if (isSw) "Omba Kurejesha Tangazo" else "Request Reinstatement",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = if (isSw)
                            "Tangazo hili lilikuwa limesitishwa kwa sababu bidhaa haikupatikana. Je, bidhaa ya '${campaign.listingTitle}' imerudi stoo?"
                        else
                            "This campaign was paused because the item was out of stock. Has '${campaign.listingTitle}' been replenished?",
                        color = Color.LightGray,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = reinstatementNote,
                        onValueChange = { reinstatementNote = it },
                        label = { Text(if (isSw) "Maelezo ya Muuzaji" else "Seller Note") },
                        placeholder = { Text(if (isSw) "Mfano: Bidhaa sasa ipo madukani..." else "E.g. In stock again...") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = SokoNeonBlue,
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                        ),
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isReinstating = true
                        scope.launch {
                            val result = adRepo.requestReinstatement(campaign.id, reinstatementNote)
                            isReinstating = false
                            campaignToReinstate = null
                            if (result.isSuccess) {
                                Toast.makeText(
                                    context,
                                    if (isSw) "Ombi la kurejesha limetumwa kwa admin" else "Reinstatement request submitted to admin",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    },
                    enabled = !isReinstating,
                    colors = ButtonDefaults.buttonColors(containerColor = SokoNeonBlue)
                ) {
                    if (isReinstating) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black)
                    } else {
                        Text(
                            text = if (isSw) "Tuma Ombi" else "Submit Request",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            dismissButton = {
                if (!isReinstating) {
                    TextButton(onClick = { campaignToReinstate = null }) {
                        Text(if (isSw) "Ghairi" else "Cancel", color = Color.LightGray)
                    }
                }
            }
        )
    }
}

@Composable
private fun SellerCampaignCard(
    campaign: AdCampaign,
    isSw: Boolean,
    onClosePermanently: () -> Unit,
    onRequestReinstatement: () -> Unit
) {
    val statusEnum = campaign.statusEnum()

    Card(
        colors = CardDefaults.cardColors(containerColor = SokoBlueBlack),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top Row: Title, Status Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(modifier = Modifier.weight(1f)) {
                    if (campaign.listingImageUrl.isNotBlank()) {
                        AsyncImage(
                            model = campaign.listingImageUrl,
                            contentDescription = campaign.listingTitle,
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = campaign.listingTitle,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1
                        )
                        Text(
                            text = "TSh ${campaign.formattedPrice}",
                            color = SokoNeonBlue,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }

                // Status Badge
                Surface(
                    color = getStatusBgColor(campaign.status),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, getStatusBorderColor(campaign.status))
                ) {
                    Text(
                        text = if (isSw) statusEnum.titleSw else statusEnum.titleEn,
                        color = getStatusTextColor(campaign.status),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Analytics Metrics Grid
            Surface(
                color = SokoDeepNavy,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp, horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    MetricItem(
                        icon = Icons.Filled.Visibility,
                        label = if (isSw) "Maonyesho" else "Views",
                        value = campaign.impressionsCount.toString()
                    )
                    MetricItem(
                        icon = Icons.Filled.TouchApp,
                        label = if (isSw) "Mibofyo" else "Clicks",
                        value = campaign.clicksCount.toString()
                    )
                    MetricItem(
                        icon = Icons.Filled.FavoriteBorder,
                        label = if (isSw) "Likes" else "Likes",
                        value = campaign.likesCount.toString()
                    )
                    MetricItem(
                        icon = Icons.Filled.ChatBubbleOutline,
                        label = if (isSw) "Maoni" else "Comments",
                        value = campaign.commentsCount.toString()
                    )
                }
            }

            // Reason notice if rejected or paused
            if (campaign.status == "REJECTED" && !campaign.rejectionReason.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${if (isSw) "Sababu ya kukataliwa:" else "Rejection reason:"} ${campaign.rejectionReason}",
                    color = Color(0xFFFF4D4F),
                    fontSize = 12.sp
                )
            }

            if (campaign.status == "PAUSED_PRODUCT_UNAVAILABLE") {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isSw)
                        "Tangazo limesitishwa kwa sababu bidhaa haipatikani. Bofya kitufe cha kurejesha bidhaa ikiwa tayari ipo stoo."
                    else
                        "Paused because product became unavailable. Request reinstatement once stock is replenished.",
                    color = Color(0xFFFAAD14),
                    fontSize = 12.sp
                )
            }

            // Action Buttons Row
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reinstatement button
                if (campaign.status == "PAUSED_PRODUCT_UNAVAILABLE") {
                    Button(
                        onClick = onRequestReinstatement,
                        colors = ButtonDefaults.buttonColors(containerColor = SokoNeonBlue),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.Refresh, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isSw) "Omba Kurejesha" else "Request Reinstatement",
                            color = Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                // Permanent Voluntary Closure button
                if (campaign.status != "PERMANENTLY_CLOSED" && campaign.status != "ENDED") {
                    OutlinedButton(
                        onClick = onClosePermanently,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF4D4F)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF4D4F).copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.Block, contentDescription = null, tint = Color(0xFFFF4D4F), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isSw) "Funga Kabisa" else "Close Permanently",
                            color = Color(0xFFFF4D4F),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricItem(icon: ImageVector, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = icon, contentDescription = null, tint = SokoNeonBlue, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text(text = label, color = Color.Gray, fontSize = 10.sp)
    }
}

private fun getStatusBgColor(status: String): Color {
    return when (status) {
        "ACTIVE" -> Color(0xFF00E676).copy(alpha = 0.15f)
        "APPROVED" -> SokoNeonBlue.copy(alpha = 0.15f)
        "AWAITING_REVIEW", "AWAITING_PAYMENT" -> Color(0xFFFAAD14).copy(alpha = 0.15f)
        "PAUSED_PRODUCT_UNAVAILABLE", "PAUSED_POLICY_REVIEW" -> Color(0xFFFF9800).copy(alpha = 0.15f)
        "REJECTED", "PERMANENTLY_CLOSED" -> Color(0xFFFF4D4F).copy(alpha = 0.15f)
        else -> Color.Gray.copy(alpha = 0.15f)
    }
}

private fun getStatusBorderColor(status: String): Color {
    return when (status) {
        "ACTIVE" -> Color(0xFF00E676)
        "APPROVED" -> SokoNeonBlue
        "AWAITING_REVIEW", "AWAITING_PAYMENT" -> Color(0xFFFAAD14)
        "PAUSED_PRODUCT_UNAVAILABLE", "PAUSED_POLICY_REVIEW" -> Color(0xFFFF9800)
        "REJECTED", "PERMANENTLY_CLOSED" -> Color(0xFFFF4D4F)
        else -> Color.Gray
    }
}

private fun getStatusTextColor(status: String): Color {
    return when (status) {
        "ACTIVE" -> Color(0xFF00E676)
        "APPROVED" -> SokoNeonBlue
        "AWAITING_REVIEW", "AWAITING_PAYMENT" -> Color(0xFFFAAD14)
        "PAUSED_PRODUCT_UNAVAILABLE", "PAUSED_POLICY_REVIEW" -> Color(0xFFFF9800)
        "REJECTED", "PERMANENTLY_CLOSED" -> Color(0xFFFF4D4F)
        else -> Color.LightGray
    }
}
