package com.flashypdfkit.ads

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object UsageManager {
    private const val PREF_NAME = "flashy_pdf_usage_prefs"
    private const val KEY_LAST_RESET_DATE = "last_reset_date"
    private const val KEY_MERGE_USES = "merge_uses_today"
    private const val KEY_SIGN_USES = "sign_uses_today"
    private const val KEY_PROTECT_AD_USES = "protect_ad_uses_today"
    private const val KEY_MERGE_REWARDED = "merge_rewarded_extra_today"
    private const val KEY_SIGN_REWARDED = "sign_rewarded_extra_today"

    const val MAX_MERGE_FREE = 8
    const val MAX_SIGN_FREE = 5
    const val MAX_PROTECT_AD_USES = 2

    private fun getPrefs(context: Context) =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private fun checkReset(context: Context) {
        val prefs = getPrefs(context)
        val todayStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val lastReset = prefs.getString(KEY_LAST_RESET_DATE, "")

        if (lastReset != todayStr) {
            prefs.edit()
                .putString(KEY_LAST_RESET_DATE, todayStr)
                .putInt(KEY_MERGE_USES, 0)
                .putInt(KEY_SIGN_USES, 0)
                .putInt(KEY_PROTECT_AD_USES, 0)
                .putInt(KEY_MERGE_REWARDED, 0)
                .putInt(KEY_SIGN_REWARDED, 0)
                .apply()
        }
    }

    fun getMergeUses(context: Context): Int {
        checkReset(context)
        return getPrefs(context).getInt(KEY_MERGE_USES, 0)
    }

    fun incrementMergeUse(context: Context) {
        checkReset(context)
        val prefs = getPrefs(context)
        val current = prefs.getInt(KEY_MERGE_USES, 0)
        prefs.edit().putInt(KEY_MERGE_USES, current + 1).apply()
    }

    fun getMergeRewardedExtra(context: Context): Int {
        checkReset(context)
        return getPrefs(context).getInt(KEY_MERGE_REWARDED, 0)
    }

    fun grantMergeRewardedExtra(context: Context) {
        checkReset(context)
        val prefs = getPrefs(context)
        val current = prefs.getInt(KEY_MERGE_REWARDED, 0)
        prefs.edit().putInt(KEY_MERGE_REWARDED, current + 1).apply()
    }

    fun getSignUses(context: Context): Int {
        checkReset(context)
        return getPrefs(context).getInt(KEY_SIGN_USES, 0)
    }

    fun incrementSignUse(context: Context) {
        checkReset(context)
        val prefs = getPrefs(context)
        val current = prefs.getInt(KEY_SIGN_USES, 0)
        prefs.edit().putInt(KEY_SIGN_USES, current + 1).apply()
    }

    fun getSignRewardedExtra(context: Context): Int {
        checkReset(context)
        return getPrefs(context).getInt(KEY_SIGN_REWARDED, 0)
    }

    fun grantSignRewardedExtra(context: Context) {
        checkReset(context)
        val prefs = getPrefs(context)
        val current = prefs.getInt(KEY_SIGN_REWARDED, 0)
        prefs.edit().putInt(KEY_SIGN_REWARDED, current + 1).apply()
    }

    fun canUseMerge(context: Context): Boolean {
        checkReset(context)
        return getMergeUses(context) < MAX_MERGE_FREE || getMergeRewardedExtra(context) > 0
    }

    fun consumeMergeUseOrExtra(context: Context) {
        checkReset(context)
        val prefs = getPrefs(context)
        val currentUses = prefs.getInt(KEY_MERGE_USES, 0)
        if (currentUses < MAX_MERGE_FREE) {
            prefs.edit().putInt(KEY_MERGE_USES, currentUses + 1).apply()
        } else {
            // Consumed rewarded extra
            prefs.edit().putInt(KEY_MERGE_REWARDED, 0).apply()
        }
    }

    fun canUseSign(context: Context): Boolean {
        checkReset(context)
        return getSignUses(context) < MAX_SIGN_FREE || getSignRewardedExtra(context) > 0
    }

    fun consumeSignUseOrExtra(context: Context) {
        checkReset(context)
        val prefs = getPrefs(context)
        val currentUses = prefs.getInt(KEY_SIGN_USES, 0)
        if (currentUses < MAX_SIGN_FREE) {
            prefs.edit().putInt(KEY_SIGN_USES, currentUses + 1).apply()
        } else {
            // Consumed rewarded extra
            prefs.edit().putInt(KEY_SIGN_REWARDED, 0).apply()
        }
    }

    fun getProtectAdUses(context: Context): Int {
        checkReset(context)
        return getPrefs(context).getInt(KEY_PROTECT_AD_USES, 0)
    }

    fun incrementProtectAdUse(context: Context) {
        checkReset(context)
        val prefs = getPrefs(context)
        val current = prefs.getInt(KEY_PROTECT_AD_USES, 0)
        prefs.edit().putInt(KEY_PROTECT_AD_USES, current + 1).apply()
    }

    fun canUseProtect(context: Context): Boolean {
        checkReset(context)
        return getProtectAdUses(context) < MAX_PROTECT_AD_USES
    }

    fun consumeProtectUse(context: Context) {
        incrementProtectAdUse(context)
    }
}
