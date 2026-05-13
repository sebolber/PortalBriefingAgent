# ADR 0030: API-Key encryption at rest (AES-256-GCM)

* Status: accepted
* Date: 2026-05-13

## Context

ADR 0018 ruled out storing API keys in clear text and chose an environment
variable indirection (`SECRETS_*`) as the phase-1 path. In practice this
forced operators to (a) edit shell-rc files or compose-files to add new
provider keys, and (b) restart the application after every key change.
Phase 1.5 introduced configuration via the UI for LLM and STT providers,
and the env-var workflow is incompatible with that flow: users expect to
paste an API key into a form and have it persist.

Sicherheit kommt vor Usability — aber das Verbot lautete "kein Klartext
in der DB", nicht "keine Geheimnisse in der DB überhaupt". Eine
authentifizierte, symmetrische Verschlüsselung mit einem Master-Key
außerhalb der DB erfüllt das Schutzziel.

## Decision

* Add a TEXT column `api_key_encrypted` to `llm_provider` and
  `stt_provider`. The column holds a Base64-encoded envelope produced by
  the new `SecretCipher` bean.
* `SecretCipher` uses **AES-256-GCM** with a 12-byte random nonce and a
  128-bit authentication tag. The wire format is
  `Base64(nonce || ciphertext-with-tag)`.
* The master key is a 32-byte value loaded from the Spring property
  `briefingagent.crypto.secret-key`, populated at runtime from the
  environment variable `BRIEFINGAGENT_SECRET_KEY` (Base64-encoded). The
  launcher (`scripts/run.sh`) generates a per-host key file at
  `~/.briefingagent/secret-key` (chmod 600, parent dir chmod 700) on
  first run and exports it for subsequent invocations.
* The existing `apiKeySecretRef` path stays as a fallback: when a
  provider has no `api_key_encrypted` value, the legacy `SecretStore`
  lookup is used. **Encrypted column takes precedence.**
* Controllers never return ciphertext or plaintext via GET endpoints —
  the JSON response exposes only `apiKeySet: boolean`. The plaintext is
  accepted only on POST/PUT and immediately encrypted before persistence.
  An explicit `clearApiKey: true` flag resets the encrypted column.

## Rationale

* AES-256-GCM is the standard authenticated encryption primitive for
  short secrets, available in the JDK without a third-party library.
* GCM gives confidentiality *and* integrity in one pass; a tampered
  envelope fails to decrypt loudly.
* Per-host master-key files mean that a stolen DB dump alone is not
  enough — the attacker also needs filesystem access on the host. This
  matches the on-premises threat model where the DB and the application
  are separate concerns operationally.
* Preserving the `SecretStore` fallback path keeps backwards-compatibility
  with operators who still want env-var-managed secrets.
* Sicherheit > Langlebigkeit > Performance > Nachhaltigkeit > Usability:
  GCM with a fresh nonce per encryption protects against
  ciphertext-equality leaks; the small overhead is irrelevant for
  per-request decryption of a single short string.

## Consequences

* The launcher gains a key-management step. Operators who run the
  application without the launcher must provide
  `BRIEFINGAGENT_SECRET_KEY` themselves; startup fails fast if it is
  missing.
* Rotating the master key requires re-encrypting every stored secret. A
  dedicated `re-encrypt` admin command is not in scope for phase 1 and
  is deferred. As long as the host disk is intact, rotation is not
  forced.
* Backup procedures must include `~/.briefingagent/secret-key` *and* the
  database, otherwise restored data is undecryptable.
* The DB migration `V202605130001__provider_api_key_encrypted.sql` is
  additive (NULLable column); existing env-var-based deployments keep
  working until an operator migrates a provider via the UI.

## Alternatives

* **Vault / Spring Cloud Vault** — still desirable for production at
  scale, but adds an external dependency that contradicts the
  single-host phase-1 deployment model. ADR 0018's deferral stands.
* **Envelope encryption with a per-row DEK wrapped by a master KEK** —
  proper layered design, but overkill for the small number of secrets
  (≤ low double digits) handled in phase 1. The current design can be
  upgraded later without changing the public API: `apiKeySet` stays
  identical; only the storage layout changes.
* **Bcrypt / Argon2 of the key** — those are one-way and would prevent
  using the secret to call the upstream provider. Not applicable here.
