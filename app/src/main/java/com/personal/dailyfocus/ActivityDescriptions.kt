package com.personal.dailyfocus

internal interface ActivityDescriptionStore {
    fun read(key: String): String?
    fun write(values: Map<String, String>): Boolean
}

internal class ActivityDescriptions(
    private val store: ActivityDescriptionStore,
) {
    fun get(key: String): String {
        return store.read(key)?.takeIf(String::isNotBlank) ?: defaultDescription(key)
    }

    fun save(descriptions: Map<String, String>): Boolean {
        if (descriptions.keys != DEFAULT_DESCRIPTIONS.keys) {
            return false
        }

        val cleaned = descriptions.mapValues { (key, value) ->
            value.trim().ifEmpty { defaultDescription(key) }
        }
        return store.write(cleaned)
    }

    private fun defaultDescription(key: String): String {
        return DEFAULT_DESCRIPTIONS[key] ?: "Set a short goal for today."
    }

    private companion object {
        val DEFAULT_DESCRIPTIONS = mapOf(
            "reading" to "Study software architecture.",
            "trading" to "Analyse the gold market.",
            "coding" to "Implement a feature.",
            "designing" to "Complete a UI design.",
        )
    }
}
