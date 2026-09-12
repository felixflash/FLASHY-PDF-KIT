package com.flashypdfkit.ui

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flashypdfkit.data.PreferencesAndHistory
import com.flashypdfkit.model.PdfToolType
import com.flashypdfkit.pdf.PdfEngine
import com.flashypdfkit.ui.components.Dropzone
import com.flashypdfkit.ui.components.FileItemCard
import com.flashypdfkit.ui.components.GradientButton
import com.flashypdfkit.ui.components.ResultCard
import com.flashypdfkit.ui.components.ToolTopBar
import com.flashypdfkit.ui.theme.ActivePalette
import com.flashypdfkit.ui.theme.AppRadius
import com.flashypdfkit.ui.theme.AppSpacing
import com.flashypdfkit.ui.theme.tactilePress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun ProtectScreen(
    initialUri: Uri? = null,
    onBack: () -> Unit,
    onPickPdf: () -> Unit,
    onSaveResult: (File, PdfToolType) -> Unit,
    onShareResult: (File) -> Unit
) {
    val context = LocalContext.current
    val prefsHistory = remember { PreferencesAndHistory(context) }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()
    val isDark = isSystemInDarkTheme()

    val canvasColor = if (isDark) ActivePalette.DarkCanvas else ActivePalette.LightCanvas
    val surfaceColor = if (isDark) ActivePalette.DarkSurface else ActivePalette.LightSurface
    val surfaceMuted = if (isDark) ActivePalette.DarkSurfaceMuted else ActivePalette.LightSurfaceMuted
    val borderColor = if (isDark) ActivePalette.DarkBorder else ActivePalette.LightBorder
    val textPrimary = if (isDark) ActivePalette.DarkTextPrimary else ActivePalette.LightTextPrimary
    val textMuted = if (isDark) ActivePalette.DarkTextMuted else ActivePalette.LightTextMuted

    var currentUri by remember { mutableStateOf(initialUri) }
    var isInputPdfEncrypted by remember { mutableStateOf(false) }

    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }

    var isProcessing by remember { mutableStateOf(false) }
    var resultFile by remember { mutableStateOf<File?>(null) }
    var resultMode by remember { mutableStateOf<String>("protected") } // "protected" or "unlocked"
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun resetState() {
        password = ""
        confirmPassword = ""
        errorMessage = null
        resultFile = null
    }

    LaunchedEffect(initialUri) {
        if (initialUri != null) {
            currentUri = initialUri
            resetState()
        }
    }

    LaunchedEffect(currentUri) {
        val uri = currentUri
        if (uri != null) {
            withContext(Dispatchers.IO) {
                isInputPdfEncrypted = PdfEngine.isPdfEncrypted(context, uri)
            }
        } else {
            isInputPdfEncrypted = false
        }
    }

    // Password strength computation (0 = empty, 1 = weak, 2 = medium, 3 = strong)
    val passwordStrength = remember(password) {
        when {
            password.isEmpty() -> 0
            password.length < 5 -> 1
            password.length < 8 || !password.any { it.isDigit() } -> 2
            else -> 3
        }
    }

    val (strengthLabel, strengthColor, strengthProgress) = when (passwordStrength) {
        1 -> Triple("Weak", ActivePalette.Danger, 0.33f)
        2 -> Triple("Medium", ActivePalette.Honey, 0.66f)
        3 -> Triple("Strong", ActivePalette.Teal, 1.0f)
        else -> Triple("", Color.Transparent, 0f)
    }

    val passwordsMatch = password.isNotEmpty() && confirmPassword.isNotEmpty() && password == confirmPassword

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(canvasColor)
    ) {
        ToolTopBar(
            title = "Lock",
            onBack = onBack,
            infoTitle = "Secure PDF Encryption",
            infoText = "Lock your document with industry-standard 128-bit encryption compatible with all major PDF readers.",
            toolKey = "protect",
            prefsHistory = prefsHistory
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(scrollState)
                .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.md)
        ) {

            if (currentUri == null) {
                Dropzone(
                    label = "Tap to choose a PDF to protect",
                    hint = "Zero-knowledge on-device encryption",
                    onClick = onPickPdf
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Security Features Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(AppRadius.xl),
                    color = surfaceColor,
                    border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(ActivePalette.Primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = null,
                                    tint = ActivePalette.Primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Standard Security Guarantees",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "• Compatible with Adobe Acrobat, Google Drive, Chrome, and all PDF viewers.\n• 100% vector fidelity and fonts preserved without lossy compression.\n• Zero cloud uploads — processed strictly on your phone in private memory.",
                            fontSize = 12.sp,
                            color = textMuted,
                            lineHeight = 19.sp
                        )
                    }
                }
            } else {
                FileItemCard(
                    name = currentUri?.lastPathSegment ?: "Selected PDF",
                    sizeText = if (isInputPdfEncrypted) "Encrypted PDF Document" else "Ready for Protection",
                    onRemove = {
                        currentUri = null
                        resetState()
                    }
                )

                if (isInputPdfEncrypted) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(AppRadius.md),
                        color = ActivePalette.Honey.copy(alpha = if (isDark) 0.18f else 0.10f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ActivePalette.Honey.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = ActivePalette.Honey,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "This PDF currently has password protection. You can decrypt and save an unlocked copy below.",
                                fontSize = 12.sp,
                                color = textPrimary,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (resultFile != null) {
                    ResultCard(
                        title = if (resultMode == "unlocked") "PDF Unlocked Successfully!" else "PDF Protected Successfully!",
                        subtitle = if (resultMode == "unlocked") {
                            "Encryption removed. Saved as ${resultFile?.name}"
                        } else {
                            "Encrypted with 128-bit security. Saved as ${resultFile?.name}"
                        },
                        onSave = { resultFile?.let { onSaveResult(it, PdfToolType.PROTECT) } },
                        onShare = { resultFile?.let { onShareResult(it) } }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = {
                            resetState()
                            currentUri = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .tactilePress {
                                resetState()
                                currentUri = null
                            }
                            .testTag("protect_another_button"),
                        shape = RoundedCornerShape(AppRadius.lg),
                        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = ActivePalette.Primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Protect Another PDF",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = textPrimary
                        )
                    }
                } else {
                    // Password input section
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(AppRadius.xl),
                        color = surfaceColor,
                        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isInputPdfEncrypted) "Document Password" else "Set Password",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary
                                )

                                if (strengthLabel.isNotEmpty() && !isInputPdfEncrypted) {
                                    Surface(
                                        shape = RoundedCornerShape(AppRadius.full),
                                        color = strengthColor.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = strengthLabel,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = strengthColor,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            if (!isInputPdfEncrypted && password.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { strengthProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = strengthColor,
                                    trackColor = borderColor
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Main Password field
                            OutlinedTextField(
                                value = password,
                                onValueChange = {
                                    password = it
                                    errorMessage = null
                                },
                                label = { Text(if (isInputPdfEncrypted) "Current Password" else "Enter Password") },
                                placeholder = { Text(if (isInputPdfEncrypted) "Enter password to unlock" else "e.g. Secret123") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (isInputPdfEncrypted) Icons.Default.LockOpen else Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = ActivePalette.Primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                trailingIcon = {
                                    IconButton(
                                        onClick = { isPasswordVisible = !isPasswordVisible },
                                        modifier = Modifier.tactilePress { isPasswordVisible = !isPasswordVisible }
                                    ) {
                                        Icon(
                                            imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (isPasswordVisible) "Hide password" else "Show password",
                                            tint = textMuted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                },
                                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = if (isInputPdfEncrypted) ImeAction.Done else ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        if (isInputPdfEncrypted) focusManager.clearFocus()
                                    }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("password_input_field"),
                                shape = RoundedCornerShape(AppRadius.md),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ActivePalette.Primary,
                                    unfocusedBorderColor = borderColor,
                                    focusedLabelColor = ActivePalette.Primary,
                                    unfocusedLabelColor = textMuted,
                                    cursorColor = ActivePalette.Primary
                                )
                            )

                            // Confirm Password field (only for protecting unencrypted PDF)
                            if (!isInputPdfEncrypted) {
                                Spacer(modifier = Modifier.height(14.dp))

                                OutlinedTextField(
                                    value = confirmPassword,
                                    onValueChange = {
                                        confirmPassword = it
                                        errorMessage = null
                                    },
                                    label = { Text("Confirm Password") },
                                    placeholder = { Text("Re-enter password") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = if (passwordsMatch) ActivePalette.Teal else ActivePalette.Primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    trailingIcon = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (passwordsMatch) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = "Passwords Match",
                                                    tint = ActivePalette.Teal,
                                                    modifier = Modifier
                                                        .size(18.dp)
                                                        .padding(end = 4.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = { isConfirmPasswordVisible = !isConfirmPasswordVisible },
                                                modifier = Modifier.tactilePress { isConfirmPasswordVisible = !isConfirmPasswordVisible }
                                            ) {
                                                Icon(
                                                    imageVector = if (isConfirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                    contentDescription = if (isConfirmPasswordVisible) "Hide password" else "Show password",
                                                    tint = textMuted,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    },
                                    visualTransformation = if (isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Password,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = { focusManager.clearFocus() }
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("confirm_password_input_field"),
                                    shape = RoundedCornerShape(AppRadius.md),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = if (passwordsMatch) ActivePalette.Teal else ActivePalette.Primary,
                                        unfocusedBorderColor = borderColor,
                                        focusedLabelColor = if (passwordsMatch) ActivePalette.Teal else ActivePalette.Primary,
                                        unfocusedLabelColor = textMuted,
                                        cursorColor = ActivePalette.Primary
                                    )
                                )

                                if (confirmPassword.isNotEmpty() && !passwordsMatch) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Passwords do not match yet",
                                        color = ActivePalette.Danger,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(start = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Error message banner
                    AnimatedVisibility(
                        visible = errorMessage != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        errorMessage?.let { msg ->
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(AppRadius.md),
                                color = ActivePalette.Danger.copy(alpha = if (isDark) 0.2f else 0.1f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ActivePalette.Danger.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = ActivePalette.Danger,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = msg,
                                        color = ActivePalette.Danger,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = ActivePalette.Teal,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Processed 100% on-device in private memory",
                            fontSize = 11.sp,
                            color = textMuted
                        )
                    }

                    if (isInputPdfEncrypted) {
                        // Action buttons for already encrypted document: Remove Protection
                        GradientButton(
                            text = "Unlock & Remove Password",
                            enabled = password.isNotBlank(),
                            isLoading = isProcessing,
                            onClick = {
                                focusManager.clearFocus()
                                val uri = currentUri ?: return@GradientButton
                                isProcessing = true
                                errorMessage = null
                                scope.launch(Dispatchers.IO) {
                                    val outFile = File(context.cacheDir, "Unlocked_${System.currentTimeMillis()}.pdf")
                                    val success = PdfEngine.removePdfProtection(context, uri, password, outFile)
                                    withContext(Dispatchers.Main) {
                                        isProcessing = false
                                        if (success) {
                                            resultMode = "unlocked"
                                            resultFile = outFile
                                        } else {
                                            errorMessage = "Could not unlock PDF. Please verify that the password is correct."
                                        }
                                    }
                                }
                            }
                        )
                    } else {
                        // Standard Protect PDF button
                        GradientButton(
                            text = "Encrypt & Protect PDF",
                            enabled = password.isNotBlank() && confirmPassword.isNotBlank() && passwordsMatch,
                            isLoading = isProcessing,
                            onClick = {
                                focusManager.clearFocus()
                                if (password != confirmPassword) {
                                    errorMessage = "Passwords do not match!"
                                    return@GradientButton
                                }
                                if (password.length < 3) {
                                    errorMessage = "Password should be at least 3 characters long."
                                    return@GradientButton
                                }
                                val uri = currentUri ?: return@GradientButton
                                isProcessing = true
                                errorMessage = null
                                scope.launch(Dispatchers.IO) {
                                    val outFile = File(context.cacheDir, "Protected_${System.currentTimeMillis()}.pdf")
                                    val success = PdfEngine.protectPdf(context, uri, password, outFile)
                                    withContext(Dispatchers.Main) {
                                        isProcessing = false
                                        if (success) {
                                            resultMode = "protected"
                                            resultFile = outFile
                                        } else {
                                            errorMessage = "Failed to encrypt PDF. Please ensure the file is a valid PDF and not corrupt."
                                        }
                                    }
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "⚠️ Keep your password in a secure password manager. Encrypted PDFs use standard AES and cannot be recovered if forgotten.",
                        fontSize = 11.sp,
                        color = textMuted,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }
    }
}

