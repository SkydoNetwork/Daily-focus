package com.personal.dailyfocus

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VocabularyLearningContentTest {
    @Test
    fun everyBundledWordHasAnExplainedProfileAndUsablePracticeContent() {
        FocusRepository.vocabulary.forEach { word ->
            val profile = VocabularyLearningContent.forWord(word)

            assertTrue(word.word, profile.detailedMeaning.isNotBlank())
            assertFalse(word.word, profile.partOfSpeech.isBlank())
            assertFalse(word.word, profile.forms.isEmpty())
            assertTrue(word.word, profile.forms.all { it.word.isNotBlank() && it.example.isNotBlank() })
            assertFalse(word.word, profile.synonyms.isEmpty())
            assertFalse(word.word, profile.antonyms.isEmpty())
            if (profile.forms.any { it.partOfSpeech == "verb" }) {
                val conjugations = requireNotNull(profile.verbConjugations)
                assertTrue(word.word, conjugations.base.isNotBlank())
                assertTrue(word.word, conjugations.thirdPersonSingular.isNotBlank())
                assertTrue(word.word, conjugations.presentParticiple.isNotBlank())
                assertTrue(word.word, conjugations.simplePast.isNotBlank())
                assertTrue(word.word, conjugations.pastParticiple.isNotBlank())
            }
        }
    }

    @Test
    fun requestedWordFamiliesIncludeTheirCommonPartsOfSpeechAndConjugations() {
        val expected = mapOf(
            "PERSUASIVE" to setOf("noun", "verb", "adjective", "adverb"),
            "COHERENT" to setOf("noun", "verb", "adjective", "adverb"),
            "PERSISTENT" to setOf("noun", "verb", "adjective", "adverb"),
        )

        expected.forEach { (word, categories) ->
            val vocabularyWord = FocusRepository.vocabulary.first { it.word == word }
            val profile = VocabularyLearningContent.forWord(vocabularyWord)
            assertTrue(word, categories.all { category -> profile.forms.any { it.partOfSpeech == category } })
            assertTrue(word, profile.verbConjugations != null)
        }

        val persuade = VocabularyLearningContent.forWord(
            FocusRepository.vocabulary.first { it.word == "PERSUASIVE" },
        ).verbConjugations
        assertTrue(persuade?.base == "persuade")
        assertTrue(persuade?.thirdPersonSingular == "persuades")
        assertTrue(persuade?.presentParticiple == "persuading")
        assertTrue(persuade?.simplePast == "persuaded")
        assertTrue(persuade?.pastParticiple == "persuaded")
    }

    @Test
    fun unfamiliarWordsFallBackToTheirStoredMeaningWithoutInventedForms() {
        val word = VocabularyWord("UNLISTED", "A locally saved definition.", "A sentence.")
        val profile = VocabularyLearningContent.forWord(word)

        assertTrue(profile.detailedMeaning == word.meaning)
        assertTrue(profile.forms.isEmpty())
        assertTrue(profile.synonyms.isEmpty())
        assertTrue(profile.antonyms.isEmpty())
    }
}
