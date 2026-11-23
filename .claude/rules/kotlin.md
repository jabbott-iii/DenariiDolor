---
paths:
  - "**/*.kt"
  - "**/*.kts"
---

# Kotlin conventions

Match the file you're editing first. These rules cover what it doesn't show.

## Files and style

- Start each new file with the 5-line proprietary copyright block, copied from any existing `.kt` file, then the `package`
  line.
- Code that needs nothing from Java or Android, and isn't injected yet, can live in `shared/src/commonMain/kotlin`, in the
  same package it would have in `:app`. Common code may use only the Kotlin standard library, kotlinx-coroutines and
  kotlinx-datetime: no `java.*`, `android.*`, `String.format`, `Charsets` or `@Inject` (`SharedCodeBoundaryTest`). Keep its
  API public, because `internal` stops at the module boundary.
- Follow ktlint's `android_studio` style: 140 columns, no wildcard imports, no trailing commas, imports in ktlint's order.
  Run `./gradlew ktlintFormat` rather than formatting by hand. Then revert any hunks it changed in files you didn't touch.
- Use expression bodies for single-expression functions. That's the house style (see `CategoryRepositoryImpl`).
- Naming:
  - Composables are PascalCase.
  - Constants are `const val UPPER_SNAKE`.
  - Test-tag constants are `*_TAG`, declared next to their screen.
  - Private mutable state uses a leading underscore (`_isSaving`), with a read-only public view.

## Types and null safety

- Don't use `!!`; the codebase has none. Use `?:`, an early return, `checkNotNull(x) { "why" }` or
  `requireNotNull(x) { "why" }` instead.
- Prefer `val`, read-only `List`, `Map` and `Set` in signatures, and `data class` with `copy()` for state. Keep
  `MutableStateFlow` private and expose it with `asStateFlow()`.
- Model closed sets with an `enum` or a `sealed interface`, and use an exhaustive `when` without `else`, so that a new case
  fails to compile until it's handled.
- Give public and internal functions explicit return types. Use the smallest visibility that works: `private`, then
  `internal`, then public.
- Use `require` and `check` for programming errors. Use `domainFailure(DomainError.X, "…", arg)` for business-rule failures,
  which reach the user through `UiMessage`.
- Use `lateinit` only for framework-injected fields. Annotate nullability at Java interop boundaries, so that platform
  types don't leak.
- Use only language features and APIs that are stable in the pinned Kotlin version. Ask before adding an `@OptIn`, an
  experimental API or a `-X` compiler flag. The one existing opt-in is `ExperimentalCoroutinesApi`.

## Coroutines and Flow

- Use structured concurrency only: `viewModelScope`, `lifecycleScope` or the caller's scope. Never use `GlobalScope`, and
  never use `runBlocking` in `app/src/main`.
- Suspend work that yields a `Result` goes in `runSuspendCatching { }`. Never catch `CancellationException` without
  rethrowing it. That includes catch-all `catch (e: Exception)` blocks around suspend calls.
- Room's `suspend` and `Flow` DAO methods are already main-safe, so don't wrap them in `withContext`.
- Blocking work runs in the data layer under `withContext(Dispatchers.IO)`: streams, files, PDF, Keystore and PBKDF2. Follow
  the pattern in `BackupManager`, `ReportExporter` and `Vault`. ViewModels never switch dispatchers.
- Screen state uses `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)`.
- One-shot events use `Channel<UiMessage>(Channel.BUFFERED)`, exposed with `receiveAsFlow()`. Don't use a replaying
  `SharedFlow` or a `StateFlow<String?>` for events: they re-fire or get lost.
- When inputs change, derive the result with `flatMapLatest` or `combine` over a `StateFlow` of the inputs, as
  `SearchViewModel` does. Don't start a new collector on every call.
- Secrets in a `ByteArray` (keys, passphrases): zero them with `fill(0)` in a `finally` block, and compare them with
  `MessageDigest.isEqual`, never `contentEquals` or `==`.

## Time and numbers

- Don't use `Date`, `Calendar` or `System.currentTimeMillis()` in logic.
- `domain/` and `util/` use kotlinx-datetime and `kotlin.time`, so they can be shared with iOS. Presentation and data still
  use `java.time` until they move.
  - Domain and util code injects `ZonedClock` and takes the zone from `clock.timeZone`. Other code injects `java.time.Clock`
    and takes the zone from `clock.zone`. Both come from `DeviceClock`; `asZonedClock()` adapts one to the other.
  - Two files keep `java.time`: `DeviceClock`, which is the Android clock, and `PlatformFormat.android.kt` (see below).
  - Dates are stored as epoch millis. Month ranges come from `DateUtils`.
  - A function that formats or parses a date takes the zone as a required argument, never with a system-zone default. Pass
    the injected clock's zone. A `MonthlyReport` carries the zone its month was computed in, and the Report screen and both
    exports show its dates in it. `ClockBoundaryTest` fails if anything but `DeviceClock` reads the system time zone.
- Amounts are `Long` cents. `Money` parses and scales them in plain Kotlin, without `BigDecimal`. Never use `Float` or `Double`
  for a value that is stored, compared or summed.
- Whatever depends on the device's locale data goes through `PlatformFormat`: how money is written, the decimal mark and
  currency symbols that parsing needs, month names and zone names, and which currency a code or region means.
  - `shared/src/commonMain/.../util/PlatformFormat.kt` declares it with `expect`, and `PlatformFormat.android.kt` in
    `androidMain` implements it on `java.text`, `java.math` and `java.time`. An iOS version must match `MoneySnapshotTest`.
  - Shared code names locales and currencies `PlatformLocale` and `PlatformCurrency`. On Android these are typealiases of
    `java.util.Locale` and `java.util.Currency`, so Android code keeps passing those. `:shared` compiles with
    `-Xexpect-actual-classes` because Kotlin still calls expect classes Beta.
  - Amounts and `parseToCents` default to the format locale (`defaultFormatLocale()`); transfer arrows, month names and the
    CSV period follow the language (`defaultLocale()`). `DefaultLocalesTest` pins which is which.

## Static analysis

detekt builds on the default rules plus `config/detekt/detekt.yml`:

- `LongParameterList` is 8, and Composables are exempt.
- `LongMethod` is 80 lines.
- `ReturnCount` is 4, and guard clauses don't count.
- `MaxLineLength` is 140.
- `MagicNumber` ignores property declarations.

Refactor instead of suppressing. When a suppression really is right, put it at the narrowest scope with a reason:
`@Suppress("TooGenericExceptionCaught") // Intentional: …`.
