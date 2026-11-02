package com.personal.dailyfocus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivityDescriptionsTest {
    @Test
    fun savedDescriptionsAreAvailableAfterRepositoryRecreation() {
        val store = InMemoryDescriptionStore()
        val descriptions = ActivityDescriptions(store)
        val edits = mapOf(
            "reading" to "Read a design systems chapter.",
            "trading" to "Review the gold chart.",
            "coding" to "Fix the navigation bug.",
            "designing" to "Refine the settings screen.",
        )

        assertTrue(descriptions.save(edits))

        val reopenedDescriptions = ActivityDescriptions(store)
        edits.forEach { (key, value) ->
            assertEquals(value, reopenedDescriptions.get(key))
        }
    }

    @Test
    fun incompleteSaveIsRejectedWithoutChangingSavedValues() {
        val store = InMemoryDescriptionStore()
        val descriptions = ActivityDescriptions(store)
        val initial = mapOf(
            "reading" to "Read.",
            "trading" to "Review.",
            "coding" to "Build.",
            "designing" to "Sketch.",
        )
        assertTrue(descriptions.save(initial))

        assertFalse(descriptions.save(mapOf("reading" to "Changed.")))

        assertEquals("Read.", descriptions.get("reading"))
    }

    private class InMemoryDescriptionStore : ActivityDescriptionStore {
        private val values = mutableMapOf<String, String>()

        override fun read(key: String): String? = values[key]

        override fun write(values: Map<String, String>): Boolean {
            this.values.putAll(values)
            return true
        }
    }
}
