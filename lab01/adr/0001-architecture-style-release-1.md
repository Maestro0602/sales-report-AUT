# ADR-0001: Modular Monolith Batch Application for Release 1

Status: Accepted  
Date: 2026-10-08 · Deciders: Maestroo, hengrith

## Context
The Angkor Mart sales report system must process approximately 3 million rows per month across three branches (PNH, REP, BTB) and produce monthly reports on the morning of day 2 of month M+1. Our key architectural drivers are:
- **QA-1 (Performance)**: Process a month of 3 M rows in $\le 60\text{ s}$ on an 8-core server.
- **QA-3 (Availability)**: Handle unreliable branch internet links and late arrivals without data loss.
- **QA-5 (Security)**: Enforce strict role-based access where branch managers see only their branch data.

Constraints include a small two-developer team, tight schedule, and running on standard server hardware without dedicated full-time DevOps infrastructure.

## Decision
We will build Release 1 as a **Modular Monolith Batch Application** executed on a single JVM. Core components are divided into well-defined packages (`model`, `ingest`, `engine`, `render`) with package dependency rules enforced by automated tests (`DependencyRulesTest`).

## Alternatives considered
- **Process Pipeline (Coordinator and separate worker processes)**: Attractive for process isolation and separate memory spaces, but rejected because Inter-Process Communication (IPC), OS pipe management, and object serialization introduce unnecessary complexity and latency that jeopardize QA-1.
- **Event-Driven Microservices (Kafka/RabbitMQ broker with distributed services)**: Attractive for horizontal scaling and asynchronous decoupling, but rejected because message serialization overhead, broker maintenance, network latency, and operational complexity are disproportionate for 3 M monthly rows and increase attack surface (QA-5).

## Consequences
+ **Performance (QA-1)**: Zero serialization overhead or network hops; in-memory data processing comfortably meets the $\le 60\text{ s}$ target.
+ **Simplicity and Testability (QA-6)**: Fast local testing and deterministic execution within a single JVM without distributed orchestration.
- **Scalability Limit (QA-2)**: Limited to single-node vertical scaling; cannot scale ingest and rendering horizontally on independent machines without architectural refactoring.
- **Fault Isolation**: A fatal error (e.g. `OutOfMemoryError`) in one component crashes the entire report generation run.

Revisit when: Monthly transaction volume exceeds 10 M rows, or execution time exceeds 45 seconds on the 8-core report server.
