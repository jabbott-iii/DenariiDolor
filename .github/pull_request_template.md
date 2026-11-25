## What changed and why

<!-- The change and the problem it solves. Link the issue it closes ("Closes #12") and the roadmap task, such as M2.4. -->

## How it was checked

<!-- Each command you ran and its result, and each check you couldn't run and why, such as the integration tests without an emulator. -->

- [ ] The Rust checks pass from `rust/`: `cargo fmt`, `cargo clippy`, `cargo test`, and `cargo doc`
- [ ] The Flutter checks pass: `dart format`, `flutter analyze`, and `flutter test`
- [ ] `tool/coverage.sh` passes, and tests cover every new line and branch
- [ ] What must travel together is here: regenerated code, a new `DomainError`'s message in all seven languages, and the documents in "Documentation" in `CONTRIBUTING.md`

## Security and stored data

<!-- Name each security-sensitive file this changes (the `paths` at the top of `.claude/rules/security.md`), and any change to how the app reads v1's data or to a stored format, with its decision record. Write "None" if there is none. -->

## Left for later

<!-- A breaking change (a schema version, a backup FORMAT_VERSION, the vault profile, or the application ID) with its decision record, follow-up work, and anything a reviewer could miss. -->
