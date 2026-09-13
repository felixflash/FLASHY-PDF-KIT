package com.flashypdfkit

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import com.flashypdfkit.data.PreferencesAndHistory
import com.flashypdfkit.model.AppTheme
import com.flashypdfkit.model.PdfToolType
import com.flashypdfkit.model.ProcessedFile
import com.flashypdfkit.model.UserSettings
import com.flashypdfkit.ui.CompressScreen
import com.flashypdfkit.ui.HomeScreen
import com.flashypdfkit.ui.ImagesToPdfScreen
import com.flashypdfkit.ui.LegalScreen
import com.flashypdfkit.ui.MergeScreen
import com.flashypdfkit.ui.OnboardingScreen
import com.flashypdfkit.ui.OrganizeScreen
import com.flashypdfkit.ui.PdfToImagesScreen
import com.flashypdfkit.ui.ProtectScreen
import com.flashypdfkit.ui.ReaderScreen
import com.flashypdfkit.ui.RecentScreen
import com.flashypdfkit.ui.SettingsScreen
import com.flashypdfkit.ui.SignScreen
import com.flashypdfkit.ui.SplitScreen
import com.flashypdfkit.ui.SplashScreen
import com.flashypdfkit.ui.components.PremiumDialog
import com.flashypdfkit.ui.theme.FlashyPDFTheme
import com.flashypdfkit.data.BillingManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import java.io.File
import java.io.FileInputStream

class MainActivity : ComponentActivity() {

    private lateinit var prefsHistory: PreferencesAndHistory
    private lateinit var billingManager: BillingManager
    private var activeToolState = mutableStateOf<PdfToolType?>(null)
    private var selectedUris = mutableStateListOf<Uri>()

    private var cameraTempUri: Uri? = null

    // Pick single PDF launcher
    private val singlePdfPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            try {
                contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {}
            selectedUris.clear()
            selectedUris.add(it)
        }
    }

    // Pick multiple PDFs launcher
    private val multiPdfPicker = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach {
                try {
                    contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (_: Exception) {}
            }
            if (activeToolState.value == PdfToolType.MERGE) {
                uris.forEach { uri ->
                    if (!selectedUris.any { it == uri || it.toString() == uri.toString() }) {
                        selectedUris.add(uri)
                    }
                }
            } else {
                selectedUris.clear()
                selectedUris.addAll(uris)
            }
        }
    }

    // Pick multiple images launcher
    private val imagePicker = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            selectedUris.clear()
            selectedUris.addAll(uris)
        }
    }

    // Take photo launcher
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && cameraTempUri != null) {
            selectedUris.add(cameraTempUri!!)
        }
    }

    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchCamera()
        } else {
            Toast.makeText(this, "Camera permission is required to scan images.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        prefsHistory = PreferencesAndHistory(this)
        billingManager = BillingManager(this, prefsHistory)
        com.flashypdfkit.ads.UnityAdsManager.initialize(this)

        // Handle incoming PDF intent if opened from external app
        if (intent?.action == Intent.ACTION_VIEW && intent.type == "application/pdf") {
            intent.data?.let { uri ->
                selectedUris.clear()
                selectedUris.add(uri)
                activeToolState.value = PdfToolType.READ
            }
        }

        setContent {
            var userSettings by remember { mutableStateOf(prefsHistory.getUserSettings()) }
            var showPremiumModal by remember { mutableStateOf(false) }
            var pendingToolForAd by remember { mutableStateOf<PdfToolType?>(null) }
            var showRewardedAdModal by remember { mutableStateOf<PdfToolType?>(null) }
            var recentFilesList by remember { mutableStateOf(prefsHistory.getRecentFiles()) }

            val isPremiumOwned by billingManager.isPremiumOwned.collectAsState()
            val purchaseError by billingManager.purchaseError.collectAsState()

            LaunchedEffect(isPremiumOwned) {
                userSettings = userSettings.copy(isPremium = isPremiumOwned)
            }

            LaunchedEffect(activeToolState.value) {
                if (activeToolState.value == PdfToolType.RECENT) {
                    recentFilesList = prefsHistory.getRecentFiles()
                }
            }

            LaunchedEffect(purchaseError) {
                purchaseError?.let {
                    Toast.makeText(this@MainActivity, it, Toast.LENGTH_LONG).show()
                    billingManager.clearPurchaseError()
                }
            }

            val systemDark = isSystemInDarkTheme()

            val isDark = when(userSettings.theme) {
                AppTheme.DARK -> true
                AppTheme.LIGHT -> false
            }

            FlashyPDFTheme(darkTheme = isDark) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var showSplash by remember { mutableStateOf(true) }
                    if (showSplash) {
                        SplashScreen(onTimeout = { showSplash = false })
                    } else {
                        val activeTool = activeToolState.value

                        BackHandler(enabled = activeTool != null) {
                            activeToolState.value = null
                        }

                        if (!userSettings.hasCompletedOnboarding) {
                            OnboardingScreen(
                                onFinish = {
                                    prefsHistory.setCompletedOnboarding(true)
                                    userSettings = prefsHistory.getUserSettings()
                                }
                            )
                        } else {
                        when (activeTool) {
                            null -> HomeScreen(
                                isPremium = userSettings.isPremium,
                                onSelectTool = { tool ->
                                    if (userSettings.isPremium) {
                                        selectedUris.clear()
                                        activeToolState.value = tool
                                    } else {
                                        when (tool) {
                                            PdfToolType.MERGE -> {
                                                if (com.flashypdfkit.ads.UsageManager.canUseMerge(this@MainActivity)) {
                                                    selectedUris.clear()
                                                    activeToolState.value = tool
                                                } else {
                                                    pendingToolForAd = PdfToolType.MERGE
                                                    showRewardedAdModal = PdfToolType.MERGE
                                                }
                                            }
                                            PdfToolType.SIGN -> {
                                                if (com.flashypdfkit.ads.UsageManager.canUseSign(this@MainActivity)) {
                                                    selectedUris.clear()
                                                    activeToolState.value = tool
                                                } else {
                                                    pendingToolForAd = PdfToolType.SIGN
                                                    showRewardedAdModal = PdfToolType.SIGN
                                                }
                                            }
                                            PdfToolType.PROTECT -> {
                                                if (com.flashypdfkit.ads.UsageManager.canUseProtect(this@MainActivity)) {
                                                    pendingToolForAd = PdfToolType.PROTECT
                                                    showRewardedAdModal = PdfToolType.PROTECT
                                                } else {
                                                    showPremiumModal = true
                                                }
                                            }
                                            else -> {
                                                selectedUris.clear()
                                                activeToolState.value = tool
                                            }
                                        }
                                    }
                                },
                                onOpenUpgradeModal = { showPremiumModal = true },
                                onOpenSettings = { activeToolState.value = PdfToolType.SETTINGS },
                                onFetchSuccess = { uri ->
                                    selectedUris.clear()
                                    selectedUris.add(uri)
                                    activeToolState.value = PdfToolType.READ
                                }
                            )

                            PdfToolType.READ -> ReaderScreen(
                                initialUri = selectedUris.firstOrNull(),
                                onBack = { activeToolState.value = null },
                                onPickPdf = { singlePdfPicker.launch(arrayOf("application/pdf")) },
                                onSharePdf = { uri -> shareUri(uri) },
                                onSavePdf = { uri -> saveUriToDownloads(uri) }
                            )

                            PdfToolType.MERGE -> MergeScreen(
                                selectedUris = selectedUris,
                                onBack = {
                                    activeToolState.value = null
                                    selectedUris.clear()
                                },
                                onPickFiles = { multiPdfPicker.launch(arrayOf("application/pdf")) },
                                onSaveResult = { file, tool -> handleSaveResult(file, tool) },
                                onShareResult = { file -> shareFile(file) }
                            )

                            PdfToolType.SPLIT -> SplitScreen(
                                initialUri = selectedUris.firstOrNull(),
                                onBack = { activeToolState.value = null },
                                onPickPdf = { singlePdfPicker.launch(arrayOf("application/pdf")) },
                                onSaveResult = { file, tool -> handleSaveResult(file, tool) },
                                onShareResult = { file -> shareFile(file) }
                            )

                            PdfToolType.SIGN -> SignScreen(
                                initialUri = selectedUris.firstOrNull(),
                                onBack = { activeToolState.value = null },
                                onPickPdf = { singlePdfPicker.launch(arrayOf("application/pdf")) },
                                onSaveResult = { file, tool -> handleSaveResult(file, tool) },
                                onShareResult = { file -> shareFile(file) }
                            )

                            PdfToolType.PROTECT -> ProtectScreen(
                                initialUri = selectedUris.firstOrNull(),
                                onBack = { activeToolState.value = null },
                                onPickPdf = { singlePdfPicker.launch(arrayOf("application/pdf")) },
                                onSaveResult = { file, tool -> handleSaveResult(file, tool) },
                                onShareResult = { file -> shareFile(file) }
                            )

                            PdfToolType.IMAGES_TO_PDF -> ImagesToPdfScreen(
                                selectedImages = selectedUris,
                                onBack = { activeToolState.value = null },
                                onPickImages = { imagePicker.launch("image/*") },
                                onTakeCameraPhoto = { launchCamera() },
                                onSaveResult = { file, tool -> handleSaveResult(file, tool) },
                                onShareResult = { file -> shareFile(file) }
                            )

                            PdfToolType.PDF_TO_IMAGES -> PdfToImagesScreen(
                                initialUri = selectedUris.firstOrNull(),
                                onBack = { activeToolState.value = null },
                                onPickPdf = { singlePdfPicker.launch(arrayOf("application/pdf")) },
                                onSaveResult = { file, tool -> handleSaveResult(file, tool) },
                                onShareResult = { file -> shareFile(file) },
                                onSaveMultipleResults = { files -> handleSaveMultipleResults(files, PdfToolType.PDF_TO_IMAGES) },
                                onShareMultipleResults = { files -> shareMultipleFiles(files) }
                            )

                            PdfToolType.ORGANIZE -> OrganizeScreen(
                                initialUri = selectedUris.firstOrNull(),
                                onBack = { activeToolState.value = null },
                                onPickPdf = { singlePdfPicker.launch(arrayOf("application/pdf")) },
                                onSaveResult = { file, tool -> handleSaveResult(file, tool) },
                                onShareResult = { file -> shareFile(file) }
                            )

                            PdfToolType.COMPRESS -> CompressScreen(
                                initialUri = selectedUris.firstOrNull(),
                                onBack = { activeToolState.value = null },
                                onPickPdf = { singlePdfPicker.launch(arrayOf("application/pdf")) },
                                onSaveResult = { file, tool -> handleSaveResult(file, tool) },
                                onShareResult = { file -> shareFile(file) }
                            )

                            PdfToolType.RECENT -> RecentScreen(
                                recentFiles = recentFilesList,
                                onBack = { activeToolState.value = null },
                                onOpenFile = { file ->
                                    selectedUris.clear()
                                    selectedUris.add(Uri.parse(file.uriString))
                                    activeToolState.value = PdfToolType.READ
                                },
                                onShareFile = { file -> shareUri(Uri.parse(file.uriString)) },
                                onDeleteFile = { file ->
                                    prefsHistory.removeRecentFile(file.id)
                                    recentFilesList = prefsHistory.getRecentFiles()
                                },
                                onClearAll = {
                                    prefsHistory.clearRecentFiles()
                                    recentFilesList = emptyList()
                                }
                            )

                            PdfToolType.SETTINGS -> SettingsScreen(
                                currentTheme = userSettings.theme,
                                isPremium = userSettings.isPremium,
                                onBack = { activeToolState.value = null },
                                onToggleTheme = { theme ->
                                    prefsHistory.setTheme(theme)
                                    userSettings = prefsHistory.getUserSettings()
                                },
                                onOpenUpgrade = { showPremiumModal = true },
                                onOpenLegal = { activeToolState.value = PdfToolType.LEGAL }
                            )

                            PdfToolType.LEGAL -> LegalScreen(
                                onBack = { activeToolState.value = PdfToolType.SETTINGS }
                            )

                            else -> HomeScreen(
                                isPremium = userSettings.isPremium,
                                onSelectTool = { tool ->
                                    selectedUris.clear()
                                    activeToolState.value = tool
                                },
                                onOpenUpgradeModal = { showPremiumModal = true },
                                onOpenSettings = { activeToolState.value = PdfToolType.SETTINGS },
                                onFetchSuccess = { uri ->
                                    selectedUris.clear()
                                    selectedUris.add(uri)
                                    activeToolState.value = PdfToolType.READ
                                }
                            )
                        }
                        }

                        if (showPremiumModal) {
                            PremiumDialog(
                                onDismiss = { showPremiumModal = false },
                                onUnlock = {
                                    billingManager.launchPurchaseFlow(this@MainActivity)
                                    showPremiumModal = false
                                }
                            )
                        }

                        if (showRewardedAdModal != null) {
                            val tool = showRewardedAdModal!!
                            val adText = when (tool) {
                                PdfToolType.MERGE -> "Watch an ad to use Merge once today"
                                PdfToolType.SIGN -> "Watch an ad to use Sign once today"
                                PdfToolType.PROTECT -> "Watch an ad to lock a PDF (${com.flashypdfkit.ads.UsageManager.getProtectAdUses(this@MainActivity)}/2 today)"
                                else -> "Watch an ad to use this tool once"
                            }
                            PremiumDialog(
                                onDismiss = { showRewardedAdModal = null; pendingToolForAd = null },
                                onUnlock = {
                                    billingManager.launchPurchaseFlow(this@MainActivity)
                                    showRewardedAdModal = null
                                    pendingToolForAd = null
                                },
                                adButtonText = adText,
                                onWatchAd = {
                                    showRewardedAdModal = null
                                    com.flashypdfkit.ads.UnityAdsManager.loadRewardedAd {
                                        com.flashypdfkit.ads.UnityAdsManager.showRewardedAd(
                                            this@MainActivity,
                                            onCompleted = {
                                                when (pendingToolForAd) {
                                                    PdfToolType.MERGE -> com.flashypdfkit.ads.UsageManager.grantMergeRewardedExtra(this@MainActivity)
                                                    PdfToolType.SIGN -> com.flashypdfkit.ads.UsageManager.grantSignRewardedExtra(this@MainActivity)
                                                    PdfToolType.PROTECT -> com.flashypdfkit.ads.UsageManager.consumeProtectUse(this@MainActivity)
                                                    else -> {}
                                                }
                                                val target = pendingToolForAd
                                                pendingToolForAd = null
                                                if (target != null) {
                                                    selectedUris.clear()
                                                    activeToolState.value = target
                                                }
                                            },
                                            onFailed = {
                                                Toast.makeText(this@MainActivity, "Ad playback failed or was skipped. Please try again.", Toast.LENGTH_SHORT).show()
                                                pendingToolForAd = null
                                            }
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun launchCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            try {
                val file = File(cacheDir, "Camera_${System.currentTimeMillis()}.jpg")
                val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
                cameraTempUri = uri
                cameraLauncher.launch(uri)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Could not launch camera", Toast.LENGTH_SHORT).show()
            }
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun getMimeType(file: File): String {
        return when (file.extension.lowercase()) {
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            else -> "application/pdf"
        }
    }

    private fun handleSaveResult(file: File, toolType: PdfToolType) {
        if (!prefsHistory.getUserSettings().isPremium) {
            when (toolType) {
                PdfToolType.MERGE -> com.flashypdfkit.ads.UsageManager.consumeMergeUseOrExtra(this)
                PdfToolType.SIGN -> com.flashypdfkit.ads.UsageManager.consumeSignUseOrExtra(this)
                else -> {}
            }
        }
        
        // Track action for review prompt
        prefsHistory.incrementActionCount()
        if (prefsHistory.getActionCount() % 5 == 0) {
            showReviewPrompt()
        }

        val savedUri = saveToDownloadsFolder(file)
        val processed = ProcessedFile(
            id = System.currentTimeMillis().toString(),
            name = file.name,
            path = file.absolutePath,
            uriString = savedUri.toString(),
            sizeBytes = file.length(),
            toolType = toolType
        )
        prefsHistory.addRecentFile(processed)
        Toast.makeText(this, "Saved ${file.name} to Downloads/FlashyPDF", Toast.LENGTH_LONG).show()
    }

    private fun showReviewPrompt() {
        val manager = com.google.android.play.core.review.ReviewManagerFactory.create(this)
        val request = manager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val reviewInfo = task.result
                manager.launchReviewFlow(this, reviewInfo)
            }
        }
    }

    private fun handleSaveMultipleResults(files: List<File>, toolType: PdfToolType) {
        if (files.isEmpty()) return
        for (file in files) {
            val savedUri = saveToDownloadsFolder(file)
            val processed = ProcessedFile(
                id = System.currentTimeMillis().toString() + "_" + file.name,
                name = file.name,
                path = file.absolutePath,
                uriString = savedUri.toString(),
                sizeBytes = file.length(),
                toolType = toolType
            )
            prefsHistory.addRecentFile(processed)
        }
        Toast.makeText(this, "Saved ${files.size} files to Downloads/FlashyPDF", Toast.LENGTH_LONG).show()
    }

    private fun saveToDownloadsFolder(file: File): Uri {
        return try {
            val mime = getMimeType(file)
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                put(MediaStore.MediaColumns.MIME_TYPE, mime)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/FlashyPDF")
                }
            }
            val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                contentResolver.openOutputStream(uri)?.use { out ->
                    FileInputStream(file).use { input ->
                        input.copyTo(out)
                    }
                }
                uri
            } else {
                Uri.fromFile(file)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Uri.fromFile(file)
        }
    }

    private fun shareFile(file: File) {
        try {
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val mime = getMimeType(file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mime
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val title = if (mime.startsWith("image/")) "Share Image" else "Share PDF Document"
            startActivity(Intent.createChooser(intent, title))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun shareMultipleFiles(files: List<File>) {
        if (files.isEmpty()) return
        if (files.size == 1) {
            shareFile(files.first())
            return
        }
        try {
            val uris = ArrayList<Uri>()
            for (file in files) {
                uris.add(FileProvider.getUriForFile(this, "$packageName.fileprovider", file))
            }
            val firstMime = getMimeType(files.first())
            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = if (firstMime.startsWith("image/")) "image/*" else "application/pdf"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val title = if (firstMime.startsWith("image/")) "Share ${files.size} Images" else "Share PDF Documents"
            startActivity(Intent.createChooser(intent, title))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun shareUri(uri: Uri) {
        try {
            val fileToShare = if (uri.scheme == "file") {
                File(uri.path ?: "")
            } else {
                val tempFile = File(cacheDir, "shared_temp_${System.currentTimeMillis()}.pdf")
                contentResolver.openInputStream(uri)?.use { input ->
                    java.io.FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }
                tempFile
            }
            val contentUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", fileToShare)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Share PDF Document"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to share PDF: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveUriToDownloads(uri: Uri) {
        try {
            val fileToSave = if (uri.scheme == "file") {
                File(uri.path ?: "")
            } else {
                val tempFile = File(cacheDir, "download_temp_${System.currentTimeMillis()}.pdf")
                contentResolver.openInputStream(uri)?.use { input ->
                    java.io.FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }
                tempFile
            }
            val savedUri = saveToDownloadsFolder(fileToSave)
            Toast.makeText(this, "Saved ${fileToSave.name} to Downloads/FlashyPDF", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to save PDF: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}
