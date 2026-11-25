#!/usr/bin/env bash
# Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
# Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
# written permission is prohibited.

# Runs the tests with coverage and fails when it falls below a floor (ADR-0020, .claude/rules/testing.md).
#
#   tool/coverage.sh          both languages
#   tool/coverage.sh rust     the Rust workspace: lines and regions
#   tool/coverage.sh dart     the Dart code in lib/: lines and branches
#
# Rust uses rustc's own instrumentation (`-C instrument-coverage`) and llvm-cov from the llvm-tools component of the
# pinned release: run `rustup component add llvm-tools` in rust/ once. A stable compiler can't measure branches, so
# regions stand in for them; a region is a stretch of code that runs or doesn't, such as each side of `&&` or each
# `match` arm. Dart uses `flutter test --branch-coverage`, so `flutter` must be Flutter 3.50.1 on PATH.

set -euo pipefail

# The floors, in whole percent. Each is the coverage measured on 2026-11-24, rounded down. Raise one whenever coverage
# passes it; never lower one, or exclude code to reach one (.claude/rules/testing.md).
RUST_LINE_FLOOR=97
RUST_REGION_FLOOR=95
DART_LINE_FLOOR=91
DART_BRANCH_FLOOR=88

# The code coverage leaves out. Only generated code, which CI regenerates and compares (ADR-0012), belongs here, and
# only with the maintainer's agreement. The Rust pattern also leaves out the dependencies, the standard library and the
# integration tests in each crate's tests/.
RUST_IGNORE='(/\.cargo/(registry|git)/|^/rustc/|/rust/crates/[^/]+/tests/|/rust/crates/bridge/src/frb_generated\.rs$)'
DART_IGNORE='^lib/platform/rust/|^lib/l10n/app_localizations[^/]*\.dart$'

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
failed=0

# check <what> <covered> <total> <floor> <variable>: prints the figure, and records a failure below the floor.
check() {
  local what="$1" covered="$2" total="$3" floor="$4" variable="$5"
  if [ "$total" -eq 0 ]; then
    echo "::error::$what: nothing was measured."
    failed=1
    return
  fi
  local whole=$(( covered * 100 / total ))
  local shown
  shown="$(awk -v c="$covered" -v t="$total" 'BEGIN { printf "%.2f", 100 * c / t }')"
  if [ $(( covered * 100 )) -lt $(( floor * total )) ]; then
    echo "::error::$what: $shown% ($covered of $total) is below the floor of $floor%."
    failed=1
  elif [ "$whole" -gt "$floor" ]; then
    echo "$what: $shown% ($covered of $total), above the floor of $floor%. Raise $variable to $whole in this script."
  else
    echo "$what: $shown% ($covered of $total), floor $floor%."
  fi
}

rust_coverage() {
  cd "$root/rust"
  local host sysroot bin
  host="$(rustc -vV | sed -n 's/^host: //p')"
  sysroot="$(rustc --print sysroot)"
  bin="$sysroot/lib/rustlib/$host/bin"
  if [ ! -x "$bin/llvm-cov" ] || [ ! -x "$bin/llvm-profdata" ]; then
    echo "::error::llvm-cov isn't installed for this toolchain. Run 'rustup component add llvm-tools' in rust/."
    exit 1
  fi

  # A target folder of its own, so the instrumented build doesn't replace the normal one. Naming --target applies
  # RUSTFLAGS only to the code under test, not to build scripts and procedural macros.
  export CARGO_TARGET_DIR="$root/rust/target/coverage"
  export RUSTFLAGS="-C instrument-coverage"
  local profiles="$CARGO_TARGET_DIR/profraw"
  rm -rf "$profiles"
  mkdir -p "$profiles"
  export LLVM_PROFILE_FILE="$profiles/%p-%m.profraw"

  cargo test --workspace --locked --target "$host"

  local objects=()
  local executable
  while IFS= read -r executable; do
    objects+=(--object "$executable")
  done < <(cargo test --workspace --locked --target "$host" --no-run --message-format=json 2>/dev/null \
    | grep '"reason":"compiler-artifact"' | grep '"test":true' \
    | sed -n 's/.*"executable":"\([^"]*\)".*/\1/p')
  if [ "${#objects[@]}" -eq 0 ]; then
    echo "::error::cargo listed no test binaries."
    exit 1
  fi

  "$bin/llvm-profdata" merge -sparse "$profiles"/*.profraw -o "$CARGO_TARGET_DIR/coverage.profdata"
  local report
  report="$("$bin/llvm-cov" report --instr-profile "$CARGO_TARGET_DIR/coverage.profdata" "${objects[@]}" \
    --ignore-filename-regex="$RUST_IGNORE")"
  echo "$report"

  # The TOTAL row: Regions, Missed Regions, Cover, Functions, Missed Functions, Executed, Lines, Missed Lines, Cover,
  # Branches, Missed Branches, Cover.
  local totals
  totals="$(echo "$report" | awk '$1 == "TOTAL" && NF == 13 { print $2, $3, $8, $9 }')"
  if [ -z "$totals" ]; then
    echo "::error::llvm-cov's report has no TOTAL row in the expected columns."
    exit 1
  fi
  local regions missed_regions lines missed_lines
  read -r regions missed_regions lines missed_lines <<< "$totals"
  check "Rust lines" $(( lines - missed_lines )) "$lines" "$RUST_LINE_FLOOR" RUST_LINE_FLOOR
  check "Rust regions" $(( regions - missed_regions )) "$regions" "$RUST_REGION_FLOOR" RUST_REGION_FLOOR
}

dart_coverage() {
  cd "$root"
  if ! command -v flutter > /dev/null; then
    echo "::error::flutter isn't on PATH. Put the bin folder of Flutter 3.50.1 on PATH."
    exit 1
  fi

  # Dart's coverage leaves out every library no test loads, as if it weren't there. This extra test imports each library
  # in lib/, so a file no test reaches counts as uncovered instead.
  local package every lcov
  package="$(sed -n 's/^name: *//p' pubspec.yaml)"
  every="$root/.dart_tool/coverage/every_library_test.dart"
  lcov="$root/coverage/lcov.info"
  mkdir -p "$(dirname "$every")" "$(dirname "$lcov")"
  rm -f "$lcov"
  {
    echo "// Written by tool/coverage.sh on every run. It imports each library in lib/, so that coverage counts the"
    echo "// libraries no test loads."
    local n=0 file
    while IFS= read -r file; do
      n=$(( n + 1 ))
      echo "import 'package:$package/${file#lib/}' as library$n;"
    done < <(find lib -name '*.dart' | sort | grep -Ev "$DART_IGNORE" | while IFS= read -r f; do
      grep -q '^part of' "$f" || echo "$f"
    done)
    echo
    echo "void main() {}"
  } > "$every"

  flutter test --branch-coverage --coverage-path "$lcov" test "$every"

  # Flutter writes no branch totals (BRF and BRH), so count the BRDA records: line, block, branch, and the times it
  # was taken, or "-" when its block never ran.
  local totals
  totals="$(awk -F'[:,]' -v ignore="$DART_IGNORE" '
    /^SF:/ { file = $2; skip = (file ~ ignore) }
    skip { next }
    /^LF:/ { lf = $2; lines += $2 }
    /^LH:/ { lh = $2; hit_lines += $2 }
    /^BRDA:/ { bf++; branches++; if ($5 != "-" && $5 + 0 > 0) { bh++; hit_branches++ } }
    /^end_of_record/ {
      if (lh < lf || bh < bf) printf "  %s: lines %d of %d, branches %d of %d\n", file, lh, lf, bh, bf > "/dev/stderr"
      lf = lh = bf = bh = 0
    }
    END { print hit_lines + 0, lines + 0, hit_branches + 0, branches + 0 }
  ' "$lcov")"
  local hit_lines lines hit_branches branches
  read -r hit_lines lines hit_branches branches <<< "$totals"
  check "Dart lines" "$hit_lines" "$lines" "$DART_LINE_FLOOR" DART_LINE_FLOOR
  check "Dart branches" "$hit_branches" "$branches" "$DART_BRANCH_FLOOR" DART_BRANCH_FLOOR
}

case "${1:-all}" in
  rust) rust_coverage ;;
  dart) dart_coverage ;;
  all)
    # Each language in a process of its own, so a failure in one still reports the other.
    status=0
    "$0" rust || status=1
    "$0" dart || status=1
    exit "$status"
    ;;
  *)
    echo "Usage: tool/coverage.sh [rust|dart]" >&2
    exit 2
    ;;
esac
exit "$failed"
