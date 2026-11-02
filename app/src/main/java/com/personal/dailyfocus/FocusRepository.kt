package com.personal.dailyfocus

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.time.LocalDate
import kotlin.random.Random

data class VocabularyWord(
    val word: String,
    val meaning: String,
    val example: String,
)

data class VocabularyDay(
    val date: String,
    val words: List<VocabularyWord>,
)

class FocusRepository(
    context: Context,
    private val todayProvider: () -> LocalDate = { LocalDate.now() },
    random: Random = Random.Default,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val activityDescriptions = ActivityDescriptions(
        object : ActivityDescriptionStore {
            override fun read(key: String): String? = prefs.getString("priority_$key", null)

            override fun write(values: Map<String, String>): Boolean {
                val editor = prefs.edit()
                values.forEach { (key, value) ->
                    editor.putString("priority_$key", value)
                }
                return editor.commit()
            }
        },
    )
    private val vocabularyRotation = DailyVocabularyRotation(
        vocabulary,
        SharedPreferencesVocabularyStore(prefs),
        todayProvider,
        random,
    )

    fun getTodaysWords(): List<VocabularyWord> = vocabularyRotation.getTodaysWords()

    fun getWord(word: String): VocabularyWord? =
        vocabulary.firstOrNull { it.word.equals(word, ignoreCase = true) }

    fun getSavedWords(): List<VocabularyWord> =
        prefs.getString(KEY_SAVED_WORDS, null)
            ?.let(VocabularyStorageCodec::decodeWords)
            .orEmpty()
            .sortedBy { it.word }

    fun isWordSaved(word: String): Boolean =
        getSavedWords().any { it.word.equals(word, ignoreCase = true) }

    fun setWordSaved(word: VocabularyWord, saved: Boolean): Boolean {
        val savedWords = getSavedWords().associateBy { it.word }.toMutableMap()
        if (saved) {
            savedWords[word.word] = word
        } else {
            savedWords.remove(word.word)
        }
        return prefs.edit()
            .putString(KEY_SAVED_WORDS, VocabularyStorageCodec.encodeWords(savedWords.values.toList()))
            .commit()
    }

    fun getVocabularyHistory(): List<VocabularyDay> {
        val history = prefs.getString(KEY_DAILY_HISTORY, null)
            ?.let(VocabularyStorageCodec::decodeHistory)
            .orEmpty()
            .toMutableMap()
        val currentDate = todayKey()
        val currentWords = prefs.getString(KEY_WORDS, null)
            ?.let(VocabularyStorageCodec::decodeWords)
            .orEmpty()
        if (currentWords.size == WORDS_PER_DAY) {
            val storedDate = prefs.getString(KEY_WORDS_DATE, currentDate) ?: currentDate
            history[storedDate] = currentWords
        }
        return history.entries
            .sortedByDescending { it.key }
            .map { (date, words) -> VocabularyDay(date, words) }
    }

    fun getReviewWords(): List<VocabularyWord> {
        val unique = linkedMapOf<String, VocabularyWord>()
        getVocabularyHistory().forEach { day -> day.words.forEach { unique[it.word] = it } }
        getSavedWords().forEach { unique[it.word] = it }
        getTodaysWords().forEach { unique[it.word] = it }
        return unique.values.toList()
    }

    fun shouldOpenOnUnlockToday(): Boolean {
        return prefs.getString(KEY_LAST_SHOWN_DATE, null) != todayKey()
    }

    fun markTodayShown(): Boolean =
        prefs.edit().putString(KEY_LAST_SHOWN_DATE, todayKey()).commit()

    fun isScreenOnDisplayEnabled(): Boolean =
        prefs.getBoolean(KEY_SCREEN_ON_DISPLAY_ENABLED, false)

    fun setScreenOnDisplayEnabled(enabled: Boolean): Boolean =
        prefs.edit().putBoolean(KEY_SCREEN_ON_DISPLAY_ENABLED, enabled).commit()

    fun getPriorityDescription(key: String): String = activityDescriptions.get(key)

    fun savePriorityDescriptions(descriptions: Map<String, String>): Boolean =
        activityDescriptions.save(descriptions)

    private fun todayKey(): String = todayProvider().toString()

    private class SharedPreferencesVocabularyStore(
        private val prefs: SharedPreferences,
    ) : DailyVocabularyStore {
        override fun readSelection(dateKey: String): List<VocabularyWord>? {
            if (prefs.getString(KEY_WORDS_DATE, null) != dateKey) {
                return null
            }
            return prefs.getString(KEY_WORDS, null)?.let(VocabularyStorageCodec::decodeWords)
        }

        override fun readHistory(): Set<String> {
            val storedHistory = prefs.getString(KEY_HISTORY, null)
            val rawHistory = storedHistory ?: prefs.getString(KEY_RECENT_WORDS, "").orEmpty()
            return rawHistory.split('|').filter(String::isNotBlank).toSet()
        }

        override fun saveSelection(
            dateKey: String,
            words: List<VocabularyWord>,
            history: Set<String>,
        ) {
            val savedHistory = VocabularyStorageCodec.decodeHistory(
                prefs.getString(KEY_DAILY_HISTORY, null),
            )
                .toMutableMap()
            val previousDate = prefs.getString(KEY_WORDS_DATE, null)
            val previousWords = prefs.getString(KEY_WORDS, null)
                ?.let(VocabularyStorageCodec::decodeWords)
                .orEmpty()
            if (previousDate != null && previousWords.size == WORDS_PER_DAY) {
                savedHistory[previousDate] = previousWords
            }
            savedHistory[dateKey] = words
            prefs.edit()
                .putString(KEY_WORDS_DATE, dateKey)
                .putString(KEY_WORDS, VocabularyStorageCodec.encodeWords(words))
                .putString(KEY_HISTORY, history.joinToString("|"))
                .putString(KEY_RECENT_WORDS, history.joinToString("|"))
                .putString(KEY_DAILY_HISTORY, VocabularyStorageCodec.encodeHistory(savedHistory))
                .apply()
        }
    }

    companion object {
        const val PREFS_NAME = "daily_focus_prefs"
        const val KEY_WORDS = "daily_words"
        const val KEY_WORDS_DATE = "daily_words_date"
        const val KEY_RECENT_WORDS = "recent_words"
        const val KEY_HISTORY = "vocabulary_history"
        const val KEY_LAST_SHOWN_DATE = "last_daily_display_date"
        const val KEY_SCREEN_ON_DISPLAY_ENABLED = "screen_on_display_enabled"
        const val KEY_DAILY_HISTORY = "daily_vocabulary_history"
        const val KEY_SAVED_WORDS = "saved_vocabulary_words"
        private const val WORDS_PER_DAY = 3

        internal val vocabulary = listOf(
        VocabularyWord("METICULOUS", "Paying careful attention to details.", "She is meticulous when reviewing her work."),
        VocabularyWord("RESILIENT", "Able to recover quickly from difficulties.", "He remained resilient after the setback."),
        VocabularyWord("ARTICULATE", "Able to express ideas clearly.", "She articulated her opinion confidently."),
        VocabularyWord("PRAGMATIC", "Dealing with things sensibly and realistically.", "He took a pragmatic approach to the problem."),
        VocabularyWord("CURIOUS", "Eager to learn or know something.", "The curious student asked many questions."),
        VocabularyWord("VIGOROUS", "Full of energy and strength.", "The team made a vigorous effort to finish on time."),
        VocabularyWord("PERSUASIVE", "Good at convincing others.", "Her persuasive speech won support."),
        VocabularyWord("THOUGHTFUL", "Showing consideration for others.", "He gave a thoughtful response."),
        VocabularyWord("CONCISE", "Expressing much in a few words.", "Her summary was clear and concise."),
        VocabularyWord("RESOURCEFUL", "Able to find quick and clever solutions.", "He was resourceful during the outage."),
        VocabularyWord("ENTHUSIASTIC", "Showing energetic excitement.", "She was enthusiastic about the new project."),
        VocabularyWord("STEADFAST", "Very loyal or determined.", "He stayed steadfast through the challenge."),
        VocabularyWord("EMPATHETIC", "Showing understanding and compassion.", "The manager was empathetic and patient."),
        VocabularyWord("COHERENT", "Logical and easy to understand.", "Her argument was coherent and well structured."),
        VocabularyWord("INNOVATIVE", "Introducing new ideas or methods.", "The team had an innovative solution."),
        VocabularyWord("CANDID", "Honest and straightforward.", "His candid feedback was useful."),
        VocabularyWord("BALANCED", "Well proportioned and calm.", "She maintained a balanced perspective."),
        VocabularyWord("INSIGHTFUL", "Having deep understanding.", "His insightful comment clarified the issue."),
        VocabularyWord("FOCUSED", "Concentrating clearly on one objective.", "She stayed focused during the presentation."),
        VocabularyWord("ADAPTABLE", "Able to change easily to suit conditions.", "The team was adaptable to new demands."),
        VocabularyWord("DISCIPLINED", "Showing control and self-mastery.", "He is disciplined about his routine."),
        VocabularyWord("REFLECTIVE", "Thoughtful and contemplative.", "She was reflective after the meeting."),
        VocabularyWord("VIVID", "Producing strong, clear images.", "The story had a vivid description."),
        VocabularyWord("PRECISION", "The quality of being exact and accurate.", "The report needed more precision."),
        VocabularyWord("TACTFUL", "Showing sensitivity and good judgment.", "She was tactful in a difficult conversation."),
        VocabularyWord("FLEXIBLE", "Able to bend or change without breaking.", "The plan remained flexible."),
        VocabularyWord("ELOQUENT", "Fluent and persuasive in speaking or writing.", "His eloquent speech inspired the team."),
        VocabularyWord("PERSISTENT", "Continuing steadily despite difficulties.", "Her persistent efforts paid off."),
        VocabularyWord("ADEPT", "Very skilled and competent.", "He is adept at explaining technical ideas."),
        VocabularyWord("STRATEGIC", "Relating to long-term planning.", "The leadership team made a strategic decision."),
        VocabularyWord("SINCERE", "Honest and genuine.", "His sincere apology was appreciated."),
        VocabularyWord("EFFICIENT", "Working in a well-organized and productive way.", "This process is more efficient than before."),
        VocabularyWord("DYNAMIC", "Full of energy and activity.", "The market is highly dynamic."),
        VocabularyWord("CLARITY", "The quality of being easy to understand.", "Clarity is essential in communication."),
        )
    }
}

private object VocabularyStorageCodec {
    fun encodeWords(words: List<VocabularyWord>): String = JSONArray().apply {
        words.forEach { word ->
            put(JSONObject().put("word", word.word).put("meaning", word.meaning).put("example", word.example))
        }
    }.toString()

    fun decodeWords(raw: String): List<VocabularyWord> = try {
        val array = JSONArray(raw)
        (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            val word = item.optString("word").takeIf(String::isNotBlank) ?: return@mapNotNull null
            VocabularyWord(word, item.optString("meaning"), item.optString("example"))
        }
    } catch (exception: JSONException) {
        raw.split("||").mapNotNull { entry ->
            val parts = entry.split("::")
            if (parts.size == 3) VocabularyWord(parts[0], parts[1], parts[2]) else null
        }
    }

    fun encodeHistory(history: Map<String, List<VocabularyWord>>): String = JSONArray().apply {
        history.forEach { (date, words) ->
            put(JSONObject().put("date", date).put("words", JSONArray(encodeWords(words))))
        }
    }.toString()

    fun decodeHistory(raw: String?): Map<String, List<VocabularyWord>> {
        if (raw.isNullOrBlank()) return emptyMap()
        return try {
            val array = JSONArray(raw)
            buildMap {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val date = item.optString("date").takeIf(String::isNotBlank) ?: continue
                    val words = decodeWords(item.optJSONArray("words")?.toString().orEmpty())
                    if (words.isNotEmpty()) put(date, words)
                }
            }
        } catch (_: JSONException) {
            emptyMap()
        }
    }
}
