# Google Play store listing (draft)

This is a draft of the English store listing, to paste into Play Console (**Grow users → Store presence → Main store listing**). It answers the testers' feedback that the description "lacks sufficient text and relevant keywords". Every claim below matches what the app does today. Re-check them whenever a feature changes.

## Limits and policy

| Field | Limit | Draft length |
|---|---|---|
| App name | 30 characters | 29 |
| Short description | 80 characters | 76 |
| Full description | 4,000 characters | 1,804 |

Google Play's metadata policy rejects or demotes listings that:
- repeat keywords unnaturally or list them out of context;
- claim a rank or award ("#1", "best", "top") without a source;
- quote user testimonials without attribution;
- mention prices or promotions in the title, or overuse emoji and capital letters.

The draft avoids all of these. Keywords appear once or twice, inside normal sentences.

## App name

```text
Denarii Dolor: Budget Tracker
```

An alternative is `Denarii Dolor: Expense Tracker` (30 characters). Pick one of the two, then measure it (see "Keywords" below).

## Short description

```text
Private budget & expense tracker. Offline, encrypted, no ads and no sign-up.
```

## Full description

```text
Denarii Dolor is a private budget planner and expense tracker that keeps your money records on your phone and nowhere else. There's no account to create, no ads and no internet connection: your data is encrypted and locked behind your PIN.

Track every expense
• Record expenses, income and transfers in seconds with the + button
• Keep separate accounts for cash, savings, cards and more; balances update automatically
• Organize your spending with categories and 24 built-in icons
• Search past transactions by text, category, amount or date

Stay on budget
• Set a monthly budget for any category, with a warning before you reach the limit
• See this month's income, expenses and net total at a glance on the Dashboard
• A spending-by-category chart shows where your money goes
• The app asks before saving an expense that would take you over budget

Reports and backups
• Export a monthly spending report as a PDF or CSV file to save, print or share
• Create an encrypted backup protected by your own passphrase, and restore it on a new phone

Private by design
• Works fully offline: the app doesn't even have the internet permission
• Your records are kept in an encrypted database (SQLCipher, 256-bit AES), unlocked only by your PIN or, if you choose, your fingerprint or face
• Signs you out automatically after 5 minutes without use
• No ads, no analytics, no tracking and no sign-up

Easy to start
• A short welcome tour and a getting-started checklist
• Built-in help and answers to common questions
• Dark mode, and the currency of your choice
• Available in English, Español, Français, Português, हिन्दी, 简体中文 and العربية

Denarii Dolor ("the pain of money" in Latin) is a simple money manager for anyone who wants to control their spending without handing their financial data to a company.
```

## Keywords

| Keyword | Where it appears |
|---|---|
| budget tracker / budget planner | Name, short and full description |
| expense tracker | Short and full description |
| spending tracker / spending report | Full description |
| money manager / personal finance | Full description |
| offline / private / encrypted / no ads | Short and full description |
| PDF / CSV export | Full description |

This is a starting set built from common search terms for personal-finance apps. It isn't measured data.
- **Measure it:** about four weeks after publishing, check **Play Console → Grow users → Store performance → Store analysis**. Its search-term report shows which terms actually bring visitors and installs, so you can rework the short description around them.
- **Test it:** use **Store listing experiments** to compare the two app names, or two short descriptions, before you settle on one.

## Still to do in Play Console

- **Translations:** add listings in Spanish, French, Portuguese (Brazil), Hindi, Simplified Chinese and Arabic. The app already ships in those languages, and a listing in the searcher's language ranks better there.
- **Screenshots:** at least 4, in this order:
  1. the Dashboard with budgets;
  2. adding an expense;
  3. a monthly report;
  4. the sign-in screen with the lock, to show privacy.

  A short caption on each helps, such as "See where your money goes".
- **Data safety form:** see `PRIVACY.md`. The new feedback, share and rate options hand off to other apps, so the answers stay "no data collected or shared".
