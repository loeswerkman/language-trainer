# Welcome to the language trainer!

This repository contains a simple language learning application.

## Project vision

This project has two aims:
1. To create a simple, free interface for learning languages.
2. To create a learning ground for fullstack Kotlin programming.

For more information on the initial vision, see [initial-vision.md](docs/initial-vision.md).

## The app

The app features a minimalistic interface for learning languages.

## The code

This is a Kotlin Multiplatform project targeting Web and Server.

### Folder structure

* [/app/shared](./app/shared/src) is for code that will be shared across your Compose Multiplatform
  applications.
  It contains several subfolders:
    - [commonMain](./app/shared/src/commonMain/kotlin) is for code that’s common for all targets.
    - Other folders are for Kotlin code that will be compiled for only the platform indicated in the
      folder name.

* [/core](./core/src) is for the code that will be shared between all targets in the project.
  The most important subfolder is [commonMain](./core/src/commonMain/kotlin). If preferred, you
  can add code to the platform-specific folders here too.

* [/server](./server/src/main/kotlin) is for the Ktor server application.

* [/vocabulary](./vocabulary) contains the vocabulary, one YAML file per language named after the
  language (e.g. [vocabulary/russian.yaml](./vocabulary/russian.yaml)). These files are the source of
  truth: the server loads them into the database on startup and logs duplicates and invalid entries.
  Each entry keeps its primary English `word` and foreign-language `translation`. Optional
  `wordAlternatives: ["large"]` and `translationAlternatives: ["крупный"]` lists add accepted answers
  for the respective sides (for example, for `big` / `большой`); omitted lists default to empty.
  Primary terms remain the prompts, and alternatives are accepted only in their answer language.
  Use alternatives for the intended meaning, not loosely related or context-dependent translations.

* [/docs/adrs](./docs/adrs) contains the architecture decision records.

### Running the apps

Use the run configurations provided by the run widget in your IDE's toolbar. You can also use these
commands and options:

- Server: `./gradlew :server:run`
- Web app:
    - Wasm target (faster, modern browsers): `./gradlew :app:webApp:wasmJsBrowserDevelopmentRun`
    - JS target (slower, supports older browsers): `./gradlew :app:webApp:jsBrowserDevelopmentRun`

To practise words locally, start the server first and then the web app, and open
[http://localhost:8081](http://localhost:8081). The web app's dev server forwards `/api` requests to the
server on port 8080.

Each page has its own URL, e.g. `/russian` for the language page, `/russian/training` for training and
`/russian/vocabulary` for the vocabulary overview. The app switches pages without reloading, and the dev server
serves the app for these URLs so refreshing works.

### Training sessions and results

Opening the training page starts a new training session. Every answer is sent to the server, which
evaluates it again and saves it with the session, word, direction and result (correct, almost correct
or incorrect). "Finish session" shows how each word was answered in that session. The vocabulary page
lists every main word with its translation, category, tags, complexity, usefulness, counts over all
sessions, score (correct minus incorrect) and when it was last answered. The counts are not stored: they
are calculated from the saved answers.

### Filtering and sorting

The vocabulary table and training each have their own filter and sort per language, saved in the
browser's `localStorage` (keys `vocabulary-query.<language>` and `training-query.<language>`). "Filter"
opens a dialog with groups of conditions: every condition in a group must match (AND), and an item
matching any group is shown (OR). The funnel icon in a column header opens the same dialog with a
condition on that column. "Sort" sets one or more sort keys (any field, or Random). Clicking a column
header sorts on that column, a second click reverses the direction and a third click removes that
column from the sort again. Removing every sort key, including the default one, leaves the items in
their original order, and that choice is remembered. Both dialogs offer "Reset to default". By default the table shows all
words sorted by word (A–Z), and training shows words answered correctly fewer than 5 times, never
answered or longest ago first, then easiest first. Confirming a new filter or sort during training
reorders the cards still to come in the round, using the latest statistics.
On the vocabulary page only, "Columns" shows or hides columns; this is saved per language under
`vocabulary-columns.<language>` and doesn't affect filtering, sorting or training.

Filtering and sorting happen in the browser. The engine (`Field`, `FieldSet`, `ListQuery` in
`core/.../query`) and the table (`DataTable` in `app/shared/.../components/table`) are generic, so other
lists can define their own fields and columns. See the [filterable tables ADR](./docs/adrs/2026-10-29-10-00-filterable-tables.md).

API endpoints:

- `POST /api/sessions` with `{"language": "russian"}` starts a session
- `POST /api/sessions/{id}/answers` with `{"wordId": 1, "direction": "TO_ENGLISH", "answer": "hello"}` saves an answer
- `GET /api/sessions/{id}/results` returns the counts per word for one session
- `GET /api/vocabulary?language=russian` returns every word with its counts over all sessions and when it was last answered

The server stores its data in a SQLite database at `data/language-trainer.db` (git-ignored). Existing
databases automatically gain the English alternatives column and the training tables on startup,
preserving word IDs and Russian alternatives. There is no general migration system yet. Because the
database now holds answer history that is not in the YAML, deleting it loses all training results.
The paths can be changed with the `DATABASE_FILE` and `VOCABULARY_DIR` environment variables.

### Running tests

Use the run button in your IDE's editor gutter, or run tests using Gradle tasks:

- Core tests: `./gradlew :core:jvmTest`
- Server tests: `./gradlew :server:test`
- Web tests:
    - Wasm target: `./gradlew :app:shared:wasmJsTest`
    - JS target: `./gradlew :app:shared:jsTest`

### Further technical readings

- [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)
- [Compose Multiplatform](https://kotlinlang.org/compose-multiplatform/)
- [Kotlin/Wasm](https://kotl.in/wasm/)