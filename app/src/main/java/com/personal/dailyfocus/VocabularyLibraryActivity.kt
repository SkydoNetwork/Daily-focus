package com.personal.dailyfocus

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class VocabularyLibraryActivity : AppCompatActivity() {
    private lateinit var repository: FocusRepository
    private lateinit var content: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = FocusRepository(applicationContext)
        setContentView(R.layout.activity_vocabulary_library)
        content = findViewById(R.id.libraryContent)
        render()
    }

    override fun onResume() {
        super.onResume()
        if (::content.isInitialized) render()
    }

    private fun render() {
        content.removeAllViews()
        val mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_SAVED
        val title = findViewById<TextView>(R.id.libraryTitle)
        val description = findViewById<TextView>(R.id.libraryDescription)
        when (mode) {
            MODE_HISTORY -> {
                title.setText(R.string.vocabulary_history_title)
                description.setText(R.string.vocabulary_history_description)
                renderHistory()
            }
            MODE_REVIEW -> {
                title.setText(R.string.word_review_title)
                description.setText(R.string.word_review_description)
                renderReview()
            }
            else -> {
                title.setText(R.string.saved_words_title)
                description.setText(R.string.saved_words_description)
                renderWordList(repository.getSavedWords(), R.string.no_saved_words)
            }
        }
    }

    private fun renderHistory() {
        val history = repository.getVocabularyHistory()
        if (history.isEmpty()) {
            content.addView(bodyText(R.string.no_vocabulary_history))
            return
        }
        history.forEach { day ->
            content.addView(sectionTitle(day.date))
            renderWordList(day.words, null)
        }
    }

    private fun renderWordList(words: List<VocabularyWord>, emptyMessage: Int?) {
        if (words.isEmpty()) {
            emptyMessage?.let { content.addView(bodyText(it)) }
            return
        }
        words.forEach { word ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundResource(R.drawable.rounded_card)
                setPadding(14, 11, 14, 11)
                isClickable = true
                isFocusable = true
                setOnClickListener { openWord(word) }
                addView(TextView(this@VocabularyLibraryActivity).apply {
                    text = word.word
                    setTextColor(getColor(R.color.accent))
                    textSize = 16f
                    setTypeface(typeface, Typeface.BOLD)
                })
                addView(TextView(this@VocabularyLibraryActivity).apply {
                    text = word.meaning
                    setTextColor(getColor(R.color.text_secondary))
                    textSize = 14f
                    setPadding(0, 3, 0, 0)
                })
            }
            content.addView(row, cardLayoutParams())
        }
    }

    private fun renderReview() {
        val words = repository.getReviewWords()
        if (words.isEmpty()) {
            content.addView(bodyText(R.string.no_review_words))
            return
        }
        val question = createQuestion(words)
        content.addView(sectionTitle(question.prompt))
        val choices = RadioGroup(this).apply {
            orientation = RadioGroup.VERTICAL
            setPadding(4, 8, 4, 8)
        }
        question.options.forEachIndexed { index, option ->
            choices.addView(RadioButton(this).apply {
                id = View.generateViewId()
                text = option
                tag = index
                textSize = 15f
                setTextColor(getColor(R.color.text_primary))
            })
        }
        content.addView(choices)

        val feedback = bodyText("")
        content.addView(feedback)
        val checkButton = Button(this).apply { setText(R.string.check_answer) }
        content.addView(checkButton, cardLayoutParams())
        val nextButton = Button(this).apply {
            setText(R.string.next_review_question)
            visibility = View.GONE
        }
        content.addView(nextButton, cardLayoutParams())
        checkButton.setOnClickListener {
            val selectedIndex = choices.checkedRadioButtonId
                .takeIf { it != -1 }
                ?.let { choices.findViewById<RadioButton>(it).tag as? Int }
            if (selectedIndex == null) {
                feedback.setText(R.string.choose_review_answer)
            } else {
                val correct = selectedIndex == question.answerIndex
                feedback.setText(
                    if (correct) R.string.review_correct else R.string.review_try_again,
                )
                if (!correct) {
                    feedback.append(" ${question.options[question.answerIndex]}")
                }
                checkButton.visibility = View.GONE
                nextButton.visibility = View.VISIBLE
            }
        }
        nextButton.setOnClickListener { render() }
    }

    private fun createQuestion(words: List<VocabularyWord>): ReviewQuestion {
        val profiles = words.associateWith(VocabularyLearningContent::forWord)
        val type = (0..4).random()
        return when (type) {
            0 -> {
                val answer = words.random()
                val options = (words.filterNot { it == answer }.shuffled().take(3) + answer).shuffled()
                val meanings = options.map { it.meaning }
                ReviewQuestion(
                    getString(R.string.review_meaning_prompt, answer.word),
                    meanings,
                    meanings.indexOf(answer.meaning),
                )
            }
            1 -> {
                val answer = words.first { profiles.getValue(it).forms.isNotEmpty() }
                val form = profiles.getValue(answer).forms.first()
                val candidates = listOf("noun", "verb", "adjective", "adverb")
                    .filterNot { it == form.partOfSpeech }
                    .shuffled().take(3) + form.partOfSpeech
                val options = candidates.shuffled()
                ReviewQuestion(
                    getString(R.string.review_form_prompt, form.word, answer.word),
                    options,
                    options.indexOf(form.partOfSpeech),
                )
            }
            2 -> relatedWordQuestion(words, profiles, synonym = true)
            3 -> relatedWordQuestion(words, profiles, synonym = false)
            else -> {
                val answer = words.first { profiles.getValue(it).forms.isNotEmpty() }
                val form = profiles.getValue(answer).forms.first()
                val options = (words.flatMap { profiles.getValue(it).forms }
                    .map { it.word }.filterNot { it == form.word }.distinct().shuffled().take(3) +
                    form.word).shuffled()
                ReviewQuestion(
                    getString(
                        R.string.review_sentence_prompt,
                        form.example.replace(form.word, "_____", ignoreCase = true),
                    ),
                    options,
                    options.indexOf(form.word),
                )
            }
        }
    }

    private fun relatedWordQuestion(
        words: List<VocabularyWord>,
        profiles: Map<VocabularyWord, VocabularyProfile>,
        synonym: Boolean,
    ): ReviewQuestion {
        val answer = words.first {
            if (synonym) profiles.getValue(it).synonyms.isNotEmpty()
            else profiles.getValue(it).antonyms.isNotEmpty()
        }
        val correctWord = if (synonym) {
            profiles.getValue(answer).synonyms.first().word
        } else {
            profiles.getValue(answer).antonyms.first().word
        }
        val alternatives = words.flatMap { word ->
            val profile = profiles.getValue(word)
            (if (synonym) profile.synonyms else profile.antonyms).map { it.word }
        }.filterNot { it == correctWord }.distinct().shuffled().take(3)
        val options = (alternatives + correctWord).shuffled()
        return ReviewQuestion(
            getString(
                if (synonym) R.string.review_synonym_prompt else R.string.review_antonym_prompt,
                answer.word,
            ),
            options,
            options.indexOf(correctWord),
        )
    }

    private fun openWord(word: VocabularyWord) {
        startActivity(
            Intent(this, WordDetailsActivity::class.java)
                .putExtra(WordDetailsActivity.EXTRA_WORD, word.word),
        )
    }

    private fun sectionTitle(text: String): TextView = TextView(this).apply {
        this.text = text
        setTextColor(getColor(R.color.text_primary))
        textSize = 17f
        setTypeface(typeface, Typeface.BOLD)
        setPadding(0, 10, 0, 7)
    }

    private fun bodyText(stringId: Int): TextView = bodyText(getString(stringId))

    private fun bodyText(text: String): TextView = TextView(this).apply {
        this.text = text
        setTextColor(getColor(R.color.text_secondary))
        textSize = 14f
        setPadding(0, 8, 0, 8)
    }

    private fun cardLayoutParams() =
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply {
            bottomMargin = 8
        }

    private data class ReviewQuestion(
        val prompt: String,
        val options: List<String>,
        val answerIndex: Int,
    )

    companion object {
        const val EXTRA_MODE = "com.personal.dailyfocus.extra.VOCABULARY_MODE"
        const val MODE_SAVED = "saved"
        const val MODE_HISTORY = "history"
        const val MODE_REVIEW = "review"
    }
}
