# Project Language Trainer: Initial Vision & Architecture Summary

1. **Vision Statement**

    - The app is a **personal, minimalist language learning application** designed for a single user to track and accelerate their vocabulary acquisition.
    - It serves as a **self-hosted training tool**, prioritising **simplicity**, **low cost**, and a **high-quality developer experience**.
    - The project is open-source and serves as a learning opportunity for full-stack Kotlin development.


2. **Guiding Principles**

    - **Developer-centric**: Besides the main use to aid language learning, this application also serves as training ground to create a well-structured, testable, and maintainable Kotlin codebase, following clean coding conventions.
    - **Single-user focus**: The application is built for one user only. Multi-user support and authentication are explicitly out of scope for the initial versions.
    - **Simplicity & low cost**: The architecture and technology choices prioritise simplicity of deployment and minimal operational cost, leveraging free or low-cost hosting tiers.
    - **Full-Stack kotlin**: The project is a vehicle to learn and apply Kotlin across the entire technology stack—from backend to frontend.
    - **Progressive development**: Start with a functional web application, with a clear path to a native mobile app in the future.


3. **Core Architecture: Kotlin Multiplatform (KMP)**

   The project is architected using Kotlin Multiplatform to maximise code reuse and ensure a smooth transition to a native mobile app in the future.

    - **Framework**: Kotlin Multiplatform (KMP) is the foundational technology.
    - **Backend**: A lightweight server built with the Ktor framework, responsible for serving the API and managing the database.

    - **Frontend (Web)**: A web-based user interface built with Compose for Web, enabling a shared UI paradigm with the future mobile application.
    - **Modular Structure**:
        - **shared module**: Contains the core business logic, data models, and learning algorithms (e.g., SRS). This code is pure Kotlin and is shared across all platforms.
        - **server module**: The Ktor application that provides the REST API.
        - **webapp module**: The Compose for Web user interface.


4. **Data Management & Workflow**

    - **Source of truth for vocabulary**: Word lists will be managed in human-readable YAML files checked into the Git repository. This is preferred over Excel for its structured nature and version-control-friendliness.
    - **Application database**: A SQLite database will be used for its simplicity, serverless nature, and file-based storage, which is ideal for a single-user, self-hosted application.
    - **Data sync workflow**: A script within the Ktor server will parse the YAML files on startup to populate and synchronise the vocabulary in the SQLite database.


5. **User Experience (UX) & Core Logic**

    - **Learning methodology**: The core learning mechanic will be a Spaced Repetition System (SRS), loosely based on the SM-2 algorithm. This will intelligently schedule words for review based on user performance, automating the learning process.
    - **Core user loop**: The primary interaction is a simple flashcard loop: (1) The user is presented with a word. (2) The user types the translation. (3) The system evaluates the answer and provides immediate feedback. (4) The system schedules the word for its next review.
    - **Answer evaluation**: User input will be compared against the correct translation using the Levenshtein distance algorithm. A distance of 1 will be considered "almost correct," while a distance greater than 1 will be "incorrect."


6. **Key UI Features**

    - **Flashcard interface**: A minimal UI to display the word and accept user input.
    - **Easily adjustable word list**: It should be easy to add, remove, or modify words in the learning list.
    - **Optional on-screen keyboard**: A toggleable keyboard to assist with typing special characters for specific languages (e.g., Cyrillic).
    - **Hint system**: A feature allowing the user to reveal letters of the correct answer, with associated penalties.
    - **Progress dashboard**: A simple view to see statistics on learned words, struggling words, and unseen words.


7. **Near-future envisioned enhancements**

    - **Native mobile App (Android/iOS)**: The primary future goal, enabled by the KMP architecture.
    - **Grammar trainer**: A more complex training mode to be considered after the core vocabulary trainer is complete.


8. **Out of Scope**

    - **Storing user data**: Any form of user data is explicitly kept out of the new versions to keep the app simple and reduce privacy risks.
    - **Advanced Analytics & Event Logging**: A detailed event log was considered but explicitly deferred to maintain initial simplicity.
