# PrestoXbasoPlusPlus

**PrestoXbasoPlusPlus** is an IntelliJ IDEA plugin project focused on delivering a **precision-grade, professional** experience for Xbase++ language tooling. The vision is unambiguous: provide a plugin that feels *confident*, *deeply technical*, and *indispensable* in daily development workflows.

If you are looking for the plan that drives the polish initiative, see **[Polish_Plan.md](Polish_Plan.md)**.

---

## ✨ What We Are Building

This project targets a best-in-class experience across the entire editing lifecycle:

- **Accurate language intelligence** (lexer, parser, AST, PSI).
- **High-signal diagnostics** that explain *why* something is wrong and *how* to fix it.
- **Fast navigation** across symbols, references, and structures.
- **IntelliSense that feels predictive**, not just reactive.
- **Beautiful UX** that respects IDE conventions while adding clarity and elegance.

---

## 🧭 Current Status

The project is in active development. Our immediate work is driven by the **Magnificent Polish Plan**, which codifies:

- Detailed quality gates.
- Explicit testing and coverage expectations.
- A phased, auditable roadmap for polishing UI/UX, intelligence, and performance.

We will not trade speed for quality. Each change must be test-backed and aligned with coverage requirements.

---

## 🧠 Architecture (High-Level)

The codebase is organized into distinct tracks to avoid cross-cutting changes:

- **`plugin-core/`** — language foundations (lexer/parser/AST/PSI).
- **`plugin-ide/`** — IntelliJ IDE integrations and features.
- **`plugin-ui/`** — Swing-based UI components.
- **`spec/`** — specifications and fixtures, including operations coverage.
- **`test-framework/`** — testing utilities and infrastructure.

This structure allows each subsystem to evolve with clear ownership and high signal-to-noise.

---

## ✅ Quality & Testing Expectations

We are uncompromising about quality:

- **≥95% line and branch coverage** for lexer/parser/interpreter features.
- **100% operations coverage** mapped from `operations.yaml`.
- **Golden fixtures** for every operation (min + edge).
- **Negative tests** for error recovery and failure paths.

If a change affects behavior, the tests must be updated immediately—no technical debt allowed.

---

## 📌 How to Contribute

1. Read **[Polish_Plan.md](Polish_Plan.md)** for the phased roadmap.
2. Follow the **AGENTS.md guardrails** to ensure correct track ownership.
3. Add tests for every change and verify coverage thresholds.
4. Keep user-facing text professional, precise, and consistent.

---

## 🌌 A Note on Philosophy

This project is designed to feel *intentional*. Every label, diagnostic, and UI surface should reflect deep expertise and an unwavering commitment to polish. We would rather ship fewer features with **magnificent quality** than ship many features with mediocre execution.
