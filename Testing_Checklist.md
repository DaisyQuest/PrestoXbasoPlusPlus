# Testing Checklist

> Use this checklist for every feature and for every operation in `operations.yaml`. Tests must prioritize **branch coverage** and maintain **95%+ code coverage** for lexer/parser/interpreter components, with 100% operation coverage.

## Coverage Gates (Required)
- [ ] 100% operation coverage enforced via `operations.yaml` IDs mapped to tests.
- [ ] ≥95% line coverage for lexer/parser/interpreter modules.
- [ ] ≥95% branch coverage for lexer/parser/interpreter modules.
- [ ] Coverage reports exported and archived in CI artifacts.

## Parser & AST
- [ ] Golden parse tests exist for **every operation** (min + edge examples).
- [ ] AST shapes match normalized expected output for **every operation**.
- [x] Precedence and associativity are validated with explicit tests.
- [x] Error recovery tests exist for malformed inputs (sync tokens confirmed).
- [ ] Fuzz/property tests executed and validated (no crashes, no hangs).

## PSI & Indexing
- [x] PSI integrity tests validate tree relationships and text ranges.
- [x] Stub/index round-trip tests pass (declaration ↔ usage).
- [ ] Find-usage tests cover shadowing, scope, and nested blocks.

## IDE Features
- [x] Syntax highlighting and annotator tests cover keyword, error, and empty-input branches.
- [x] Structure view and breadcrumbs tests cover fallback labels and offset matching.
- [x] Navigation/usages tests validate declaration and missing-symbol paths.
- [x] Text-based PSI builder and navigation adapter tests cover function and variable resolution.
- [x] Completion and rename tests cover case sensitivity and error paths.
- [x] Formatter and folding tests cover indentation, blank lines, and fold filtering.
- [x] Completion fixture loader tests validate schema requirements and caret handling.
- [x] Completion coverage gate tests validate missing/invalid fixture detection.
- [x] Completion snapshot harness tests validate ordered comparisons and mismatch reporting.
- [x] Completion test harness validates operation-level execution and mismatch surfacing.
- [x] Completion mapping/insertion strategy tests validate lookup metadata and insert plans.
- [x] Completion prefix extraction tests validate caret-based filtering.
- [x] Completion case policy tests validate case-sensitivity rules.

## Semantic & Interpreter Paths
- [ ] Resolution tests validate locals, statics, publics, memvars, and class members.
- [ ] Negative semantic tests assert expected diagnostics.
- [ ] Interpreter-facing logic (if applicable) has full branch coverage.

## UI (Swing)
- [x] Settings logic is covered with headless unit tests.
- [x] Presenter/model state transitions are fully tested.
- [x] Snapshot tests verify stable component structure (non-rendering).

## CI and Tooling
- [ ] Coverage threshold check fails the build if any threshold is missed.
- [ ] Mutation testing runs for expression/precedence logic.
- [x] IntelliJ SDK compatibility tests are executed and passing.

## Recent Test Additions
- Operations registry loader/schema validation edge cases (non-map entries, duplicate IDs, expected_errors handling).
- Module detection strategy tests for marker naming, source root discovery, and error handling.
- Language code style settings provider tests for indent options and sample exposure.
- Structure view nesting and extract refactoring service tests for variable/function extraction behavior.
- Structure view static variable labeling and icon mapping tests for navigation cues.
- WAIT/EXIT keyword lexing coverage plus parser golden tests and fixtures for wait/exit statements.
- Parser golden coverage for macro stress sample with block literals, hash literals, macro operators, and sequence recovery.
- Parser golden coverage for macro stress user variant with trailing line continuation and sequence recovery.
- Lexer coverage for codeblock literals with nested braces, comments, and string content.
- Header insight tab coverage for include resolution, missing headers, conflict detection, presenter fallbacks, and all table model branches.
- AST tool window async presentation coordinator tests for loading-state messaging, large-file indicators, and stale-result suppression.
- Parser compatibility coverage for production snippets using pass-by-reference `@` arguments and colon-based method dispatch in call expressions.
- Parser compatibility coverage for legacy `DO CASE` / `SET ... TO` / `PARAMETERS` statements and sparse argument lists (e.g., double-comma calls).

- Scope-resolution coverage for LOCAL parameters plus PRIVATE/PUBLIC/GLOBAL declaration semantics across PSI builder, navigation, and rename services.
- XPJ visual editor coverage for section parsing/serialization, intelligent file suggestions, glossary rendering, and editor model mutation paths.
- IDE syntax highlighting semantic token coverage for function declaration/call classification, macro directives, and IntelliJ highlighter token mapping branches.
- DBF editor/provider compatibility coverage for DumbAware + getFile override contracts, and first-class Reverse Engineering tab visibility/workflow-stage rendering.
- Reverse engineering workspace coverage for Analyze/Generate guards, tab population, relation inference display, and API-profile-driven preview generation.
- Reverse engineering generation coverage for macro emission, alias-macro toggles, preview rendering, and warning surfacing when invalid class names are skipped.
