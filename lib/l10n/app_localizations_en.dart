// ignore: unused_import
import 'package:intl/intl.dart' as intl;

import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for English (`en`).
class AppLocalizationsEn extends AppLocalizations {
  AppLocalizationsEn([String locale = 'en']) : super(locale);

  @override
  String get appName => 'Denarii Dolor';

  @override
  String get dashboard => 'Dashboard';

  @override
  String get signIn => 'Sign In';

  @override
  String get signInWithBiometrics => 'Sign In with Biometrics';

  @override
  String get biometricSignInAccessibilityLabel => 'Sign in with biometrics';

  @override
  String get biometricSignInTitle => 'Unlock Denarii Dolor';

  @override
  String get biometricSignInSubtitle =>
      'Use your enrolled biometrics to continue';

  @override
  String get biometricSignInFailed =>
      'Biometric authentication failed. Try again or use your PIN.';

  @override
  String get biometricSignInError =>
      'Biometric sign-in is currently unavailable. Use your PIN instead.';

  @override
  String get usePinInstead => 'Use PIN instead';

  @override
  String get biometricSignInInvalidated =>
      'Biometric sign-in was turned off because the biometrics on this device changed. Sign in with your PIN, then turn it on again in Settings.';

  @override
  String get biometricEnrollTitle => 'Turn on biometric sign-in';

  @override
  String get biometricEnrollSubtitle => 'Confirm with your fingerprint or face';

  @override
  String get biometricEnabled => 'Biometric sign-in is on.';

  @override
  String get biometricEnableFailed => 'Couldn\'t turn on biometric sign-in.';

  @override
  String get pinHint => 'PIN';

  @override
  String get showPin => 'Show';

  @override
  String get hidePin => 'Hide';

  @override
  String get newPinHint => 'New PIN';

  @override
  String get confirmPinHint => 'Confirm PIN';

  @override
  String get securityQuestionHint => 'Security question';

  @override
  String get securityAnswerHint => 'Security answer';

  @override
  String get securityAnswerVerifyHint => 'Security answer';

  @override
  String get forgotPin => 'Forgot PIN?';

  @override
  String get backToSignIn => 'Back to Sign In';

  @override
  String get createSecurityProfile => 'Create Security Profile';

  @override
  String get wipeAllData => 'Wipe All App Data';

  @override
  String get wipeAllDataDestructiveLabel =>
      'Destructive action: wipe all app data';

  @override
  String get wipeDataTitle => 'Delete all app data?';

  @override
  String get wipeDataWarning =>
      'This permanently deletes all app data, financial records, PIN, and recovery settings. This cannot be undone.';

  @override
  String get wipeDataConfirm => 'Delete Everything';

  @override
  String wipeDataConfirmCountdown(int seconds) {
    return 'Delete Everything ($seconds)';
  }

  @override
  String get wipeDataConfirmationWord => 'WIPE';

  @override
  String wipeDataTypeToConfirm(String word) {
    return 'Type $word to confirm';
  }

  @override
  String get wipeDataConfirmDestructiveLabel =>
      'Destructive confirmation: delete all app data permanently';

  @override
  String get wipeDataCancel => 'Cancel';

  @override
  String get pinFormatError => 'PIN must be 6 to 12 digits.';

  @override
  String get pinConfirmationMismatch => 'PIN and confirmation must match.';

  @override
  String get securityQuestionRequired => 'Security question is required.';

  @override
  String get securityAnswerRequired => 'Security answer is required.';

  @override
  String securityQuestionTooLong(int maxLength) {
    return 'Keep the security question to $maxLength characters or fewer.';
  }

  @override
  String securityAnswerTooShort(int minLength) {
    return 'The security answer needs at least $minLength characters. Capital letters and extra spaces don\'t count.';
  }

  @override
  String securityAnswerTooLong(int maxLength) {
    return 'Keep the security answer to $maxLength characters or fewer.';
  }

  @override
  String get securityAnswerInQuestion =>
      'The answer can\'t appear in the question.';

  @override
  String get securityAnswerMatchesPin =>
      'The answer can\'t be the same as your PIN.';

  @override
  String securityAnswerRules(int minLength) {
    return 'At least $minLength characters. Capital letters and extra spaces are ignored. Choose an answer others can\'t guess or look up.';
  }

  @override
  String get upgradeRecoveryIntro =>
      'Denarii Dolor now protects your data with your PIN and a stronger recovery answer. Set a security question and answer to finish signing in.';

  @override
  String get saveAndContinue => 'Save and Continue';

  @override
  String get storageErrorMessage =>
      'Secure storage on this device can\'t be read, so your data can\'t be unlocked. Try again, or wipe all app data to start over.';

  @override
  String get tryAgain => 'Try Again';

  @override
  String get profileSetupSuccess =>
      'Security profile created. Sign in with your PIN.';

  @override
  String get profileAlreadyConfigured =>
      'Security profile is already configured.';

  @override
  String get profileNotConfigured =>
      'Set up a security profile before recovering a PIN.';

  @override
  String get invalidSecurityAnswer => 'Security answer is incorrect.';

  @override
  String get pinRecoverySuccess =>
      'PIN reset successful. Sign in with your new PIN.';

  @override
  String get wipeDataSuccess =>
      'All app data was deleted. Set up a new security profile.';

  @override
  String get wipeDataError => 'Unable to wipe app data. Please try again.';

  @override
  String get securityQuestionUnavailable => 'Security question unavailable.';

  @override
  String get addTransaction => 'Add Transaction';

  @override
  String get transactionDescriptionHint => 'Description';

  @override
  String get transactionTypeLabel => 'Transaction type';

  @override
  String get transactionAmountHint => 'Amount';

  @override
  String get transactionReferenceRequiredMessage =>
      'Review the account and category IDs before saving this transaction.';

  @override
  String get dateHint => 'Date (YYYY-MM-DD)';

  @override
  String get saveTransaction => 'Save Transaction';

  @override
  String get transactionTypeExpense => 'Expense';

  @override
  String get transactionTypeIncome => 'Income';

  @override
  String get transactionTypeTransfer => 'Transfer';

  @override
  String get search => 'Search';

  @override
  String get searchDescriptionHint => 'Description';

  @override
  String get searchMinAmountHint => 'Min amount';

  @override
  String get searchMaxAmountHint => 'Max amount';

  @override
  String get searchStartDateHint => 'Start date (YYYY-MM-DD)';

  @override
  String get searchEndDateHint => 'End date (YYYY-MM-DD)';

  @override
  String get invalidDateMessage => 'Use the YYYY-MM-DD date format.';

  @override
  String get invalidSearchFiltersMessage =>
      'Check your search dates and numeric filters, then try again.';

  @override
  String get invalidSearchRangeMessage =>
      'Enter a valid amount and date range.';

  @override
  String get reports => 'Reports';

  @override
  String get settings => 'Settings';

  @override
  String settingsSessionTimeout(int minutes) {
    return 'Session timeout: $minutes minutes of inactivity';
  }

  @override
  String get settingsPinConfigured => 'PIN is configured for quick sign-in.';

  @override
  String get settingsPinNotConfigured => 'No PIN is configured yet.';

  @override
  String get signOut => 'Sign Out';

  @override
  String get resetPin => 'Reset PIN';

  @override
  String get settingsDarkMode => 'Dark mode';

  @override
  String get settingsBiometricSignIn => 'Biometric sign-in';

  @override
  String get settingsCurrency => 'Currency';

  @override
  String settingsCurrencyValue(String currencyName, String symbol) {
    return '$currencyName ($symbol)';
  }

  @override
  String get settingsCurrencyTitle => 'Choose a currency';

  @override
  String get settingsCurrencyNote =>
      'Only the symbol changes. Amounts you\'ve already entered aren\'t converted.';

  @override
  String get settingsBackupRestore => 'Backup and restore';

  @override
  String get settingsPrivacyPolicy => 'Privacy policy';

  @override
  String get settingsLicenses => 'Open-source licenses';

  @override
  String get backupExplanation =>
      'A backup file holds your accounts, categories, budgets, transactions and currency setting, encrypted with a passphrase you choose. It doesn\'t hold your PIN, security question or biometric sign-in: on a new phone, set up the app first, then restore the backup here.';

  @override
  String get backupPassphraseWarning =>
      'Keep the passphrase safe. Without it the backup can\'t be opened, and nobody can recover it.';

  @override
  String get backupCreate => 'Create backup';

  @override
  String get backupRestore => 'Restore from backup';

  @override
  String get backupCreateTitle => 'Choose a passphrase';

  @override
  String get backupRestoreTitle => 'Backup passphrase';

  @override
  String get backupPassphrase => 'Passphrase';

  @override
  String get backupPassphraseConfirm => 'Confirm passphrase';

  @override
  String backupPassphraseRules(int minLength) {
    return 'At least $minLength characters. A few unrelated words work well.';
  }

  @override
  String get backupContinue => 'Continue';

  @override
  String get backupWorking => 'Working…';

  @override
  String get backupSaved => 'Backup saved.';

  @override
  String get backupSaveFailed => 'Couldn\'t save the backup.';

  @override
  String get backupRestoreConfirmTitle => 'Replace all data?';

  @override
  String backupRestoreContents(
    String created,
    int accounts,
    int categories,
    int budgets,
    int transactions,
  ) {
    return 'Created: $created\nAccounts: $accounts\nCategories: $categories\nBudgets: $budgets\nTransactions: $transactions';
  }

  @override
  String get backupRestoreWarning =>
      'Restoring erases everything now on this phone and replaces it with the backup. This can\'t be undone.';

  @override
  String get backupRestoreConfirmationWord => 'RESTORE';

  @override
  String get backupRestoreAction => 'Replace data';

  @override
  String get backupRestored => 'Backup restored.';

  @override
  String get backupRestoreFailed =>
      'Couldn\'t restore the backup. Your data hasn\'t changed.';

  @override
  String backupErrorPassphraseLength(int minLength, int maxLength) {
    return 'The passphrase must be $minLength to $maxLength characters.';
  }

  @override
  String get backupErrorPassphraseMismatch => 'The passphrases don\'t match.';

  @override
  String get backupErrorNotBackup => 'This file isn\'t a Denarii Dolor backup.';

  @override
  String get backupErrorNewerVersion =>
      'This backup was made by a newer version of Denarii Dolor. Update the app, then try again.';

  @override
  String get backupErrorWrongPassphrase =>
      'Wrong passphrase, or the file is damaged.';

  @override
  String get backupErrorDamaged =>
      'The backup is damaged and can\'t be restored.';

  @override
  String get backupErrorRead => 'Couldn\'t read the file.';

  @override
  String get legalLoadFailed => 'Couldn\'t open this document.';

  @override
  String get legalEnglishOnly => 'This document is available in English only.';

  @override
  String get transactionCategoryLabel => 'Category';

  @override
  String get transactionAccountLabel => 'Account';

  @override
  String get transactionTransferAccountLabel => 'Transfer to account';

  @override
  String get editTransaction => 'Edit Transaction';

  @override
  String get updateTransaction => 'Update Transaction';

  @override
  String get transactionSaved => 'Transaction saved.';

  @override
  String get transactionDeleted => 'Transaction deleted.';

  @override
  String get transactionNotFound => 'Transaction not found.';

  @override
  String get recentTransactions => 'Recent transactions';

  @override
  String get noTransactions => 'No transactions yet. Tap + to add one.';

  @override
  String get accountBalances => 'Account balances';

  @override
  String get deleteTransactionTitle => 'Delete transaction?';

  @override
  String deleteTransactionMessage(String description) {
    return 'Delete \"$description\"? Account balances will be adjusted.';
  }

  @override
  String deleteItemTitle(String name) {
    return 'Delete $name?';
  }

  @override
  String get deleteItemMessage => 'This cannot be undone.';

  @override
  String get pickerPlaceholder => 'Select…';

  @override
  String get edit => 'Edit';

  @override
  String get delete => 'Delete';

  @override
  String get cancel => 'Cancel';

  @override
  String get save => 'Save';

  @override
  String get back => 'Back';

  @override
  String get genericError => 'Something went wrong. Please try again.';

  @override
  String get manageCategories => 'Manage Categories';

  @override
  String get manageAccounts => 'Manage Accounts';

  @override
  String get manageBudgets => 'Manage Budgets';

  @override
  String get addCategory => 'Add Category';

  @override
  String get categoryName => 'Category name';

  @override
  String get categorySaved => 'Category saved.';

  @override
  String get categoryDeleted => 'Category deleted.';

  @override
  String get addAccount => 'Add Account';

  @override
  String get renameAccount => 'Rename Account';

  @override
  String get accountName => 'Account name';

  @override
  String get openingBalance => 'Opening balance';

  @override
  String accountBalance(String balance) {
    return 'Balance: $balance';
  }

  @override
  String get accountSaved => 'Account saved.';

  @override
  String get accountDeleted => 'Account deleted.';

  @override
  String get invalidOpeningBalance =>
      'Enter a valid opening balance (at most 2 decimals).';

  @override
  String get setBudget => 'Set Budget';

  @override
  String budgetForCategory(String category) {
    return 'Budget: $category';
  }

  @override
  String get budgetLimit => 'Monthly limit';

  @override
  String get budgetWarningPercent => 'Warning threshold (percent of limit)';

  @override
  String budgetSummary(String limit, int percent) {
    return 'Limit $limit · warn at $percent%';
  }

  @override
  String get noBudget => 'No budget set';

  @override
  String get remove => 'Remove';

  @override
  String get budgetSaved => 'Budget saved.';

  @override
  String get budgetDeleted => 'Budget removed.';

  @override
  String get invalidBudgetInput =>
      'Enter a limit with at most 2 decimals and a warning percentage from 1 to 100.';

  @override
  String invalidPinAttemptsLeft(int attempts) {
    return 'Invalid PIN. $attempts attempt(s) left before a temporary lockout.';
  }

  @override
  String pinLockedOut(int seconds) {
    return 'Too many failed attempts. Try again in $seconds seconds.';
  }

  @override
  String get searchAllCategories => 'All categories';

  @override
  String get searchClear => 'Clear';

  @override
  String searchResultCount(int count) {
    return '$count result(s)';
  }

  @override
  String get searchNoResults => 'No matching transactions.';

  @override
  String get previousMonth => '‹ Prev';

  @override
  String get nextMonth => 'Next ›';

  @override
  String get reportTitle => 'Monthly Spending Report';

  @override
  String reportGenerated(String generated) {
    return 'Generated $generated';
  }

  @override
  String reportPage(int page) {
    return 'Page $page';
  }

  @override
  String reportTotals(String income, String expense, String net) {
    return 'Income $income · Expense $expense · Net $net';
  }

  @override
  String get reportColDate => 'Date';

  @override
  String get reportColType => 'Type';

  @override
  String get reportColCategory => 'Category';

  @override
  String get reportColDescription => 'Description';

  @override
  String get reportColAmount => 'Amount';

  @override
  String get reportColPayment => 'Payment Method';

  @override
  String get reportEmpty => 'No transactions in this month.';

  @override
  String get reportLoadFailed => 'Couldn\'t generate the report. Try again.';

  @override
  String get saveCsv => 'Save CSV';

  @override
  String get savePdf => 'Save PDF';

  @override
  String get shareCsv => 'Share CSV';

  @override
  String get sharePdf => 'Share PDF';

  @override
  String get shareReport => 'Share report';

  @override
  String get reportSaved => 'Report saved.';

  @override
  String get reportExportFailed => 'Couldn\'t export the report.';

  @override
  String get netThisMonth => 'Net this month';

  @override
  String get incomeThisMonth => 'Income';

  @override
  String get expenseThisMonth => 'Expenses';

  @override
  String get spendingByCategory => 'Spending by category';

  @override
  String get spendingNone => 'No expenses this month.';

  @override
  String get spendingOther => 'Other';

  @override
  String get budgetsThisMonth => 'Budgets this month';

  @override
  String budgetSpentOfLimit(String spent, String limit) {
    return '$spent of $limit';
  }

  @override
  String budgetStatusLine(String status, int percent) {
    return '$status · $percent% used';
  }

  @override
  String get budgetStatusOk => 'On track';

  @override
  String get budgetStatusWarning => 'Near limit';

  @override
  String get budgetStatusOver => 'Over budget';

  @override
  String budgetAlerts(int count) {
    String _temp0 = intl.Intl.pluralLogic(
      count,
      locale: localeName,
      other: '$count budgets are near or over their limits.',
      one: '$count budget is near or over its limit.',
    );
    return '$_temp0';
  }

  @override
  String get editCategory => 'Edit Category';

  @override
  String categoryIconLabel(String icon) {
    return 'Icon: $icon';
  }

  @override
  String get categoryIconGeneral => 'General';

  @override
  String get categoryIconIncome => 'Income';

  @override
  String get categoryIconTransfer => 'Transfer';

  @override
  String get categoryIconGroceries => 'Groceries';

  @override
  String get categoryIconDining => 'Dining';

  @override
  String get categoryIconCoffee => 'Coffee';

  @override
  String get categoryIconHousing => 'Housing';

  @override
  String get categoryIconUtilities => 'Utilities';

  @override
  String get categoryIconInternet => 'Internet';

  @override
  String get categoryIconPhone => 'Phone';

  @override
  String get categoryIconCar => 'Car';

  @override
  String get categoryIconFuel => 'Fuel';

  @override
  String get categoryIconTransit => 'Transit';

  @override
  String get categoryIconTravel => 'Travel';

  @override
  String get categoryIconHealth => 'Health';

  @override
  String get categoryIconFitness => 'Fitness';

  @override
  String get categoryIconShopping => 'Shopping';

  @override
  String get categoryIconClothing => 'Clothing';

  @override
  String get categoryIconEntertainment => 'Entertainment';

  @override
  String get categoryIconEducation => 'Education';

  @override
  String get categoryIconPets => 'Pets';

  @override
  String get categoryIconGifts => 'Gifts';

  @override
  String get categoryIconSavings => 'Savings';

  @override
  String get categoryIconBank => 'Bank & fees';

  @override
  String get invalidAmountMessage =>
      'Enter an amount greater than 0 with at most 2 decimals.';

  @override
  String get errorIdRequired =>
      'This item could not be found. Go back and try again.';

  @override
  String get errorItemNotFound => 'This item no longer exists.';

  @override
  String get errorBlankDescription => 'Enter a description.';

  @override
  String errorDescriptionTooLong(int maxLength) {
    return 'Description must be at most $maxLength characters.';
  }

  @override
  String get errorInvalidDate => 'Choose a valid date.';

  @override
  String get errorTransferDestinationRequired =>
      'Choose the account to transfer to.';

  @override
  String get errorTransferDestinationSame =>
      'Choose a different account to transfer to.';

  @override
  String get errorCategoryNotFound =>
      'That category no longer exists. Choose another.';

  @override
  String get errorAccountNotFound =>
      'That account no longer exists. Choose another.';

  @override
  String get errorBudgetExceeded =>
      'This expense would put the category over its monthly budget.';

  @override
  String get overBudgetTitle => 'Over budget';

  @override
  String overBudgetMessage(String category, String overBy, String limit) {
    return 'This expense puts $category $overBy over its monthly budget of $limit.';
  }

  @override
  String get overBudgetConfirm => 'Save anyway';

  @override
  String errorInvalidName(int maxLength) {
    return 'Name must be 1 to $maxLength characters.';
  }

  @override
  String errorDuplicateCategoryName(String name) {
    return 'A category named \"$name\" already exists.';
  }

  @override
  String errorDuplicateAccountName(String name) {
    return 'An account named \"$name\" already exists.';
  }

  @override
  String get errorDefaultCategory => 'Default categories can\'t be deleted.';

  @override
  String get errorDefaultAccount => 'Default accounts can\'t be deleted.';

  @override
  String errorCategoryInUse(int count) {
    String _temp0 = intl.Intl.pluralLogic(
      count,
      locale: localeName,
      other: 'This category is used by $count transactions.',
      one: 'This category is used by $count transaction.',
    );
    return '$_temp0';
  }

  @override
  String errorAccountInUse(int count) {
    String _temp0 = intl.Intl.pluralLogic(
      count,
      locale: localeName,
      other: 'This account is used by $count transactions.',
      one: 'This account is used by $count transaction.',
    );
    return '$_temp0';
  }

  @override
  String get errorInvalidWarningPercent =>
      'Warning threshold must be from 1 to 100 percent.';

  @override
  String get defaultAccountCash => 'Cash';

  @override
  String get defaultAccountSavings => 'Savings';

  @override
  String get defaultCategoryExpense => 'General Expense';

  @override
  String get defaultCategoryIncome => 'General Income';

  @override
  String get defaultCategoryTransfer => 'Transfer';

  @override
  String get dashboardLoadFailed =>
      'Your transactions couldn\'t be loaded. Leave the app and come back to try again.';

  @override
  String get tourPrivateTitle => 'Your money, kept private';

  @override
  String get tourPrivateBody =>
      'Track expenses, income and transfers between your accounts. Everything stays on this phone, encrypted and locked with your PIN. No account, no ads, no internet.';

  @override
  String get tourAddTitle => 'Add in seconds';

  @override
  String get tourAddBody =>
      'Tap + to record an expense, income or transfer. Pick an account and a category, and your balances update on their own.';

  @override
  String get tourBudgetTitle => 'Stay on budget';

  @override
  String get tourBudgetBody =>
      'Give a category a monthly limit. The Dashboard shows how much is left and warns you before you go over.';

  @override
  String get tourReportsTitle => 'Reports and backups';

  @override
  String get tourReportsBody =>
      'Save or share a monthly report as a PDF or CSV file, and keep an encrypted backup so you never lose your records.';

  @override
  String get tourSkip => 'Skip';

  @override
  String get tourNext => 'Next';

  @override
  String get tourGetStarted => 'Get started';

  @override
  String tourPageIndicator(int page, int pageCount) {
    return 'Page $page of $pageCount';
  }

  @override
  String get gettingStartedTitle => 'Get started';

  @override
  String gettingStartedProgress(int done, int total) {
    return '$done of $total done';
  }

  @override
  String get gettingStartedAddAccount => 'Add your bank or card account';

  @override
  String get gettingStartedAddTransaction => 'Record your first transaction';

  @override
  String get gettingStartedSetBudget => 'Set a monthly budget';

  @override
  String get gettingStartedStepDone => 'Done';

  @override
  String get gettingStartedStepTodo => 'Not done yet';

  @override
  String get gettingStartedHide => 'Hide the getting-started checklist';

  @override
  String get settingsHelpSection => 'Help and support';

  @override
  String get settingsHelpFaq => 'Help & FAQ';

  @override
  String get settingsShowTour => 'Show the welcome tour';

  @override
  String get settingsSendFeedback => 'Send feedback';

  @override
  String get settingsRateApp => 'Rate this app';

  @override
  String get settingsShareApp => 'Share this app';

  @override
  String get helpTitle => 'Help & FAQ';

  @override
  String get helpExpanded => 'Expanded';

  @override
  String get helpCollapsed => 'Collapsed';

  @override
  String get helpContactTitle => 'Still need help?';

  @override
  String get helpContactBody =>
      'Send a question, a problem or an idea. Please don\'t include your PIN, a backup passphrase or your financial records.';

  @override
  String get faqDataQ => 'Where is my data stored?';

  @override
  String get faqDataA =>
      'Only on this phone. Denarii Dolor has no account or server and can\'t connect to the internet. Your records are kept in an encrypted database that only your PIN, your security answer or, if you turn it on, your fingerprint or face can unlock. Android\'s cloud backup is turned off for the app.';

  @override
  String get faqForgotPinQ => 'I forgot my PIN. What can I do?';

  @override
  String faqForgotPinA(String forgotPin, String wipeAllData) {
    return 'On the sign-in screen, tap “$forgotPin” and answer your security question to choose a new PIN. If you can\'t answer it either, your data can\'t be unlocked, because nobody else has the key. The only way forward then is “$wipeAllData” on the sign-in screen, which erases everything so you can start over.';
  }

  @override
  String get faqSignedOutQ => 'Why was I signed out?';

  @override
  String faqSignedOutA(int minutes, String biometricSetting) {
    return 'To protect your data, the app signs you out after $minutes minutes without use. Sign in again with your PIN, or turn on “$biometricSetting” in Settings to sign in with your fingerprint or face.';
  }

  @override
  String get faqBackupQ =>
      'How do I back up my data or move it to a new phone?';

  @override
  String faqBackupA(String backupSetting) {
    return 'In Settings, open “$backupSetting” and create a backup protected by a passphrase. Save the file somewhere safe, such as a cloud drive. On the new phone, install Denarii Dolor, set it up, then restore the file with the same passphrase. Nobody can recover a forgotten passphrase.';
  }

  @override
  String get faqBudgetsQ => 'How do budgets work?';

  @override
  String faqBudgetsA(String budgetsScreen) {
    return 'In Settings, open “$budgetsScreen” to give a category a monthly limit and a warning threshold. The Dashboard shows how much of each budget you\'ve used this month. If an expense would go over its budget, the app asks before saving it.';
  }

  @override
  String get faqTransferQ => 'What is a transfer?';

  @override
  String get faqTransferA =>
      'A transfer moves money between two of your accounts, for example from Savings to Cash. It changes both balances but doesn\'t count as income or as an expense.';

  @override
  String get faqDeleteQ => 'Why can\'t I delete a category or an account?';

  @override
  String get faqDeleteA =>
      'The default categories and accounts can\'t be deleted, and neither can one that transactions still use. Change or delete those transactions first, then try again.';

  @override
  String get faqCurrencyQ => 'How do I change the currency?';

  @override
  String faqCurrencyA(String currencySetting) {
    return 'In Settings, tap “$currencySetting” and pick one. Only the symbol changes: amounts you already entered aren\'t converted.';
  }

  @override
  String get faqReportsQ => 'How do I export a report?';

  @override
  String faqReportsA(String reportsScreen) {
    return 'Open “$reportsScreen”, choose a month, then save or share it as a PDF or CSV file. Exported files aren\'t encrypted, so keep them somewhere private.';
  }

  @override
  String get faqBiometricQ => 'Why did fingerprint or face sign-in turn off?';

  @override
  String get faqBiometricA =>
      'It turns off when the fingerprints or faces enrolled on the phone change, so that a newly added one can\'t open your data. Your PIN still works, and you can turn biometric sign-in back on in Settings.';

  @override
  String get ratePromptTitle => 'Enjoying Denarii Dolor?';

  @override
  String get ratePromptMessage =>
      'A quick rating on Google Play helps other people find the app. It only takes a moment.';

  @override
  String get ratePromptRate => 'Rate now';

  @override
  String get ratePromptLater => 'Not now';

  @override
  String get ratePromptNever => 'Don\'t ask again';

  @override
  String get rateUnavailable =>
      'Google Play couldn\'t be opened on this device.';

  @override
  String get shareAppChooser => 'Share Denarii Dolor';

  @override
  String shareAppMessage(String storeLink) {
    return 'I track my spending and budgets with Denarii Dolor. Everything stays private on the phone. Get it on Google Play: $storeLink';
  }

  @override
  String feedbackEmailSubject(String appVersion) {
    return 'Denarii Dolor feedback (version $appVersion)';
  }

  @override
  String feedbackEmailBody(String appVersion, String androidVersion) {
    return '\n\n———\nApp version: $appVersion\nAndroid version: $androidVersion';
  }

  @override
  String feedbackNoEmailApp(String emailAddress) {
    return 'No email app was found. You can write to $emailAddress.';
  }

  @override
  String placeholderAmountInCents(int cents) {
    return '$cents cents';
  }

  @override
  String placeholderAmountRejected(String code) {
    return 'Rejected: $code';
  }
}
