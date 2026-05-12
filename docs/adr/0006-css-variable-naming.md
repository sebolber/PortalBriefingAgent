# ADR 0006: CSS variables under brand-neutral names

* Status: accepted
* Date: 2026-05-12

## Context

The implementation rules forbid any company-identifying token from the
codebase, configuration and documentation. The Briefing Agent UI still
uses the corporate-design colours (primary `#006ec7`, Fira Sans
typography, FontAwesome icons) but the variable names themselves must be
neutral so the palette can change without a rename cascade and so no
identifying token leaks into committed sources.

## Decision

CSS variables follow the pattern `--color-brand-*`, `--color-surface-*`,
`--space-*`, `--radius-*`, `--font-stack-*`, `--shadow-*`. Hex codes
remain inside the variable definition (e.g. `--color-brand-primary:
#006ec7`).

## Rationale

* The constraint is naming, not the colour itself. Variable names are
  read by every reviewer; colour values are opaque data.
* Reusing the same variable names for a different brand becomes a pure
  data change.

## Consequences

* No `--color-<company>` variables anywhere in the SCSS sources.
* Theming experiments only need to swap the `:root` values, not the
  variable identifiers.

## Alternatives

* Tailwind utility classes — rejected; pulls in another build step and
  larger learning curve for a tiny surface area.
