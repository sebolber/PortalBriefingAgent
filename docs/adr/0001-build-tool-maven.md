# ADR 0001: Maven as the backend build tool

* Status: accepted
* Date: 2026-05-12

## Context

The phase-1 spec calls for Java 21 + Spring Boot 3 with Flyway. Two
build tools are typical: Maven and Gradle.

## Decision

Use **Maven**.

## Rationale

* Stable, well-known, friction-free for Spring Boot.
* No daemon, no Kotlin DSL learning curve, no caching surprises during
  a fresh `run.sh` execution on a developer's machine.
* The Spring Boot Maven plugin and the project's BOM are first-class.
* No personal preference of the author for Gradle; convention favours
  Maven inside the team that hosts Briefing Agent.

## Consequences

* Build profiles `skip-integration-tests` (Surefire only) and the default
  (Surefire + Failsafe) are used to separate fast unit tests from
  Testcontainers-based integration tests.
* The Maven repo cache (`~/.m2/`) survives between launcher runs.

## Alternatives

* Gradle 8 with Kotlin DSL — rejected for the reasons above.
