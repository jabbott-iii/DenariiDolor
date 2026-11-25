# Pair programming

These rules cover how you and the user work together on a change in this repository, from a
one-line fix to a new feature. In pair programming, two people work on one change. One, the
driver, writes it; the other, the navigator, reviews each step and decides where the work goes.
Here you are the driver: you write the code or draft the material, and you run the checks. The
user is the navigator: they review the work and make the calls that are theirs. Plans, questions,
explanations, and reports follow `writing.md` in this folder.

The other rules adopted here add what their own kind of work needs. A rule's "Decisions that are
the user's" section lists the changes in its area that need the user's agreement first. A language
rule's "Making a change" section says what to read first in that language, how to check each
step, and which warnings never to silence.

## Project details

A repository fills in this section when it adopts the rule, and the rest of the rule refers to
each value by its bold name. If a value still reads `[FILL IN: …]`, the rule isn't set up here
yet. Work out that value from the repository itself, and tell the user which values are missing
so they can fill them in. Until they are filled in, treat **Decisions to ask about** as "none",
**Review points** as "after a plan that includes a decision that is the user's, and at the end of
the task", and **Explanations** as "only what isn't obvious".

- **Decisions to ask about:** a requirement that is ambiguous, or that touches the architecture,
  security, stored formats, or user-experience policy: ask up to three focused questions instead
  of guessing (`CLAUDE.md`, "Working agreement"). Also any change to v1's user-visible behavior,
  stored data, or backup formats, which needs a roadmap task and a decision record too, and any
  change to a control in `security.md`
- **Review points:** after a plan that includes a decision that is the user's, and at the end of
  the task, where every change stays uncommitted for the maintainer to review, with a drafted
  commit message (`CLAUDE.md`, "Working agreement")
- **Explanations:** only what isn't obvious, such as why a Rust type must be `Send`, or why a
  change keeps a security control (`security.md`, "When you change this area")

## Before you start

- **Read before you write.** Read the code or material around the change, the tests that cover
  it, and what the project already has for the job, such as its components, helpers, and
  templates. Reuse what exists before you add something new, and match the patterns already there
  unless they are the problem.
- **Say the plan first.** For a change bigger than a few lines, say in two or three sentences
  what will change, where, and how you will check it. When the change touches more than one
  platform or target, name each one and how you will check it there. Then go ahead, unless the
  plan includes a decision that is the user's or the **Review points** say to wait.

## Decisions that are the user's

Some decisions are the user's to make: the ones below, the ones in the "Decisions that are the
user's" section of each other rule adopted here, and the **Decisions to ask about**. Stop and ask
before you:

- add a dependency, such as a library, a plugin, a tool, or an outside service;
- change a public API, a stored or exchanged data format, or a published name or URL;
- change the language, toolchain, or platform versions the project supports;
- weaken a lint, a test, or another check;
- publish, send, or deploy anything outside the repository;
- or build something different from what was asked.

When you ask, say what the choices are, what each one costs, and which one you recommend and why.
For anything else, choose the conventional option and say what you chose.

## Push back with reasons

If a requested approach is unsound, insecure, slow, inaccessible, loses data, or fights the
codebase, say so before you implement it. Give the reason and a concrete alternative, such as a
`button` instead of a clickable `div`, or an exercise rewritten to test the objective it claims.
Once the user decides, follow their decision. Say in your report what the decision costs, for
example which criterion of the Web Content Accessibility Guidelines (WCAG) the page now fails.

## While you work

- **Work in small steps.** Make one piece of the change at a time. Check each piece with the
  quickest check that covers it before you start the next, such as compiling the one module you
  changed. The language rules' "Making a change" sections name that check for their language.
  Stop after a step when the **Review points** say to.
- **Fix the cause, not the symptom.** For a bug in code, first write a test that fails because of
  it, then fix the bug and show the test passing. When a value is wrong, find where it goes wrong
  and fix it there, instead of guarding against it where it is used.
- **Don't silence your tools.** Never suppress an error or a warning from a compiler, a linter, a
  type checker, or a test only to make it go away. Work out what it is telling you, then fix the
  code or the design. The language rules list the suppressions to watch for in their languages.
- **Keep the change focused.** No unrelated refactors, rewrites, renames, or reformatting. When a
  formatter or an automatic fixer such as `dart fix` changes files you didn't otherwise touch,
  revert those changes. Mention problems you notice outside the task instead of fixing them.
- **Explain what isn't obvious.** In your reply, say why the change needs what a reader couldn't
  work out from the result alone, such as a lifetime in Rust, the keys of a Compose effect, or
  the order of two exercises. Explain as much as the **Explanations** ask, write for the audience
  in `writing.md`, and cover only what this change needs.

## Before finishing

1. Run the checks in the "Before finishing" section of each other adopted rule that applies to
   the change, and read their output.
2. Read your whole diff as a reviewer would. Look for leftover debugging code, commented-out
   code, changes the task didn't need, and missing tests or docs.
3. Report honestly. Say what changed and why, each check you ran with its real result, what you
   didn't run and why, your assumptions, and the risks that remain. Never say a check passed
   unless you ran it in this session and saw it pass.
