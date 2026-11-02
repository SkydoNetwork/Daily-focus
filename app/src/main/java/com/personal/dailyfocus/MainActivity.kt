package com.personal.dailyfocus

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var repository: FocusRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = FocusRepository(applicationContext)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        setContentView(R.layout.activity_main)
        refreshFocusContent()
    }

    override fun onResume() {
        super.onResume()
        if (::repository.isInitialized) {
            showTodaysFocus()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        showTodaysFocus()
    }

    private fun showTodaysFocus() {
        if (!repository.markTodayShown()) {
            Log.e(TAG, "Could not persist the date for today's display.")
        }
        refreshFocusContent()
    }

    private fun refreshFocusContent() {
        val words = repository.getTodaysWords()
        bindWord(R.id.wordOne, R.id.meaningOne, R.id.exampleOne, words.getOrNull(0))
        bindWord(R.id.wordTwo, R.id.meaningTwo, R.id.exampleTwo, words.getOrNull(1))
        bindWord(R.id.wordThree, R.id.meaningThree, R.id.exampleThree, words.getOrNull(2))
        listOf(R.id.wordOne, R.id.wordTwo, R.id.wordThree).forEachIndexed { index, viewId ->
            val word = words.getOrNull(index) ?: return@forEachIndexed
            findViewById<TextView>(viewId).setOnClickListener {
                startActivity(
                    Intent(this, WordDetailsActivity::class.java)
                        .putExtra(WordDetailsActivity.EXTRA_WORD, word.word),
                )
            }
        }

        findViewById<TextView>(R.id.priorityReading).text = repository.getPriorityDescription("reading")
        findViewById<TextView>(R.id.priorityTrading).text = repository.getPriorityDescription("trading")
        findViewById<TextView>(R.id.priorityCoding).text = repository.getPriorityDescription("coding")
        findViewById<TextView>(R.id.priorityDesigning).text = repository.getPriorityDescription("designing")
    }

    private fun bindWord(wordId: Int, meaningId: Int, exampleId: Int, word: VocabularyWord?) {
        val wordView = findViewById<TextView>(wordId)
        val meaningView = findViewById<TextView>(meaningId)
        val exampleView = findViewById<TextView>(exampleId)

        if (word == null) {
            wordView.text = ""
            meaningView.text = ""
            exampleView.text = ""
            return
        }

        wordView.text = word.word
        meaningView.text = getString(
            R.string.word_meaning,
            getString(R.string.meaning_label),
            word.meaning,
        )
        exampleView.text = getString(
            R.string.word_example,
            getString(R.string.example_label),
            word.example,
        )
    }

    private companion object {
        const val TAG = "DailyFocus"
    }
}
