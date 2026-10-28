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

Each page has its own URL, e.g. `/russian` for the language page and `/russian/training` for training. The app
switches pages without reloading, and the dev server serves the app for these URLs so refreshing works.

The server stores its data in a SQLite database at `data/language-trainer.db` (git-ignored). There are no
migrations yet: after a schema change, delete this file and restart the server to rebuild it from the YAML.
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