package com.personal.dailyfocus

import java.time.LocalDate
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyVocabularyRotationTest {
    private val words = (1..6).map { index ->
        VocabularyWord("WORD$index", "Meaning $index.", "Example $index.")
    }

    @Test
    fun selectionIsStableForDateAndSurvivesSelectorRecreation() {
        var date = LocalDate.of(2026, 10, 25)
        val store = InMemoryVocabularyStore()
        val firstSelector = selector(store) { date }

        val firstSelection = firstSelector.getTodaysWords()
        val recreatedSelector = selector(store) { date }

        assertEquals(firstSelection, recreatedSelector.getTodaysWords())
        assertEquals(3, firstSelection.size)
        assertEquals(3, firstSelection.map { it.word }.distinct().size)
    }

    @Test
    fun dateChangeUsesEveryWordBeforeStartingANewRotation() {
        var date = LocalDate.of(2026, 10, 25)
        val store = InMemoryVocabularyStore()
        val rotation = selector(store) { date }
        val firstDay = rotation.getTodaysWords()

        date = date.plusDays(1)
        val secondDay = rotation.getTodaysWords()

        assertTrue(firstDay.map { it.word }.toSet().intersect(secondDay.map { it.word }.toSet()).isEmpty())
        assertEquals(words.map { it.word }.toSet(), store.history)
        assertEquals(3, secondDay.size)

        date = date.plusDays(1)
        val nextRotation = rotation.getTodaysWords()
        assertEquals(3, nextRotation.size)
        assertEquals(3, nextRotation.map { it.word }.distinct().size)
        assertEquals(3, store.history.size)
    }

    @Test
    fun finalUnusedWordIsIncludedBeforeRolloverRepeats() {
        var date = LocalDate.of(2026, 10, 25)
        val fourWords = words.take(4)
        val store = InMemoryVocabularyStore()
        val rotation = selector(store, fourWords) { date }
        val firstDay = rotation.getTodaysWords()

        date = date.plusDays(1)
        val secondDay = rotation.getTodaysWords()

        assertEquals(3, secondDay.size)
        assertEquals(3, secondDay.map { it.word }.distinct().size)
        assertEquals(fourWords.map { it.word }.toSet(), (firstDay + secondDay).map { it.word }.toSet())
    }

    private fun selector(
        store: InMemoryVocabularyStore,
        vocabulary: List<VocabularyWord> = words,
        today: () -> LocalDate,
    ) = DailyVocabularyRotation(vocabulary, store, today, Random(42))

    private class InMemoryVocabularyStore : DailyVocabularyStore {
        private var dateKey: String? = null
        private var selection: List<VocabularyWord>? = null
        var history: Set<String> = emptySet()
            private set

        override fun readSelection(dateKey: String): List<VocabularyWord>? =
            if (this.dateKey == dateKey) selection else null

        override fun readHistory(): Set<String> = history

        override fun saveSelection(
            dateKey: String,
            words: List<VocabularyWord>,
            history: Set<String>,
        ) {
            this.dateKey = dateKey
            selection = words.toList()
            this.history = history.toSet()
        }
    }
}
