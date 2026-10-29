package io.github.loeswerkman.language_trainer.trainer

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.loeswerkman.language_trainer.components.query.QueryState
import io.github.loeswerkman.language_trainer.currentTimeMillis
import io.github.loeswerkman.language_trainer.query.ListQuery
import io.github.loeswerkman.language_trainer.storage.ListQueryStore
import io.github.loeswerkman.language_trainer.training.AnswerSubmission
import io.github.loeswerkman.language_trainer.training.RecordedAnswer
import io.github.loeswerkman.language_trainer.training.TrainingSession
import io.github.loeswerkman.language_trainer.vocabulary.VocabularyItem
import io.github.loeswerkman.language_trainer.vocabulary.Word
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlin.random.Random

/** The server calls training needs. */
interface TrainingApi {
    suspend fun fetchVocabulary(language: String): List<VocabularyItem>
    suspend fun submitAnswer(sessionId: Long, submission: AnswerSubmission): RecordedAnswer
}

/**
 * Runs a training [session]: practises the words matching the training query in its order, saves every answer, and
 * reorders the cards still to come when the query changes. Statistics are a snapshot, refreshed when a round starts
 * or the query changes, after the answers still being saved are in. Holds Compose state, but no UI.
 */
@Stable
class TrainingController(
    private val api: TrainingApi,
    private val session: TrainingSession,
    initialItems: List<VocabularyItem>,
    store: ListQueryStore<VocabularyItem>,
    private val scope: CoroutineScope,
    private val now: () -> Long = ::currentTimeMillis,
    private val random: Random = Random.Default,
) {
    val query = QueryState(store, onChange = ::applyQuery)

    var items: List<VocabularyItem> by mutableStateOf(initialItems)
        private set
    private var round: FlashcardSession? by mutableStateOf(roundOf(initialItems, query.query))
    var answeredCount: Int by mutableStateOf(0)
        private set
    var error: String? by mutableStateOf(null)
        private set
    private var reloadJob: Job? by mutableStateOf(null)
    private val pendingSaves = mutableListOf<Job>()

    val isReloading: Boolean get() = reloadJob != null

    /** The cards to show, or null while the next round loads or when no words match the query. */
    val visibleRound: FlashcardSession? get() = round?.takeUnless { isReloading && it.isRoundFinished }

    val matchCount: Int get() = items.count { query.fields.matches(it, query.query.filter, now()) }

    fun submit(answer: String) {
        val current = checkNotNull(round) { "There is no card to answer" }
        val submission = AnswerSubmission(current.currentWord.id, current.direction, answer)
        answeredCount++
        pendingSaves += scope.launch {
            try {
                api.submitAnswer(session.id, submission)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = "Could not save your answer: ${e.message ?: e}"
            }
        }
        round = current.submit(answer)
    }

    /** Shows the next card, or starts a new round with fresh statistics after the last one. */
    fun next() {
        val current = checkNotNull(round) { "There is no card to move on from" }
        if (current.isRoundFinished) reload { round = roundOf(it, query.query) } else round = current.next()
    }

    /** Waits until every answer given so far is saved. */
    suspend fun awaitPendingSaves() = pendingSaves.joinAll()

    private fun applyQuery(next: ListQuery) = reload { latest ->
        val words = cards(latest, next)
        round = round?.withRemaining(words) ?: words.takeIf { it.isNotEmpty() }?.let { FlashcardSession.start(it) }
    }

    private fun cards(items: List<VocabularyItem>, query: ListQuery): List<Word> =
        this.query.fields.apply(items, query, now(), random).map { it.word }

    private fun roundOf(items: List<VocabularyItem>, query: ListQuery): FlashcardSession? =
        cards(items, query).takeIf { it.isNotEmpty() }?.let { FlashcardSession.start(it) }

    /** Fetches the latest statistics once pending answers are saved, then calls [update]. A newer reload cancels this one. */
    private fun reload(update: (List<VocabularyItem>) -> Unit) {
        reloadJob?.cancel()
        lateinit var job: Job
        job = scope.launch(start = CoroutineStart.LAZY) {
            try {
                awaitPendingSaves()
                val latest = api.fetchVocabulary(session.language)
                items = latest
                update(latest)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = "Could not load the words: ${e.message ?: e}"
            } finally {
                if (reloadJob === job) reloadJob = null
            }
        }
        reloadJob = job
        job.start()
    }
}
