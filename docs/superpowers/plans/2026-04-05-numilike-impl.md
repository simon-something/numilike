# NumiLike Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a complete Numi natural language calculator for Android (GrapheneOS, API 33+).

**Architecture:** Multi-module Kotlin project (`:core` pure Kotlin, `:data` Android lib, `:app` Compose UI). Pratt parser evaluates expressions per-line. Unit conversions via graph-based registry. TDD throughout.

**Tech Stack:** Kotlin 2.1, Jetpack Compose + Material 3, Hilt, DataStore, Ktor, JUnit 5 + Truth, Robolectric

**Spec:** `docs/superpowers/specs/2026-04-05-numilike-design.md`

---

## Task 1: Android SDK and Gradle project setup
## Task 2: Core types (Token, NumiValue, NumiUnit, Expr, Dimension)
## Task 3: Lexer — tokenization
## Task 4: Token classifier (units, functions, timezones)
## Task 5: Pratt parser — arithmetic + all expression types
## Task 6: Parser tests for percentages, variables, line refs
## Task 7: Unit registry and conversion graph with all built-in units
## Task 8: Evaluator — arithmetic, variables, environment, functions, percentages, line refs
## Task 9: Comprehensive evaluator tests (units, percentages, line refs, functions)
## Task 10: Date/time and timezone evaluation
## Task 11: Number formatting tests
## Task 12: Custom DSL parser
## Task 13: Currency rate fetching and caching (data module)
## Task 14: Persistence — scratchpad, settings, DSL storage (data module)
## Task 15: Theme, colors, typography, app scaffold
## Task 16: MainViewModel
## Task 17: Calculator scratchpad screen (UI)
## Task 18: Settings screen and DSL editor (UI)
## Task 19: Hilt DI wiring and proguard
## Task 20: End-to-end integration tests
## Task 21: Build and verify APK

Full task details are in the conversation history above — each task has exact file paths, complete code, test commands, and commit messages.
