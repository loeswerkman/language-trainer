package io.github.loeswerkman.language_trainer

import io.github.loeswerkman.language_trainer.database.TrainingRepository
import io.github.loeswerkman.language_trainer.database.WordRepository
import io.github.loeswerkman.language_trainer.database.connectDatabase
import io.github.loeswerkman.language_trainer.training.AnswerSubmission
import io.github.loeswerkman.language_trainer.training.RecordedAnswer
import io.github.loeswerkman.language_trainer.training.StartSessionRequest
import io.github.loeswerkman.language_trainer.vocabulary.VocabularySync
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
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
    val database = connectDatabase(config.databaseFile)
    val repository = WordRepository(database)
    val training = TrainingRepository(database, repository)
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
        get("/api/vocabulary") {
            val language = call.request.queryParameters["language"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "The language parameter is required")
            call.respond(training.vocabularyStatistics(language))
        }
        post("/api/sessions") {
            val request = call.receive<StartSessionRequest>()
            if (request.language !in repository.findLanguages()) {
                return@post call.respond(HttpStatusCode.BadRequest, "Unknown language: ${request.language}")
            }
            call.respond(HttpStatusCode.Created, training.startSession(request.language))
        }
        post("/api/sessions/{id}/answers") {
            val session = call.parameters["id"]?.toLongOrNull()?.let(training::findSession)
                ?: return@post call.respond(HttpStatusCode.NotFound, "Session not found")
            val submission = call.receive<AnswerSubmission>()
            val result = training.recordAnswer(session, submission)
                ?: return@post call.respond(HttpStatusCode.BadRequest, "Unknown word for this session: ${submission.wordId}")
            call.respond(HttpStatusCode.Created, RecordedAnswer(result))
        }
        get("/api/sessions/{id}/results") {
            val session = call.parameters["id"]?.toLongOrNull()?.let(training::findSession)
                ?: return@get call.respond(HttpStatusCode.NotFound, "Session not found")
            call.respond(training.sessionStatistics(session.id))
        }
    }
}
