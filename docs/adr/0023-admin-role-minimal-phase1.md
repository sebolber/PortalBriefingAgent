# ADR 0023: Minimal admin role via a boolean flag

* Status: accepted
* Date: 2026-05-13

## Context

Iter 6 introduces destructive admin actions (deactivate user,
tombstone person). Phase 1 has no Entra integration yet, so the role
distinction must live in the local user table.

## Decision

* `user_account.is_admin BOOLEAN NOT NULL DEFAULT FALSE`.
* `AuthorPrincipal` adds `ROLE_ADMIN` whenever the loaded user has the
  flag.
* `SecurityConfig` routes `/api/admin/**` through a single
  `hasAuthority(ROLE_ADMIN)` rule.
* The dev-bootstrap user (`demo`) is promoted to admin so the
  walking-skeleton flow stays usable.

## Rationale

* Two states (admin / not) cover everything Iter 6 needs (deactivation,
  tombstone). Group-based permissions arrive with Phase 2's Entra
  integration and will replace this column.
* Putting the rule on the URL prefix keeps the controller code free of
  scattered `@PreAuthorize` annotations for now.

## Consequences

* All admin endpoints sit under `/api/admin/`.
* Migration to Entra groups will replace the column with a synced
  membership table; the URL pattern stays.
* The seeded dev user is an admin — production deployments must
  override the password and consider the admin flag explicitly.

## Alternatives

* `@PreAuthorize` per controller method — more flexible but noisier
  for an iteration with two endpoints.
* A separate `role` column with multiple values — premature; we have
  exactly one elevated role in phase 1.
