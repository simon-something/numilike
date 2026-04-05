# NumiLike — Design Specification

**Date**: 2026-04-05
**Status**: Draft
**Target**: Android (GrapheneOS), Kotlin + Jetpack Compose

## 1. Overview

NumiLike is a native Android port of [Numi](https://numi.app), the natural language calculator. It targets GrapheneOS on Pixel devices (API 33+). The app is a single-screen scratchpad where each line is an independently evaluated expression, with results shown in a right-aligned column alongside the input.

The app is functionally equivalent to Numi's full desktop feature set, with adaptations for mobile (touch-to-copy, no live-updating clock, on-demand currency fetch).

## 2. Decisions Log

| Decision | Choice | Rationale |
|---|---|---|
| Stack | Kotlin + Jetpack Compose | Native Android, best Compose/Material 3 support |
| Currency | On-demand fetch, fiat + crypto, cached offline | Respects GrapheneOS privacy ethos, no background network |
| Extensions | Lightweight DSL (custom units, functions, constants) | Covers plugin use cases without JS engine security concerns |
| Localization | English only (parser + UI) | Avoids parser ambiguity, keeps testing surface manageable |
| Document model | Single scratchpad + share/export | Simple UX, auto-persist, Android share sheet integration |
| Display | Side-by-side, numbered line gutter, autowrap | Faithful to Numi, clear line-to-result mapping |
| Copy UX | Tap-to-copy, long-press options, 48dp+ targets | Adapted Numi's click-to-copy for touch |
| Min API | 33 (Android 13) | All current GrapheneOS devices, latest Compose APIs |
| Architecture | Multi-module, Pratt parser | Testable, extensible, no external parser dependency |

## 3. Module Architecture

```
numilike/
├── app/                          # Android app (UI, DI, ViewModel)
│   └── src/main/java/com/numilike/
│       ├── MainActivity.kt
│       ├── NumiApp.kt
│       ├── di/                   # Hilt modules
│       ├── ui/
│       │   ├── theme/            # Material 3 theme (light/dark)
│       │   ├── calculator/       # Main scratchpad screen
│       │   ├── settings/         # Settings + DSL editor
│       │   └── components/       # Shared composables
│       └── viewmodel/
├── core/                         # Pure Kotlin (parser, evaluator, units)
│   └── src/
│       ├── main/kotlin/com/numilike/core/
│       │   ├── lexer/            # Tokenizer
│       │   ├── parser/           # Pratt parser
│       │   ├── eval/             # Evaluator + environment
│       │   ├── units/            # Unit registry, conversion graph
│       │   ├── functions/        # Built-in functions
│       │   └── types/            # Value types
│       └── test/                 # Pure JVM tests
├── data/                         # Android library (network, persistence)
│   └── src/main/kotlin/com/numilike/data/
│       ├── currency/             # Rate fetching + caching
│       ├── persistence/          # Scratchpad auto-save (DataStore)
│       └── customdsl/            # User-defined definitions storage
└── build.gradle.kts
```

**`:core`** is pure Kotlin with zero Android dependencies. All parser and evaluator tests run on JVM in milliseconds.

**`:data`** is an Android library handling network (Ktor), persistence (DataStore), and caching.

**`:app`** depends on both modules and composes the UI with Hilt for DI.

## 4. Parser & Evaluator

### 4.1 Pipeline

```
Input string → Lexer (tokens) → Parser (AST) → Evaluator (NumiValue) → Formatter (display string)
```

### 4.2 Lexer

Converts raw text to tokens. Keyword-aware: recognizes `plus`, `times`, `in`, `of`, `off`, `on`, `what`, `as`, `prev`, `sum`, `avg`, etc.

Token examples:

| Input | Tokens |
|---|---|
| `20 + 3` | `NUMBER(20)`, `PLUS`, `NUMBER(3)` |
| `$20 in EUR` | `CURRENCY_SYMBOL($)`, `NUMBER(20)`, `KEYWORD_IN`, `UNIT(EUR)` |
| `sin 45°` | `FUNCTION(sin)`, `NUMBER(45)`, `UNIT(°)` |
| `v = 10 kg` | `IDENTIFIER(v)`, `ASSIGN`, `NUMBER(10)`, `UNIT(kg)` |
| `// comment` | `COMMENT(// comment)` |

Number literals support decimal, binary (`0b`), octal (`0o`), and hexadecimal (`0x`).

### 4.3 Pratt Parser — Precedence Table

From lowest to highest binding power:

| Level | Operators | Example |
|---|---|---|
| 1 | Assignment `=` | `v = 10` |
| 2 | Percentage phrases (`of`, `on`, `off`, `as a % of`, `of what is`) | `5% off $30` |
| 3 | Conversion (`in`, `to`, `as`) | `20 inches in cm` |
| 4 | Bitwise OR `\|`, XOR | `0xFF \| 0x0F` |
| 5 | Bitwise AND `&` | `0xFF & 0x0F` |
| 6 | Shift `<<`, `>>` | `1 << 8` |
| 7 | Addition `+`, `-`, `plus`, `minus`, `with`, `without` | `3 + 4` |
| 8 | Multiplication `*`, `/`, `times`, `mod` | `6 * 7` |
| 9 | Exponent `^` | `2 ^ 10` |
| 10 | Unary `-`, `+` | `-5` |
| 11 | Postfix unit attachment | `20 kg`, `$30` |
| 12 | Function call | `sin 45°`, `sqrt 16` |
| 13 | Parentheses, literals | `(3 + 4)` |

### 4.4 AST Nodes

```kotlin
sealed class Expr {
    data class Number(val value: BigDecimal) : Expr()
    data class UnaryOp(val op: Token, val operand: Expr) : Expr()
    data class BinaryOp(val left: Expr, val op: Token, val right: Expr) : Expr()
    data class UnitAttach(val expr: Expr, val unit: UnitRef) : Expr()
    data class Conversion(val expr: Expr, val targetUnit: UnitRef) : Expr()
    data class Percentage(val kind: PctKind, val pct: Expr, val base: Expr?) : Expr()
    data class FunctionCall(val name: String, val args: List<Expr>) : Expr()
    data class Assignment(val name: String, val expr: Expr) : Expr()
    data class VariableRef(val name: String) : Expr()
    data class LineRef(val kind: LineRefKind) : Expr()  // prev, sum, avg
    data class Comment(val text: String) : Expr()
    data class Label(val text: String) : Expr()
}
```

### 4.5 NumiValue

```kotlin
data class NumiValue(
    val amount: BigDecimal,
    val unit: NumiUnit?
)
```

`BigDecimal` with `MathContext` of 34 significant digits (IEEE 754 decimal128). Avoids floating-point rounding errors for currency and chained percentage operations. Converts to `Double` only for trig/log functions.

### 4.6 Evaluator

Walks the AST top-to-bottom, per line. Maintains an `Environment`:
- Variable bindings (user-assigned + custom DSL constants)
- Line results list (for `prev`/`sum`/`avg`)
- Unit registry reference
- Clock reference (injectable for testing)

Each line evaluates independently. A broken line produces `null` in the results list without affecting other lines.

## 5. Unit System

### 5.1 Graph-Based Conversion

Units are organized by **dimension** (length, mass, time, temperature, data, area, volume, angle, currency, CSS). Within each dimension, conversions are ratios to a base unit.

To convert between two units in the same dimension: source → base → target (two multiplications).

### 5.2 Conversion Rules

```kotlin
sealed class ConversionRule {
    data class Ratio(val toBase: BigDecimal) : ConversionRule()
    data class Formula(
        val toBase: (BigDecimal) -> BigDecimal,
        val fromBase: (BigDecimal) -> BigDecimal
    ) : ConversionRule()
}
```

Most units use `Ratio`. Temperature uses `Formula` (Celsius/Fahrenheit have offsets).

### 5.3 Special Cases

**Currency**: Ratios are dynamic, fetched on demand. Base unit is USD. Fiat rates from the [Frankfurter API](https://www.frankfurter.app/) (open-source, ECB-backed, no API key required). Crypto rates from [CoinGecko free API](https://www.coingecko.com/en/api) (no API key required, 10-30 req/min). Cached locally, used offline when available.

**CSS units**: Flow through configurable `ppi` (default 96) and `em` (default 16px). Users can reassign: `em = 20px`, `ppi = 326`.

**Area & Volume**: Derived from length units. `square X` = X's ratio squared (dimension = area). `cubic X` = X's ratio cubed (dimension = volume). Plus dedicated units (hectare, acre, gallon, etc.).

**SI Prefixes**: Lexer recognizes prefixes (pico through tera), registry computes ratio dynamically. `5 kilometers` = `5 * 1000 meters`. No need to hardcode every combination.

**Data units**: Decimal (KB = 1000 B) and binary (KiB = 1024 B) scales. `1 byte = 8 bits`.

### 5.4 Full Unit Catalog

| Dimension | Base Unit | Units |
|---|---|---|
| Length | meter | meter (+ SI), mil, point, line, inch, hand, foot, yard, rod, chain, furlong, mile, cable, nautical mile, league |
| Area | square meter | square [length], hectare, are, acre |
| Volume | cubic meter | cubic [length], teaspoon, tablespoon, cup, pint, quart, gallon, liter (+ SI) |
| Mass | gram | gram (+ SI), tonne, carat, centner, pound, stone, ounce |
| Temperature | Kelvin | Kelvin, Celsius, Fahrenheit |
| Time | second | second (+ SI), minute, hour, day, week, month (1/12 year), year (365 days) |
| Angle | radian | radian, degree |
| Data | byte | bit, byte (+ decimal SI), kibibyte (+ binary SI) |
| CSS | px | px, pt, em |
| Currency | USD | ~150+ fiat (ISO 4217, all from Frankfurter API) + top 50 crypto by market cap from CoinGecko (includes BTC, ETH, SOL, ADA, DOT, XRP, DOGE, etc.) |

## 6. Percentage System

### 6.1 Nine Expression Types

| Expression | Kind | Evaluation |
|---|---|---|
| `20% of $10` | PctOf | `base * pct` |
| `5% on $30` | PctOn | `base * (1 + pct)` |
| `6% off 40 EUR` | PctOff | `base * (1 - pct)` |
| `$50 as a % of $100` | AsPctOf | `value / base` |
| `$70 as a % on $20` | AsPctOn | `(value - base) / base` |
| `$20 as a % off $70` | AsPctOff | `(base - value) / base` |
| `5% of what is 6 EUR` | PctOfWhat | `result / pct` |
| `5% on what is 6 EUR` | PctOnWhat | `result / (1 + pct)` |
| `5% off what is 6 EUR` | PctOffWhat | `result / (1 - pct)` |

### 6.2 Percentage + Units

Units propagate from the base value: `5% on $30 CAD` → `$31.50 CAD`.

### 6.3 Variables as Percentages

When a variable holds a percentage and appears in `+`/`-` with a non-percentage value:
```
tax = 21%
$200 + tax    → $242     (behaves as "21% on $200")
$200 - tax    → $158     (behaves as "21% off $200")
```

## 7. Line References & Aggregates

### 7.1 Keywords

| Keyword | Behavior |
|---|---|
| `prev` | Result of the nearest non-empty line above |
| `sum`, `total` | Sum of consecutive non-empty result lines above, stopping at first empty line/comment/label |
| `avg`, `average` | Average of the same group `sum` would use |

### 7.2 Rules

- `prev` walks backward through results, skips nulls, returns first non-null.
- `sum`/`avg` collect backward until hitting null (empty line) or document start.
- Unit compatibility required for `sum`/`avg`. Incompatible units produce an error for that line.
- All values converted to the unit of the first value in the group.
- `prev` composes freely in expressions: `prev * 2`, `sqrt prev`.

## 8. Date, Time & Timezone

### 8.1 Duration Arithmetic

```
today + 2 weeks          → April 19, 2026
today - 30 days          → March 6, 2026
now + 3 hours            → (current time + 3h)
2 hours + 30 minutes     → 2.5 hours
round(1 month in days)   → 30
```

### 8.2 Duration Units

| Unit | Aliases | Definition |
|---|---|---|
| second | sec, seconds, s | base |
| minute | min, minutes | 60s |
| hour | hours, hr | 3600s |
| day | days | 86400s |
| week | weeks, wk | 604800s |
| month | months, mo | 1/12 year (30.4375 days) |
| year | years, yr | 365 days |

Duration units use the standard unit registry (dimension = time, base = second).

### 8.3 Special Values

- **`today`**: midnight of current date, type `DateTime`
- **`now`**: current date+time, type `DateTime`

Adding a duration to a `DateTime` uses calendar-aware arithmetic via `java.time` (adding 1 month to Jan 31 → Feb 28).

### 8.4 Timezone Conversion

```
time                     → 14:32 (local)
PST time                 → 07:32
New York time            → 10:32
2:30 pm HKT in Berlin   → 8:30 am
```

- Standard abbreviations: `PST`, `EST`, `HKT`, `CET`, `UTC`, `GMT`, etc.
- City names mapped to IANA zones: ~50 common cities (`New York` → `America/New_York`, `Berlin` → `Europe/Berlin`, `Tokyo` → `Asia/Tokyo`, etc.)
- Results re-evaluate on input change and on app resume (not live-updating, to save battery).

### 8.5 Result Formatting

- DateTime results: formatted as dates (`April 19, 2026`) or times (`2:30 pm`)
- Duration results: `2 hours 30 minutes` or numeric when converted (`1 year in hours` → `8,760`)

## 9. Custom DSL

### 9.1 Access

Settings > Custom Definitions — a text editor screen.

### 9.2 Syntax

```
# Custom units
1 horse = 2.4 m
1 league = 3 miles
1 smoot = 170.18 cm

# Custom functions (semicolon-separated args)
bmi(w; h) = w / h ^ 2
markup(cost; margin) = cost + margin% on cost
hyp(a; b) = sqrt(a^2 + b^2)

# Custom constants
tax = 21%
rent = 1450 EUR
```

### 9.3 Implementation

- **Custom units**: registered in unit registry with ratio to referenced base unit.
- **Custom functions**: stored as named AST fragments. Arguments substituted at call time. No recursion (prevents infinite loops).
- **Custom constants**: stored as variable bindings, available on every scratchpad line.

### 9.4 Validation

- Duplicate names flagged (can't redefine built-in units or functions).
- Circular references detected at definition time.
- Base unit must exist.
- Errors shown inline in the definitions editor, per-line.

### 9.5 Namespace

Custom DSL definitions are read-only from the scratchpad. Scratchpad variables and DSL definitions live in separate namespaces. DSL definitions take priority on conflict.

## 10. UI Design

### 10.1 Layout

```
┌──────────────────────────────────┐
│  ⚙  NumiLike              ⋮  │  ← Toolbar (gear → Settings)
├──────────────────────────────────┤
│ 1 │ Groceries:          │        │  ← Label, no result
│ 2 │ Bread               │  $3.50 │
│ 3 │ Milk                │  $4.20 │
│ 4 │ Eggs                │  $2.80 │
│ 5 │ sum                 │ $10.50 │  ← Tap to copy
│ 6 │                     │        │  ← Empty line
│ 7 │ $20 CAD + 5 USD     │ $18.23 │
│   │   - 7% in EUR       │        │  ← Wrapped, no extra number
│ 8 │ today + 2 weeks     │ Apr 19 │
└──────────────────────────────────┘
```

- **Line numbers** in muted gutter. Wrapped lines don't get a new number.
- **Left column**: one continuous `TextField`, free-form typing.
- **Right column**: results right-aligned, vertically aligned per line number.
- **Divider**: thin 1dp line.
- Results for comments, labels, empty lines are blank.

### 10.2 Colors

**Light mode:**

| Element | Color |
|---|---|
| Background | `#FFFFFF` |
| Input text | `#1A1A1A` |
| Result text | `#6B7280` |
| Line numbers | `#D1D5DB` |
| Divider | `#E5E7EB` |
| Error indicator | `#EF4444` |
| Labels/headers | `#1A1A1A` bold |
| Comments | `#9CA3AF` italic |

**Dark mode:**

| Element | Color |
|---|---|
| Background | `#1A1A1A` |
| Input text | `#F3F4F6` |
| Result text | `#9CA3AF` |
| Line numbers | `#4B5563` |
| Divider | `#374151` |
| Error indicator | `#F87171` |
| Labels/headers | `#F3F4F6` bold |
| Comments | `#6B7280` italic |

Follows system dark/light mode by default, with manual toggle in settings.

### 10.3 Typography

- **All text**: JetBrains Mono (bundled, OFL licensed, excellent readability at small sizes)
- **Input**: 16sp, regular weight
- **Results**: 16sp, lighter weight
- **Line numbers**: 12sp, muted color

### 10.4 Tap Targets & Copy

- Each result row: minimum 48dp height.
- **Single tap** on result: copies value to clipboard, snackbar "Copied: $10.50".
- **Long press** on result: bottom sheet with:
  - Copy with unit (`$10.50`)
  - Copy number only (`10.50`)
  - Copy full line (`sum → $10.50`)

### 10.5 Toolbar & Menu

**Toolbar**: app title, settings icon (gear, navigates directly to Settings screen — no drawer, only 2 screens), overflow menu (⋮).

**Overflow menu (⋮)**:
- Share (export as plain text via share sheet)
- Import (receive plain text)
- Clear all (with confirmation dialog)

### 10.6 Settings Screen

- Theme: System / Light / Dark
- Number format: decimal places (auto / 0-10), thousands separator (on/off)
- Custom Definitions: opens DSL editor
- About: version, source link

## 11. Number Formatting

### 11.1 Base Representations

| Prefix | Base | Keyword for conversion |
|---|---|---|
| `0b` | Binary | `binary` |
| `0o` | Octal | `octal` |
| `0x` | Hex | `hex` |
| (none) | Decimal | `decimal` |
| — | Scientific | `sci`, `scientific` |

### 11.2 Display Rules

- **Auto-precision**: strip trailing zeros (`3.50` → `3.5`), except currency (always 2 decimals, crypto up to 8).
- **Thousands separator**: on by default, locale-appropriate.
- **Scientific notation**: triggered by `in sci` or automatically for values > 10^12 or < 10^-6.

### 11.3 Scales

| Scale | Aliases | Factor |
|---|---|---|
| k | thousand | 10^3 |
| M | million | 10^6 |
| B | billion | 10^9 |
| T | trillion | 10^12 |

Case-sensitive: `k` = thousand, `K` = Kelvin. Works in input and output: `$2000000 in M` → `$2M`.

## 12. Error Handling

### 12.1 Philosophy

Per-line resilience. A broken line never affects other lines. Each line evaluates independently (except `prev`/`sum`/`avg` references).

### 12.2 Error Types

| Error | Display |
|---|---|
| Syntax error | Blank result |
| Unknown identifier | Blank result |
| Incompatible units | Muted `⚠`, tap for tooltip |
| Division by zero | `∞` or `-∞` |
| Currency rate unavailable | Muted `⚠`, tooltip: "No rate available" |
| Circular variable reference | Blank result on both lines |
| Expression too complex (>100ms) | Muted `⚠`, "Expression too complex" |

### 12.3 Key Rules

- No modal error dialogs. Errors are inline and non-disruptive.
- Errors are transient — fix the expression and the result appears.
- Partial evaluation: `5 kg + 3` → `8 kg` (dimensionless inherits unit).
- Implicit multiplication: `6(3)` → `18`.
- Division by zero returns infinity, not an error.

### 12.4 Currency Errors

- First launch + offline: `⚠` with "Tap to fetch rates when online".
- Cached rates available: used silently. Settings shows rate age.
- Fetch fails: falls back to cache. `⚠` only if no cache exists.

## 13. Persistence & Data Flow

### 13.1 Scratchpad Auto-Save

The scratchpad text is persisted to DataStore on every change (debounced ~500ms). On app launch, it restores the last saved text and re-evaluates all lines.

### 13.2 Currency Cache

- Rates stored in DataStore as JSON (timestamp + rate map).
- On currency expression evaluation: check cache age. If > 1 hour, attempt fetch. If fetch fails, use cache. If no cache, show error.
- Crypto rates cached separately (may use different API).

### 13.3 Custom DSL

Definitions stored in DataStore as plain text. Parsed and validated on save. Loaded into evaluator environment on app start and on change.

## 14. Dependencies

| Dependency | Purpose |
|---|---|
| Jetpack Compose BOM | UI toolkit |
| Material 3 | Theming, dark mode, components |
| Hilt | Dependency injection |
| DataStore | Persistence |
| Ktor Client (OkHttp engine) | Currency rate fetching |
| kotlinx.serialization | JSON parsing |
| java.time | Date/time/timezone (stdlib at API 33) |
| java.math.BigDecimal | Precision arithmetic (stdlib) |
| JUnit 5 + Truth | Testing |
| Turbine | Flow testing |
| Robolectric | Compose UI tests on JVM |

### Build Target

```
minSdk = 33
targetSdk = 35
compileSdk = 35
kotlin = 2.1.x
java = 17
```

Estimated APK size: ~4-5 MB after R8 minification.

## 15. Testing Strategy

### 15.1 Test Pyramid

**`:core` — ~300-400 pure JVM tests (<5s)**
- Lexer: tokenization of every syntax variant
- Parser: AST construction, precedence, all edge cases
- Evaluator: arithmetic, units, percentages, variables, aggregates, dates
- Unit registry: conversions, SI prefixes, derived units, temperature formulas
- Percentage algebra: all 9 types, full truth table
- Line references: prev/sum/avg, groups, boundaries
- Number formatting: bases, scales, precision, currency
- Error cases: every error type degrades gracefully
- Custom DSL: parsing, validation, registration

**`:data` — ~30-50 integration tests (<10s)**
- Currency fetcher: response parsing, error handling, cache logic
- Persistence: scratchpad save/restore round-trip
- Custom DSL storage: save/load/validate

**`:app` — ~30-50 UI tests (<60s, Robolectric)**
- Scratchpad: type expression → result appears on correct line
- Line numbers: wrapping doesn't create extra numbers
- Tap-to-copy: tap result → clipboard contains value
- Long-press: options appear, each copies correct format
- Dark mode: theme switch applies correct colors
- Settings: theme toggle, number format, DSL editor
- Share/import: export produces correct text

### 15.2 Property-Based Tests

- Commutativity: `a + b == b + a` for dimensionless values
- Conversion round-trip: `x meters in feet in meters ≈ x`
- Percentage identity: `N% of X + (100-N)% of X == X`
- Unit preservation: `(a unit + b unit)` always yields same unit
