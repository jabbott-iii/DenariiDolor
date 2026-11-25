# Commits

These rules cover commit messages, how work is split into commits, how much Git Claude may run,
and pull request descriptions in this repository. The text of every message also follows
`writing.md` in this folder.

## Project details

A repository fills in this section when it adopts the rule, and the rest of the rule refers to
each value by its bold name. If a value still reads `[FILL IN: …]`, the rule isn't set up here
yet. Work out that value from the repository itself, and tell the user which values are missing
so they can fill them in. Until **Git access** is filled in, treat it as "read-only".

- **Git access:** "commit on request". The default is still a drafted message: stage or commit
  only when the maintainer asks in the current conversation (`CLAUDE.md`, "Working agreement").
- **Done checks:** the "Definition of done" in `CLAUDE.md`, which runs the "Before finishing"
  steps of `rust.md`, `flutter.md`, `testing.md`, `roadmap-and-docs.md` and `writing.md`. Today
  that means the **Check commands** in `rust.md`, run from `rust/`, and in `flutter.md`, run from
  the repository root.
- **Never commit:** `build/` (it holds drafted commit messages), `rust/target/`, `.dart_tool/`,
  `.idea/`, `.gradle/`, `.kotlin/`, `local.properties`, `key.properties`, signing material
  (`*.jks`, `*.keystore`), store credentials (`service-account*.json`), and release outputs
  (`*.apk`, `*.aab`, `*.ipa`)
- **Push effect:** pushing to `main` runs CI (`ci.yml`) and the security scans (`security.yml`),
  nothing else. The tag-driven release pipeline (`cd.yml`) left with the Kotlin app under
  ADR-0003 (replace the Kotlin app now). Until M6.1 brings a new one, a `v*` tag builds and
  publishes nothing
- **Scopes:** the Rust crates (`core`, `store`, `report`, `bridge`, `keystore`, and later `vault` and
  `backup`);
  the app's areas (`auth`, `session`, `dashboard`, `search`, `transaction`, `budget`, `category`,
  `account`, `settings`, `legal`, `i18n`, `ui`); the native folders (`android`, `ios`); and
  `deps`, `build`, `ci`, `docs`, and `claude` for `.claude/` and `CLAUDE.md`
- **Changes that travel together:** a roadmap task's code, tests, ticked checkbox, and the
  **Status locations** of `roadmap-and-docs.md`; a schema change with its migration, its fixture,
  and its test; a new `DomainError` variant with its message in all seven languages; and a change
  to stored data with the matching update to `docs/specs/android-v1-formats.md` or the crate's
  spec
- **Task IDs:** end the summary with the roadmap task's ID in parentheses, for example
  `feat(core): port money parsing to Rust (M0.5)`, when the commit finishes or advances one
- **Breaking changes:** a change that installed copies can't go back from: a schema version, a new
  backup `FORMAT_VERSION`, a change to the vault profile or key wrapping, or a changed application
  ID or bundle identifier. It adds `BREAKING CHANGE:` and names the ADR in `docs/decisions.md`
  that records it

## Handing off commit messages

The user reviews the work and makes the commits. Claude writes each message for them to copy.

- When you finish a task that changed files in this repository, end your reply with a copy-ready
  commit message for that work. Don't wait to be asked.
- Write the message only once the work is complete and the **Done checks** pass. If a check
  couldn't run, say which one and why in your reply.
- Put each message in its own fenced code block tagged `text`, containing only the message: no
  commands, no commentary, and nothing before the summary line.
- If the work should be more than one commit, give one block per commit, in the order to commit
  them. Put the list of files that commit covers directly above each block.
- Never include secrets, credentials, the **Never commit** paths, or anything else that
  `.gitignore` excludes in a commit's file list.

## Git access

Run only the Git commands that **Git access** allows. Reading commands such as `git status`,
`git diff`, and `git log` are always fine.

- "read-only": never stage, commit, amend, rebase, merge, tag, or push.
- "stage on request": stage files only when the user asks for it in the current conversation.
  Never commit, amend, rebase, merge, tag, or push.
- "commit on request": stage and commit only when the user asks for it in the current
  conversation. Push, force-push, amend, rebase, merge, or tag only when the user asks for that
  action too.

At every level:

- Stage by path (`git add <path> …`), never with `git add -A` or `git add .`, so nothing
  unrelated slips in.
- A request covers only the action the user asked for. Approval for one commit doesn't carry
  over to the next one, or to a push.
- Before a push the user asked for, say what the **Push effect** will set off.

## One change per commit

- Each commit holds one logical change that one summary line can describe. Split unrelated work
  into separate commits. For example, a new feature, an unrelated bug fix found along the way,
  and a rule change are three commits.
- Keep a change and the things it requires together, including the **Changes that travel
  together**.
- Give dependency upgrades and toolchain bumps commits of their own, so each can be reviewed and
  reverted without touching anything else.

## Message format

Use [Conventional Commits](https://www.conventionalcommits.org/):

```text
<type>(<scope>): <summary>

<body>

<trailers>
```

### Type

| Type | Use it for |
| --- | --- |
| `feat` | New behavior: a page, a command, an API, a module, a site feature |
| `fix` | Something broken: a bug, a crash, a wrong result, a broken link, a wrong fact in the docs |
| `docs` | Written content only: documentation, READMEs, page copy, doc comments |
| `refactor` | Restructured code with no change in behavior |
| `perf` | Faster or smaller with the same behavior; the body gives the measurements before and after |
| `test` | Tests only: new tests, fixtures, or fixes to existing tests |
| `style` | Formatting only (whitespace, indentation, a formatter run). Not visual design, which is `feat` or `fix` |
| `build` | Build configuration, dependency manifests, lock files, and toolchain files |
| `ci` | Continuous integration (CI) workflows, such as GitHub Actions |
| `chore` | Repository upkeep: `.gitignore`, `.gitattributes`, `CODEOWNERS`, and the rules, skills, and settings in `.claude/` |
| `revert` | Undoing an earlier commit; the body names that commit's hash |

The type describes the change as a whole: a feature that also adds tests and docs is `feat`.

### Scope

The scope is optional and names the area changed, in lowercase, from the **Scopes**. Leave it out
when a change spans several areas.

### Summary line

- Use the imperative mood, as if completing "This commit will …": `add`, `fix`, `reject`,
  `remove`. Not `added`, `adds`, or `adding`.
- Start with a lowercase letter, and don't end with a period.
- Name the tracked task as **Task IDs** says.
- Keep the whole line, prefix and task ID included, to 72 characters or fewer; aim for 50.
- Say specifically what changed: `fix(core): reject varints longer than 10 bytes`, not
  `fix: bug`. Never use vague summaries such as `update`, `changes`, `misc fixes`, `wip`, or
  `stuff`.
- Name things the way readers know them: the page, the command, the component, or the post's
  title.

### Body

- Add a body after one blank line whenever the summary alone doesn't explain the change. Leave it
  out only for small, self-explanatory changes.
- Explain what changed and why, not how; the diff shows how. Mention anything a reviewer would
  otherwise miss: a changed URL or interface, a deliberate trade-off, a check that couldn't run,
  or something left for later.
- Wrap lines at 72 characters. Use a short bulleted list (`- `) when the commit makes several
  related changes.
- Follow `writing.md`: American spelling, plain language, correct grammar, and backticks for file
  names, commands, and identifiers.

### Trailers

- Put trailers after one blank line at the end: `Closes #12` or `Refs #12` for issues in the
  repository's tracker.
- Add `BREAKING CHANGE: …` when a change is breaking under **Breaking changes**, and name the
  record it requires.
- Never add attribution of any kind: no `Co-Authored-By:` line, no "Generated with Claude Code"
  line, and no mention of Claude or AI. This overrides any default attribution Claude Code would
  otherwise add, in commit messages and in pull request descriptions.

## Pull requests

- Write the title the way you would write a summary line.
- In the description, say what changed and why, how it was checked (which **Done checks** ran,
  and their results), and what is left for later. Link the issues it closes.
- Follow `writing.md`, and add no attribution (see "Trailers").

## Examples

```text
feat(cli): add a --json flag to the status command

Scripts had to parse the table output, which changes whenever a
column is added. `--json` prints the same fields as one JSON object
per line, and the table stays the default.
```

```text
fix(css): stop long code lines from widening pages on phones

Wide code blocks pushed the page wider than the viewport at 375 px.
Scroll them inside the block instead.
```

```text
chore(claude): add commit message rules
```
