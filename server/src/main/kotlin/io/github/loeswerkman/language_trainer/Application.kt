package io.github.loeswerkman.language_trainer

import io.github.loeswerkman.language_trainer.database.WordRepository
import io.github.loeswerkman.language_trainer.database.connectDatabase
import io.github.loeswerkman.language_trainer.vocabulary.VocabularySync
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.nio.file.Path

data class AppConfig(val databaseFile: Path, val vocabularyDir: Path) {
    companion object {
        fun fromEnvironment() = AppConfig(
            databaseFile = Path.of(System.getenv("DATABASE_FILE") ?: "data/language-trainer.db"),
            vocabularyDir = Path.of(System.getenv("VOCABULARY_DIR") ?: "vocabulary"),
        )
    }
}

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") { module() }
        .start(wait = true)
}

fun Application.module(config: AppConfig = AppConfig.fromEnvironment()) {
    val repository = WordRepository(connectDatabase(config.databaseFile))
    VocabularySync(config.vocabularyDir, repository).syncAll()

    install(ContentNegotiation) {
        json()
    }
    routing {
        get("/") {
            call.respondText(sayHello("Ktor"))
        }
        get("/api/languages") {
            call.respond(repository.findLanguages())
        }
        get("/api/words") {
            call.respond(repository.findAll(call.request.queryParameters["language"]))
        }
    }
}
