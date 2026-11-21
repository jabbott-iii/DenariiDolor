---
paths:
  - "PRIVACY.md"
  - "THIRD_PARTY_NOTICES.md"
  - "LICENSE"
  - "licenses/**"
  - "app/src/main/java/com/denariidolor/data/legal/**"
---

# Legal documents shipped in the app

- **They ship in the app.** `PRIVACY.md`, `THIRD_PARTY_NOTICES.md` and `licenses/` are copied into the app's `legal` assets
  at build time (the `CopyLegalDocuments` task in `app/build.gradle.kts`), and `LegalText` renders them in Settings.
- **Markdown subset.** Keep both Markdown files to what `LegalText` renders:
  - `#` headings, paragraphs and `-` lists;
  - pipe tables;
  - `**bold**`, `` `code` `` and links.

  `LegalTextTest` checks both files.
- **Privacy policy.**
  - Update `PRIVACY.md` whenever the app changes what it stores, exports or shares, or which permissions it requests.
  - Tell the maintainer that the Play Console Data safety form and the public privacy-policy URL must match it.
  - Today's position: the app collects and shares nothing, and backups and reports are made by the user on the device.
- **Third-party notices.**
  - When a dependency that ships in the APK is added or removed, update `THIRD_PARTY_NOTICES.md`, and add or remove its
    license text in `licenses/` when the license requires one.
  - Build-only tools don't belong there.
- **License.**
  - `LICENSE` is the proprietary end-user license, and an attorney review is still pending.
  - Never edit it, or the copyright header in source files, unless you're explicitly told to.
  - Never add an open-source license header to a source file.
