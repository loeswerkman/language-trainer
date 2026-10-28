package io.github.loeswerkman.language_trainer.vocabulary

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class WordCategory(val label: String) {
    @SerialName("noun") NOUN("noun"),
    @SerialName("verb") VERB("verb"),
    @SerialName("adjective") ADJECTIVE("adjective"),
    @SerialName("adverb") ADVERB("adverb"),
    @SerialName("pronoun") PRONOUN("pronoun"),
    @SerialName("preposition") PREPOSITION("preposition"),
    @SerialName("conjunction") CONJUNCTION("conjunction"),
    @SerialName("numeral") NUMERAL("numeral"),
    @SerialName("interjection") INTERJECTION("interjection"),
    @SerialName("phrase") PHRASE("phrase"),
}
