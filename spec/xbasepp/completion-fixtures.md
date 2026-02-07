# Completion Fixtures Specification

## Purpose
Completion fixtures define **deterministic** IntelliSense inputs and expected outputs. They enable golden snapshot testing for completion lists, ranking, and context-sensitive filtering.

## Fixture Structure
Fixtures live under `spec/xbasepp/fixtures/completion/` and follow the operation IDs in `operations.yaml`.

```
spec/xbasepp/fixtures/completion/
  <operation-id>/
    min/
      input.xb
      expected.completions.json
      expected.ranking.json
    edge/
      input.xb
      expected.completions.json
      expected.ranking.json
```

## Required Files
- `input.xb`
  - Source input with a completion caret marker: `/*caret*/`.
- `expected.completions.json`
  - Ordered list of completion entries with `label`, `kind`, `source`, and `insertText`.
- `expected.ranking.json`
  - Ranked candidates with `score`, `scope`, and tie-breaker fields.

## Completion Entry Schema (v1)
```json
{
  "entries": [
    {
      "label": "MyFunction",
      "kind": "FUNCTION",
      "source": "LOCAL|GLOBAL|BUILTIN|MEMBER|MACRO",
      "insertText": "MyFunction()",
      "detail": "optional",
      "type": "optional"
    }
  ]
}
```

## Ranking Schema (v1)
```json
{
  "ranked": [
    {
      "label": "MyFunction",
      "score": 0.92,
      "scope": "LOCAL",
      "typeCompat": "EXACT|COMPATIBLE|UNKNOWN",
      "tieBreak": "ALPHA"
    }
  ]
}
```

## Required Coverage
For every `operations.yaml` entry:
- **min** and **edge** completion fixtures.
- At least one entry covering **error recovery** contexts.
- A ranking file that exercises **all scoring branches**.

## Determinism Requirements
- The expected completions list is **fully ordered**.
- No unstable ordering based on hash iteration.
- Tie-breaking must always fall back to alphabetical ordering.

## Validation Rules
- Fixture operation IDs must exist in `operations.yaml`.
- A fixture is invalid if required files are missing.
- Completion entries must include `label`, `kind`, `source`, and `insertText`.

## Notes
- Use the **smallest** representative input for `min`.
- `edge` should stress scope shadowing, ambiguous resolution, or incomplete syntax.
