# ADR 0015: Markdown rendering via marked + DOMPurify

* Status: accepted
* Date: 2026-05-12

## Context

Spec §9.4 calls for Markdown rendering in the summary read mode. The
LLM-generated body could contain HTML, scripts or unexpected
constructs. Even with on-prem providers we cannot trust raw output to
pass through to `[innerHTML]`.

## Decision

* Convert Markdown to HTML with `marked` (synchronous mode) and run the
  result through `DOMPurify` before handing it to Angular's
  `DomSanitizer.bypassSecurityTrustHtml(...)`.
* Forbid `style`, `iframe`, `object`, `embed`, `script` tags and any
  inline event handler attributes.
* Encapsulate the chain in `MarkdownRendererService` so future swaps
  (e.g. `markdown-it`) only touch one file.

## Rationale

* `marked` + `DOMPurify` is the de-facto stack for safe Markdown in
  Angular and adds ~80 KB total to the bundle, acceptable for an
  internal tool.
* Bypassing Angular sanitisation on already-sanitised HTML avoids
  Angular silently stripping legitimate Markdown features (e.g.
  `target=_blank`).

## Consequences

* The summary review UI renders headings, lists, code, blockquotes and
  tables as expected.
* The service is a single seam where future hardening (e.g. allow-list
  refinements) can land.
* Two new runtime dependencies: `marked@14`, `dompurify@3`. Locked to
  these majors so updates can be reviewed manually.

## Alternatives

* Plain-text rendering — rejected; the spec requires Markdown.
* Hand-rolled Markdown subset — rejected; high XSS risk and reinvents
  the wheel.
* Angular Material's Markdown component — heavier and pulls in other
  Material primitives we don't need.
