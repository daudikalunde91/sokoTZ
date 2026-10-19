package com.example.sokotz.ui.screens.auth

import android.app.Activity
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sokotz.data.auth.AuthManager
import com.example.sokotz.data.model.AppLanguage
import com.example.ui.theme.SokoBlueBlack
import com.example.ui.theme.SokoCardBorder
import com.example.ui.theme.SokoCardSurface
import com.example.ui.theme.SokoDeepNavy
import com.example.ui.theme.SokoError
import com.example.ui.theme.SokoGradients
import com.example.ui.theme.SokoLightText
import com.example.ui.theme.SokoMidnightBlue
import com.example.ui.theme.SokoMutedText
import com.example.ui.theme.SokoNeonBlue
import com.example.ui.theme.SokoNeonViolet
import com.example.ui.theme.SokoWarning

@Composable
fun SignInScreen(
    authManager: AuthManager,
    language: AppLanguage,
    onToggleLanguage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isLoading by authManager.isLoading.collectAsState()
    val authError by authManager.authError.collectAsState()
    val isSw = language == AppLanguage.SW

    // 0 = Sign In, 1 = Register
    var selectedTab by remember { mutableIntStateOf(0) }

    // Form States
    var registerName by remember { mutableStateOf("") }
    var registerEmail by remember { mutableStateOf("") }
    var registerPhone by remember { mutableStateOf("") }
    var registerPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var registerRole by remember { mutableStateOf("CUSTOMER") }
    var hasAcceptedTerms by remember { mutableStateOf(false) }

    var signInEmail by remember { mutableStateOf("") }
    var signInPassword by remember { mutableStateOf("") }
    var isSignInPasswordVisible by remember { mutableStateOf(false) }

    var validationError by remember { mutableStateOf<String?>(null) }
    var showTermsDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SokoBlueBlack)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 500.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .testTag("signin_screen"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar: Language Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onToggleLanguage,
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SokoNeonBlue.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = SokoMidnightBlue),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "Language",
                        tint = SokoNeonBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isSw) "Kiswahili" else "English",
                        color = SokoLightText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Hero Brand Logo & Title
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(SokoGradients.AccentGradient),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "SZ",
                    fontWeight = FontWeight.Black,
                    fontSize = 28.sp,
                    color = SokoBlueBlack
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "SokoTZ",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                color = SokoLightText
            )

            Text(
                text = if (isSw) "Soko Kuu Mtandaoni Tanzania" else "Tanzania Premier Marketplace",
                style = MaterialTheme.typography.titleSmall,
                color = SokoNeonBlue,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Tab Switcher: Ingia (Sign In) vs Sajili (Register)
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = SokoMidnightBlue,
                contentColor = SokoNeonBlue,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = SokoNeonBlue,
                        height = 3.dp
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, SokoCardBorder, RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        validationError = null
                        authManager.clearError()
                    },
                    text = {
                        Text(
                            text = if (isSw) "Ingia (Sign In)" else "Sign In",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 0) SokoNeonBlue else SokoMutedText,
                            fontSize = 13.sp
                        )
                    },
                    modifier = Modifier.height(48.dp)
                )

                Tab(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        validationError = null
                        authManager.clearError()
                    },
                    text = {
                        Text(
                            text = if (isSw) "Sajili Akaunti" else "Register",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 1) SokoNeonBlue else SokoMutedText,
                            fontSize = 13.sp
                        )
                    },
                    modifier = Modifier.height(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Auth Card Container
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, SokoCardBorder, RoundedCornerShape(18.dp)),
                colors = CardDefaults.cardColors(containerColor = SokoCardSurface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Error message display
                    val currentError = validationError ?: authError
                    if (currentError != null) {
                        Surface(
                            color = SokoError.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = currentError,
                                color = SokoError,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(10.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    if (selectedTab == 0) {
                        // SIGN IN FORM
                        Text(
                            text = if (isSw) "Karibu tena kwenye SokoTZ" else "Welcome back to SokoTZ",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = SokoLightText
                        )

                        OutlinedTextField(
                            value = signInEmail,
                            onValueChange = { signInEmail = it; validationError = null },
                            label = { Text(if (isSw) "Barua Pepe au Simu" else "Email or Phone") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = SokoNeonBlue) },
                            singleLine = true,
                            colors = authTextFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = signInPassword,
                            onValueChange = { signInPassword = it; validationError = null },
                            label = { Text(if (isSw) "Nenosiri" else "Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = SokoNeonBlue) },
                            trailingIcon = {
                                IconButton(onClick = { isSignInPasswordVisible = !isSignInPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isSignInPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle password",
                                        tint = SokoMutedText
                                    )
                                }
                            },
                            visualTransformation = if (isSignInPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            colors = authTextFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Standard sign-in action button
                        Button(
                            onClick = {
                                if (signInEmail.isBlank() || signInPassword.isBlank()) {
                                    validationError = if (isSw) "Tafadhali weka barua pepe na nenosiri" else "Please enter email and password"
                                    return@Button
                                }
                                if (context is Activity) {
                                    authManager.signInWithGoogle(context, scope)
                                }
                            },
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = SokoNeonBlue),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("standard_signin_button")
                        ) {
                            Text(
                                text = if (isSw) "Ingia kwenye Akaunti" else "Sign In to Account",
                                color = SokoBlueBlack,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        // OR DIVIDER
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(modifier = Modifier.weight(1f), color = SokoCardBorder)
                            Text(
                                text = if (isSw) " AU " else " OR ",
                                color = SokoMutedText,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            HorizontalDivider(modifier = Modifier.weight(1f), color = SokoCardBorder)
                        }

                        // Google Sign-In Button
                        GoogleSignInButton(
                            isLoading = isLoading,
                            isSw = isSw,
                            onClick = {
                                if (context is Activity) {
                                    authManager.signInWithGoogle(context, scope)
                                }
                            }
                        )

                    } else {
                        // REGISTRATION FORM
                        Text(
                            text = if (isSw) "Unda Akaunti Mpya ya SokoTZ" else "Create New SokoTZ Account",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = SokoLightText
                        )

                        OutlinedTextField(
                            value = registerName,
                            onValueChange = { registerName = it; validationError = null },
                            label = { Text(if (isSw) "Jina Kamili au la Biashara" else "Full Name or Business Name") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = SokoNeonBlue) },
                            singleLine = true,
                            colors = authTextFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = registerEmail,
                            onValueChange = { registerEmail = it; validationError = null },
                            label = { Text(if (isSw) "Barua Pepe (Email)" else "Email Address") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = SokoNeonBlue) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            colors = authTextFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = registerPhone,
                            onValueChange = { registerPhone = it; validationError = null },
                            label = { Text(if (isSw) "Namba ya Simu / WhatsApp" else "Phone / WhatsApp Number") },
                            placeholder = { Text("+255 7XX XXX XXX", color = SokoMutedText.copy(alpha = 0.5f)) },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = SokoNeonBlue) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            colors = authTextFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = registerPassword,
                            onValueChange = { registerPassword = it; validationError = null },
                            label = { Text(if (isSw) "Nenosiri (angalau tarakimu 6)" else "Password (min 6 characters)") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = SokoNeonBlue) },
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle password",
                                        tint = SokoMutedText
                                    )
                                }
                            },
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            colors = authTextFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Role Selector
                        Text(
                            text = if (isSw) "Aina ya Akaunti:" else "Account Type:",
                            style = MaterialTheme.typography.bodySmall,
                            color = SokoMutedText,
                            modifier = Modifier.align(Alignment.Start)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val isCustomer = registerRole == "CUSTOMER"
                            OutlinedButton(
                                onClick = { registerRole = "CUSTOMER" },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isCustomer) SokoNeonBlue else SokoCardBorder
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isCustomer) SokoDeepNavy else Color.Transparent
                                )
                            ) {
                                Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = if (isCustomer) SokoNeonBlue else SokoMutedText, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isSw) "Mnunuzi" else "Buyer",
                                    color = if (isCustomer) SokoNeonBlue else SokoMutedText,
                                    fontSize = 12.sp,
                                    fontWeight = if (isCustomer) FontWeight.Bold else FontWeight.Normal
                                )
                            }

                            OutlinedButton(
                                onClick = { registerRole = "BUSINESS_OWNER" },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (!isCustomer) SokoNeonViolet else SokoCardBorder
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (!isCustomer) SokoDeepNavy else Color.Transparent
                                )
                            ) {
                                Icon(Icons.Default.Storefront, contentDescription = null, tint = if (!isCustomer) SokoNeonViolet else SokoMutedText, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isSw) "Muuzaji" else "Seller",
                                    color = if (!isCustomer) SokoNeonViolet else SokoMutedText,
                                    fontSize = 12.sp,
                                    fontWeight = if (!isCustomer) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }

                        // TERMS AND CONDITIONS CHECKBOX
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = hasAcceptedTerms,
                                onCheckedChange = { hasAcceptedTerms = it; validationError = null },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = SokoNeonBlue,
                                    uncheckedColor = SokoMutedText,
                                    checkmarkColor = SokoBlueBlack
                                ),
                                modifier = Modifier.testTag("terms_checkbox")
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isSw) "Ninakubali Vigezo na Masharti ya SokoTZ" else "I accept SokoTZ Terms and Conditions",
                                    color = SokoLightText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (isSw) "Soma Vigezo na Masharti kamili" else "Read full Terms & Conditions",
                                    color = SokoNeonBlue,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .clickable { showTermsDialog = true }
                                        .testTag("open_terms_dialog_button")
                                )
                            }
                        }

                        // Submit Registration Button
                        Button(
                            onClick = {
                                if (!hasAcceptedTerms) {
                                    validationError = if (isSw)
                                        "Tafadhali weka alama ya tiki kukubali Vigezo na Masharti ya SokoTZ."
                                    else
                                        "Please check the box to accept SokoTZ Terms & Conditions."
                                    return@Button
                                }
                                if (registerName.isBlank()) {
                                    validationError = if (isSw) "Tafadhali weka jina lako kamili." else "Please enter your full name."
                                    return@Button
                                }
                                if (registerPhone.isBlank()) {
                                    validationError = if (isSw) "Tafadhali weka namba yako ya simu." else "Please enter your phone number."
                                    return@Button
                                }

                                authManager.setPendingRegistration(
                                    name = registerName,
                                    phone = registerPhone,
                                    role = registerRole,
                                    acceptedTerms = hasAcceptedTerms
                                )

                                if (context is Activity) {
                                    authManager.signInWithGoogle(context, scope)
                                }
                            },
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = SokoNeonBlue),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("standard_register_button")
                        ) {
                            Text(
                                text = if (isSw) "Kamilisha Usajili wa Akaunti" else "Complete Registration",
                                color = SokoBlueBlack,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        // OR DIVIDER
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(modifier = Modifier.weight(1f), color = SokoCardBorder)
                            Text(
                                text = if (isSw) " AU SAJILI KWA " else " OR REGISTER WITH ",
                                color = SokoMutedText,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            )
                            HorizontalDivider(modifier = Modifier.weight(1f), color = SokoCardBorder)
                        }

                        // Google Sign-In / Register Button
                        GoogleSignInButton(
                            isLoading = isLoading,
                            isSw = isSw,
                            onClick = {
                                if (!hasAcceptedTerms) {
                                    validationError = if (isSw)
                                        "Tafadhali weka alama ya tiki kukubali Vigezo na Masharti kabla ya kuendelea."
                                    else
                                        "Please accept Terms & Conditions before continuing."
                                    return@GoogleSignInButton
                                }
                                authManager.setPendingRegistration(
                                    name = registerName,
                                    phone = registerPhone,
                                    role = registerRole,
                                    acceptedTerms = hasAcceptedTerms
                                )
                                if (context is Activity) {
                                    authManager.signInWithGoogle(context, scope)
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Trust Pillars Footnotes
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TrustBadge(
                    icon = Icons.Default.VerifiedUser,
                    label = if (isSw) "Wauzaji Halisi" else "Verified Sellers"
                )
                TrustBadge(
                    icon = Icons.Default.Storefront,
                    label = if (isSw) "Kariakoo & Mikoani" else "Nationwide"
                )
                TrustBadge(
                    icon = Icons.Default.Security,
                    label = if (isSw) "Hifadhi ya Wingu" else "Cloud Secured"
                )
            }
        }
    }

    // TERMS AND CONDITIONS DIALOG
    if (showTermsDialog) {
        TermsAndConditionsDialog(
            isSw = isSw,
            onDismiss = { showTermsDialog = false },
            onAccept = {
                hasAcceptedTerms = true
                validationError = null
                showTermsDialog = false
            }
        )
    }
}

@Composable
private fun GoogleSignInButton(
    isLoading: Boolean,
    isSw: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("google_signin_button")
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = SokoBlueBlack,
                strokeWidth = 2.5.dp
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "G",
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    color = Color(0xFF4285F4)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (isSw) "Ingia kwa Google" else "Sign in with Google",
                    color = Color(0xFF1F1F1F),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun TermsAndConditionsDialog(
    isSw: Boolean,
    onDismiss: () -> Unit,
    onAccept: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SokoMidnightBlue,
        titleContentColor = SokoLightText,
        textContentColor = SokoMutedText,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = if (isSw) "Vigezo na Masharti ya SokoTZ" else "SokoTZ Terms and Conditions",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (isSw)
                        "Karibu kwenye SokoTZ (Soko Kuu Mtandaoni Tanzania). Kwa kusajili akaunti na kutumia huduma zetu, unakubaliana na kanuni na masharti yafuatayo:"
                    else
                        "Welcome to SokoTZ (Tanzania Premier Marketplace). By registering an account and using our services, you agree to the following terms:",
                    fontSize = 13.sp,
                    color = SokoLightText
                )

                TermsItem(
                    number = "1.",
                    title = if (isSw) "Usajili na Usahihi wa Taarifa" else "Account & Data Accuracy",
                    body = if (isSw)
                        "Mtumiaji anathibitisha kuwa jina, barua pepe, na namba ya simu/WhatsApp zilizotolewa ni za kweli na zinafanya kazi."
                    else
                        "Users warrant that all names, email, and contact phone/WhatsApp details provided are truthful and active."
                )

                TermsItem(
                    number = "2.",
                    title = if (isSw) "Matangazo na Bidhaa Halisi" else "Authentic Listings & Anti-Fraud",
                    body = if (isSw)
                        "Ni marufuku kuweka matangazo ya bidhaa bandia au za udanganyifu. Wauzaji wanaokiuka watafungiwa akaunti mara moja."
                    else
                        "Posting counterfeit goods, deceptive services, or fraudulent listings is strictly prohibited and subject to immediate suspension."
                )

                TermsItem(
                    number = "3.",
                    title = if (isSw) "Mawasiliano na Malipo" else "Direct Transactions & Communication",
                    body = if (isSw)
                        "SokoTZ ni jukwaa la kuunganisha wanunuzi na wauzaji. Hakikisha unakagua bidhaa kabla ya kufanya malipo kupitia huduma za kifedha."
                    else
                        "SokoTZ connects buyers and sellers. Users are advised to inspect goods prior to payment using approved financial services."
                )

                TermsItem(
                    number = "4.",
                    title = if (isSw) "Faragha na Usalama wa Data" else "Privacy & Cloud Security",
                    body = if (isSw)
                        "Data zako zote huhifadhiwa kwenye mfumo wa wingu wa Google Cloud Firestore kwa viwango vya juu vya usalama."
                    else
                        "All data is secured via Google Cloud Firestore with enterprise-grade encryption and privacy protection."
                )

                TermsItem(
                    number = "5.",
                    title = if (isSw) "sokoTZ AI (Msaidizi wa SokoTZ)" else "sokoTZ AI Assistant",
                    body = if (isSw)
                        "Msaidizi wa AI hutoa mapendekezo kulingana na data za soko. Thibitisha bei na akiba ya bidhaa moja kwa moja na muuzaji."
                    else
                        "The AI assistant recommends products based on live catalog data. Always confirm exact prices and stock directly with sellers."
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onAccept,
                colors = ButtonDefaults.buttonColors(containerColor = SokoNeonBlue)
            ) {
                Text(if (isSw) "Ninakubali" else "I Accept", color = SokoBlueBlack, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isSw) "Funga" else "Close", color = SokoMutedText)
            }
        }
    )
}

@Composable
private fun TermsItem(number: String, title: String, body: String) {
    Column {
        Text(
            text = "$number $title",
            fontWeight = FontWeight.Bold,
            color = SokoNeonBlue,
            fontSize = 12.sp
        )
        Text(
            text = body,
            color = SokoMutedText,
            fontSize = 11.sp,
            lineHeight = 16.sp
        )
    }
}

@Composable
private fun TrustBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(SokoDeepNavy)
                .border(1.dp, SokoCardBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = SokoWarning,
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            text = label,
            fontSize = 10.sp,
            color = SokoMutedText,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun authTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = SokoNeonBlue,
    unfocusedBorderColor = SokoCardBorder,
    focusedLabelColor = SokoNeonBlue,
    unfocusedLabelColor = SokoMutedText,
    focusedTextColor = SokoLightText,
    unfocusedTextColor = SokoLightText,
    cursorColor = SokoNeonBlue
)
