# ADR 0002: Spring Boot 3.3.x as the backend baseline

* Status: accepted
* Date: 2026-05-12

## Context

The phase-1 spec calls for "Spring Boot 3.x" without pinning a minor
version. Boot 3.3 is the long-term-support line at the time of writing;
Boot 3.4 is current but introduces additional auto-configuration that
would force more churn in this walking skeleton.

## Decision

Pin to **Spring Boot 3.3.5** as the parent BOM.

## Rationale

* 3.3 is in an LTS line with active maintenance.
* Compatible with Java 21 (required by ADR 0001).
* Spring Security 6.3, Hibernate 6.5, Tomcat 10.1 — all current and well
  documented.

## Consequences

* Upgrades to 3.4+ are deferred; a future ADR may amend this.
* Some transitive dependencies (e.g. `flyway-database-postgresql`) come
  out of the BOM and do not need explicit version pins.

## Alternatives

* Boot 3.2 — rejected: would soon be unsupported.
* Boot 3.4 — rejected for now to keep churn low while the skeleton settles.
