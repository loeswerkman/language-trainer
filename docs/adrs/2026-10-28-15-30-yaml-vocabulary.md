# ADR: Maintain vocabulary in YAML files (initial approach)

- **Status**: Accepted (intended as a temporary, initial approach)
- **Date**: 2026-10-28

## Context

The app needs a way to add and edit words. The first priority is growing the vocabulary quickly while keeping the project simple. Building an admin UI, import pipeline or API for editing words would be a lot of work before the core trainer is proven. See the [initial vision](../initial-vision.md).

## Decision

- Vocabulary lives in one YAML file per language in `vocabulary/` (e.g. `vocabulary/russian.yaml`), and is edited by hand by the developer.
- The YAML files are the **source of truth**. On startup the server validates them (required fields, duplicates on word + category) and syncs them into the SQLite `words` table (see the [SQLite ADR](2026-10-28-13-15-sqlite.md)).
- Each entry has `word`, `translation`, `category`, `complexity` and `usefulness`, plus optional `wordAlternatives`, `translationAlternatives`, `tags` and `notes`. The field reference is documented in the header of each YAML file.
- Words are changed by editing the file and restarting the server. Changes go through git, so they are reviewed and versioned like code.

## Why YAML

- **Easy to read and write**: a developer can add a word in a text editor, with comments (e.g. why an alternative is formal or informal) next to the data.
- **No extra tooling**: no admin UI, API or database client is needed to grow the vocabulary.
- **Version control**: diffs, history and review come for free.
- **Rebuildable database**: since the database is derived from the YAML, it can be deleted and recreated at any time.

## Alternatives considered

- **JSON**: no comments and noisier syntax; worse for hand editing.
- **CSV**: poor fit for list fields (alternatives, tags) and long notes.
- **Editing the database directly / admin UI**: more work up front and the data would no longer live in the repository.

## Consequences

- **Positive**: very fast to start, transparent, reviewable and trivially reproducible.
- **Negative**:
  - Only the developer can add words; there is no way for users to contribute or for words to be added without a redeploy/restart.
  - Large files become hard to navigate, and mistakes (typos, inconsistent categories) are only caught at server startup.
  - Syncing deletes words removed from the YAML, so any data that references a word (e.g. learning progress) needs stable identifiers. Today a word is identified by word + category, so renaming a word looks like a delete plus an insert.
  - No tooling for bulk import, quality checks (e.g. audio, example sentences) or multiple contributors.

## Future

This approach is **expected to be replaced or complemented** as the app grows. Likely triggers:

- Learning progress (SRS) is stored per word, which requires stable word IDs independent of the text.
- More languages, or a vocabulary too large to manage by hand.
- Wanting to add words without a restart/redeploy, or letting others contribute.

Possible directions are a vocabulary editor in the app backed by the database (making the database the source of truth, with YAML as an export/seed), or import tools for existing word lists. A new ADR should be written when that decision is made. Until then, keep the YAML format simple so it is easy to migrate.
