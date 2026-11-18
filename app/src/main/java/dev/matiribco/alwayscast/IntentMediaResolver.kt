package dev.matiribco.alwayscast

import android.content.Intent
import java.util.regex.Pattern

object IntentMediaResolver {
    private const val ACTION_SEND = "android.intent.action.SEND"
    private const val ACTION_VIEW = "android.intent.action.VIEW"
    private val URL_PATTERN: Pattern =
        Pattern.compile("https?://[^\\s<>\"]+", Pattern.CASE_INSENSITIVE)

    fun extractMediaUrl(intent: Intent?): String? {
        if (intent == null) return null
        val dataStr = intent.dataString
        val extraText = intent.getStringExtra(Intent.EXTRA_TEXT)
        return extractMediaUrl(intent.action, dataStr, extraText)
    }

    fun extractMediaUrl(
        action: String?,
        dataString: String?,
        extraText: String?,
    ): String? =
        when (action) {
            ACTION_SEND -> extractFromText(extraText)
            ACTION_VIEW -> dataString
            else -> null
        }

    private fun extractFromText(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val matcher = URL_PATTERN.matcher(text)
        return if (matcher.find()) {
            matcher.group(0)
        } else {
            text.trim()
        }
    }
}
