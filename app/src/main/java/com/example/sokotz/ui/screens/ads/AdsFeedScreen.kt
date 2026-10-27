package com.example.sokotz.ui.screens.ads

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.sokotz.data.model.AdCampaign
import com.example.sokotz.data.model.AdComment
import com.example.sokotz.data.model.AppLanguage
import com.example.sokotz.data.repository.AdCampaignRepository
import com.example.sokotz.ui.components.SokoListingImage
import com.example.ui.theme.SokoBlueBlack
import com.example.ui.theme.SokoColors
import com.example.ui.theme.SokoDeepNavy
import com.example.ui.theme.SokoGold
import com.example.ui.theme.SokoGradients
import com.example.ui.theme.SokoLightText
import com.example.ui.theme.SokoMidnightBlue
import com.example.ui.theme.SokoNeonBlue
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdsFeedScreen(
    adRepo: AdCampaignRepository,
    language: AppLanguage,
    currentUser: FirebaseUser?,
    onNavigateToListing: (String) -> Unit,
    onNavigateToBusiness: (String) -> Unit,
    onNavigateToCreateCampaign: () -> Unit,
    onNavigateToSignIn: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isSw = language == AppLanguage.SW

    val rawCampaigns by adRepo.activeAdsFeed.collectAsState()
    val hiddenIds by adRepo.hiddenAdIds.collectAsState()

    // Filter out ads that the user has hidden
    val eligibleAds = remember(rawCampaigns, hiddenIds) {
        rawCampaigns.filter { it.id !in hiddenIds }
    }

    if (eligibleAds.isEmpty()) {
        EmptyAdsFeedView(
            isSw = isSw,
            onCreateAdClicked = {
                if (currentUser == null) {
                    onNavigateToSignIn()
                } else {
                    onNavigateToCreateCampaign()
                }
            }
        )
        return
    }

    val pagerState = rememberPagerState(pageCount = { eligibleAds.size })
    var isMuted by remember { mutableStateOf(true) }

    // ExoPlayer management
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 0f
            playWhenReady = true
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                Lifecycle.Event.ON_RESUME -> {
                    if (exoPlayer.playbackState == Player.STATE_READY) {
                        exoPlayer.play()
                    }
                }
                Lifecycle.Event.ON_DESTROY -> {
                    exoPlayer.release()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
        }
    }

    // Keep volume synced with mute toggle
    LaunchedEffect(isMuted) {
        exoPlayer.volume = if (isMuted) 0f else 1f
    }

    // Modal Bottom Sheet for Comments
    var selectedCommentCampaignId by remember { mutableStateOf<String?>(null) }
    var selectedReportCampaign by remember { mutableStateOf<AdCampaign?>(null) }

    VerticalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("ads_feed_pager")
    ) { page ->
        val campaign = eligibleAds[page]
        val isCurrentPage = pagerState.currentPage == page

        AdFeedPageItem(
            campaign = campaign,
            isCurrentPage = isCurrentPage,
            player = exoPlayer,
            isMuted = isMuted,
            isSw = isSw,
            currentUser = currentUser,
            adRepo = adRepo,
            onToggleMute = { isMuted = !isMuted },
            onOpenListing = {
                adRepo.recordClick(campaign.id)
                onNavigateToListing(campaign.listingId)
            },
            onOpenBusiness = {
                if (campaign.businessId.isNotBlank()) {
                    onNavigateToBusiness(campaign.businessId)
                }
            },
            onOpenComments = {
                selectedCommentCampaignId = campaign.id
            },
            onOpenReport = {
                selectedReportCampaign = campaign
            },
            onHideAd = {
                currentUser?.let { user ->
                    scope.launch {
                        adRepo.hideAd(campaign.id, user.uid)
                    }
                }
            },
            onNavigateToSignIn = onNavigateToSignIn
        )
    }

    // Comments Bottom Sheet
    selectedCommentCampaignId?.let { campaignId ->
        val activeCampaign = eligibleAds.find { it.id == campaignId }
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        AdCommentsBottomSheet(
            campaignId = campaignId,
            campaignSellerId = activeCampaign?.sellerId ?: "",
            adRepo = adRepo,
            currentUser = currentUser,
            isSw = isSw,
            sheetState = sheetState,
            onDismiss = { selectedCommentCampaignId = null },
            onNavigateToSignIn = onNavigateToSignIn
        )
    }

    // Report Dialog
    selectedReportCampaign?.let { campaign ->
        AdReportDialog(
            campaign = campaign,
            adRepo = adRepo,
            currentUser = currentUser,
            isSw = isSw,
            onDismiss = { selectedReportCampaign = null },
            onNavigateToSignIn = onNavigateToSignIn
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun AdFeedPageItem(
    campaign: AdCampaign,
    isCurrentPage: Boolean,
    player: ExoPlayer,
    isMuted: Boolean,
    isSw: Boolean,
    currentUser: FirebaseUser?,
    adRepo: AdCampaignRepository,
    onToggleMute: () -> Unit,
    onOpenListing: () -> Unit,
    onOpenBusiness: () -> Unit,
    onOpenComments: () -> Unit,
    onOpenReport: () -> Unit,
    onHideAd: () -> Unit,
    onNavigateToSignIn: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isPlaying by remember { mutableStateOf(true) }
    var showPauseOverlay by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }

    // Likes state
    var isLiked by remember { mutableStateOf(false) }
    var currentLikesCount by remember { mutableLongStateOf(campaign.likesCount) }

    // Saved state
    val savedAds by adRepo.savedAds.collectAsState()
    val isSaved = remember(savedAds, campaign.id) {
        savedAds.any { it.campaignId == campaign.id }
    }

    // Check liked status on load
    LaunchedEffect(campaign.id, currentUser?.uid) {
        currentLikesCount = campaign.likesCount
        if (currentUser != null) {
            isLiked = adRepo.hasUserLiked(campaign.id, currentUser.uid)
        }
    }

    // Manage mediaItem when current page changes
    LaunchedEffect(isCurrentPage, campaign.videoUrl) {
        if (isCurrentPage) {
            adRepo.recordImpression(campaign.id, currentUser?.uid)

            if (campaign.videoUrl.isNotBlank() && (campaign.videoUrl.startsWith("http") || campaign.videoUrl.startsWith("file") || campaign.videoUrl.startsWith("content"))) {
                try {
                    val mediaItem = MediaItem.fromUri(Uri.parse(campaign.videoUrl))
                    player.setMediaItem(mediaItem)
                    player.prepare()
                    player.playWhenReady = true
                    hasError = false
                    isPlaying = true
                } catch (e: Exception) {
                    hasError = true
                }
            } else {
                hasError = true
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (isCurrentPage && !hasError) {
                    if (player.isPlaying) {
                        player.pause()
                        isPlaying = false
                        showPauseOverlay = true
                    } else {
                        player.play()
                        isPlaying = true
                        showPauseOverlay = false
                    }
                }
            }
    ) {
        // Video Player or Fallback Showcase
        if (isCurrentPage && !hasError && campaign.videoUrl.isNotBlank()) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // High-quality static/animated visual fallback when video is unavailable or loading
            Box(modifier = Modifier.fillMaxSize()) {
                if (campaign.listingImageUrl.isNotBlank()) {
                    AsyncImage(
                        model = campaign.listingImageUrl,
                        contentDescription = campaign.listingTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(SokoMidnightBlue, SokoDeepNavy, Color.Black)
                                )
                            )
                    )
                }
                // Gradient overlay to maintain high video-app feel
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.35f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.85f)
                                )
                            )
                        )
                )
            }
        }

        // Tap-to-play / Pause indicator
        AnimatedVisibility(
            visible = !isPlaying && showPauseOverlay,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                    .border(1.5.dp, Color.White.copy(alpha = 0.7f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        // Top Controls: "Sponsored" Tag & Sound Mute Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Official Sponsored Badge
            Surface(
                color = Color.Black.copy(alpha = 0.65f),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SokoNeonBlue.copy(alpha = 0.7f)),
                modifier = Modifier.shadow(4.dp, RoundedCornerShape(20.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Campaign,
                        contentDescription = "Sponsored",
                        tint = SokoNeonBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isSw) "Tangazo la Kulipiwa" else "Sponsored Ad",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Mute / Unmute Button
            IconButton(
                onClick = onToggleMute,
                modifier = Modifier
                    .size(38.dp)
                    .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                    .testTag("btn_mute_toggle")
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                    contentDescription = if (isMuted) "Unmute" else "Mute",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Right-Side Interactive Actions Column (TikTok-Style)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 90.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Seller Avatar
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(SokoDeepNavy)
                    .border(2.dp, SokoNeonBlue, CircleShape)
                    .clickable { onOpenBusiness() },
                contentAlignment = Alignment.Center
            ) {
                if (campaign.listingImageUrl.isNotBlank()) {
                    AsyncImage(
                        model = campaign.listingImageUrl,
                        contentDescription = campaign.sellerName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = campaign.sellerName.take(1).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }

            // Like Action
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = {
                        if (currentUser == null) {
                            onNavigateToSignIn()
                        } else {
                            scope.launch {
                                val result = adRepo.toggleLike(campaign.id, currentUser.uid)
                                if (result.isSuccess) {
                                    val nowLiked = result.getOrDefault(false)
                                    isLiked = nowLiked
                                    if (nowLiked) currentLikesCount++ else currentLikesCount--
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                        .testTag("btn_ad_like")
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) Color(0xFFFF2A6D) else Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Text(
                    text = formatCount(currentLikesCount),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Comments Action
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onOpenComments,
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                        .testTag("btn_ad_comment")
                ) {
                    Icon(
                        imageVector = Icons.Filled.ChatBubble,
                        contentDescription = "Comment",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = formatCount(campaign.commentsCount),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Save / Bookmark Action
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = {
                        if (currentUser == null) {
                            onNavigateToSignIn()
                        } else {
                            scope.launch {
                                adRepo.toggleSaveAd(campaign, currentUser.uid)
                            }
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                        .testTag("btn_ad_save")
                ) {
                    Icon(
                        imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                        contentDescription = "Save",
                        tint = if (isSaved) SokoGold else Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Text(
                    text = if (isSw) "Hifadhi" else "Save",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Share Action
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = {
                        adRepo.recordShare(campaign.id)
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_SUBJECT,
                                "${campaign.listingTitle} - sokoTZ Ads"
                            )
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Tazama tangazo hili la ${campaign.listingTitle} kwenye sokoTZ!\nMuuzaji: ${campaign.sellerName}\nBei: TSh ${campaign.formattedPrice}\nhttps://sokotz.app/ads/${campaign.id}"
                            )
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Ad via"))
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                        .testTag("btn_ad_share")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = "Share",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = if (isSw) "Shiriki" else "Share",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // More Options (Report / Hide)
            var showMenu by remember { mutableStateOf(false) }
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "More",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(SokoBlueBlack)
                ) {
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.VisibilityOff,
                                    contentDescription = null,
                                    tint = SokoLightText,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isSw) "Ficha Tangazo Hili" else "Hide This Ad",
                                    color = SokoLightText,
                                    fontSize = 13.sp
                                )
                            }
                        },
                        onClick = {
                            showMenu = false
                            if (currentUser == null) {
                                onNavigateToSignIn()
                            } else {
                                onHideAd()
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Flag,
                                    contentDescription = null,
                                    tint = Color(0xFFFF4D4F),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isSw) "Ripoti Tangazo" else "Report Ad",
                                    color = Color(0xFFFF4D4F),
                                    fontSize = 13.sp
                                )
                            }
                        },
                        onClick = {
                            showMenu = false
                            if (currentUser == null) {
                                onNavigateToSignIn()
                            } else {
                                onOpenReport()
                            }
                        }
                    )
                }
            }
        }

        // Bottom Left Content Overlay (Seller Details, Product Info, Verified Price, CTA Button)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.82f)
                .padding(start = 16.dp, bottom = 24.dp)
        ) {
            // Seller Name + Verified badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onOpenBusiness() }
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = "@${campaign.businessName.ifBlank { campaign.sellerName }}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Filled.Verified,
                    contentDescription = "Verified",
                    tint = SokoNeonBlue,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Listing Title
            Text(
                text = campaign.listingTitle,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Campaign Description
            if (campaign.adDescription.isNotBlank()) {
                var isExpanded by remember { mutableStateOf(false) }
                Text(
                    text = campaign.adDescription,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    maxLines = if (isExpanded) 6 else 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .clickable { isExpanded = !isExpanded }
                        .padding(top = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Verified Price Tag
            if (campaign.listingPriceTzs > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            Brush.horizontalGradient(
                                listOf(SokoNeonBlue.copy(alpha = 0.25f), Color.Transparent)
                            ),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "TSh ${campaign.formattedPrice}",
                        color = SokoNeonBlue,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Primary Call-To-Action Button
            Button(
                onClick = onOpenListing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SokoNeonBlue,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(44.dp)
                    .testTag("btn_ad_cta")
            ) {
                Icon(
                    imageVector = Icons.Filled.ShoppingBag,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = getCtaLabel(campaign.callToActionType, campaign.listingType, isSw),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

private fun getCtaLabel(ctaType: String, listingType: String, isSw: Boolean): String {
    return when (ctaType) {
        "VIEW_SERVICE" -> if (isSw) "Angalia Huduma" else "View Service"
        "CONTACT_SELLER" -> if (isSw) "Wasiliana na Muuzaji" else "Contact Seller"
        "BUY_NOW" -> if (isSw) "Nunua Sasa" else "Buy Now"
        else -> {
            if (listingType == "SERVICE") {
                if (isSw) "Angalia Huduma" else "View Service"
            } else {
                if (isSw) "Angalia Bidhaa" else "View Product"
            }
        }
    }
}

private fun formatCount(count: Long): String {
    return when {
        count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
        count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
        else -> count.toString()
    }
}

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdCommentsBottomSheet(
    campaignId: String,
    campaignSellerId: String,
    adRepo: AdCampaignRepository,
    currentUser: FirebaseUser?,
    isSw: Boolean,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onNavigateToSignIn: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var commentText by remember { mutableStateOf("") }
    var isPosting by remember { mutableStateOf(false) }
    var comments by remember { mutableStateOf<List<AdComment>>(emptyList()) }

    DisposableEffect(campaignId) {
        val listener = adRepo.listenToCampaignComments(campaignId) { updatedComments ->
            comments = updatedComments
        }
        onDispose {
            listener.remove()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SokoDeepNavy,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .background(Color.Gray.copy(alpha = 0.5f), CircleShape)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isSw) "Maoni (${comments.size})" else "Comments (${comments.size})",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Comments List
            if (comments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isSw) "Hakuna maoni bado. Kuwa wa kwanza kutoa maoni!" else "No comments yet. Be the first to comment!",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(comments, key = { it.id }) { item ->
                        CommentItemRow(
                            comment = item,
                            isSeller = item.userId == campaignSellerId,
                            isSw = isSw
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Comment Input Box
            if (currentUser == null) {
                Button(
                    onClick = {
                        onDismiss()
                        onNavigateToSignIn()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SokoNeonBlue)
                ) {
                    Text(
                        text = if (isSw) "Ingia ili utoe maoni" else "Sign in to leave a comment",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = {
                            Text(
                                text = if (currentUser.uid == campaignSellerId) {
                                    if (isSw) "Jibu kama muuzaji..." else "Reply as seller..."
                                } else {
                                    if (isSw) "Andika maoni yako..." else "Add a comment..."
                                },
                                fontSize = 13.sp,
                                color = Color.Gray
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SokoNeonBlue,
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_ad_comment")
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (commentText.isNotBlank() && !isPosting) {
                                isPosting = true
                                val isSeller = currentUser.uid == campaignSellerId
                                scope.launch {
                                    adRepo.postComment(
                                        campaignId = campaignId,
                                        userId = currentUser.uid,
                                        userName = currentUser.displayName ?: (if (isSw) "Mtumiaji" else "User"),
                                        userPhotoUrl = currentUser.photoUrl?.toString() ?: "",
                                        text = commentText.trim(),
                                        isSellerReply = isSeller
                                    )
                                    commentText = ""
                                    isPosting = false
                                }
                            }
                        },
                        enabled = commentText.isNotBlank() && !isPosting,
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                if (commentText.isNotBlank()) SokoNeonBlue else Color.Gray.copy(alpha = 0.3f),
                                CircleShape
                            )
                            .testTag("btn_send_comment")
                    ) {
                        if (isPosting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.Black,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if (commentText.isNotBlank()) Color.Black else Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentItemRow(comment: AdComment, isSeller: Boolean, isSw: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // User Avatar
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(if (isSeller) SokoNeonBlue.copy(alpha = 0.2f) else SokoBlueBlack)
                .border(
                    1.dp,
                    if (isSeller) SokoNeonBlue else Color.Gray.copy(alpha = 0.3f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (comment.userPhotoUrl.isNotBlank()) {
                AsyncImage(
                    model = comment.userPhotoUrl,
                    contentDescription = comment.userName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = comment.userName.take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = comment.userName,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )

                if (isSeller || comment.isSellerReply) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = SokoNeonBlue.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, SokoNeonBlue)
                    ) {
                        Text(
                            text = if (isSw) "Muuzaji" else "Seller",
                            color = SokoNeonBlue,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = comment.text,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun AdReportDialog(
    campaign: AdCampaign,
    adRepo: AdCampaignRepository,
    currentUser: FirebaseUser?,
    isSw: Boolean,
    onDismiss: () -> Unit,
    onNavigateToSignIn: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var selectedReason by remember { mutableStateOf("MISLEADING_PRICE") }
    var details by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var isSubmitted by remember { mutableStateOf(false) }

    val reasons = listOf(
        "MISLEADING_PRICE" to (if (isSw) "Bei ya Kudanganya / Haieleweki" else "Misleading or False Price"),
        "FRAUD_SCAM" to (if (isSw) "Utapeli au Udanganyifu" else "Fraud or Scam"),
        "PROHIBITED_ITEM" to (if (isSw) "Bidhaa Iliyopigwa Marufuku" else "Prohibited Product or Service"),
        "SPAM" to (if (isSw) "Ujumbe Usiofaa (Spam)" else "Spam or Unwanted Ads"),
        "OTHER" to (if (isSw) "Sababu Nyingine" else "Other Reason")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SokoDeepNavy,
        title = {
            Text(
                text = if (isSw) "Ripoti Tangazo Hili" else "Report This Ad",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            if (isSubmitted) {
                Text(
                    text = if (isSw) "Asante. Ripoti yako imepokelewa na jopo letu la usimamizi litaikagua mara moja." else "Thank you. Your report has been submitted and will be reviewed promptly by our moderation team.",
                    color = Color.White,
                    fontSize = 13.sp
                )
            } else {
                Column {
                    Text(
                        text = if (isSw) "Chagua sababu ya kuripoti tangazo la ${campaign.listingTitle}:" else "Select why you are reporting ${campaign.listingTitle}:",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    reasons.forEach { (key, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedReason = key }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedReason == key,
                                onClick = { selectedReason = key },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = SokoNeonBlue,
                                    unselectedColor = Color.Gray
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = label,
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = details,
                        onValueChange = { details = it },
                        placeholder = {
                            Text(
                                text = if (isSw) "Maelezo ya ziada (hiari)..." else "Additional details (optional)...",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        },
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
            }
        },
        confirmButton = {
            if (isSubmitted) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = SokoNeonBlue)
                ) {
                    Text(if (isSw) "Sawa" else "OK", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = {
                        if (currentUser == null) {
                            onDismiss()
                            onNavigateToSignIn()
                        } else {
                            isSubmitting = true
                            scope.launch {
                                adRepo.submitReport(
                                    targetType = "AD",
                                    targetId = campaign.id,
                                    campaignId = campaign.id,
                                    reporterUserId = currentUser.uid,
                                    reporterName = currentUser.displayName ?: (if (isSw) "Mtumiaji" else "User"),
                                    reason = selectedReason,
                                    details = details.trim(),
                                    severity = selectedReason
                                )
                                isSubmitting = false
                                isSubmitted = true
                            }
                        }
                    },
                    enabled = !isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4D4F))
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                    } else {
                        Text(if (isSw) "Tuma Ripoti" else "Submit Report", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        dismissButton = {
            if (!isSubmitted) {
                TextButton(onClick = onDismiss) {
                    Text(if (isSw) "Ghairi" else "Cancel", color = Color.LightGray)
                }
            }
        }
    )
}

@Composable
private fun EmptyAdsFeedView(
    isSw: Boolean,
    onCreateAdClicked: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SokoBlueBlack),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(SokoDeepNavy, CircleShape)
                    .border(2.dp, SokoNeonBlue, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Campaign,
                    contentDescription = null,
                    tint = SokoNeonBlue,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (isSw) "Hakuna Matangazo Yanayoendelea Hivi Sasa" else "No Active Ads Right Now",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isSw)
                    "Kuwa wa kwanza kufikia maelfu ya wateja kupitia Ads Feed ya sokoTZ kwa video fupi ya bidhaa au huduma yako!"
                else
                    "Be the first to reach thousands of buyers across Tanzania with full-screen video ads on sokoTZ!",
                color = Color.Gray,
                fontSize = 13.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onCreateAdClicked,
                colors = ButtonDefaults.buttonColors(containerColor = SokoNeonBlue),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(46.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Campaign,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isSw) "Anzisha Tangazo Lako Sasa" else "Launch Your Ad Now",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
