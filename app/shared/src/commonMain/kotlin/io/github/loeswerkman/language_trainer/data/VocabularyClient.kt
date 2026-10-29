package io.github.loeswerkman.language_trainer.data

import io.github.loeswerkman.language_trainer.training.AnswerSubmission
import io.github.loeswerkman.language_trainer.training.RecordedAnswer
import io.github.loeswerkman.language_trainer.training.StartSessionRequest
import io.github.loeswerkman.language_trainer.training.TrainingSession
import io.github.loeswerkman.language_trainer.trainer.TrainingApi
import io.github.loeswerkman.language_trainer.vocabulary.VocabularyItem
import io.github.loeswerkman.language_trainer.vocabulary.Word
import io.github.loeswerkman.language_trainer.vocabulary.WordStatistics
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json

/** Talks to the Ktor server. Uses relative URLs, so the API must be served from the same origin. */
class VocabularyClient(
    private val httpClient: HttpClient = HttpClient {
        expectSuccess = true
        install(ContentNegotiation) { json() }
    },
) : TrainingApi {
    suspend fun fetchLanguages(): List<String> = httpClient.get("/api/languages").body()

    suspend fun fetchWords(language: String): List<Word> =
        httpClient.get("/api/words") { parameter("language", language) }.body()

    /** Every word of [language] with how often and when it was answered. */
    override suspend fun fetchVocabulary(language: String): List<VocabularyItem> =
        httpClient.get("/api/vocabulary") { parameter("language", language) }.body()

    suspend fun startSession(language: String): TrainingSession =
        httpClient.post("/api/sessions") {
            contentType(ContentType.Application.Json)
            setBody(StartSessionRequest(language))
        }.body()

    override suspend fun submitAnswer(sessionId: Long, submission: AnswerSubmission): RecordedAnswer =
        httpClient.post("/api/sessions/$sessionId/answers") {
            contentType(ContentType.Application.Json)
            setBody(submission)
        }.body()

    suspend fun fetchSessionResults(sessionId: Long): List<WordStatistics> =
        httpClient.get("/api/sessions/$sessionId/results").body()
}
