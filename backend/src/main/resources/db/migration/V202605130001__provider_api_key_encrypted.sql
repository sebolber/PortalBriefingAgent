-- Encrypted at rest: API keys are now stored as Base64-encoded
-- AES-256-GCM ciphertext (nonce || ciphertext) in api_key_encrypted.
-- The existing api_key_secret_ref column is kept as a fallback path
-- for deployments that still want a Vault-style env-var indirection.

ALTER TABLE llm_provider ADD COLUMN api_key_encrypted TEXT;
ALTER TABLE stt_provider ADD COLUMN api_key_encrypted TEXT;
