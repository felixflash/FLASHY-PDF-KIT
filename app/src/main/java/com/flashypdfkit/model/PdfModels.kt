package com.flashypdfkit.model

import android.net.Uri

enum class PdfToolType(val title: String, val subtitle: String) {
    READ("Read PDF", "Immersive view"),
    MERGE("Merge PDFs", "Combine files"),
    SPLIT("Split PDF", "Extract pages"),
    SIGN("Sign PDF", "Draw & place signature"),
    PROTECT("Password Protect", "Secure PDF"),
    IMAGES_TO_PDF("Images → PDF", "Convert photos"),
    PDF_TO_IMAGES("PDF → Images", "Export pages"),
    ORGANIZE("Organize PDF", "Reorder & rotate"),
    COMPRESS("Compress PDF", "Shrink size"),
    RECENT("History", "Recent files"),
    SETTINGS("Settings", "App preferences"),
    LEGAL("Legal & Policies", "Privacy & terms"),
    ONBOARDING("Welcome", "Get started")
}

data class ProcessedFile(
    val id: String,
    val name: String,
    val path: String,
    val uriString: String,
    val sizeBytes: Long,
    val pageCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val toolType: PdfToolType = PdfToolType.READ
)

data class PageOrganizeItem(
    val originalIndex: Int,
    val pageNumber: Int,
    val rotationDegrees: Int = 0
)

enum class AppTheme {
    DARK, LIGHT
}

data class UserSettings(
    val isPremium: Boolean = false,
    val theme: AppTheme = AppTheme.DARK,
    val hasCompletedOnboarding: Boolean = false,
    val freeUsageCount: Map<String, Int> = emptyMap()
)
