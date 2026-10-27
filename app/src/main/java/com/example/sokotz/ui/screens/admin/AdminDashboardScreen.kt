package com.example.sokotz.ui.screens.admin

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
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.PriceChange
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.sokotz.data.model.AdCampaign
import com.example.sokotz.data.model.AdPackage
import com.example.sokotz.data.model.AdReport
import com.example.sokotz.data.model.AppLanguage
import com.example.sokotz.data.model.PackagePriceAuditRecord
import com.example.sokotz.data.model.UserProfile
import com.example.sokotz.data.repository.AdCampaignRepository
import com.example.ui.theme.SokoBlueBlack
import com.example.ui.theme.SokoColors
import com.example.ui.theme.SokoDeepNavy
import com.example.ui.theme.SokoGold
import com.example.ui.theme.SokoGreen
import com.example.ui.theme.SokoNeonBlue
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

@Composable
fun AdminDashboardScreen(
    adRepo: AdCampaignRepository,
    currentUser: FirebaseUser?,
    userProfile: UserProfile?,
    language: AppLanguage,
    onBack: () -> Unit,
    onNavigateToListing: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isSw = language == AppLanguage.SW

    // Role check: Only ADMIN can access
    val isAdmin = userProfile?.role == "ADMIN"

    if (!isAdmin) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SokoBlueBlack)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.AdminPanelSettings,
                    contentDescription = null,
                    tint = Color(0xFFFF4D4F),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (isSw) "Ufikiaji Umezuiliwa" else "Access Restricted",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isSw)
                        "Ukurasa huu ni kwa ajili ya wasimamizi (Admins) wa sokoTZ pekee."
                    else
                        "This dashboard is strictly reserved for authorized sokoTZ administrators.",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = SokoNeonBlue)
                ) {
                    Text(if (isSw) "Rudi Nyuma" else "Go Back", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
        return
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = if (isSw) {
        listOf("Ukaguzi wa Kampeni", "Vifurushi & Bei", "Ripoti za Watumiaji", "Kurejesha Matangazo")
    } else {
        listOf("Campaign Reviews", "Packages & Pricing", "User Reports", "Reinstatements")
    }

    // Flows
    val allCampaigns by adRepo.adminAllCampaigns.collectAsState()
    val packages by adRepo.allPackagesForAdmin.collectAsState()
    val reports by adRepo.adminPendingReports.collectAsState()
    val audits by adRepo.packagePriceAudits.collectAsState()

    // Dialog states
    var campaignToReject by remember { mutableStateOf<AdCampaign?>(null) }
    var rejectionReason by remember { mutableStateOf("") }
    var isRejecting by remember { mutableStateOf(false) }

    var packageToEdit by remember { mutableStateOf<AdPackage?>(null) }
    var newPriceText by remember { mutableStateOf("") }
    var priceChangeReason by remember { mutableStateOf("") }
    var isSavingPrice by remember { mutableStateOf(false) }

    var showCreatePackageDialog by remember { mutableStateOf(false) }
    var showAuditHistoryDialog by remember { mutableStateOf(false) }

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
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Filled.AdminPanelSettings,
                        contentDescription = null,
                        tint = SokoGold,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isSw) "Dashibodi ya Msimamizi (Admin)" else "Admin Operations Dashboard",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
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
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(SokoColors.background)
        ) {
            when (selectedTab) {
                0 -> {
                    // TAB 0: Campaign Reviews (Awaiting Review)
                    val pendingCampaigns = remember(allCampaigns) {
                        allCampaigns.filter { it.status == "AWAITING_REVIEW" || it.status == "AWAITING_PAYMENT" }
                    }

                    if (pendingCampaigns.isEmpty()) {
                        EmptyAdminState(
                            message = if (isSw) "Hakuna kampeni zinazosubiri ukaguzi kwa sasa." else "No campaigns awaiting review."
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(pendingCampaigns, key = { it.id }) { campaign ->
                                AdminCampaignReviewCard(
                                    campaign = campaign,
                                    isSw = isSw,
                                    onApprove = {
                                        scope.launch {
                                            val result = adRepo.approveCampaign(
                                                campaignId = campaign.id,
                                                adminId = currentUser?.uid ?: "admin"
                                            )
                                            if (result.isSuccess) {
                                                Toast.makeText(
                                                    context,
                                                    if (isSw) "Tangazo limeidhinishwa na kuanza kuonyeshwa!" else "Campaign approved and activated!",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    },
                                    onReject = {
                                        campaignToReject = campaign
                                        rejectionReason = ""
                                    },
                                    onViewListing = { onNavigateToListing(campaign.listingId) }
                                )
                            }
                        }
                    }
                }

                1 -> {
                    // TAB 1: Packages & Dynamic Pricing Manager
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isSw) "Vifurushi vya Matangazo" else "Advertising Packages",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )

                            Row {
                                OutlinedButton(
                                    onClick = { showAuditHistoryDialog = true },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SokoGold),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, SokoGold.copy(alpha = 0.6f)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Filled.History, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isSw) "Ukaguzi wa Bei" else "Audit Trail", fontSize = 11.sp)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = { showCreatePackageDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = SokoNeonBlue),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Filled.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isSw) "Kifurushi Kipya" else "New Package", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(packages, key = { it.id }) { pkg ->
                                AdminPackageCard(
                                    pkg = pkg,
                                    isSw = isSw,
                                    onEditPrice = {
                                        packageToEdit = pkg
                                        newPriceText = pkg.priceTzs.toString()
                                        priceChangeReason = ""
                                    },
                                    onToggleActive = { active ->
                                        scope.launch {
                                            adRepo.togglePackageActive(pkg.id, active, currentUser?.uid ?: "admin")
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                2 -> {
                    // TAB 2: Reported Ads & Comments
                    if (reports.isEmpty()) {
                        EmptyAdminState(
                            message = if (isSw) "Hakuna ripoti mpya za watumiaji." else "No pending user reports."
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(reports, key = { it.id }) { report ->
                                AdminReportCard(
                                    report = report,
                                    isSw = isSw,
                                    onTakeDownAd = {
                                        scope.launch {
                                            adRepo.suspendCampaignForPolicy(
                                                campaignId = report.campaignId,
                                                adminId = currentUser?.uid ?: "admin",
                                                reason = "Policy violation reported: ${report.reason}"
                                            )
                                            adRepo.resolveReport(
                                                reportId = report.id,
                                                adminId = currentUser?.uid ?: "admin",
                                                actionTaken = "SUSPENDED_CAMPAIGN"
                                            )
                                            Toast.makeText(context, if (isSw) "Tangazo limesitishwa" else "Campaign suspended", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onDismissReport = {
                                        scope.launch {
                                            adRepo.resolveReport(
                                                reportId = report.id,
                                                adminId = currentUser?.uid ?: "admin",
                                                actionTaken = "REJECTED_NO_ACTION"
                                            )
                                            Toast.makeText(context, if (isSw) "Ripoti imefutwa" else "Report dismissed", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                3 -> {
                    // TAB 3: Reinstatement Requests
                    val reinstatementRequests = remember(allCampaigns) {
                        allCampaigns.filter { it.reinstatementRequested }
                    }

                    if (reinstatementRequests.isEmpty()) {
                        EmptyAdminState(
                            message = if (isSw) "Hakuna maombi ya kurejesha matangazo." else "No pending reinstatement requests."
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(reinstatementRequests, key = { it.id }) { campaign ->
                                AdminReinstatementCard(
                                    campaign = campaign,
                                    isSw = isSw,
                                    onApprove = {
                                        scope.launch {
                                            adRepo.approveCampaign(campaign.id, currentUser?.uid ?: "admin")
                                            Toast.makeText(context, if (isSw) "Tangazo limerejeshwa!" else "Campaign reinstated!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onReject = {
                                        scope.launch {
                                            adRepo.rejectCampaign(campaign.id, currentUser?.uid ?: "admin", "Reinstatement declined by admin.")
                                            Toast.makeText(context, if (isSw) "Ombi limekataliwa" else "Request declined", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Rejection Dialog
    campaignToReject?.let { campaign ->
        AlertDialog(
            onDismissRequest = { if (!isRejecting) campaignToReject = null },
            containerColor = SokoDeepNavy,
            title = {
                Text(
                    text = if (isSw) "Kataa Tangazo Hili" else "Reject Campaign",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = if (isSw) "Weka sababu rasmi ya kukataa tangazo la '${campaign.listingTitle}':" else "Enter official reason for rejecting '${campaign.listingTitle}':",
                        color = Color.LightGray,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = rejectionReason,
                        onValueChange = { rejectionReason = it },
                        placeholder = { Text(if (isSw) "Mfano: Video haina ubora au bei inachanganya..." else "E.g. Low quality video...") },
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
                        if (rejectionReason.isBlank()) {
                            Toast.makeText(context, if (isSw) "Tafadhali weka sababu" else "Please enter reason", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isRejecting = true
                        scope.launch {
                            val result = adRepo.rejectCampaign(
                                campaignId = campaign.id,
                                adminId = currentUser?.uid ?: "admin",
                                rejectionReason = rejectionReason.trim()
                            )
                            isRejecting = false
                            campaignToReject = null
                            if (result.isSuccess) {
                                Toast.makeText(context, if (isSw) "Tangazo limekataliwa" else "Campaign rejected", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = !isRejecting,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4D4F))
                ) {
                    if (isRejecting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                    } else {
                        Text(if (isSw) "Thibitisha Kukataa" else "Confirm Rejection", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                if (!isRejecting) {
                    TextButton(onClick = { campaignToReject = null }) {
                        Text(if (isSw) "Ghairi" else "Cancel", color = Color.LightGray)
                    }
                }
            }
        )
    }

    // Edit Price Dialog (With Mandatory Reason & Audit Trail)
    packageToEdit?.let { pkg ->
        AlertDialog(
            onDismissRequest = { if (!isSavingPrice) packageToEdit = null },
            containerColor = SokoDeepNavy,
            title = {
                Text(
                    text = if (isSw) "Badili Bei ya ${pkg.nameSw}" else "Edit Price for ${pkg.nameEn}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = if (isSw) "Bei ya sasa: TSh ${pkg.formattedPrice}" else "Current price: TSh ${pkg.formattedPrice}",
                        color = SokoNeonBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newPriceText,
                        onValueChange = { newPriceText = it },
                        label = { Text(if (isSw) "Bei Mpya (TSh)" else "New Price (TZS)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = SokoNeonBlue,
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = priceChangeReason,
                        onValueChange = { priceChangeReason = it },
                        label = { Text(if (isSw) "Sababu ya Mabadiliko (Ukaguzi)" else "Reason for Change (Audit)") },
                        placeholder = { Text(if (isSw) "Mfano: Ofa maalum ya sikukuu..." else "E.g. Holiday promo...") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = SokoNeonBlue,
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newPrice = newPriceText.toLongOrNull()
                        if (newPrice == null || newPrice <= 0) {
                            Toast.makeText(context, if (isSw) "Weka bei halali" else "Enter valid price", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (priceChangeReason.isBlank()) {
                            Toast.makeText(context, if (isSw) "Weka sababu ya ukaguzi" else "Enter audit reason", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isSavingPrice = true
                        scope.launch {
                            val result = adRepo.updateAdPackagePrice(
                                packageId = pkg.id,
                                newPriceTzs = newPrice,
                                adminId = currentUser?.uid ?: "admin",
                                adminName = userProfile?.displayName ?: "Admin",
                                reason = priceChangeReason.trim()
                            )
                            isSavingPrice = false
                            packageToEdit = null
                            if (result.isSuccess) {
                                Toast.makeText(context, if (isSw) "Bei imesasishwa na kurekodiwa!" else "Price updated and audit recorded!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = !isSavingPrice,
                    colors = ButtonDefaults.buttonColors(containerColor = SokoNeonBlue)
                ) {
                    if (isSavingPrice) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black)
                    } else {
                        Text(if (isSw) "Hifadhi Bei" else "Save Price", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                if (!isSavingPrice) {
                    TextButton(onClick = { packageToEdit = null }) {
                        Text(if (isSw) "Ghairi" else "Cancel", color = Color.LightGray)
                    }
                }
            }
        )
    }

    // Create New Package Dialog
    if (showCreatePackageDialog) {
        CreatePackageDialog(
            adRepo = adRepo,
            currentUser = currentUser,
            isSw = isSw,
            onDismiss = { showCreatePackageDialog = false }
        )
    }

    // Audit Trail Dialog
    if (showAuditHistoryDialog) {
        AuditTrailDialog(
            audits = audits,
            isSw = isSw,
            onDismiss = { showAuditHistoryDialog = false }
        )
    }
}

@Composable
private fun AdminCampaignReviewCard(
    campaign: AdCampaign,
    isSw: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onViewListing: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SokoBlueBlack),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
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
                                .size(54.dp)
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
                            text = "Muuzaji: ${campaign.sellerName}",
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Bei ya Bidhaa: TSh ${campaign.formattedPrice}",
                            color = SokoNeonBlue,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }
                }

                Surface(
                    color = Color(0xFFFAAD14).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFAAD14))
                ) {
                    Text(
                        text = if (isSw) "Inasubiri Ukaguzi" else "Awaiting Review",
                        color = Color(0xFFFAAD14),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Campaign details
            Surface(
                color = SokoDeepNavy,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "Tangazo: ${campaign.adTitle}",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                    if (campaign.adDescription.isNotBlank()) {
                        Text(
                            text = campaign.adDescription,
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            maxLines = 3,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Malipo: ${campaign.paymentMethod} • Ref: ${campaign.paymentReference} • ${campaign.packageDurationDays} Siku (TSh ${campaign.packagePriceTzs})",
                        color = SokoGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onViewListing,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SokoNeonBlue),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SokoNeonBlue.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(if (isSw) "Angalia Bidhaa" else "View Listing", fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedButton(
                    onClick = onReject,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF4D4F)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF4D4F).copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(if (isSw) "Kataa" else "Reject", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onApprove,
                    colors = ButtonDefaults.buttonColors(containerColor = SokoGreen),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(if (isSw) "Idhinisha" else "Approve", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AdminPackageCard(
    pkg: AdPackage,
    isSw: Boolean,
    onEditPrice: () -> Unit,
    onToggleActive: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SokoBlueBlack),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = pkg.nameSw,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "(${pkg.durationDays} ${if (isSw) "siku" else "days"})",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "TSh ${pkg.formattedPrice}",
                    color = SokoNeonBlue,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp
                )

                if (pkg.descriptionSw.isNotBlank()) {
                    Text(
                        text = pkg.descriptionSw,
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                IconButton(onClick = onEditPrice) {
                    Icon(imageVector = Icons.Filled.Edit, contentDescription = "Edit Price", tint = SokoNeonBlue)
                }
                Switch(
                    checked = pkg.isActive,
                    onCheckedChange = onToggleActive,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = SokoNeonBlue,
                        checkedTrackColor = SokoDeepNavy
                    )
                )
            }
        }
    }
}

@Composable
private fun AdminReportCard(
    report: AdReport,
    isSw: Boolean,
    onTakeDownAd: () -> Unit,
    onDismissReport: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SokoBlueBlack),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF4D4F).copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${if (isSw) "Aina" else "Type"}: ${report.targetType} • ${report.severity}",
                    color = Color(0xFFFF4D4F),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = report.status,
                    color = Color.LightGray,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Sababu: ${report.reason}",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )

            if (report.details.isNotBlank()) {
                Text(
                    text = "Maelezo: ${report.details}",
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Text(
                text = "Mtoa ripoti: ${report.reporterName} (ID: ${report.reporterUserId})",
                color = Color.Gray,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onDismissReport,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.LightGray),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text(if (isSw) "Puuza Ripoti" else "Dismiss", fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onTakeDownAd,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4D4F)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text(if (isSw) "Sitisha Tangazo" else "Suspend Ad", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AdminReinstatementCard(
    campaign: AdCampaign,
    isSw: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SokoBlueBlack),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SokoNeonBlue.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Ombi la Kurejesha: ${campaign.listingTitle}",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(
                text = "Muuzaji: ${campaign.sellerName}",
                color = Color.LightGray,
                fontSize = 12.sp
            )
            if (!campaign.reinstatementReason.isNullOrBlank()) {
                Text(
                    text = "Maelezo: ${campaign.reinstatementReason}",
                    color = SokoNeonBlue,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onReject,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF4D4F)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text(if (isSw) "Kataa" else "Decline", fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onApprove,
                    colors = ButtonDefaults.buttonColors(containerColor = SokoGreen),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text(if (isSw) "Idhinisha Kurejesha" else "Approve Reinstatement", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CreatePackageDialog(
    adRepo: AdCampaignRepository,
    currentUser: FirebaseUser?,
    isSw: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var nameSw by remember { mutableStateOf("") }
    var nameEn by remember { mutableStateOf("") }
    var durationDaysText by remember { mutableStateOf("1") }
    var priceTzsText by remember { mutableStateOf("2000") }
    var descSw by remember { mutableStateOf("") }
    var isCreating by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SokoDeepNavy,
        title = {
            Text(
                text = if (isSw) "Unda Kifurushi Kipya cha Matangazo" else "Create New Advertising Package",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nameSw,
                    onValueChange = { nameSw = it },
                    label = { Text("Jina (Kiswahili)") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = nameEn,
                    onValueChange = { nameEn = it },
                    label = { Text("Name (English)") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = durationDaysText,
                        onValueChange = { durationDaysText = it },
                        label = { Text(if (isSw) "Siku" else "Days") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = priceTzsText,
                        onValueChange = { priceTzsText = it },
                        label = { Text("Bei (TSh)") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = descSw,
                    onValueChange = { descSw = it },
                    label = { Text(if (isSw) "Maelezo" else "Description") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val days = durationDaysText.toIntOrNull() ?: 1
                    val price = priceTzsText.toLongOrNull() ?: 2000L
                    if (nameSw.isBlank()) {
                        Toast.makeText(context, "Weka jina la kifurushi", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isCreating = true
                    scope.launch {
                        adRepo.createAdPackage(
                            nameSw = nameSw,
                            nameEn = nameEn.ifBlank { nameSw },
                            durationDays = days,
                            priceTzs = price,
                            descSw = descSw,
                            descEn = descSw,
                            adminId = currentUser?.uid ?: "admin",
                            adminName = "Admin"
                        )
                        isCreating = false
                        onDismiss()
                        Toast.makeText(context, "Kifurushi kimeundwa!", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = !isCreating,
                colors = ButtonDefaults.buttonColors(containerColor = SokoNeonBlue)
            ) {
                Text(if (isSw) "Unda Kifurushi" else "Create Package", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isSw) "Ghairi" else "Cancel", color = Color.LightGray)
            }
        }
    )
}

@Composable
private fun AuditTrailDialog(
    audits: List<PackagePriceAuditRecord>,
    isSw: Boolean,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SokoDeepNavy,
        title = {
            Text(
                text = if (isSw) "Kumbukumbu za Ukaguzi wa Bei" else "Package Price Audit Trail",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            if (audits.isEmpty()) {
                Text(
                    text = if (isSw) "Hakuna mabadiliko ya bei yaliyorekodiwa bado." else "No price changes recorded yet.",
                    color = Color.LightGray
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(audits, key = { it.id }) { audit ->
                        Surface(
                            color = SokoBlueBlack,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = audit.packageName,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "TSh ${audit.formattedOldPrice} ➔ TSh ${audit.formattedNewPrice}",
                                    color = SokoNeonBlue,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "Sababu: ${audit.reason}",
                                    color = Color.LightGray,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "Msimamizi: ${audit.adminName} (${audit.adminId})",
                                    color = Color.Gray,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = SokoNeonBlue)
            ) {
                Text(if (isSw) "Funga" else "Close", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun EmptyAdminState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = SokoGreen,
                modifier = Modifier.size(54.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                color = Color.LightGray,
                fontSize = 14.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
