package com.personal.dailyfocus

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class EditPrioritiesActivity : AppCompatActivity() {
    private lateinit var repository: FocusRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = FocusRepository(applicationContext)
        setContentView(R.layout.activity_edit_priorities)

        val readingInput = findViewById<EditText>(R.id.readingInput)
        val tradingInput = findViewById<EditText>(R.id.tradingInput)
        val codingInput = findViewById<EditText>(R.id.codingInput)
        val designingInput = findViewById<EditText>(R.id.designingInput)

        readingInput.setText(repository.getPriorityDescription("reading"))
        tradingInput.setText(repository.getPriorityDescription("trading"))
        codingInput.setText(repository.getPriorityDescription("coding"))
        designingInput.setText(repository.getPriorityDescription("designing"))

        findViewById<Button>(R.id.saveButton).setOnClickListener {
            val saved = repository.savePriorityDescriptions(
                mapOf(
                    "reading" to readingInput.text.toString(),
                    "trading" to tradingInput.text.toString(),
                    "coding" to codingInput.text.toString(),
                    "designing" to designingInput.text.toString(),
                ),
            )
            if (saved) {
                finish()
            } else {
                Toast.makeText(this, R.string.save_failed, Toast.LENGTH_LONG).show()
            }
        }

        findViewById<Button>(R.id.cancelButton).setOnClickListener {
            finish()
        }
    }
}
