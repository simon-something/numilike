# NumiLike

A natural language calculator for Android, inspired by [Numi](https://numi.app). Built for GrapheneOS / Pixel devices (API 33+).

Type expressions in plain English — results appear instantly on the right.

## Features

**Math & operators**
```
4 + 3                          7
(2 + 3) * 4                   20
2 ^ 10                      1024
0xFF & 0x0F                   15
6(3)                          18
```

**Unit conversions** — 70+ built-in units across length, mass, time, temperature, angle, data, area, volume, CSS
```
10 km to miles             6.214
100 celsius in fahrenheit    212
1 GB in MB                 1,000
180 degrees in radians     3.142
```

**Currency** — live fiat rates (ECB via Frankfurter) and crypto (CoinGecko), fetched on demand
```
$50 in EUR                €46.00
10 EUR to HUF          3,839 HUF
1 BTC in USD         $68,432.00
```

**Percentages** — all 9 Numi-style patterns
```
20% of 100                    20
5% on 30                    31.5
6% off 40                   37.6
5% of what is 6              120
$50 as a % of $100           50%
```

**Variables & constants**
```
tax = 21%
$200 + tax                  $242
$200 - tax                  $158
pi * 2                     6.283
```

**Line references**
```
Bread $3.50
Milk $4.20
Eggs $2.80
sum                       $10.50
avg                        $3.50
prev * 2                   $7.00
```

**Date & time**
```
today + 2 weeks       Apr 19, 2026
now - 3 hours           11:30 AM
1 year in hours            8,766
PST time                  7:30 AM
```

**Functions** — sqrt, cbrt, abs, round, ceil, floor, sin, cos, tan, log, ln, fact, and more
```
sqrt 16                        4
fact 5                       120
log(2;8)                       3
```

**Number formats**
```
255 in hex                  0xFF
10 in binary              0b1010
5300 in sci              5.3E+3
```

**Custom definitions** — define your own units, functions, and constants in Settings
```
1 horse = 2.4 m
bmi(w; h) = w / h ^ 2
rent = 1450 EUR
```

**Comments & labels**
```
# Monthly Budget
Rent:
1450 EUR                €1,450.00
Groceries $400            $400.00
// don't forget utilities
```

## Install

### From source

Requires Android SDK (API 35) and JDK 17.

```bash
git clone https://github.com/simon-something/numilike.git
cd numilike
echo "sdk.dir=$ANDROID_HOME" > local.properties
./gradlew assembleRelease
```

The APK is at `app/build/outputs/apk/release/app-release-unsigned.apk`. Sign it with your key:

```bash
apksigner sign --ks your-keystore.jks app/build/outputs/apk/release/app-release-unsigned.apk
```

Or install the debug build directly:

```bash
./gradlew installDebug
```

### From APK

Download the latest APK from [Releases](https://github.com/simon-something/numilike/releases) and sideload it.

## Architecture

Three Gradle modules:

- **`:core`** — Pure Kotlin JVM. Pratt parser, evaluator, unit registry, DSL parser. 340 tests, runs on JVM in seconds.
- **`:data`** — Android library. Currency rate fetching (Ktor), scratchpad/settings persistence (DataStore).
- **`:app`** — Jetpack Compose + Material 3 UI. Hilt DI. Per-line LazyColumn layout with tap-to-copy results.

## License

MIT
