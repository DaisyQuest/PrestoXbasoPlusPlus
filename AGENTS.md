# AGENTS.md — Development Guardrails for PrestoXbasoPlusPlus

This document defines **mandatory** instructions for all Codex agents contributing to this repository. The goal is to maintain high-quality IntelliJ IDEA Plugin SDK-compatible code with **95%+ code coverage** for lexer/parser/interpreter features and **100% operation coverage** driven by `operations.yaml`.

## 1) Scope & Authority
- This file applies to the entire repository.
- If a more specific `AGENTS.md` exists in a subdirectory, follow the **more specific** instructions for files under that path.
- System/developer/user instructions override this file.

## 2) Absolute Requirements
- **Coverage**: Maintain ≥95% line and branch coverage for lexer/parser/interpreter features. No exceptions.
- **Operation Coverage**: Every entry in `operations.yaml` must have:
  - A golden parse test (min + edge),
  - AST shape assertions,
  - PSI integrity checks (where applicable).
- **Test Discipline**: Update tests **immediately** when changing behavior. No technical debt.
- **IntelliJ Compatibility**: All implementations must be compatible with the IntelliJ IDEA Plugin SDK. Avoid unsupported APIs.

## 3) Development Plan & Checklist Discipline
- The authoritative plan is `Development_Plan.md` and the task checklist is `Development_Checklist.md`.
- **You must check a box in `Development_Checklist.md` only after completing the associated work**, including tests and coverage verification.
- Never check a box early or “optimistically.”
- If a task requires coordination or changes outside its track, flag it in your notes and defer until coordination is done.

## 4) Parallel Execution Model (5–10x Agents)
After the **Foundation** items are complete (DP-0001 to DP-0005), work proceeds in parallel by track:

### Track Ownership Rules
- **Track A — Specification & Fixtures**: `spec/xbasepp/` only.
- **Track B — Lexer & Preprocessor**: `plugin-core` → `lexer/` and preprocessor packages.
- **Track C — Parser**: `plugin-core` → `parser/`, `ast/` packages.
- **Track D — PSI & Indexing**: `plugin-core` → `psi/`, `stubs/`, `index/` packages.
- **Track E — IDE Features**: `plugin-ide/` only.
- **Track F — UI (Swing)**: `plugin-ui/` only.
- **Track G — Quality/CI/Test Framework**: `test-framework/` and CI configs.

### Conflict Avoidance
- Do not modify files outside your track unless explicitly coordinated.
- Shared interfaces and contracts (DP-0002) are **frozen** once established.
- If a shared interface change is required, open a coordination note and await approval before altering.

## 5) Testing Expectations
- For each change:
  - Add or update **golden tests**.
  - Add **negative tests** for error recovery paths.
  - Validate **branch coverage** for new logic.
  - Run the appropriate module test suite and coverage report.
- Coverage regressions are **blocking**. Fix them before merging.

## 6) Required Artifacts
- `operations.yaml` is the single source of truth for coverage.
- For each operation, required artifacts live under:
  - `spec/xbasepp/fixtures/operations/` (min + edge inputs)
  - `spec/xbasepp/fixtures/operations/` (expected AST text)
- Testing tooling must verify one-to-one coverage mapping.

## 7) Agent Workflow Checklist (Per Task)
1. Identify the track and the specific DP-XXXX item you are addressing.
2. Work **only** within your assigned directories.
3. Implement feature + tests + coverage checks.
4. Verify coverage thresholds are met.
5. Update `Development_Checklist.md` by checking the corresponding box.
6. If applicable, update `Testing_Checklist.md` to record the tests you added.

## 8) No-Compromise Rules
- No unchecked coverage thresholds.
- No untested code paths.
- No box-checking without completion.
- No breaking IntelliJ SDK compatibility.

