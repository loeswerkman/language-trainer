package io.github.loeswerkman.language_trainer

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform