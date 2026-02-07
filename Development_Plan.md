# Development Plan (IntelliJ xbase++ Plugin)

## Overview
This plan derives from `PRE-PLAN.MD` and organizes work into discrete, independently parallelizable tracks after the initial foundation is complete. Each item has a **unique identifier** used for cross-referencing in checklists and for coordinating parallel workstreams. The plan is structured to avoid merge conflicts by isolating changes by module, folder, and feature vertical.

## Foundation (must be completed before parallel execution)
- **DP-0001**: Initialize repository structure per `PRE-PLAN.MD` (modules: `plugin-core`, `plugin-ide`, `plugin-ui`, `test-framework`, `spec/xbasepp`).
- **DP-0002**: Establish shared interfaces and contracts between modules (e.g., PSI interfaces, stub contracts, service boundaries).
- **DP-0003**: Define IntelliJ SDK-compatible Gradle configuration and baseline plugin metadata.
- **DP-0004**: Implement operations registry schema for `operations.yaml` and the coverage gate mechanism.
- **DP-0005**: Create golden test harness framework, AST dumper format, and baseline test utilities.

## Parallel Track A — Specification & Fixtures
- **DP-0101**: Author `operations.yaml` skeleton with IDs, categories, and precedence groups.
- **DP-0102**: Define canonical fixtures and edge fixtures under `spec/xbasepp/fixtures/operations/`.
- **DP-0103**: Document normalized AST shapes and expected diagnostic rules.
- **DP-0104**: Maintain compat matrix for dialect and version nuances.

## Parallel Track B — Lexer & Preprocessor
- **DP-0201**: Implement token definitions and keyword groups.
- **DP-0202**: Add literal parsing (strings, numerics, dates, symbol literals, codeblocks).
- **DP-0203**: Implement preprocessor directive tokenization and source-offset mapping.
- **DP-0204**: Add lexer fuzzing utilities and negative tests.

## Parallel Track C — Parser (Statements & Expressions)
- **DP-0301**: Implement Pratt expression parser with precedence table.
- **DP-0302**: Implement statement parsing for control-flow blocks.
- **DP-0303**: Add error recovery strategy with sync tokens.
- **DP-0304**: Build AST dumper and golden AST comparison tooling.

## Parallel Track D — PSI & Indexing
- **DP-0401**: Define PSI elements for core syntax nodes.
- **DP-0402**: Implement stub generation for indexed PSI elements.
- **DP-0403**: Build index keys and queries (find usages, declarations).
- **DP-0404**: Add PSI integrity validators and round-trip tests.

## Parallel Track E — IDE Features
- **DP-0501**: Syntax highlighting and annotators.
- **DP-0502**: Structure view and breadcrumbs.
- **DP-0503**: Navigation and usages.
- **DP-0504**: Completion and rename refactoring.
- **DP-0505**: Formatter and folding.

## Parallel Track F — UI (Swing)
- **DP-0601**: Settings model and persistence.
- **DP-0602**: UI panels and presenters with headless-testable logic.
- **DP-0603**: Snapshot/presenter tests for UI logic.

## Parallel Track G — Quality, Coverage, and CI
- **DP-0701**: Coverage threshold enforcement (95%+ lexer/parser/interpreter branches).
- **DP-0702**: Operation coverage enforcement (100% required).
- **DP-0703**: Property-based and fuzz testing integration.
- **DP-0704**: Mutation testing pipeline for precedence/expressions.
- **DP-0705**: IntelliJ SDK compatibility checks and CI validation.

## Parallelization Strategy (5–10x)
- After **DP-0001** to **DP-0005** are completed, work splits cleanly by track with minimal overlap:
  - Track A: `spec/xbasepp/` only.
  - Track B/C: `plugin-core` parsing subpackages; enforce file-level ownership by feature (e.g., `lexer/`, `parser/`, `ast/`).
  - Track D: PSI, stubs, indexing classes under `plugin-core` (separate package namespace).
  - Track E: `plugin-ide` module only.
  - Track F: `plugin-ui` module only.
  - Track G: `test-framework` and CI configuration.
- Each track must update tests in its own module to prevent cross-track merge conflicts.
- Shared interfaces in **DP-0002** are frozen before parallel work begins. Any changes require a coordination note and explicit review.

