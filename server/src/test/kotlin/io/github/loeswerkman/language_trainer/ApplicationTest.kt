package io.github.loeswerkman.language_trainer

import io.github.loeswerkman.language_trainer.vocabulary.Word
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
}
