package com.example.sokotz.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sokotz.data.auth.AuthManager
import com.example.sokotz.data.model.AppLanguage
import com.example.sokotz.data.repository.AdCampaignRepository
import com.example.sokotz.data.repository.FirestoreMarketplaceRepository
import com.example.sokotz.ui.components.SokoTzHeader
import com.example.sokotz.ui.screens.account.AccountScreen
import com.example.sokotz.ui.screens.admin.AdminDashboardScreen
import com.example.sokotz.ui.screens.ads.AdsFeedScreen
import com.example.sokotz.ui.screens.ads.SavedAdsScreen
import com.example.sokotz.ui.screens.ads.SellerAdsManagerScreen
import com.example.sokotz.ui.screens.auth.SignInScreen
import com.example.sokotz.ui.screens.assistant.AssistantScreen
import com.example.sokotz.ui.screens.business.BusinessDetailScreen
import com.example.sokotz.ui.screens.business.BusinessHubScreen
import com.example.sokotz.ui.screens.discover.DiscoverScreen
import com.example.sokotz.ui.screens.listing.ListingDetailScreen
import com.example.sokotz.ui.screens.search.SearchScreen
import com.example.ui.theme.SokoBlueBlack
import androidx.activity.compose.BackHandler
import com.example.ui.theme.SokoCardBorder
import com.example.ui.theme.SokoColors
import com.example.ui.theme.SokoDeepNavy
import com.example.ui.theme.SokoLightText
import com.example.ui.theme.SokoMidnightBlue
import com.example.ui.theme.SokoMutedText
import com.example.ui.theme.SokoNeonBlue

sealed class Screen {
    object Discover : Screen()
    data class Search(val query: String? = null, val categorySlug: String? = null) : Screen()
    object AdsFeed : Screen()
    object Assistant : Screen()
    object BusinessHub : Screen()
    object Account : Screen()
    object SignIn : Screen()
    data class BusinessDetail(val businessId: String) : Screen()
    data class ListingDetail(val listingId: String) : Screen()
    object SellerAdsManager : Screen()
    object AdminDashboard : Screen()
    object SavedAds : Screen()
}

enum class BottomNavTab(
    val titleSw: String,
    val titleEn: String,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector,
    val testTag: String
) {
    DISCOVER("Gundua", "Discover", Icons.Filled.Explore, Icons.Outlined.Explore, "nav_tab_discover"),
    SEARCH("Tafuta", "Search", Icons.Filled.Search, Icons.Outlined.Search, "nav_tab_search"),
    ADS("Matangazo", "Ads", Icons.Filled.PlayCircle, Icons.Outlined.PlayCircle, "nav_tab_ads"),
    BUSINESS("Biashara", "Business", Icons.Filled.Storefront, Icons.Outlined.Storefront, "nav_tab_business"),
    ACCOUNT("Akaunti", "Account", Icons.Filled.AccountCircle, Icons.Outlined.AccountCircle, "nav_tab_account")
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun SokoTzApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authManager = remember { AuthManager.getInstance(context) }
    val repository = remember { FirestoreMarketplaceRepository.getInstance(context) }
    val adRepo = remember { AdCampaignRepository.getInstance(context) }

    val language by repository.language.collectAsState()
    val currentUser by authManager.currentUser.collectAsState()
    val userProfile by authManager.userProfile.collectAsState()
    val isSw = language == AppLanguage.SW

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Discover) }
    var selectedTab by remember { mutableStateOf(BottomNavTab.DISCOVER) }

    LaunchedEffect(Unit) {
        authManager.attemptAutoSignIn(scope)
    }

    // Sync personalized name with sokoTZ AI (Requirement 5)
    LaunchedEffect(userProfile, currentUser) {
        val preferred = userProfile?.aiPreferredName?.ifBlank { userProfile?.displayName }
            ?: currentUser?.displayName ?: ""
        if (preferred.isNotBlank()) {
            repository.setAssistantUserName(preferred)
        }
    }

    // When user logs in from SignIn screen, return to Discover
    LaunchedEffect(currentUser) {
        if (currentUser != null && currentScreen is Screen.SignIn) {
            currentScreen = Screen.Discover
            selectedTab = BottomNavTab.DISCOVER
        }
    }

    // Back button handling
    BackHandler(enabled = currentScreen !is Screen.Discover) {
        if (currentScreen is Screen.SellerAdsManager || currentScreen is Screen.AdminDashboard || currentScreen is Screen.SavedAds) {
            currentScreen = Screen.Account
            selectedTab = BottomNavTab.ACCOUNT
        } else {
            currentScreen = Screen.Discover
            selectedTab = BottomNavTab.DISCOVER
        }
    }

    if (currentScreen is Screen.SignIn) {
        SignInScreen(
            authManager = authManager,
            language = language,
            onToggleLanguage = { repository.toggleLanguage() },
            onContinueAsGuest = {
                currentScreen = Screen.Discover
                selectedTab = BottomNavTab.DISCOVER
            }
        )
    } else {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(SokoColors.background),
        topBar = {
            if (currentScreen is Screen.Discover || currentScreen is Screen.Search ||
                currentScreen is Screen.Assistant || currentScreen is Screen.BusinessHub ||
                currentScreen is Screen.Account
            ) {
                SokoTzHeader(
                    currentLanguage = language,
                    onToggleLanguage = { repository.toggleLanguage() }
                )
            }
        },
        bottomBar = {
            if (!WindowInsets.isImeVisible) {
                NavigationBar(
                    containerColor = SokoColors.navBarSurface,
                    contentColor = SokoColors.textPrimary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .border(1.dp, SokoColors.cardBorder, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                    windowInsets = WindowInsets.navigationBars
                ) {
                    BottomNavTab.values().forEach { tab ->
                        val isSelected = selectedTab == tab && (
                                (tab == BottomNavTab.DISCOVER && currentScreen is Screen.Discover) ||
                                (tab == BottomNavTab.SEARCH && currentScreen is Screen.Search) ||
                                (tab == BottomNavTab.ADS && currentScreen is Screen.AdsFeed) ||
                                (tab == BottomNavTab.BUSINESS && currentScreen is Screen.BusinessHub) ||
                                (tab == BottomNavTab.ACCOUNT && currentScreen is Screen.Account)
                        )

                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                selectedTab = tab
                                currentScreen = when (tab) {
                                    BottomNavTab.DISCOVER -> Screen.Discover
                                    BottomNavTab.SEARCH -> Screen.Search()
                                    BottomNavTab.ADS -> Screen.AdsFeed
                                    BottomNavTab.BUSINESS -> Screen.BusinessHub
                                    BottomNavTab.ACCOUNT -> Screen.Account
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) tab.activeIcon else tab.inactiveIcon,
                                    contentDescription = if (isSw) tab.titleSw else tab.titleEn,
                                    tint = if (isSelected) SokoNeonBlue else SokoColors.textMuted,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = if (isSw) tab.titleSw else tab.titleEn,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) SokoNeonBlue else SokoColors.textMuted
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = if (SokoColors.isDark) SokoDeepNavy else androidx.compose.ui.graphics.Color(0xFFE0F2FE),
                                selectedIconColor = SokoNeonBlue,
                                unselectedIconColor = SokoColors.textMuted
                            ),
                            modifier = Modifier.testTag(tab.testTag)
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
            when (val screen = currentScreen) {
                is Screen.Discover -> {
                    DiscoverScreen(
                        repository = repository,
                        onNavigateToSearch = { query, categorySlug ->
                            selectedTab = BottomNavTab.SEARCH
                            currentScreen = Screen.Search(query, categorySlug)
                        },
                        onNavigateToBusiness = { businessId ->
                            currentScreen = Screen.BusinessDetail(businessId)
                        },
                        onNavigateToListing = { listingId ->
                            currentScreen = Screen.ListingDetail(listingId)
                        },
                        onNavigateToAssistant = {
                            currentScreen = Screen.Assistant
                        },
                        onNavigateToBusinessHub = {
                            selectedTab = BottomNavTab.BUSINESS
                            currentScreen = Screen.BusinessHub
                        },
                        currentUser = currentUser,
                        onNavigateToSignIn = {
                            currentScreen = Screen.SignIn
                        }
                    )
                }
                is Screen.Search -> {
                    SearchScreen(
                        repository = repository,
                        initialQuery = screen.query,
                        initialCategory = screen.categorySlug,
                        onNavigateToListing = { listingId ->
                            currentScreen = Screen.ListingDetail(listingId)
                        }
                    )
                }
                is Screen.Assistant -> {
                    AssistantScreen(
                        repository = repository,
                        onNavigateToListing = { listingId ->
                            currentScreen = Screen.ListingDetail(listingId)
                        },
                        currentUser = currentUser,
                        onNavigateToSignIn = {
                            currentScreen = Screen.SignIn
                        }
                    )
                }
                is Screen.AdsFeed -> {
                    AdsFeedScreen(
                        adRepo = adRepo,
                        language = language,
                        currentUser = currentUser,
                        onNavigateToListing = { listingId ->
                            currentScreen = Screen.ListingDetail(listingId)
                        },
                        onNavigateToBusiness = { businessId ->
                            currentScreen = Screen.BusinessDetail(businessId)
                        },
                        onNavigateToCreateCampaign = {
                            currentScreen = Screen.SellerAdsManager
                        },
                        onNavigateToSignIn = {
                            currentScreen = Screen.SignIn
                        }
                    )
                }
                is Screen.BusinessHub -> {
                    BusinessHubScreen(
                        repository = repository,
                        authManager = authManager,
                        onNavigateToListing = { listingId ->
                            currentScreen = Screen.ListingDetail(listingId)
                        },
                        onNavigateToAssistant = {
                            currentScreen = Screen.Assistant
                        },
                        onNavigateToSignIn = {
                            currentScreen = Screen.SignIn
                        },
                        onNavigateToAdsManager = {
                            currentScreen = Screen.SellerAdsManager
                        }
                    )
                }
                is Screen.Account -> {
                    AccountScreen(
                        authManager = authManager,
                        repository = repository,
                        onNavigateToBusinessHub = {
                            selectedTab = BottomNavTab.BUSINESS
                            currentScreen = Screen.BusinessHub
                        },
                        onNavigateToSignIn = {
                            currentScreen = Screen.SignIn
                        },
                        onNavigateToSellerAdsManager = {
                            currentScreen = Screen.SellerAdsManager
                        },
                        onNavigateToSavedAds = {
                            currentScreen = Screen.SavedAds
                        },
                        onNavigateToAdminDashboard = {
                            currentScreen = Screen.AdminDashboard
                        }
                    )
                }
                is Screen.SellerAdsManager -> {
                    SellerAdsManagerScreen(
                        adRepo = adRepo,
                        marketplaceRepo = repository,
                        currentUser = currentUser,
                        userProfile = userProfile,
                        language = language,
                        onBack = {
                            selectedTab = BottomNavTab.ACCOUNT
                            currentScreen = Screen.Account
                        },
                        onNavigateToSignIn = {
                            currentScreen = Screen.SignIn
                        }
                    )
                }
                is Screen.AdminDashboard -> {
                    AdminDashboardScreen(
                        adRepo = adRepo,
                        currentUser = currentUser,
                        userProfile = userProfile,
                        language = language,
                        onBack = {
                            selectedTab = BottomNavTab.ACCOUNT
                            currentScreen = Screen.Account
                        },
                        onNavigateToListing = { listingId ->
                            currentScreen = Screen.ListingDetail(listingId)
                        }
                    )
                }
                is Screen.SavedAds -> {
                    SavedAdsScreen(
                        adRepo = adRepo,
                        currentUser = currentUser,
                        language = language,
                        onBack = {
                            selectedTab = BottomNavTab.ACCOUNT
                            currentScreen = Screen.Account
                        },
                        onNavigateToListing = { listingId ->
                            currentScreen = Screen.ListingDetail(listingId)
                        },
                        onNavigateToSignIn = {
                            currentScreen = Screen.SignIn
                        }
                    )
                }
                is Screen.BusinessDetail -> {
                    BusinessDetailScreen(
                        businessId = screen.businessId,
                        repository = repository,
                        onBack = { currentScreen = Screen.Discover },
                        onNavigateToListing = { listingId ->
                            currentScreen = Screen.ListingDetail(listingId)
                        }
                    )
                }
                is Screen.ListingDetail -> {
                    ListingDetailScreen(
                        listingId = screen.listingId,
                        repository = repository,
                        onBack = { currentScreen = Screen.Discover },
                        onNavigateToBusiness = { businessId ->
                            currentScreen = Screen.BusinessDetail(businessId)
                        }
                    )
                }
                is Screen.SignIn -> {
                    // Handled at top-level; fallback to Discover if reached here
                    currentScreen = Screen.Discover
                }
            }
        }
    }
}
}
