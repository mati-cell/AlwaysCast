package dev.matiribco.alwayscast.feature.browser

import java.util.Locale

class AntiPopupShield {
    fun shouldBlockUrl(url: String): Boolean {
        val lower = url.lowercase(Locale.ROOT)
        return when {
            lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("about:blank") -> false
            else -> true
        }
    }

    fun getShieldScript(): String = SHIELD_SCRIPT

    companion object {
        const val SHIELD_SCRIPT = """
(function() {
    window.open = function() {
        console.warn('[AlwaysCast] Blocked popup window');
        return null;
    };
})();
"""
    }
}
