# ADR 0010: Audio capture accepts only an explicit MIME whitelist

* Status: accepted
* Date: 2026-05-12

## Context

Browsers produce audio in different container/codec combinations
(Chromium → `audio/webm;codecs=opus`, Firefox → `audio/ogg;codecs=opus`,
Safari → `audio/mp4`). Whisper accepts a broad range, but accepting an
arbitrary MIME type from a client would expand the attack surface (file
type confusion, format-specific exploits in upstream decoders).

## Decision

The capture endpoint accepts only the following MIME types (case-
insensitive, optional parameters allowed):

* `audio/webm`
* `audio/ogg`
* `audio/mp4`
* `audio/mpeg`
* `audio/wav`
* `audio/x-wav`
* `audio/x-m4a`

Anything else returns `415 Unsupported Media Type` at the controller
boundary, before any bytes reach the STT provider.

## Rationale

* All major desktop browsers used to record via `MediaRecorder` produce
  one of the whitelisted types.
* `audio/aiff` and other rarely-seen formats are excluded; we can add
  them later if a real use case appears.
* The whitelist lives in a small constants class
  (`AudioMediaTypes`) so adding a type means one edit + one extra
  parameterised test case.

## Consequences

* Tests verify both the accepted set and a curated list of rejections
  (`video/mp4`, `text/plain`, etc.).
* The check is duplicated in the service layer so internal callers
  (future iOS/Mac clients) can not bypass it.

## Alternatives

* Allow any `audio/*` type — rejected; weaker safety net.
* Sniff the bytes — overkill for phase 1; can be added if a real-world
  attack vector turns up.
