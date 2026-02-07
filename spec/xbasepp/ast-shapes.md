# Normalized AST Shapes & Diagnostics

This document defines the **normalized** AST shapes for the xbase++ parser. The normalized form is used by the
`GoldenTestHarness` so every operation can compare against a consistent structure. The goal is to keep the
AST readable, deterministic, and stable across dialect nuances.

## General Principles
- Every file/fixture is wrapped in a `File` root node.
- Nodes are named using a dotted namespace: `Expr.*`, `Stmt.*`, `Decl.*`.
- Attributes are lowercase with snake-case keys, and values are normalized (no quotes unless needed).
- Optional constructs still appear as nodes when normalization requires a stable tree (for example, `Block` nodes
  for `IF/ELSE` branches).

## Expression Shapes

### Identifier
```
File
  Expr.Identifier[name=foo]
```

### Numeric Literal
```
File
  Expr.Literal.Number[value=1]
```

### String Literal
```
File
  Expr.Literal.String[value=hello]
```

### Unary Negation
```
File
  Expr.Unary.Negation
    Expr.Literal.Number[value=1]
```

### Binary Operators
Binary operators are normalized as left-associative nodes with ordered children.

```
File
  Expr.Binary.Multiply
    Expr.Literal.Number[value=2]
    Expr.Literal.Number[value=3]
```

```
File
  Expr.Binary.Add
    Expr.Literal.Number[value=1]
    Expr.Literal.Number[value=2]
```

## Statement Shapes

### Assignment
```
File
  Stmt.Assignment
    Expr.Identifier[name=x]
    Expr.Literal.Number[value=1]
```

### IF / ELSE
A missing `ELSE` branch is still represented as an empty `Block[branch=else]` for stability.

```
File
  Stmt.If
    Expr.Identifier[name=x]
    Block[branch=then]
    Block[branch=else]
```

### FOR Loop
`STEP` defaults to `1` when omitted.

```
File
  Stmt.For
    Expr.Identifier[name=i]
    Expr.Literal.Number[value=1]
    Expr.Literal.Number[value=10]
    Expr.Literal.Number[value=1]
```

## Declaration Shapes

### Function
```
File
  Decl.Function[name=Foo]
    Params
    Block
      Stmt.Return
        Expr.Literal.Nil
```

## Diagnostic Rules
Diagnostics are normalized to a small, deterministic set to make golden comparisons stable:

- `error.syntax.unexpected-token`: Unexpected token at the current parse position.
- `error.syntax.missing-token`: Required token missing before recovery.
- `error.syntax.unclosed-block`: Block starter missing its closing keyword.
- `error.syntax.invalid-literal`: Literal has invalid format (string/number/date).
- `error.syntax.invalid-operator`: Operator appears in an invalid position.

Each operation in `operations.yaml` should specify the exact diagnostic IDs expected for its
min and edge fixtures (empty list when no errors are expected).
