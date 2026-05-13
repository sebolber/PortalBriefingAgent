# ADR 0005: Angular 18 with standalone components and signals

* Status: accepted
* Date: 2026-05-12

## Context

The frontend stack is Angular 18 (per spec §9.1). Angular offers two
state-management styles: NgRx (Redux-style global stores) or built-in
signals.

## Decision

* Use **standalone components** (no `NgModule` ceremony).
* Use **signals** for component state.
* No NgRx in phase 1. If a future iteration produces cross-feature state
  worth centralising, a follow-up ADR will introduce NgRx selectively.

## Rationale

* Briefing Agent's screens are simple: dashboard, capture form, login,
  feature lists. There is no shared global state today.
* Signals integrate with Angular's change detection without an extra
  framework.
* Standalone components reduce boilerplate and lazy loading is the
  default in the routes (`loadComponent`).

## Consequences

* Routes use `loadComponent` rather than `loadChildren`.
* Services that hold shared state (`AuthService`) expose a signal so
  templates can read it without subscriptions.

## Alternatives

* NgRx Signals — adds an extra dependency without clear payoff today.
* NgRx Store — overkill for the feature surface in phase 1.
