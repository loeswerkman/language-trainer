package io.github.loeswerkman.language_trainer.vocabulary

/** Which side of a [Word] is shown, and which side the user has to type. */
enum class TranslationDirection {
    /** Show the foreign word, answer in English. */
    TO_ENGLISH,

    /** Show the English word, answer in the foreign language. */
    FROM_ENGLISH,
}

fun Word.prompt(direction: TranslationDirection): String = when (direction) {
    TranslationDirection.TO_ENGLISH -> translation
    TranslationDirection.FROM_ENGLISH -> word
}

/** The accepted answers, with the main answer first. */
fun Word.acceptedAnswers(direction: TranslationDirection): List<String> = when (direction) {
    TranslationDirection.TO_ENGLISH -> listOf(word)
    TranslationDirection.FROM_ENGLISH -> listOf(translation) + alternatives
}
