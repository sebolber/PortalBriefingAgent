# ADR 0018: Secret-Storage abstraction (deferred to env vars in phase 1)

* Status: accepted
* Date: 2026-05-12

## Context

Spec §7.1 forbids storing API keys in the database in clear text and
calls for a Secret-Storage reference (Vault, Azure Key Vault, Spring
Cloud Vault). For phase 1 the operator runs everything on a single VM
and a Vault deployment would be premature.

## Decision

* `LlmProvider.api_key_secret_ref` and `SttProvider.api_key_secret_ref`
  hold a textual reference to a secret, not the secret itself.
* In phase 1 the reference is interpreted as the **name of an
  environment variable** (e.g. `SECRETS_OPENAI_PROD`). The actual key
  is read from the process environment when the provider is invoked.
* The interface for resolving secrets is a future
  `SecretStore` Spring bean; the provider entities never see clear
  text. Phase 1 ships only the `EnvSecretStore` implementation; phase 2
  may add a Vault-backed implementation by registering an additional
  bean.

## Rationale

* Environment variables fit the existing internal-cloud bootstrap
  pipeline.
* Storing the *reference* in DB keeps the provider configuration
  declarative and audit-friendly while the actual secret stays in the
  operating environment.

## Consequences

* The `parameters` JSONB column intentionally has no `api_key` field.
* A future `SecretStore` bean swap is local to the call site that
  resolves a key, not to the schema.

## Alternatives

* Spring Cloud Vault from day one — deferred until the team commits
  to a Vault deployment.
