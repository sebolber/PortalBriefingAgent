# ADR 0009: Audio uploads are never written to a persistent disk path

* Status: accepted
* Date: 2026-05-12

## Context

The phase-1 spec (§6.1, §8.2) states that captured audio must not be
persisted: only the transcript is stored, and the binary audio is
discarded once Whisper has produced text. This is the cornerstone of
the DSGVO data-minimisation argument.

## Decision

* The multipart upload is exposed via Spring's `MultipartFile`. Tomcat
  may spool the body to a short-lived temp file (auto-deleted after the
  request), but no code path under our control writes the audio to a
  named, persistent location.
* `EreignisService.captureAudio(InputStream, …)` streams directly into
  the `SttProviderClient`. The input stream is closed in a
  try-with-resources block immediately after the upstream call.
* No `audio_path`, `audio_blob` or similar column exists on the
  `ereignis` table; the schema cannot, even by accident, persist the
  binary.
* The `WhisperSttClient` uses chunked transfer encoding (returning
  `-1` from `InputStreamResource.contentLength()`), which lets us
  forward the bytes without buffering them in memory either.

## Rationale

* Storing audio expands the data-protection surface significantly and
  is not required by any phase-1 use case.
* Tomcat's auto-deleted temp file is acceptable per the spec ("Memory-
  Buffer oder kurzlebiges Tempfile").

## Consequences

* Errors that happen between upload and transcription leave nothing
  behind. There is no "re-transcribe yesterday's recording" capability.
* If we later need a regenerate-summary-from-original feature, we would
  store the transcript (already retained) rather than re-introduce
  audio persistence.

## Alternatives

* Persist audio for a short retention window — rejected; bigger DSGVO
  surface than the value justifies, and the spec is explicit on the
  point.
