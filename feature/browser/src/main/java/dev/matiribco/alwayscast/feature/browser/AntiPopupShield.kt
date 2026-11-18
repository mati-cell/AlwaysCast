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
        console.warn('[AlwaysCast] Blocked window.open');
        return {
            focus: function() {},
            close: function() {},
            closed: false
        };
    };
    document.addEventListener('click', function(e) {
        var el = e.target;
        while (el && el.tagName !== 'A') {
            el = el.parentElement;
        }
        if (el && el.tagName === 'A' && el.target === '_blank') {
            el.target = '_self';
        }
    }, true);
})();
"""
    }
}
