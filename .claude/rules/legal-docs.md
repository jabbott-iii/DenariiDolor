---
paths:
  - "PRIVACY.md"
  - "THIRD_PARTY_NOTICES.md"
  - "LICENSE"
  - "licenses/**"
  - "lib/features/legal/**"
---

# Legal documents shipped in the app

- **They ship in the app.** v1 copied `PRIVACY.md`, `THIRD_PARTY_NOTICES.md` and `licenses/` into the app's `legal`
  assets at build time (the `CopyLegalDocuments` task in `app/build.gradle.kts` at `6d57526`), and `LegalText` rendered
  them in Settings. The Flutter app ships them the same way from M4.7 (`docs/roadmap.md`).
- **Markdown subset.** Keep both Markdown files to what `LegalText` renders:
  - `#` headings, paragraphs and `-` lists;
  - pipe tables;
  - `**bold**`, `` `code` `` and links.

  v1's `LegalTextTest` checked both files, and M4.7 brings the same check to the Flutter app.
- **Privacy policy.**
  - Update `PRIVACY.md` whenever the app changes what it stores, exports or shares, or which permissions it requests.
  - Tell the maintainer that the Play Console Data safety form and the public privacy-policy URL must match it.
  - Today's position: the app collects and shares nothing, and backups and reports are made by the user on the device.
- **Third-party notices.**
  - When a dependency that ships in the app is added or removed, update `THIRD_PARTY_NOTICES.md`, and add or remove its
    license text in `licenses/` when the license requires one. Today's file still lists v1's libraries; M6.2 rewrites it
    for the new app's.
  - Build-only tools don't belong there.
- **License.**
  - `LICENSE` is the proprietary end-user license, and an attorney review is still pending.
  - Never edit it, or the copyright header in source files, unless you're explicitly told to.
  - Never add an open-source license header to a source file.
