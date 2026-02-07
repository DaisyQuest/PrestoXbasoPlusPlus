IntelliJ xbase++ Plugin – Development Plan & Specification
0. Goals and Non-Goals
Goals

Build an IntelliJ IDEA plugin that provides first-class xbase++ language support:

Correct parsing (AST) for all supported syntax.

Accurate PSI model, indexing, navigation, and refactoring-safe features.

Strong IDE UX: syntax highlighting, inspections, quick-fixes, completions, formatting, structure view, breadcrumbs, etc.

Testing

100% coverage of xbase++ operations at the parser/semantic layer.

Maximum practical testing coverage for Swing UI, with strict separation of logic to enable headless testing.

The specification must be clear, structured, and agent-friendly for AI coding agents.

Non-Goals (Initial)

Full compiler-grade semantic correctness for every dialect edge case.

Running/debugging xbase++ binaries inside IntelliJ (deferred to later phases).

1. Architecture Overview
1.1 Modules (Gradle)

plugin-core

Language definition: file type, lexer, parser, AST, PSI.

Stubs and indexing.

Core analysis: symbol tables, resolution engine, basic inference.

Formatter, folding, structure view.

plugin-ide

Inspections, quick-fixes, completion contributors, intentions.

Navigation and refactoring support.

plugin-ui

Swing UI: settings pages, tool windows, inspection configuration.

UI logic depends on interfaces from plugin-core.

test-framework

Shared testing utilities:

Golden tests

Fuzz/property tests

AST/PSI diff tools

Mutation testing

Coverage enforcement

1.2 Data Flow

Text → Lexer → Parser → AST → PSI → Stubs/Index → Resolve & Analysis → IDE Features

2. Language Scope Definition
2.1 Specification Inputs

Directory: /spec/xbasepp/

grammar.md – Human-readable grammar and notes.

operations.yaml – Authoritative list of all operations (coverage source of truth).

fixtures/ – Canonical example programs for each operation.

compat-matrix.md – Version and dialect differences.

Rule: Implementation and tests must be driven by operations.yaml.

2.2 Definition of an “Operation”

An operation includes any language construct that must be covered by tests:

Operators

Arithmetic

Logical

Comparison

Bitwise

Assignment variants

Unary

Ternary

Short-circuit

Statements

IF / ELSEIF / ELSE / ENDIF

Loop constructs

DO CASE

BEGIN SEQUENCE / RECOVER

Declarations

Functions and procedures

Classes and methods

Properties, access/assign

Locals, statics, publics, memvars

Preprocessor

#include

#define

#ifdef

Macro handling (token-level minimum)

Literals and Types

Strings

Numerics

Dates

Codeblocks

Arrays and hashes

NIL, TRUE, FALSE

Symbol literals

Calls and Dispatch

Function calls

Method calls

Dynamic invocation patterns

Error Handling and Pragmas

Error-handling constructs

Compiler pragmas and xbase++-specific keywords

Each entry in operations.yaml must define:

id

category

example_min

example_edge

expected_ast_shape

expected_errors

precedence_group (if applicable)

3. Parsing Strategy
3.1 Parser Technology

Recommended approach

Custom Pratt parser for expressions.

Structured statement parser for blocks.

Implemented on top of IntelliJ PsiBuilder.

Rationale

Explicit operator precedence.

Easier correctness testing.

Better control over error recovery.

3.2 Parsing Stages
1. Lexer

Tokens for keywords, identifiers, literals, operators, comments.

Token groups:

KEYWORDS

OPERATORS

LITERALS

COMMENTS

PP_DIRECTIVES

2. Preprocessor Layer (v1)

Parse directives and preserve source-offset mapping.

Initial support for includes and defines without full macro expansion.

3. Parser

File-level parsing.

Statement parser with explicit terminators.

Pratt expression parser with table-driven precedence.

4. AST to PSI

PSI classes:

XbFile

XbFunctionDecl

XbClassDecl

XbMethodDecl

XbIfStmt

XbBinaryExpr

Additional nodes as required

5. Error Recovery

Statement-level synchronization tokens:

ENDIF

ENDDO

ENDCASE

Expression recovery tokens:

)

]

,

;

Newline

3.3 Normalized AST Shape (Golden Tests)

Stable, whitespace-insensitive representation:

Node kind

Operator or keyword

Ordered children

Example:

BinaryExpr "+"

Identifier "a"

BinaryExpr "*"

Identifier "b"

Number "2"

4. Semantic Layer Plan
4.1 Name Resolution (v1)

Symbol tables by scope:

File or module

Function

Method

Block

Resolution targets:

Locals

Statics

Publics

Memvars

Class members

Functions and procedures

IDE features:

Go to Declaration

Find Usages (index-backed)

4.2 Type-like Inference (Optional)

Dynamic language model:

Literal-based types

Known built-ins

Simple intra-function propagation

Used for:

Completion ranking

Inspections

5. IntelliJ Feature Roadmap
Phase 1 – Language Core

File type and icons

Lexer, parser, PSI

Syntax highlighting

Parser error annotations

Structure view

Folding and breadcrumbs

Phase 2 – Navigation and Productivity

Stubs and indexing

Go to declaration and usages

Code completion

Rename refactoring

Code formatter

Phase 3 – Inspections and Quick-Fixes

Unused locals

Unreachable code

Block mismatch

Constant condition checks

Auto-fix for missing terminators

Phase 4 – Build and Run (Optional)

Compiler integration

Run configurations

Clickable diagnostics

6. Testing Strategy (Quality Gate)
6.1 Coverage Dimensions
Operation Coverage (100% Required)

For every operation in operations.yaml:

Parse success or expected parse error test

AST shape assertion

PSI integrity test

Precedence validation if applicable

Code Coverage

Parser and semantic core: near-100% line and branch coverage.

UI: maximize logic coverage; exclude paint-only paths.

6.2 Test Harnesses
Golden Parse Tests

Input: fixtures/operations/<id>.xb

Output: fixtures/operations/<id>.ast.txt

Assertions:

Exact AST match

Expected diagnostics only

PSI Round-Trip Tests

Parse → PSI → validate:

Text ranges

Parent/child relationships

Stub and index consistency

Property-Based and Fuzz Tests

Random expression generation based on precedence table.

Invariants:

No crashes or hangs

AST always well-formed

Negative Tests

Malformed syntax

Missing terminators

Validation of recovery and continued parsing

Semantic Tests

Symbol resolution correctness

Shadowing behavior

Find-usages accuracy

6.3 Swing Testing Strategy

Rule: All UI logic must be testable headlessly.

Unit tests:

Settings models

Validation logic

State transitions

Limited UI or robot tests:

Open settings panels

Toggle options

Verify persistence

Snapshot tests:

Component tree structure (not pixel rendering)

No flaky or timing-based tests.

6.4 CI Coverage Gates

Operation coverage: 100% enforced via ID matching.

Code coverage thresholds:

plugin-core: 95–100% lines, 90–100% branches.

plugin-ide: high coverage expected.

plugin-ui: logic coverage required.

Mutation testing recommended for operator and precedence logic.

7. Work Breakdown Structure
Epic A – Specification and Fixtures

A1. Define operations.yaml

A2. Create minimal and edge examples

A3. Define normalized AST and dumper

Epic B – Lexer

B1. Token definitions

B2. Literal parsing

B3. Preprocessor tokens

Epic C – Parser

C1. Pratt expression parser

C2. Statement parsing

C3. Error recovery

C4. AST dumper

Epic D – PSI and Indexing

D1. PSI elements

D2. Stub generation

D3. Index keys and queries

Epic E – IDE Features

E1. Syntax highlighting

E2. Structure view

E3. Completion

E4. Navigation

Epic F – Swing UI

F1. Settings model

F2. UI panels and presenters

F3. Presenter tests and minimal robot tests

Epic G – CI and Quality

G1. Operation coverage enforcement

G2. Coverage thresholds

G3. Fuzz testing job

8. Definition of Done

A feature or operation is complete only if:

It exists in operations.yaml.

It has:

Golden parse test

PSI integrity test

Negative tests where applicable

It contributes to operation coverage.

It does not reduce coverage thresholds.

It is documented in the specification.

9. Repository Layout

/spec/xbasepp/

grammar.md

operations.yaml

compat-matrix.md

fixtures/

operations/

OP_0001_min.xb

OP_0001_edge.xb

OP_0001.ast.txt

projects/

/plugin-core/

src/main/

src/test/

/plugin-ui/

src/main/

src/test/

/test-framework/

src/main/

src/test/

10. Immediate Next Steps (Sprint 1)

Create the full operations.yaml skeleton.

Implement lexer for core tokens.

Implement Pratt expression parser and AST dumper.

Build golden test harness with coverage gate.

Implement approximately 20–30 operations end-to-end, then scale to full coverage.

End of specification.
