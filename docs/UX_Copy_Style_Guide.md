# PrestoXbasoPlusPlus — UX Copy Style Guide

This guide ensures user-facing text feels **professional, precise, and authoritative**. Use it for inspections, tooltips, quick-fixes, gutter labels, and UI surfaces.

---

## 1) Voice & Tone

**We are:**
- Precise and technical.
- Helpful without being verbose.
- Confident, never speculative.

**We are not:**
- Casual or slang-heavy.
- Vague (“maybe,” “might,” “could”).
- Apologetic or uncertain.

**Example**
- ✅ “Missing `ENDCASE` for `DO CASE` block.”
- ❌ “You might have forgotten an ENDCASE here.”

---

## 2) Structure Rules

**Short, actionable, and direct.**

- Start with the *problem*, not the solution.
- Use concrete terms (`identifier`, `keyword`, `token`) instead of “thing”.
- Provide a fix suggestion only when we can be confident.

**Pattern**
> **Problem** + **Context** + **Action**  
> “Unexpected `ENDIF` in `DO WHILE` block. Remove `ENDIF` or close the `DO WHILE`.”

---

## 3) Capitalization & Formatting

- Use **sentence case** for messages.
- Use **code formatting** for tokens/keywords (`ENDCASE`, `FUNCTION`, `:=`).
- Avoid excessive punctuation.

**Example**
- ✅ “Expected `THEN` after `IF` condition.”
- ❌ “Expected THEN after IF condition!!!”

---

## 4) Severity & Confidence

**Error**  
Use for definitive syntax or semantic violations.

**Warning**  
Use for ambiguous or risky patterns with strong evidence.

**Info / Hint**  
Use sparingly for suggestions that are non-blocking.

If confidence is low, do **not** emit a message.

---

## 5) Quick Fix Language

- Begin with a **verb**: “Insert”, “Remove”, “Rename”, “Replace”.
- Be explicit about the target.

**Examples**
- “Insert `ENDIF`”
- “Replace `:=` with `=`”
- “Rename symbol to `CustomerId`”

---

## 6) Example Glossary

| Term | Usage |
|------|-------|
| “keyword” | A language keyword like `IF`, `ELSE`, `RETURN` |
| “identifier” | A variable, function, or symbol name |
| “token” | A lexer token |
| “expression” | A syntactic expression |
| “statement” | A syntactic statement |
| “block” | A structured region (e.g., `IF` / `ENDIF`) |

---

## 7) Review Checklist

- [ ] Problem is precise and unambiguous.
- [ ] Tokens/keywords are formatted with backticks.
- [ ] Message is neutral and professional.
- [ ] If a fix is suggested, it is safe and deterministic.

