package io.github.loeswerkman.language_trainer.components.table

import kotlin.test.Test
import kotlin.test.assertEquals

class ColumnVisibilityTest {

    private val ids = listOf("word", "translation", "score")

    @Test
    fun hidesAndShowsColumns() {
        val hidden = toggleHidden(emptySet(), "score", ids)
        assertEquals(setOf("score"), hidden)
        assertEquals(emptySet(), toggleHidden(hidden, "score", ids))
    }

    @Test
    fun keepsTheLastVisibleColumn() {
        val hidden = setOf("word", "translation")
        assertEquals(hidden, toggleHidden(hidden, "score", ids))
    }
}
