# Design Review: Angkor Mart Sales Report Architecture

## Package Responsibilities and Cohesion

- **`edu.itc.salesreport.model`**: Defines the canonical domain model records and enums (`SaleTransaction`, `BranchSummary`, `MonthlyReport`, etc.) and enforces invariants such as positive quantities and `BigDecimal` rounding rules.
- **`edu.itc.salesreport.ingest`**: Ingests and validates daily sales CSV files into domain objects, publishing progress events via the Observer pattern.
- **`edu.itc.salesreport.render`**: Implements the Strategy pattern to format aggregated `MonthlyReport` data into target representations (TXT, HTML, CSV).

## Package Coupling and Instability Metrics

Only internal project dependencies (`edu.itc.salesreport.*`) are counted:
- **$C_e$ (Efferent Coupling)**: Number of outgoing dependencies to other internal packages.
- **$C_a$ (Afferent Coupling)**: Number of incoming dependencies from other internal packages.
- **$I$ (Instability)**: $I = \frac{C_e}{C_a + C_e}$ (0 = maximally stable, 1 = maximally unstable).

| Package | Responsibility | $C_a$ | $C_e$ | Instability $I = \frac{C_e}{C_a + C_e}$ |
|---------|----------------|-------|-------|----------------------------------------|
| `model`  | Canonical domain abstractions and invariants | 2 (`ingest`, `render`) | 0 | $I = \frac{0}{2 + 0} = 0.00$ (Maximally stable) |
| `ingest` | CSV parsing and observable batch ingestion   | 1 (`App`)              | 1 (`model`) | $I = \frac{1}{1 + 1} = 0.50$ (Balanced) |
| `render` | Pluggable report format rendering            | 0                      | 1 (`model`) | $I = \frac{1}{0 + 1} = 1.00$ (Maximally unstable) |

### Arithmetic Justification
- `model`: Has zero outgoing dependencies ($C_e = 0$). Both `ingest` and `render` depend on it ($C_a = 2$). Thus, $I = 0.0$, making it the stable foundation.
- `ingest`: Depends only on `model` ($C_e = 1$) and is depended upon by the application composition root ($C_a = 1$). $I = \frac{1}{2} = 0.5$.
- `render`: Depends only on `model` ($C_e = 1$) and no internal packages depend on concrete renderers ($C_a = 0$). $I = \frac{1}{1} = 1.0$.

## Accepted Coupling
We accept the coupling of both `ingest` and `render` to `model`. The `model` package contains only immutable records and core business concepts without external dependencies. Because `model` is maximally stable ($I = 0$), changes originate from fundamental business changes rather than technical volatility, minimizing ripple effects. In contrast, `DependencyRulesTest` prevents `ingest` from coupling to `render`, preserving strict layering.

## Sealed vs. Open Interface for QA-4 (Modifiability)
QA-4 requires adding a new format in $\le 2$ days ($< 0.5$ person-days of effort).
- **Sealed Interface (`ReportRenderer`)**: Limits implementations to explicitly permitted classes in the same package. The compiler enforces exhaustive pattern matching in `switch` statements without requiring fallback default branches. Adding a new format requires adding the class, declaring it in `permits`, and updating `forExtension`, taking $< 1$ hour.
- **Open Interface**: Allows third parties to implement new renderers without modifying the base package, but loses compile-time exhaustiveness guarantees.

**Conclusion**: For Release 1 (a modular monolith built by our internal team), a **sealed interface** better serves QA-4. It enables rapid additions with complete compiler assistance, guaranteeing that all dispatch points and tests account for new formats without runtime errors. If third-party plugins are required in future releases, the interface can be relaxed to open or non-sealed.
