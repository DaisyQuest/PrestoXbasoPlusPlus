# xbase++ Compatibility Matrix

This document captures dialect/version nuances that affect parsing, AST normalization, and diagnostics. It is
intended to be updated as additional language constructs are formalized in `operations.yaml`.

## Dialects & Versions (Planned Coverage)
| Dialect | Version Range | Status | Notes |
| --- | --- | --- | --- |
| xbase++ | 1.9.x | Planned | Baseline target for initial parser and fixtures. |
| xbase++ | 2.0.x | Planned | Adds incremental syntax updates; verify against real-world codebases. |
| xbase++ | 2.1.x | Planned | Track IDE behavior changes and keyword expansions. |

## Keyword & Operator Notes
| Feature | 1.9.x | 2.0.x | 2.1.x | Normalized Handling |
| --- | --- | --- | --- | --- |
| Case-insensitive keywords | Yes | Yes | Yes | Treat all keywords as case-insensitive in lexer/parser. |
| Assignment operator `:=` | Yes | Yes | Yes | Canonical assignment operator in normalized AST. |
| Alternative assignment `=` | TBD | TBD | TBD | If supported, normalize to `Stmt.Assignment` with operator metadata. |
| `IF/ELSE/ENDIF` blocks | Yes | Yes | Yes | Always normalize to `Stmt.If` with explicit `then/else` blocks. |
| `FOR/NEXT` loop | Yes | Yes | Yes | Normalize to `Stmt.For` with default `STEP = 1`. |

## Literal Compatibility
| Literal Type | 1.9.x | 2.0.x | 2.1.x | Normalized Handling |
| --- | --- | --- | --- | --- |
| Integer | Yes | Yes | Yes | `Expr.Literal.Number` value in decimal form. |
| String | Yes | Yes | Yes | `Expr.Literal.String` with unescaped value. |
| Date | TBD | TBD | TBD | Track when `DATE()` or literal date syntax is confirmed. |

## Diagnostics Expectations
- Diagnostics are version-agnostic where possible.
- Dialect-specific diagnostics should be tagged with a dialect prefix once confirmed, for example:
  `error.syntax.xbasepp2.unexpected-token`.

## TODO / Research Tasks
- Confirm whether `=` is permitted as assignment (legacy dialects).
- Validate literal date syntax differences across 1.9.x and 2.x.
- Collect sample codebases for each version and update operations/fixtures accordingly.
