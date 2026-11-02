package com.personal.dailyfocus

import java.time.LocalDate
import kotlin.random.Random

internal interface DailyVocabularyStore {
    fun readSelection(dateKey: String): List<VocabularyWord>?
    fun readHistory(): Set<String>
    fun saveSelection(dateKey: String, words: List<VocabularyWord>, history: Set<String>)
}

internal class DailyVocabularyRotation(
    private val vocabulary: List<VocabularyWord>,
    private val store: DailyVocabularyStore,
    private val todayProvider: () -> LocalDate,
    private val random: Random = Random.Default,
) {
    private val uniqueVocabulary = vocabulary.distinctBy { it.word }

    init {
        require(uniqueVocabulary.size >= 3) {
            "The vocabulary collection must contain at least three distinct words."
        }
    }

    fun getTodaysWords(): List<VocabularyWord> {
        val todayKey = todayProvider().toString()
        val saved = store.readSelection(todayKey)
        saved?.takeIf { it.isValidSelection() }?.let {
            return it
        }

        val vocabularyNames = uniqueVocabulary.mapTo(HashSet()) { it.word }
        val history = store.readHistory().intersect(vocabularyNames)
        val unused = uniqueVocabulary.filterNot { it.word in history }
        val selected: List<VocabularyWord>
        val nextHistory: Set<String>

        if (unused.size >= WORDS_PER_DAY) {
            selected = unused.shuffled(random).take(WORDS_PER_DAY)
            nextHistory = history + selected.map { it.word }
        } else {
            val unusedNames = unused.mapTo(HashSet()) { it.word }
            val usedWords = uniqueVocabulary.filterNot { it.word in unusedNames }
            val rolloverWords = usedWords.shuffled(random).take(WORDS_PER_DAY - unused.size)
            selected = unused + rolloverWords
            nextHistory = selected.map { it.word }.toSet()
        }

        store.saveSelection(todayKey, selected, nextHistory)
        return selected
    }

    private fun List<VocabularyWord>?.isValidSelection(): Boolean {
        return this != null &&
            size == WORDS_PER_DAY &&
            map { it.word }.distinct().size == WORDS_PER_DAY &&
            all { selected -> uniqueVocabulary.any { it.word == selected.word } }
    }

    private companion object {
        const val WORDS_PER_DAY = 3
    }
}
