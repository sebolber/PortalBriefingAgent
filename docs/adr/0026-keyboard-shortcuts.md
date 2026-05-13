# ADR 0026: Single keyboard shortcut — `/` focuses the global search box

* Status: accepted
* Date: 2026-05-13

## Context

Spec §9.3 promises "Anna → Briefing in 2 Klicks" / 5 seconds. A
keyboard-first short-cut to the search box is the obvious enabler.
Bigger schemes (Vim-style modal navigation, command palettes) would
exceed phase-1 scope.

## Decision

* The Shell component listens for `keydown` on the window. When the
  pressed key is `/` and the focus is **not** in an `<input>`,
  `<textarea>` or `<select>`, the event is consumed and the global
  search input receives focus.
* Modifier keys (Cmd / Ctrl / Alt) are ignored so existing browser
  shortcuts (Cmd+/ for "Help" in some browsers, etc.) keep working.
* The placeholder text spells the binding (`"Suche · "/" zum
  Fokussieren"`) so users discover it without docs.

## Rationale

* `/` is the de-facto "focus search" key on the modern web (GitHub,
  GitLab, YouTube, …).
* Skipping the binding inside form fields prevents surprises while
  the user is typing.

## Consequences

* The shortcut adds one short HostListener; no bundle bloat.
* Future shortcuts can layer on top of the same listener.

## Alternatives

* A full command palette (`Cmd+K`) — out of phase-1 scope.
* No shortcut at all — slower in the "5-seconds-to-briefing" use case.
