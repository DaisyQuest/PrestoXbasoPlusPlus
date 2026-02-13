# REVERSE_ENGINEERING_SPEC.md

## 1. Purpose and Outcomes

This specification defines a complete reverse engineering workflow for DBF files in **PrestoXbasoPlusPlus**, with both:

1. **IntelliJ-integrated UX** via the Ultra DBF Master utility and IntelliJ actions.
2. **Headless JVM/CLI execution** driven entirely by JSON configuration.

The system is intentionally split into two independently versioned phases:

- **Phase 1 — Metadata Extraction**: parse DBF structure and emit canonical metadata artifacts.
- **Phase 2 — Code Generation**: consume metadata + generation config to emit rich, configurable Xbase++ APIs.

Core goals:

- Rich and maintainable generated APIs (static + instance convenience macros/methods).
- Strong relationship support (foreign keys, parent/child helpers, relational persistence operations).
- Highly configurable but ergonomic UX (optimized controls over raw text input).
- Clear schema/version boundaries to support long-term maintainability.
- High quality: full branch coverage for new logic, comprehensive tests, and deterministic outputs.

---

## 2. Scope

### 2.1 In Scope

- Ultra DBF Master tabbed reverse-engineering workflow.
- IntelliJ Action: `Reverse Engineer DBF`.
- Config-driven execution for both plugin and CLI.
- JSON schema definitions for all persisted artifacts.
- Metadata extraction engine (Phase 1).
- Code generation engine (Phase 2).
- Preview UI for generated code.
- JSON editor support (completion, syntax highlighting, validation).
- XPJ editor integration for include/dependency updates based on reverse-engineering config.

### 2.2 Out of Scope (Initial Iteration)

- Live DB connection reverse engineering (non-DBF stores).
- Non-Xbase++ code generation templates.
- Runtime ORM engine; this is compile-time generation of maintainable source.

---

## 3. Architectural Principles

1. **Separation of Concerns**
   - Extraction and generation are separate modules with explicit contracts.
   - GUI is an orchestration and editing layer, never the source of truth.

2. **Configuration as Source of Truth**
   - JSON config drives generation in both GUI and CLI.
   - GUI edits produce/consume the same JSON documents as CLI.

3. **Versioned Contracts**
   - Metadata schema and generation config schema are independently versioned.
   - Backward-compatible migration strategies defined.

4. **Deterministic Outputs**
   - Given the same input DBF + config + generator version, output is identical.

5. **Extensibility by Layers**
   - Stable domain model interfaces, pluggable generators/policies.

6. **IntelliJ + Standard JVM Parity**
   - Same engine code invoked by plugin and CLI wrappers.

---

## 4. High-Level System Components

1. **reverse-engineering-core**
   - Domain models, schema models, validation, orchestration.

2. **reverse-engineering-extractor**
   - DBF parser adapter(s), metadata normalization, metadata emission.

3. **reverse-engineering-generator**
   - Template/context builders, method policy resolution, Xbase++ renderers.

4. **reverse-engineering-cli**
   - `dbf-reverseengineer` command entry, argument parsing, orchestration.

5. **plugin-ide integration**
   - IntelliJ action, tool windows, tabbed editors, preview panel.

6. **plugin-ui integration**
   - Rich controls, relationship configuration dialogs, smart selectors.

7. **json-language-support**
   - JSON schema registration, completion provider hooks, validation diagnostics.

8. **xpj-editor integration**
   - Include/dependency updater from generation plan.

---

## 5. Two-Phase Pipeline

## 5.1 Phase 1 — Metadata Extraction

Input:
- One DBF file or a directory of DBF files.
- Optional extraction profile in config.

Output:
- `DbfMetadataBundle` JSON artifact(s), versioned schema.

Metadata includes:
- Table name, source path, checksum/fingerprint.
- Field list:
  - original field name
  - inferred type
  - width/precision/scale
  - nullable hint
  - default value hint (if derivable)
  - indexing hints (if available)
- Candidate primary key detection heuristics.
- Candidate foreign key heuristics.
- Warnings/errors with source offsets where possible.

## 5.2 Phase 2 — Code Generation

Input:
- Metadata bundle(s) from phase 1.
- Generation config JSON (may include overrides and relationship model).

Output:
- Generated Xbase++ classes/macros/docs/tests (configurable).
- Optional XPJ integration patch actions.

Generation concerns:
- Class naming, field naming, alias naming.
- Alternate typing overrides per field.
- API surface selection (read-only vs mutable vs full relational persistence).
- Macro and convenience method policy.
- Static and instance variants.
- Validation/guard policy in generated methods.

---

## 6. Tabbed UX in Ultra DBF Master

The reverse engineering process is integrated as a dedicated tab group.

### 6.1 Tabs

1. **Overview**
   - Workflow summary, quick-start presets, recent configs.

2. **Inputs**
   - File chooser(s) for DBF source(s), config path, output path.
   - “Analyze” trigger for Phase 1.

3. **Metadata (Phase 1 Results)**
   - Table/field browser.
   - Type inference diagnostics.
   - Key candidate visualization.

4. **Model Mapping**
   - Class name editing (prefilled from table).
   - Field inclusion toggles.
   - Override names + alias accumulator.
   - Alternate typing controls.

5. **Relations**
   - Foreign key management popup:
     - load/select target DBF metadata
     - choose local/remote columns
     - relation cardinality/persistence options
   - Relationship helper API configuration.

6. **API Surface**
   - Method family toggles:
     - read methods
     - persistence methods
     - upsert/update/delete controls
     - macro alias generation
   - Profile presets: `READ_ONLY`, `CRUD_BASIC`, `CRUD_RELATIONAL`, `FULL`.

7. **Generation & Output**
   - Artifact directory chooser (required).
   - Package/module options.
   - Include/XPJ update options.

8. **Preview**
   - Rendered class preview with syntax highlighting.
   - Method list panel and search.
   - Diff between current and generated output.

9. **Validation**
   - Schema validity, unresolved relations, naming collisions.
   - Blocking/non-blocking issue list.

10. **Run/Logs**
   - Execute Phase 2.
   - Rich logs, timings, generated file summary.

---

## 7. IntelliJ Action Integration

Action name: **Reverse Engineer DBF**

Behavior:
- Available from project view, editor context, and tools menu.
- Opens/focuses Ultra DBF Master with Reverse Engineering tab set.
- Pre-populates selected DBF path when invoked on a DBF file.
- Supports batch selection and project-level configuration discovery.

Implementation notes:
- Register action in plugin XML.
- Add keyboard shortcut capability.
- Integrate with IntelliJ notifications and background task API.

---

## 8. CLI Specification

Executable: `dbf-reverseengineer`

### 8.1 Command Forms

```bash
./dbf-reverseengineer (-h|--help) -a|--all <configDir> <outputDir> (-v|--verbose)
./dbf-reverseengineer (-h|--help) <configPath> <outputPath>
```

### 8.2 Behavior

- `--all`: execute all config files under `configDir`.
- `<configPath>`: execute single reverse-engineering config.
- `<outputPath>/<outputDir>`: destination override (if allowed by config policy).
- `--verbose`: detailed phase logs and diagnostics.

### 8.3 Exit Codes

- `0`: success.
- `1`: invalid CLI args.
- `2`: config/schema validation failure.
- `3`: extraction failure.
- `4`: generation failure.
- `5`: IO/system failure.

---

## 9. JSON Artifacts and Schema Versioning

At minimum, define and validate:

1. `dbf-metadata.schema.json` (Phase 1 output)
2. `dbf-reverseengineer-config.schema.json` (Phase 2 input)
3. `dbf-generation-report.schema.json` (Phase 2 output summary)

Schema recommendations:
- Include top-level `schemaVersion` and `engineVersion`.
- Use enums for method profiles and relation behaviors.
- Include migration annotations (`deprecated`, `replacedBy`).

### 9.1 Config Concepts

- Global defaults (naming, output layout, method profiles).
- Per-table overrides.
- Per-field overrides including alternate type.
- Relationship model definitions.
- API method inclusion/exclusion matrix.
- XPJ integration settings.

---

## 10. Method Generation Model

Generated API should support both long-form and macro-friendly aliases.

### 10.1 Static Methods (examples)

- `ClassName.load(id)` + alias `ClassName.l(id)`
- `ClassName.findBy(colName, aValues)` + alias `ClassName.f(colName, aValues)`
- `ClassName.insert(classObject)` + alias `ClassName.i(classObject)`
- `ClassName.delete(...)` + alias `ClassName.d(...)`
- `ClassName.upsert(...)` + alias `ClassName.us(...)`
- `ClassName.update(...)` + alias `ClassName.u(...)`
- child access helpers, field helpers, setter helpers
- relationship mutation helpers (`addChild`, `removeChild`, etc.)

### 10.2 Instance Methods

Instance equivalents for selected static methods, respecting profile policy.

### 10.3 Read-Only Profile

For read-only tables, persistence methods are omitted and compile-time stubs are not emitted unless explicitly requested.

### 10.4 Relational API

For configured foreign keys:
- navigation methods (parent/children)
- bind/unbind helpers
- optional cascade behaviors
- relation-aware query helpers

---

## 11. Foreign Key Management UX + Domain

### 11.1 UX Requirements

- “Manage Foreign Keys” popup/dialog from Relations tab.
- Load additional DBF metadata for target table selection.
- Choose local field ↔ target field mappings.
- Choose relation name, cardinality, ownership semantics.

### 11.2 Domain Representation

Each relation includes:
- source table/class
- target table/class
- source field(s)
- target field(s)
- cardinality
- optional cascade policy
- generated method naming policy

---

## 12. Field/Member Configuration Requirements

Per table, minimally configurable items:

- **Class Name**
  - default: derived from table name
  - UI: prefilled text field

- **Members to Include**
  - include/exclude checkbox
  - override member name field
  - alias accumulator for macro generation

- **Foreign Keys**
  - popup for cross-table binding and field selection

Additionally:
- Alternate type override per field.
- Field-level doc comments / tags for generated docs.

---

## 13. Preview and Developer Ergonomics

Preview requirements:
- Full generated class preview.
- Method group filtering.
- Highlight based on profile toggles.
- Optional side-by-side view with existing file.
- Copy/export snippet action.

Quality-of-life:
- Smart search for fields/methods.
- Preset templates for common DBF conventions.
- Conflict hints when alias/method collisions occur.

---

## 14. IntelliJ Smart Support

1. **JSON Support**
   - schema-backed completion
   - validation diagnostics
   - documentation tooltips

2. **Code Completion for Generated APIs**
   - ensure generated symbols are indexable by plugin logic
   - optionally emit metadata index for enhanced completion

3. **Module Recognition**
   - generated code registered/scanned as project sources as needed

4. **XPJ Editor Integration**
   - derive include updates from config + generation outputs
   - preview and apply include statement changes

---

## 15. Layered Abstractions and Maintainability

Recommended layering:

- `domain` (immutable models)
- `schema` (json DTO + validation)
- `extractor` (adapters + heuristics)
- `generator` (planning + render)
- `integration` (cli/plugin)

Maintainability strategies:
- explicit interfaces and adapters
- versioned migration service for configs/metadata
- compatibility matrix tests across schema versions

---

## 16. Testing Strategy (Mandatory)

Testing must be exhaustive and branch-complete for all new logic.

### 16.1 Phase 1 Tests

- DBF fixture matrix for field types and edge cases.
- Metadata extraction golden outputs.
- Heuristic key detection branches.
- Corrupt/partial DBF error recovery tests.

### 16.2 Phase 2 Tests

- Config schema validation tests.
- Generator golden tests for each API profile.
- Field override/alias collision tests.
- Relationship generation tests including edge cardinalities.
- Read-only omission tests for persistence APIs.

### 16.3 Plugin UI Tests

- Tab flow tests.
- Relation dialog behavior tests.
- Preview panel rendering tests.
- Action launch and prefill tests.

### 16.4 CLI Tests

- Arg parsing matrix (`--help`, `--all`, single config, verbose).
- Exit code behavior matrix.
- Batch processing and failure isolation.

### 16.5 Integration + Regression

- End-to-end: DBF -> metadata -> generation -> XPJ update.
- Determinism tests across repeated runs.
- Snapshot tests for generated code.

### 16.6 Coverage Gates

- Enforce >=95% line + branch coverage on affected modules.
- PR-blocking coverage regression checks.

---

## 17. Proposed Milestones

1. **M1**: Schemas + domain model contracts.
2. **M2**: Phase 1 extractor engine + metadata UI tab.
3. **M3**: Phase 2 generation core + CLI skeleton.
4. **M4**: API profile system + relationship generation.
5. **M5**: Full UI tab set + preview + validation tab.
6. **M6**: IntelliJ action + JSON smart support + XPJ integration.
7. **M7**: hardening, coverage closure, documentation.

---

## 18. Example Generated Usage (Target Ergonomics)

```xbase
LOCAL d := Dog.load(1)
? d.NAME
d.setNAME("PRESTON")
d.store()
```

Generated APIs should remain explicit, readable, and macro-friendly.

---

## 19. Non-Functional Requirements

- Performance: scalable to large DBF collections and large schemas.
- Reliability: robust error handling, clear diagnostics.
- Observability: per-phase logs and timings.
- Determinism: stable output ordering and formatting.
- Documentation: generated API docs and operator references.

---

## 20. Deliverables Checklist for Implementation Phase

- [ ] Versioned metadata schema + tests.
- [ ] Versioned generation config schema + tests.
- [ ] Phase 1 extractor + fixtures + coverage gates.
- [ ] Phase 2 generator + profile matrix + coverage gates.
- [ ] IntelliJ action + tabbed UI flow.
- [ ] Preview panel.
- [ ] JSON completion/validation integration.
- [ ] XPJ include update integration.
- [ ] CLI parity and docs.
- [ ] Full regression suite with branch-complete coverage.

