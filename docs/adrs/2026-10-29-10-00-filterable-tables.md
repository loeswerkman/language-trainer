# ADR: Build our own filterable, sortable tables

- **Status**: Accepted
- **Date**: 2026-10-29

## Context

The vocabulary page shows a table of words with their statistics. Users want to filter it (single conditions, or
several combined with AND/OR) and sort it on any field, and the **same** filter and sort must decide which words
are trained and in which order. Later views, such as an alphabet and a grammar overview, will show other kinds of
items with other columns and will need the same abilities.

We looked at existing Compose Multiplatform table libraries that support the web targets (JS and Wasm):

- [sproctor/compose-data-table](https://github.com/sproctor/compose-data-table): layout of a data table (headers,
  column widths, pagination), with sorting left to the app and no filtering.
- [windedge/compose-table](https://github.com/windedge/compose-table) and compose-material-data-table: similar,
  layout-focused, with little or no filtering.

None of them offers a filter model with AND/OR groups, and none can apply its filter and sort to something that
isn't a table, such as the order of flashcards.

## Decision

Build the filtering, sorting and table ourselves, split into reusable layers:

- **Query engine (core, no UI)**: `ListQuery` (a `Filter` of OR-ed `ConditionGroup`s of AND-ed `Condition`s, plus a
  list of `SortKey`s, including `Random`), `Field` types (text, choice, tags, number, date) that decide their
  operators, matching and comparing, and `FieldSet`, which filters, sorts, sanitizes and describes queries for a list.
  Each list defines its fields once, e.g. `VocabularyFields`.
- **UI components (app)**: a generic `DataTable` whose columns may be linked to a field (clickable header to sort,
  funnel icon to filter), and generic filter and sort dialogs plus a toolbar that work with any `FieldSet`. Fields
  supply their own value options (`Field.options`), so the dialogs need no knowledge of concrete field types.
- **Query state (app)**: `QueryState` owns the current query, the open dialog and header-click sorting, and saves every
  change through a `ListQueryStore`. Each view creates one; the dialogs and toolbar only talk to it. Training logic
  lives in a `TrainingController` (behind a `TrainingApi` interface) that reacts to query changes, so it can be tested
  without UI.
- **Separate saved queries per view**: the vocabulary table (`vocabulary-query.<language>`) and training
  (`training-query.<language>`) each save their own query in `localStorage`, using the same engine and dialogs. Browsing
  the table shouldn't silently change what is trained.
- **Explicit defaults**: each view has a complete default `ListQuery` (`VocabularyQueries`), used when nothing is saved
  and offered by "Reset to default". An empty sort means original order and is saved like any other choice, so a user can turn the default sort
  off and it stays off; there is no hidden "null means default".
  - Table: no filter, sorted by word A–Z.
  - Training: correct < 5 (words not yet learned), sorted by last answered ascending (never answered or answered
    longest ago first), then complexity ascending (easiest first).
- **Stable persisted format**: operators and sort directions have explicit `@SerialName`s, and field ids are part of
  the saved format, so renaming Kotlin code doesn't break saved queries. A test pins the JSON.
- **Client-side**: the server returns all items with their statistics; filtering and sorting happen in the browser.

## Why

- The hard part, a shared filter and sort model, isn't provided by any library, so a library would only save the
  table layout, which is little code.
- One model and one set of dialogs for both the table and training keeps their filtering and sorting identical, while
  each view keeps its own query and default.
- The generic `Field`/`FieldSet`/`DataTable` split lets future views reuse everything by defining fields and columns.
- No extra dependency whose web support and styling we'd have to track.

## Consequences

- **Positive**: full control over the UX; the query engine, query state and training controller are plain Kotlin and
  well tested; new lists only need a field set, columns and a default query.
- **Negative**:
  - We maintain the table ourselves: no built-in pagination, column resizing or virtualization of columns.
  - Client-side filtering loads all items. That is fine for hundreds or a few thousand words, but large lists would
    need server-side filtering (the serializable `ListQuery` could then be sent to the server).
  - Saved queries live in one browser only and are lost when its storage is cleared.
  - Saved queries may refer to fields that later change; `FieldSet.sanitize` drops what no longer applies, and a query
    that can't be decoded falls back to the default. Field ids and serial names must not be renamed casually.
  - A saved query stores its sort explicitly, so changing a default in code only affects users without a saved query
    (or who reset to default).
