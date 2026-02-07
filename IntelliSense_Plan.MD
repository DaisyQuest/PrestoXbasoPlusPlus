# IntelliSense Integration Plan (PrestoXbasoPlusPlus)

## Goals
- Deliver **robust, accurate completions** across Xbase++ language constructs (functions, procedures, classes, methods, fields, macros, preprocessor symbols, and built-ins).
- Provide a **modular, testable, maintainable** architecture aligned with IntelliJ IDEA Plugin SDK constraints.
- Ensure **deterministic, reproducible** completion results with full coverage of error paths and edge cases.
- Maintain **≥95% line/branch coverage** for lexer/parser/interpreter components and **100% operation coverage** driven by `operations.yaml`.

## Guiding Principles
- **Single-responsibility modules** with explicit contracts (interfaces in `plugin-core`), IDE integration only in `plugin-ide`.
- **Pure logic first**: keep completion ranking, filtering, and scoring in test-friendly classes without IDE dependencies.
- **Snapshot/golden testing** for completion lists and ranking outcomes.
- **Determinism**: avoid nondeterministic iteration; sort by stable keys.
- **Performance-aware**: caching with invalidation tied to PSI/stub changes.

## Target Architecture
### Layered Structure
1. **Core Model (plugin-core)**
   - **Symbols & Types**
     - `XbSymbol` (function/procedure/class/member/variable), `XbType` hierarchy, `XbSignature`.
   - **Resolution & Scope**
     - `XbScope`, `XbScopeGraph`, `XbResolveResult`.
   - **Inference**
     - `XbTypeInferenceEngine` with deterministic rules and test fixtures.
   - **Completion Engine (Pure)**
     - `XbCompletionEngine` accepts `CompletionContext` and returns `CompletionCandidates`.

2. **Indexing & PSI Integration (plugin-core)**
   - **Stub-based symbol indices** for fast lookup.
   - **PSI adapters** for building scope graphs and resolution results.

3. **IDE Integration (plugin-ide)**
   - IntelliJ **`CompletionContributor`** and `CompletionProvider` that
     - Build `CompletionContext` from PSI + editor state.
     - Invoke `XbCompletionEngine`.
     - Map candidates to `LookupElement`s with consistent presentation and insert handlers.

### Module Responsibilities
- **plugin-core**
  - symbol model, scopes, resolution, completion logic, testable utilities.
- **plugin-ide**
  - IntelliJ SDK wiring, UI presentation, insertion behaviors.
- **test-framework**
  - golden test harness extensions for completion snapshots and ranking assertions.

## Detailed Work Plan
### Phase 1 — Contracts & Core Models
- Define `XbCompletionEngine` contract and `CompletionContext` model.
- Establish `XbCompletionCandidate` (name, kind, origin, type, score, insert strategy).
- Introduce `XbCompletionScope` with deterministic ordering.
- Create `XbCompletionRanking` with explicit, testable scoring rules.

### Phase 2 — Resolution & Inference Integration
- Integrate completion with existing PSI and stubs:
  - Resolve **locals, statics, publics, memvars, params**.
  - Resolve **class members**, **inheritance**, and **method overloads**.
  - Resolve **preprocessor symbols** and macros.
- Implement **type inference hooks** for more precise candidate filtering.
- Add **fallback completions** when inference fails (defensive but predictable).

### Phase 3 — IDE Contributor Implementation
- Implement IntelliJ `CompletionContributor` for Xbase++:
  - Trigger on `.`/`:` member access, `->`, `::`, `&` macro expansion, and identifiers.
  - Rank results using `XbCompletionRanking`.
  - Provide **contextual insertion** (method call parentheses, named params, etc.).
- Add **caching** and **smart invalidation** (PSI modifications, stub changes).
- Ensure **thread safety** in read actions and avoid heavy computations in UI thread.

### Phase 4 — Robust Testing & Coverage
- **Golden completion snapshots**:
  - Create fixtures for each operation in `operations.yaml`.
  - Include min + edge completion scenarios per operation.
- **Branch coverage**
  - Each ranking branch, fallback rule, and error path must be tested.
- **Negative tests**
  - Invalid syntax, unresolved identifiers, ambiguous resolution paths.
- **Performance tests**
  - Synthetic large-file completion responsiveness.
- **PSI integrity tests**
  - Ensure completion uses correct PSI elements and ranges.

## Completion Scenarios (Minimum Set)
- Identifier in **local scope** with shadowing and nested blocks.
- `self:` and `super:` member completions.
- Static class member completion (`ClassName:` / `ClassName::`).
- Preprocessor macro completion in `#define` and usage sites.
- Function/procedure call completions with **parameter hints**.
- Global/public/memvar completions with **scope precedence**.
- Completions in **string interpolation** or **code blocks**.
- Error recovery contexts (unfinished tokens, mismatched delimiters).

## Data Flow Overview
1. **IDE layer** builds `CompletionContext` from PSI + editor position.
2. **Core layer** resolves scope + symbol table via stubs.
3. **Completion engine** filters + ranks candidates.
4. **IDE layer** maps to `LookupElement`s and returns sorted results.

## Determinism & Ranking Rules
- Stable ordering by:
  1. **Exact scope precedence** (locals > params > members > globals > built-ins).
  2. **Type compatibility** and inferred relevance.
  3. **Alphabetical fallback** to guarantee deterministic output.

## Coverage & Checklist Integration
- All completion paths must be covered by **golden tests** and **branch tests**.
- Each entry in `operations.yaml` must have:
  - min + edge completion fixtures,
  - AST/PSI integrity checks,
  - completion snapshot comparisons.
- Update **Testing_Checklist.md** whenever new completion tests are added.
- Completion fixtures must follow `spec/xbasepp/completion-fixtures.md` and be stored under `spec/xbasepp/fixtures/completion/`.

## Deliverables
- `plugin-core` completion contracts + engine.
- `plugin-core` scope + resolution extensions for completion.
- `plugin-ide` completion contributor with full context support.
- `test-framework` completion golden test harness.
- Fixtures for each `operations.yaml` entry.

## Definition of Done
- Completion across all supported constructs with deterministic ordering.
- ≥95% line/branch coverage for lexer/parser/interpreter-related paths.
- 100% operation coverage validated by coverage gate.
- All completion tests green, with CI coverage thresholds enforced.
- IntelliJ SDK compatibility validated.
