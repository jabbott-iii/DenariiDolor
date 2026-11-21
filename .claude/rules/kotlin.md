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

- Use `java.time` only. Don't use `Date`, `Calendar` or `System.currentTimeMillis()` in logic.
  - Inject `Clock`, and take the zone from `clock.zone`.
  - Dates are stored as epoch millis. Month ranges come from `DateUtils`.
- Amounts are `Long` cents. Use `BigDecimal` only inside `Money` for parsing and scaling. Never use `Float` or `Double` for a
  value that is stored, compared or summed.

## Static analysis

detekt builds on the default rules plus `config/detekt/detekt.yml`:

- `LongParameterList` is 8, and Composables are exempt.
- `LongMethod` is 80 lines.
- `ReturnCount` is 4, and guard clauses don't count.
- `MaxLineLength` is 140.
- `MagicNumber` ignores property declarations.

Refactor instead of suppressing. When a suppression really is right, put it at the narrowest scope with a reason:
`@Suppress("TooGenericExceptionCaught") // Intentional: …`.
