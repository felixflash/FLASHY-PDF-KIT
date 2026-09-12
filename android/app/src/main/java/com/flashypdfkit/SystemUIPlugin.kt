package com.flashypdfkit

import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import androidx.appcompat.app.AlertDialog
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin

@CapacitorPlugin(name = "SystemUI")
class SystemUIPlugin : Plugin() {

    @PluginMethod
    fun setImmersive(call: PluginCall) {
        val immersive = call.getBoolean("immersive") ?: true
        
        activity.runOnUiThread {
            val window = activity.window
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            
            if (immersive) {
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
            call.resolve()
        }
    }

    @PluginMethod
    fun isIntentLaunch(call: PluginCall) {
        val res = JSObject()
        res.put("value", MainActivity.isIntentLaunch)
        call.resolve(res)
    }

    @PluginMethod
    fun getPendingPDF(call: PluginCall) {
        MainActivity.webReady = true // Mark web as ready for direct JS events
        val pdf = MainActivity.pendingPdf
        if (pdf != null) {
            val res = JSObject()
            res.put("name", pdf.getString("name"))
            res.put("path", pdf.getString("path"))
            MainActivity.pendingPdf = null
            call.resolve(res)
        } else {
            call.resolve()
        }
    }

    @PluginMethod
    fun markWebReady(call: PluginCall) {
        MainActivity.webReady = true
        call.resolve()
    }

    @PluginMethod
    fun exitApp(call: PluginCall) {
        activity.finishAffinity()
        System.exit(0)
    }

    @PluginMethod
    fun requestInput(call: PluginCall) {
        val title = call.getString("title") ?: "Input required"
        val message = call.getString("message") ?: "Please enter the value."
        val hint = call.getString("hint") ?: "Type here"
        val defaultValue = call.getString("value") ?: ""
        
        activity.runOnUiThread {
            val input = EditText(activity)
            input.inputType = InputType.TYPE_CLASS_TEXT
            input.hint = hint
            input.setText(defaultValue)
            input.setSingleLine(true)
            input.setPadding(dp(16), dp(2), dp(16), dp(2))
            input.background = GradientDrawable().apply {
                setColor(Color.argb(20, 125, 117, 255))
                setStroke(dp(1), Color.argb(90, 125, 117, 255))
                cornerRadius = dp(14).toFloat()
            }

            val container = LinearLayout(activity)
            container.orientation = LinearLayout.VERTICAL
            container.setPadding(dp(24), dp(6), dp(24), 0)

            val heading = TextView(activity)
            heading.text = title
            heading.textSize = 18f
            heading.setTextColor(Color.WHITE)
            heading.gravity = Gravity.CENTER
            heading.setTypeface(null, android.graphics.Typeface.BOLD)
            container.addView(heading, LinearLayout.LayoutParams(-1, dp(32)))

            val description = TextView(activity)
            description.text = message
            description.textSize = 13f
            description.setTextColor(Color.LTGRAY)
            description.gravity = Gravity.CENTER
            description.setPadding(0, 0, 0, dp(14))
            container.addView(description, LinearLayout.LayoutParams(-1, -2))

            container.addView(input)

            val actionLabel = call.getString("actionLabel") ?: "OK"
            val dialog = AlertDialog.Builder(activity)
                .setView(container)
                .setNegativeButton("Cancel") { _, _ -> call.resolve() }
                .setPositiveButton(actionLabel, null)
                .create()

            dialog.setOnShowListener {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    val value = input.text.toString()
                    val result = JSObject()
                    result.put("value", value)
                    call.resolve(result)
                    dialog.dismiss()
                }
                input.requestFocus()
                dialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
            }
            dialog.setOnCancelListener { call.resolve() }
            dialog.show()
        }
    }

    @PluginMethod
    fun requestPassword(call: PluginCall) {
        val title = call.getString("title") ?: "Password required"
        val message = call.getString("message") ?: "Enter the password to continue."
        activity.runOnUiThread {
            val input = EditText(activity)
            input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            input.hint = "Enter password"
            input.setSingleLine(true)
            input.setPadding(dp(16), dp(2), dp(16), dp(2))
            input.background = GradientDrawable().apply {
                setColor(Color.argb(20, 125, 117, 255))
                setStroke(dp(1), Color.argb(90, 125, 117, 255))
                cornerRadius = dp(14).toFloat()
            }

            val container = LinearLayout(activity)
            container.orientation = LinearLayout.VERTICAL
            container.setPadding(dp(24), dp(6), dp(24), 0)

            val icon = TextView(activity)
            icon.text = "🔐"
            icon.textSize = 30f
            icon.gravity = Gravity.CENTER
            icon.setPadding(0, 0, 0, dp(8))
            container.addView(icon, LinearLayout.LayoutParams(-1, dp(52)))

            val heading = TextView(activity)
            heading.text = title
            heading.textSize = 20f
            heading.setTextColor(Color.WHITE)
            heading.gravity = Gravity.CENTER
            heading.setTypeface(null, android.graphics.Typeface.BOLD)
            container.addView(heading, LinearLayout.LayoutParams(-1, dp(32)))

            val description = TextView(activity)
            description.text = message
            description.textSize = 13f
            description.setTextColor(Color.LTGRAY)
            description.gravity = Gravity.CENTER
            description.setPadding(0, 0, 0, dp(14))
            container.addView(description, LinearLayout.LayoutParams(-1, dp(48)))

            container.addView(input)

            val actionLabel = call.getString("actionLabel") ?: "Unlock"
            val dialog = AlertDialog.Builder(activity)
                .setView(container)
                .setNegativeButton("Cancel") { _, _ -> call.resolve() }
                .setPositiveButton(actionLabel, null)
                .create()

            dialog.setOnShowListener {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    val password = input.text.toString()
                    if (password.isNotEmpty()) {
                        val result = JSObject()
                        result.put("password", password)
                        call.resolve(result)
                        dialog.dismiss()
                    } else {
                        input.error = "Enter a password"
                    }
                }
                input.requestFocus()
                dialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
            }
            dialog.setOnCancelListener { call.resolve() }
            dialog.show()
        }
    }

    private fun dp(value: Int): Int = (value * activity.resources.displayMetrics.density).toInt()
}
