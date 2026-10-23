package com.example.sokotz.ui.screens.assistant

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.sokotz.data.model.AppLanguage
import com.example.sokotz.data.model.AssistantMessage
import com.example.sokotz.data.repository.FirestoreMarketplaceRepository
import com.example.sokotz.data.util.DocumentReader
import com.example.sokotz.data.util.ParsedAttachment
import com.example.sokotz.ui.components.ListingCard
import com.example.ui.theme.SokoBlueBlack
import com.example.ui.theme.SokoColors
import com.example.ui.theme.SokoDeepNavy
import com.example.ui.theme.SokoDeepPurple
import com.example.ui.theme.SokoGradients
import com.example.ui.theme.SokoLightText
import com.example.ui.theme.SokoMidnightBlue
import com.example.ui.theme.SokoMutedText
import com.example.ui.theme.SokoNeonBlue
import com.example.ui.theme.SokoNeonViolet
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch
import java.io.File

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
    var currentAttachment by remember { mutableStateOf<ParsedAttachment?>(null) }
    var attachedUriString by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()

    // Activity Result Launchers for Photos and Documents
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val parsed = DocumentReader.parseAttachment(context, uri)
            if (parsed != null) {
                currentAttachment = parsed
                attachedUriString = uri.toString()
            }
        }
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val parsed = DocumentReader.parseAttachment(context, uri)
            if (parsed != null) {
                currentAttachment = parsed
                attachedUriString = uri.toString()
            }
        }
    }

    // Auto-scroll when new messages arrive, thinking state toggles, or keyboard appears
    val isImeVisible = WindowInsets.isImeVisible
    LaunchedEffect(messages.size, isThinking, isImeVisible) {
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
                    onClick = {
                        currentAttachment = null
                        attachedUriString = null
                        repository.clearAssistantConversation()
                    },
                    modifier = Modifier.testTag("new_conversation_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "New Conversation",
                        tint = SokoColors.textMuted
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
                            .padding(vertical = 6.dp),
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

            // Suggested queries chip block + Ad Poster Generator quick action
            if (messages.size <= 2 && !isThinking) {
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isSw) "Vitendo vya Haraka & Maswali:" else "Quick Actions & Questions:",
                        style = MaterialTheme.typography.labelMedium,
                        color = SokoNeonBlue
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Quick Action: Ad Poster Generator
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(SokoColors.cardSurface)
                                .border(1.dp, SokoNeonBlue.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                                .clickable {
                                    val listings = repository.listings.value
                                    val targetListing = listings.firstOrNull { repository.isOwnerOfListing(it.id) } ?: listings.firstOrNull()
                                    if (targetListing != null) {
                                        scope.launch {
                                            repository.generateAdPoster(context, targetListing, scope)
                                        }
                                    } else {
                                        repository.sendAssistantMessage("Nisaidie kutengeneza bango la tangazo (Ad poster) kwa ajili ya bidhaa yangu.", scope)
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                .testTag("generate_poster_quick_action")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isSw) "Tengeneza Picha/Bango la Tangazo (Ad Poster)" else "Generate Promotional Ad Poster",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = SokoNeonBlue
                                )
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Ad Poster",
                                    tint = SokoNeonBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

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

                // Attachment preview pill if a photo or document was picked
                currentAttachment?.let { attachment ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SokoColors.inputBackground)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (attachment.previewBitmap != null) {
                                Image(
                                    bitmap = attachment.previewBitmap.asImageBitmap(),
                                    contentDescription = "Attached thumbnail",
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(6.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = "Document",
                                    tint = SokoNeonBlue,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = attachment.name,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = SokoColors.textPrimary,
                                    maxLines = 1
                                )
                                Text(
                                    text = if (attachment.base64Data != null)
                                        (if (isSw) "Picha/Hati ipo tayari kusomwa" else "Image/Document ready for analysis")
                                    else
                                        (if (isSw) "Hati ya maandishi" else "Text Document"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SokoColors.textMuted
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                currentAttachment = null
                                attachedUriString = null
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove attachment",
                                tint = SokoColors.textMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    HorizontalDivider(color = SokoColors.cardBorder, thickness = 1.dp)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Photo Upload Button
                    IconButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        enabled = !isThinking,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Upload Photo",
                            tint = SokoNeonBlue
                        )
                    }

                    // Document Upload Button
                    IconButton(
                        onClick = {
                            documentPickerLauncher.launch(
                                arrayOf("application/pdf", "text/plain", "image/*")
                            )
                        },
                        enabled = !isThinking,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = "Upload Document",
                            tint = SokoNeonViolet
                        )
                    }

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                text = if (isSw) "Uliza au ambatanisha picha/hati..." else "Ask or attach photo/document...",
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
                    Spacer(modifier = Modifier.width(6.dp))

                    val canSend = (inputText.isNotBlank() || currentAttachment != null) && !isThinking
                    IconButton(
                        onClick = {
                            if (canSend) {
                                val textToSend = inputText
                                val attachment = currentAttachment
                                val uriString = attachedUriString
                                inputText = ""
                                currentAttachment = null
                                attachedUriString = null

                                repository.sendAssistantMessage(
                                    userText = textToSend,
                                    scope = scope,
                                    attachmentBase64 = attachment?.base64Data,
                                    attachmentMimeType = attachment?.mimeType,
                                    documentText = attachment?.extractedText,
                                    attachmentName = attachment?.name,
                                    attachmentUri = uriString
                                )
                            }
                        },
                        enabled = canSend,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (canSend) SokoNeonBlue else SokoColors.cardBorder)
                            .testTag("assistant_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (canSend) SokoBlueBlack else SokoColors.textMuted
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
    val context = LocalContext.current
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
                // Attached image thumbnail if user uploaded one
                if (!message.attachedImageUri.isNullOrBlank()) {
                    AsyncImage(
                        model = message.attachedImageUri,
                        contentDescription = "Attached Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Attached document indicator chip
                if (!message.attachedDocumentName.isNullOrBlank() && message.attachedImageUri.isNullOrBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SokoColors.inputBackground)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = "Attached Document",
                            tint = SokoNeonBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = message.attachedDocumentName,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = SokoColors.textPrimary,
                            maxLines = 1
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Message Text
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = SokoColors.textPrimary,
                    lineHeight = 22.sp
                )

                // Generated Ad Poster Banner
                if (!message.generatedAdImageUrl.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    AsyncImage(
                        model = File(message.generatedAdImageUrl),
                        contentDescription = "Generated SokoTZ Ad Poster",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, SokoNeonBlue, RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SokoNeonBlue)
                            .clickable {
                                try {
                                    val posterFile = File(message.generatedAdImageUrl)
                                    val contentUri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        posterFile
                                    )
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "image/jpeg"
                                        putExtra(Intent.EXTRA_STREAM, contentUri)
                                        putExtra(Intent.EXTRA_TEXT, "Bango langu jipya kwenye SokoTZ! Nunua na uuze kwa uhakika.")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Shiriki Bango la Tangazo"))
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Hifadhi ya picha ipo kwenye kifaa chako.", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Poster",
                            tint = SokoBlueBlack,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (language == AppLanguage.SW) "Shiriki Bango (WhatsApp / Status)" else "Share Ad Poster",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = SokoBlueBlack
                        )
                    }
                }

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
