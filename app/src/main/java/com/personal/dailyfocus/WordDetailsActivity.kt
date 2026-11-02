package com.personal.dailyfocus

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class WordDetailsActivity : AppCompatActivity() {
    private lateinit var repository: FocusRepository
    private lateinit var vocabularyWord: VocabularyWord

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = FocusRepository(applicationContext)
        val selectedWord = intent.getStringExtra(EXTRA_WORD)
        vocabularyWord = selectedWord?.let(repository::getWord) ?: run {
            finish()
            return
        }

        setContentView(R.layout.activity_word_details)
        val profile = VocabularyLearningContent.forWord(vocabularyWord)
        findViewById<TextView>(R.id.detailWord).text = vocabularyWord.word
        findViewById<TextView>(R.id.detailPartOfSpeech).text = profile.partOfSpeech
        findViewById<TextView>(R.id.detailMeaning).text = profile.detailedMeaning
        findViewById<TextView>(R.id.detailExample).text = vocabularyWord.example
        populateForms(profile.forms)
        populateVerbConjugations(profile.verbConjugations)
        populateRelatedWords(R.id.detailSynonyms, profile.synonyms)
        populateRelatedWords(R.id.detailAntonyms, profile.antonyms)
        refreshBookmarkButton()

        findViewById<Button>(R.id.saveWordButton).setOnClickListener {
            val saved = !repository.isWordSaved(vocabularyWord.word)
            if (repository.setWordSaved(vocabularyWord, saved)) {
                refreshBookmarkButton()
            } else {
                Toast.makeText(this, R.string.save_word_failed, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun refreshBookmarkButton() {
        findViewById<Button>(R.id.saveWordButton).setText(
            if (repository.isWordSaved(vocabularyWord.word)) {
                R.string.remove_saved_word
            } else {
                R.string.save_word
            },
        )
    }

    private fun populateForms(forms: List<WordForm>) {
        val container = findViewById<LinearLayout>(R.id.detailForms)
        if (forms.isEmpty()) {
            container.addView(message(R.string.no_word_forms))
            return
        }
        forms.forEach { form ->
            container.addView(card("${form.word}  ·  ${form.partOfSpeech}", form.meaning, form.example))
        }
    }

    private fun populateVerbConjugations(conjugations: VerbConjugations?) {
        val section = findViewById<LinearLayout>(R.id.verbConjugationSection)
        if (conjugations == null) return

        section.visibility = View.VISIBLE
        val container = findViewById<LinearLayout>(R.id.detailVerbConjugations)
        listOf(
            getString(R.string.verb_base) to conjugations.base,
            getString(R.string.verb_third_person) to conjugations.thirdPersonSingular,
            getString(R.string.verb_present_participle) to conjugations.presentParticiple,
            getString(R.string.verb_simple_past) to conjugations.simplePast,
            getString(R.string.verb_past_participle) to conjugations.pastParticiple,
        ).forEach { (label, form) ->
            container.addView(card(label, form, ""))
        }
    }

    private fun populateRelatedWords(containerId: Int, words: List<RelatedWord>) {
        val container = findViewById<LinearLayout>(containerId)
        if (words.isEmpty()) {
            container.addView(message(R.string.no_related_words))
            return
        }
        words.forEach { related ->
            container.addView(card(related.word, related.explanation, related.example))
        }
    }

    private fun message(textId: Int): TextView = TextView(this).apply {
        setText(textId)
        setTextColor(getColor(R.color.text_secondary))
        textSize = 14f
        setPadding(0, 4, 0, 4)
    }

    private fun card(title: String, description: String, example: String): View =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.rounded_card)
            setPadding(14, 12, 14, 12)
            val titleView = TextView(this@WordDetailsActivity).apply {
                text = title
                setTextColor(getColor(R.color.accent))
                textSize = 15f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }
            addView(titleView)
            addView(TextView(this@WordDetailsActivity).apply {
                text = description
                setTextColor(getColor(R.color.text_secondary))
                textSize = 14f
                setPadding(0, 4, 0, 0)
            })
            if (example.isNotBlank()) {
                addView(TextView(this@WordDetailsActivity).apply {
                    text = example
                    setTextColor(getColor(R.color.text_secondary))
                    textSize = 13f
                    setPadding(0, 4, 0, 0)
                })
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                bottomMargin = 8
                gravity = Gravity.CENTER_VERTICAL
            }
        }

    companion object {
        const val EXTRA_WORD = "com.personal.dailyfocus.extra.WORD"
    }
}
