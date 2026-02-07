# Development Checklist

> Each checklist item maps 1:1 to `Development_Plan.md` and must be checked **only after completion**.

## Foundation
- [x] **DP-0001**: Initialize repository structure per `PRE-PLAN.MD` (modules: `plugin-core`, `plugin-ide`, `plugin-ui`, `test-framework`, `spec/xbasepp`).
- [x] **DP-0002**: Establish shared interfaces and contracts between modules (e.g., PSI interfaces, stub contracts, service boundaries).
- [x] **DP-0003**: Define IntelliJ SDK-compatible Gradle configuration and baseline plugin metadata.
- [x] **DP-0004**: Implement operations registry schema for `operations.yaml` and the coverage gate mechanism.
- [x] **DP-0005**: Create golden test harness framework, AST dumper format, and baseline test utilities.

## Parallel Track A — Specification & Fixtures
- [x] **DP-0101**: Author `operations.yaml` skeleton with IDs, categories, and precedence groups.
- [x] **DP-0102**: Define canonical fixtures and edge fixtures under `spec/xbasepp/fixtures/operations/`.
- [x] **DP-0103**: Document normalized AST shapes and expected diagnostic rules.
- [x] **DP-0104**: Maintain compat matrix for dialect and version nuances.

## Parallel Track B — Lexer & Preprocessor
- [x] **DP-0201**: Implement token definitions and keyword groups.
- [x] **DP-0202**: Add literal parsing (strings, numerics, dates, symbol literals, codeblocks).
- [x] **DP-0203**: Implement preprocessor directive tokenization and source-offset mapping.
- [x] **DP-0204**: Add lexer fuzzing utilities and negative tests.

## Parallel Track C — Parser (Statements & Expressions)
- [x] **DP-0301**: Implement Pratt expression parser with precedence table.
- [x] **DP-0302**: Implement statement parsing for control-flow blocks.
- [x] **DP-0303**: Add error recovery strategy with sync tokens.
- [x] **DP-0304**: Build AST dumper and golden AST comparison tooling.

## Parallel Track D — PSI & Indexing
- [x] **DP-0401**: Define PSI elements for core syntax nodes.
- [x] **DP-0402**: Implement stub generation for indexed PSI elements.
- [x] **DP-0403**: Build index keys and queries (find usages, declarations).
- [x] **DP-0404**: Add PSI integrity validators and round-trip tests.

## Parallel Track E — IDE Features
- [x] **DP-0501**: Syntax highlighting and annotators.
- [x] **DP-0502**: Structure view and breadcrumbs.
- [x] **DP-0503**: Navigation and usages.
- [x] **DP-0504**: Completion and rename refactoring.
- [x] **DP-0505**: Formatter and folding.

## Parallel Track F — UI (Swing)
- [ ] **DP-0601**: Settings model and persistence.
- [ ] **DP-0602**: UI panels and presenters with headless-testable logic.
- [ ] **DP-0603**: Snapshot/presenter tests for UI logic.

## Parallel Track G — Quality, Coverage, and CI
- [ ] **DP-0701**: Coverage threshold enforcement (95%+ lexer/parser/interpreter branches).
- [ ] **DP-0702**: Operation coverage enforcement (100% required).
- [ ] **DP-0703**: Property-based and fuzz testing integration.
- [ ] **DP-0704**: Mutation testing pipeline for precedence/expressions.
- [x] **DP-0705**: IntelliJ SDK compatibility checks and CI validation.
