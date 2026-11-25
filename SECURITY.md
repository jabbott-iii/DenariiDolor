# Security policy

Denarii Dolor keeps each user's financial records only on their phone, encrypted. If you find a way
around that protection, report it privately, as this page describes, so it can be fixed before
anyone else learns of it.

## Supported versions

Security fixes go to the version on Google Play.

| Version | Gets security fixes |
| --- | --- |
| v1, the Kotlin app on Google Play today | Yes, in its latest release |
| Earlier releases of v1 | No. Update from Google Play |
| The Flutter rewrite on `main` | Not released yet. Reports are welcome, and fixes land on `main` |

## Report a vulnerability

Report it through GitHub's private vulnerability reporting:

1. Open the repository's **Security and quality** tab.
2. Select **Report a vulnerability**. It opens the
   [private report form](https://github.com/jabbott-iii/DenariiDolor/security/advisories/new).
3. Fill in the form and submit it. Only you and the maintainer can see the report.

Never put a vulnerability's details in a public issue, discussion, or pull request.

## What to include

- The app version and where it came from (Google Play, or a commit on `main`), and the phone model
  and Android version.
- The steps that show the problem, or a proof of concept.
- What an attacker could do with it, and what they would need first, such as the unlocked phone or a
  copy of a backup file.
- How to reach you, if not through the report itself.

Test only on your own phone, with your own data and backups. Never test against someone else's
device, data, or account.

## What happens next

The maintainer reads the report and keeps you informed in it. When a fix ships, the maintainer
publishes a GitHub security advisory that credits you, unless you'd rather not be named. GitHub can
give the advisory a Common Vulnerabilities and Exposures (CVE) ID.

## Scope

This policy covers the app, the data and backup files it writes, and this repository's code and
workflows.

- The app has no network access, so attacks over a network don't apply to it.
- Attacks that need root access to the phone get best-effort fixes only.
- A vulnerability in a library the app uses goes to that library's own security policy. If it
  affects Denarii Dolor, report it here as well.
