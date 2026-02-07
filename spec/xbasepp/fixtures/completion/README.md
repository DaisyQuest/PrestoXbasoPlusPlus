# Completion Fixtures

Completion fixtures are organized by operation ID and provide **min** and **edge** cases for IntelliSense.

## Layout
```
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

## Conventions
- The completion caret is marked with `/*caret*/`.
- `expected.completions.json` is **fully ordered**.
- `expected.ranking.json` must exercise **all ranking branches**.

Refer to `spec/xbasepp/completion-fixtures.md` for schema details.
