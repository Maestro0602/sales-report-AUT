# ADR-0002: Daily Branch-to-Head-Office Data Flow and Partial Failure Policy

Status: Accepted  
Date: 2026-10-08 · Deciders: Maestroo, hengrith

## Context
Each branch (PNH, REP, BTB) generates ~33,000 transaction rows daily and exports them as CSV files to head office by 23:00. Branch internet connections are unreliable and occasionally experience outages (QA-3). The monthly report must be available by the morning of day 2 of month M+1, and data integrity must be guaranteed without creating duplicate transactions during network retries.

## Decision
We will adopt a **Daily Batch Push over SFTP with Idempotent Landing Zone Ingestion**.
1. **Transport**: Each branch POS exports a single daily CSV named `<BRANCH>-<yyyy-MM-dd>.csv` accompanied by a `.sha256` checksum file, pushing them directly via SFTP into head office's Landing Zone directory (`data/<yyyy-MM>/`).
2. **Cut-off & Partial Report Policy (QA-3)**: The scheduled report job starts at 06:00 on day 2. If a branch file is missing due to a network outage:
   - A **preliminary partial report** is generated for available branches, with the missing branch explicitly listed in the report anomalies (`anomalies` list in `MonthlyReport`).
   - Head office operations are alerted immediately.
3. **Late File Ingestion & Idempotency**:
   - When the branch link recovers, the branch re-transmits the missing file and checksum.
   - The landing zone uses the deterministic filename `<BRANCH>-<yyyy-MM-dd>.csv` and overwrites any existing partial file.
   - Ingestion calculates SHA-256 against the uploaded checksum before parsing.
   - A re-run of `MonthLoader` completely reprocesses the month's directory from scratch, ensuring 100% idempotent results with zero duplicate rows.

## Alternatives considered
- **Head Office Pull (Polling/SFTP fetch)**: Head office polls branch servers. Rejected because branch firewalls, dynamic IP addresses, and NAT configuration across retail stores complicate inbound connections.
- **Real-Time Streaming per Transaction (Message Broker / MQTT / Kafka)**: POS streams transactions live. Rejected because unstable branch internet leads to dropped connections, store operations halt if offline queuing fails, and broker maintenance exceeds current team capacity.

## Consequences
+ **Availability & Fault Resilience (QA-3)**: Decouples retail branch operations from head-office batch runs; branch POS stores data locally during outages and pushes when restored.
+ **Auditability and Idempotency**: Files are named deterministically and verified with SHA-256; re-running ingestion produces identical results.
- **Latency**: Branch transactions are only accessible at head office on a daily batch basis rather than in real time.
- **Operational Burden**: Requires monitoring SFTP landing directory permissions, disk capacity, and branch push alerts.

Revisit when: Business requires real-time intra-day sales dashboards across branches, or branch count exceeds 20.
