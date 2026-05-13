# ADR 0029: Default prompts seeded at author creation, not via repeatable migration

* Status: accepted
* Date: 2026-05-13

## Context

Spec §7a.6 calls for default prompt templates per author. The
iteration plan suggested a Flyway repeatable migration
`R__seed_default_prompts.sql` plus a service-layer hook on first
login. In practice, repeatable migrations cannot easily target
"every existing author" without holding the whole user list in SQL,
and the first-login hook duplicates the path that already runs at
account creation.

## Decision

* Default prompts are seeded inside `UserAccountService.createLocalAuthor(...)`
  immediately after the account is persisted, by iterating over
  `LlmPurpose.values()` and calling
  `PromptTemplateService.saveNewVersion(...)` once per purpose.
* Default content lives as Java string constants in
  `DefaultPromptContent`. A unit test (`DefaultPromptContentTest`)
  asserts that every default contains the placeholders required by
  `PromptPlaceholders` for its purpose, so a future edit can not
  ship a default that the validator would reject.

## Rationale

* The seed is part of the "create author" transaction, so an author
  never exists without their default prompts.
* Java constants are easier to edit and review than long SQL strings
  and avoid migration churn when the wording is tuned.
* Existing accounts created before this change can be backfilled by
  a one-shot CLI / scheduled task — out of scope for this ADR but
  trivial to add (call `saveNewVersion` for every purpose missing).

## Consequences

* New authors immediately have all four prompt templates available
  in the configuration UI.
* Updating the default content does **not** retroactively change
  existing authors' prompts (which is the desired behaviour: the
  versioning history per author stays intact).

## Alternatives

* Repeatable Flyway migration — rejected; would either embed the
  prompt strings in SQL or require a stored procedure.
* First-login hook only — duplicates work, leaves a window where the
  account exists without prompts.
