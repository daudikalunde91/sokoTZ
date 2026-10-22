package com.example.sokotz.ui.screens.assistant

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseUser
import com.example.sokotz.data.model.AppLanguage
import com.example.sokotz.data.model.AssistantMessage
import com.example.sokotz.data.repository.FirestoreMarketplaceRepository
import com.example.sokotz.ui.components.ListingCard
import com.example.ui.theme.SokoColors
import com.example.ui.theme.SokoBlueBlack
import com.example.ui.theme.SokoCardBorder
import com.example.ui.theme.SokoCardSurface
import com.example.ui.theme.SokoDeepNavy
import com.example.ui.theme.SokoDeepPurple
import com.example.ui.theme.SokoGradients
import com.example.ui.theme.SokoLightText
import com.example.ui.theme.SokoMidnightBlue
import com.example.ui.theme.SokoMutedText
import com.example.ui.theme.SokoNeonBlue
import com.example.ui.theme.SokoNeonViolet

@Composable
fun AssistantScreen(
    repository: FirestoreMarketplaceRepository,
    onNavigateToListing: (String) -> Unit,
    currentUser: FirebaseUser? = null,
    onNavigateToSignIn: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val language by repository.language.collectAsState()
    val messages by repository.assistantMessages.collectAsState()
    val isThinking by repository.isAssistantThinking.collectAsState()
    val isSw = language == AppLanguage.SW

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, isThinking) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val suggestedQuestions = if (isSw) {
        listOf(
            "Nipate wauzaji wa vifaa vya solar na umeme",
            "Natafuta mazao na mchele kutoka Mbeya",
            "Nisaidie kutunga maelezo ya kuvutia ya bidhaa yangu",
            "Jinsi ya kuanza na kusajili duka langu hapa SokoTZ"
        )
    } else {
        listOf(
            "Find verified electronics & solar suppliers",
            "Agricultural produce wholesale inquiries",
            "Write a persuasive product description draft",
            "How do I onboard my business profile on SokoTZ?"
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SokoColors.background)
            .imePadding()
            .testTag("assistant_screen")
    ) {
        // Assistant Header
        Surface(color = SokoColors.headerSurface) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SokoGradients.PurpleNeonGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI",
                            tint = SokoLightText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isSw) "sokoTZ Msaidizi" else "sokoTZ AI",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = SokoColors.textPrimary
                        )
                        Text(
                            text = if (isSw) "Msaidizi Mahiri • Mazungumzo ya Moja kwa Moja" else "Intelligent Assistant • Multi-turn Live Chat",
                            style = MaterialTheme.typography.bodySmall,
                            color = SokoNeonBlue
                        )
                    }
                }

                // New conversation action
                IconButton(
                    onClick = { repository.clearAssistantConversation() },
                    modifier = Modifier.testTag("new_conversation_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "New Conversation",
                        tint = SokoMutedText
                    )
                }
            }
        }

        // Grounding & Transparency Disclaimer Notice
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SokoColors.cardSurface)
                .border(1.dp, SokoColors.cardBorder)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Notice",
                    tint = SokoNeonViolet,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isSw)
                        "Msaidizi anatumia teknolojia ya sokoTZ Msaidizi na takwimu halisi za SokoTZ. Daima thibitisha taarifa moja kwa moja na wauzaji."
                    else
                        "Powered by sokoTZ AI and grounded in live SokoTZ marketplace data. Always verify transaction terms directly with sellers.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = SokoColors.textMuted
                )
            }
        }

        // Messages list
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(messages) { message ->
                MessageBubble(
                    message = message,
                    language = language,
                    onNavigateToListing = onNavigateToListing,
                    onCopyText = { text ->
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("SokoTZ Assistant", text)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, if (isSw) "Imenakiliwa!" else "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // Assistant Thinking Indicator
            if (isThinking) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = SokoNeonViolet,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isSw) "sokoTZ Msaidizi (inafikiria...)" else "sokoTZ AI (thinking...)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                            color = SokoNeonViolet,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Suggested queries chip block
            if (messages.size <= 2 && !isThinking) {
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isSw) "Maswali Yanayopendekezwa:" else "Suggested Questions:",
                        style = MaterialTheme.typography.labelMedium,
                        color = SokoNeonBlue
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        suggestedQuestions.forEach { question ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SokoColors.cardSurface)
                                    .border(1.dp, SokoColors.cardBorder, RoundedCornerShape(10.dp))
                                    .clickable {
                                        repository.sendAssistantMessage(question, scope)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                                    .testTag("suggested_prompt_chip")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = question,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = SokoColors.textPrimary
                                    )
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "Ask",
                                        tint = SokoNeonViolet,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Input bar
        Surface(
            color = SokoColors.navBarSurface,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SokoColors.cardBorder, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
        ) {
            Column {
                if (currentUser == null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SokoColors.surface)
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isSw) "Unatumia kama mteja mgeni." else "Browsing as guest customer.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = SokoColors.textMuted
                        )
                        Text(
                            text = if (isSw) "Ingia hapa" else "Sign In",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                            color = SokoNeonBlue,
                            modifier = Modifier.clickable { onNavigateToSignIn() }
                        )
                    }
                    HorizontalDivider(color = SokoColors.cardBorder, thickness = 1.dp)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                text = if (isSw) "Uliza kwa Kiswahili au Kiingereza..." else "Ask in Kiswahili or English...",
                                color = SokoColors.textMuted,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SokoColors.inputBackground)
                            .testTag("assistant_text_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SokoNeonBlue,
                            unfocusedBorderColor = SokoColors.cardBorder,
                            focusedTextColor = SokoColors.textPrimary,
                            unfocusedTextColor = SokoColors.textPrimary
                        ),
                        maxLines = 3,
                        enabled = !isThinking
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank() && !isThinking) {
                                val textToSend = inputText
                                inputText = ""
                                repository.sendAssistantMessage(textToSend, scope)
                            }
                        },
                        enabled = inputText.isNotBlank() && !isThinking,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (inputText.isNotBlank() && !isThinking) SokoNeonBlue else SokoColors.cardBorder)
                            .testTag("assistant_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (inputText.isNotBlank() && !isThinking) SokoBlueBlack else SokoColors.textMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: AssistantMessage,
    language: AppLanguage,
    onNavigateToListing: (String) -> Unit,
    onCopyText: (String) -> Unit
) {
    val isUser = message.isUser
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // Sender label
        Text(
            text = if (isUser) "Wewe / You" else if (language == AppLanguage.SW) "sokoTZ Msaidizi" else "sokoTZ AI",
            style = MaterialTheme.typography.labelSmall,
            color = if (isUser) SokoNeonBlue else SokoNeonViolet,
            modifier = Modifier.padding(bottom = 4.dp, start = if (isUser) 0.dp else 4.dp, end = if (isUser) 4.dp else 0.dp)
        )

        // Bubble card
        Card(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .border(
                    1.dp,
                    if (isUser) SokoNeonBlue.copy(alpha = 0.5f) else SokoColors.cardBorder,
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                ),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) (if (SokoColors.isDark) SokoDeepPurple.copy(alpha = 0.6f) else androidx.compose.ui.graphics.Color(0xFFEDE9FE)) else SokoColors.cardSurface
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = SokoColors.textPrimary,
                    lineHeight = 22.sp
                )

                // If this is an AI drafting suggestion, provide a prominent copy button
                if (message.isAiDraft) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (SokoColors.isDark) SokoMidnightBlue else androidx.compose.ui.graphics.Color(0xFFF1F5F9))
                            .clickable { onCopyText(message.text) }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = SokoNeonBlue, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (language == AppLanguage.SW) "Nakili Pendekezo Hili" else "Copy This Draft",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = SokoNeonBlue
                        )
                    }
                }
            }
        }

        // Grounded Listing Cards if referenced
        if (message.referencedListings.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier.widthIn(max = 320.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                message.referencedListings.forEach { listing ->
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
