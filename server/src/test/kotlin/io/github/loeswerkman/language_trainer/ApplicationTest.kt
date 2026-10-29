package io.github.loeswerkman.language_trainer

import io.github.loeswerkman.language_trainer.training.AnswerSubmission
import io.github.loeswerkman.language_trainer.training.RecordedAnswer
import io.github.loeswerkman.language_trainer.training.TrainingSession
import io.github.loeswerkman.language_trainer.vocabulary.AnswerResult
import io.github.loeswerkman.language_trainer.vocabulary.TranslationDirection
import io.github.loeswerkman.language_trainer.vocabulary.Word
import io.github.loeswerkman.language_trainer.vocabulary.WordStatistics
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import java.nio.file.Files
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteRecursively
import kotlin.io.path.writeText
import kotlin.test.*

@OptIn(ExperimentalPathApi::class)
class ApplicationTest {

    private val tempDir = Files.createTempDirectory("application-test")
    private val config = AppConfig(
        databaseFile = tempDir.resolve("test.db"),
        vocabularyDir = tempDir.resolve("vocabulary"),
    )

    @AfterTest
    fun cleanUp() {
        tempDir.deleteRecursively()
    }

    @Test
    fun testRoot() = testApplication {
        application {
            module(config)
        }
        val response = client.get("/")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Hello, Ktor!", response.bodyAsText())
    }

    @Test
    fun loadsVocabularyFromYamlAndServesIt() = testApplication {
        config.vocabularyDir.createDirectories().resolve("russian.yaml").writeText(
            """
            - word: "cat"
              wordAlternatives: ["feline"]
              translation: "кошка"
              translationAlternatives: ["кот"]
              category: "noun"
              complexity: 1
              usefulness: 4
            - word: "cat"
              translation: "кот"
              category: "noun"
              complexity: 1
              usefulness: 4
            """.trimIndent(),
        )
        application {
            module(config)
        }

        val response = client.get("/api/words?language=russian")

        assertEquals(HttpStatusCode.OK, response.status)
        val words = Json.decodeFromString<List<Word>>(response.bodyAsText())
        assertEquals(listOf("кошка"), words.map { it.translation })
        assertEquals(listOf("feline"), words.single().wordAlternatives)
        assertEquals(listOf("кот"), words.single().translationAlternatives)
        assertEquals("[\"russian\"]", client.get("/api/languages").bodyAsText())
    }

    @Test
    fun savesAnswersPerSessionAndCountsThemPerWord() = testApplication {
        config.vocabularyDir.createDirectories().resolve("russian.yaml").writeText(
            """
            - word: "cat"
              translation: "кошка"
              category: "noun"
              complexity: 1
              usefulness: 4
            """.trimIndent(),
        )
        application {
            module(config)
        }
        val cat = Json.decodeFromString<List<Word>>(client.get("/api/words?language=russian").bodyAsText()).single()

        val sessionResponse = client.post("/api/sessions") {
            contentType(ContentType.Application.Json)
            setBody("""{"language":"russian"}""")
        }
        assertEquals(HttpStatusCode.Created, sessionResponse.status)
        val session = Json.decodeFromString<TrainingSession>(sessionResponse.bodyAsText())

        suspend fun answer(sessionId: Long, wordId: Long, text: String) = client.post("/api/sessions/$sessionId/answers") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(AnswerSubmission(wordId, TranslationDirection.TO_ENGLISH, text)))
        }

        val correct = answer(session.id, cat.id, "cat")
        assertEquals(HttpStatusCode.Created, correct.status)
        assertEquals(RecordedAnswer(AnswerResult.CORRECT), Json.decodeFromString<RecordedAnswer>(correct.bodyAsText()))
        answer(session.id, cat.id, "cot")
        answer(session.id, cat.id, "dog")

        val expected = listOf(WordStatistics(cat.id, "cat", "кошка", correct = 1, almostCorrect = 1, incorrect = 1))
        assertEquals(expected, Json.decodeFromString<List<WordStatistics>>(client.get("/api/sessions/${session.id}/results").bodyAsText()))
        assertEquals(expected, Json.decodeFromString<List<WordStatistics>>(client.get("/api/vocabulary?language=russian").bodyAsText()))

        assertEquals(HttpStatusCode.NotFound, answer(999, cat.id, "cat").status)
        assertEquals(HttpStatusCode.BadRequest, answer(session.id, 999, "cat").status)
        assertEquals(HttpStatusCode.NotFound, client.get("/api/sessions/999/results").status)
        assertEquals(HttpStatusCode.BadRequest, client.get("/api/vocabulary").status)
        val unknownLanguage = client.post("/api/sessions") {
            contentType(ContentType.Application.Json)
            setBody("""{"language":"klingon"}""")
        }
        assertEquals(HttpStatusCode.BadRequest, unknownLanguage.status)
    }
}
