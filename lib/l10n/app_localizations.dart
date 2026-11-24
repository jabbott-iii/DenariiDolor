import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:intl/intl.dart' as intl;

import 'app_localizations_ar.dart';
import 'app_localizations_en.dart';
import 'app_localizations_es.dart';
import 'app_localizations_fr.dart';
import 'app_localizations_hi.dart';
import 'app_localizations_pt.dart';
import 'app_localizations_zh.dart';

// ignore_for_file: type=lint

/// Callers can lookup localized strings with an instance of AppLocalizations
/// returned by `AppLocalizations.of(context)`.
///
/// Applications need to include `AppLocalizations.delegate()` in their app's
/// `localizationDelegates` list, and the locales they support in the app's
/// `supportedLocales` list. For example:
///
/// ```dart
/// import 'l10n/app_localizations.dart';
///
/// return MaterialApp(
///   localizationsDelegates: AppLocalizations.localizationsDelegates,
///   supportedLocales: AppLocalizations.supportedLocales,
///   home: MyApplicationHome(),
/// );
/// ```
///
/// ## Update pubspec.yaml
///
/// Please make sure to update your pubspec.yaml to include the following
/// packages:
///
/// ```yaml
/// dependencies:
///   # Internationalization support.
///   flutter_localizations:
///     sdk: flutter
///   intl: any # Use the pinned version from flutter_localizations
///
///   # Rest of dependencies
/// ```
///
/// ## iOS Applications
///
/// iOS applications define key application metadata, including supported
/// locales, in an Info.plist file that is built into the application bundle.
/// To configure the locales supported by your app, you’ll need to edit this
/// file.
///
/// First, open your project’s ios/Runner.xcworkspace Xcode workspace file.
/// Then, in the Project Navigator, open the Info.plist file under the Runner
/// project’s Runner folder.
///
/// Next, select the Information Property List item, select Add Item from the
/// Editor menu, then select Localizations from the pop-up menu.
///
/// Select and expand the newly-created Localizations item then, for each
/// locale your application supports, add a new item and select the locale
/// you wish to add from the pop-up menu in the Value field. This list should
/// be consistent with the languages listed in the AppLocalizations.supportedLocales
/// property.
abstract class AppLocalizations {
  AppLocalizations(String locale)
    : localeName = intl.Intl.canonicalizedLocale(locale.toString());

  final String localeName;

  static AppLocalizations of(BuildContext context) {
    return Localizations.of<AppLocalizations>(context, AppLocalizations)!;
  }

  static const LocalizationsDelegate<AppLocalizations> delegate =
      _AppLocalizationsDelegate();

  /// A list of this localizations delegate along with the default localizations
  /// delegates.
  ///
  /// Returns a list of localizations delegates containing this delegate along with
  /// GlobalMaterialLocalizations.delegate, GlobalCupertinoLocalizations.delegate,
  /// and GlobalWidgetsLocalizations.delegate.
  ///
  /// Additional delegates can be added by appending to this list in
  /// MaterialApp. This list does not have to be used at all if a custom list
  /// of delegates is preferred or required.
  static const List<LocalizationsDelegate<dynamic>> localizationsDelegates =
      <LocalizationsDelegate<dynamic>>[
        delegate,
        GlobalMaterialLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
      ];

  /// A list of this localizations delegate's supported locales.
  static const List<Locale> supportedLocales = <Locale>[
    Locale('en'),
    Locale('ar'),
    Locale('es'),
    Locale('fr'),
    Locale('hi'),
    Locale('pt'),
    Locale('zh'),
  ];

  /// The app's name, shown in screen titles. Don't translate it.
  ///
  /// In en, this message translates to:
  /// **'Denarii Dolor'**
  String get appName;

  /// Title of the Dashboard screen and its navigation item.
  ///
  /// In en, this message translates to:
  /// **'Dashboard'**
  String get dashboard;

  /// Button on the sign-in screen that signs in with the typed PIN.
  ///
  /// In en, this message translates to:
  /// **'Sign In'**
  String get signIn;

  /// Button on the sign-in screen that opens the fingerprint or face prompt.
  ///
  /// In en, this message translates to:
  /// **'Sign In with Biometrics'**
  String get signInWithBiometrics;

  /// Screen reader label of the biometric sign-in button.
  ///
  /// In en, this message translates to:
  /// **'Sign in with biometrics'**
  String get biometricSignInAccessibilityLabel;

  /// Title of the system fingerprint or face prompt at sign-in.
  ///
  /// In en, this message translates to:
  /// **'Unlock Denarii Dolor'**
  String get biometricSignInTitle;

  /// Subtitle of the system fingerprint or face prompt at sign-in.
  ///
  /// In en, this message translates to:
  /// **'Use your enrolled biometrics to continue'**
  String get biometricSignInSubtitle;

  /// Message after the fingerprint or face check fails at sign-in.
  ///
  /// In en, this message translates to:
  /// **'Biometric authentication failed. Try again or use your PIN.'**
  String get biometricSignInFailed;

  /// Message when biometric sign-in can't be used on the device right now.
  ///
  /// In en, this message translates to:
  /// **'Biometric sign-in is currently unavailable. Use your PIN instead.'**
  String get biometricSignInError;

  /// Button in the system biometric prompt that closes it so the user can type their PIN.
  ///
  /// In en, this message translates to:
  /// **'Use PIN instead'**
  String get usePinInstead;

  /// Message at sign-in after a change to the device's enrolled fingerprints or faces turned biometric sign-in off.
  ///
  /// In en, this message translates to:
  /// **'Biometric sign-in was turned off because the biometrics on this device changed. Sign in with your PIN, then turn it on again in Settings.'**
  String get biometricSignInInvalidated;

  /// Title of the system biometric prompt shown when turning on biometric sign-in in Settings.
  ///
  /// In en, this message translates to:
  /// **'Turn on biometric sign-in'**
  String get biometricEnrollTitle;

  /// Subtitle of the system biometric prompt shown when turning on biometric sign-in in Settings.
  ///
  /// In en, this message translates to:
  /// **'Confirm with your fingerprint or face'**
  String get biometricEnrollSubtitle;

  /// Confirmation after biometric sign-in is turned on in Settings.
  ///
  /// In en, this message translates to:
  /// **'Biometric sign-in is on.'**
  String get biometricEnabled;

  /// Error when biometric sign-in couldn't be turned on in Settings.
  ///
  /// In en, this message translates to:
  /// **'Couldn\'t turn on biometric sign-in.'**
  String get biometricEnableFailed;

  /// Label of the PIN field on the sign-in screen.
  ///
  /// In en, this message translates to:
  /// **'PIN'**
  String get pinHint;

  /// Button that shows the typed PIN or answer in plain text.
  ///
  /// In en, this message translates to:
  /// **'Show'**
  String get showPin;

  /// Button that hides the typed PIN or answer again.
  ///
  /// In en, this message translates to:
  /// **'Hide'**
  String get hidePin;

  /// Label of the field for a new PIN, at setup and when resetting the PIN.
  ///
  /// In en, this message translates to:
  /// **'New PIN'**
  String get newPinHint;

  /// Label of the field where the new PIN is typed a second time.
  ///
  /// In en, this message translates to:
  /// **'Confirm PIN'**
  String get confirmPinHint;

  /// Label of the field for the security question the user writes at setup.
  ///
  /// In en, this message translates to:
  /// **'Security question'**
  String get securityQuestionHint;

  /// Label of the field for the security answer at setup.
  ///
  /// In en, this message translates to:
  /// **'Security answer'**
  String get securityAnswerHint;

  /// Label of the field where the user answers their security question to reset a forgotten PIN.
  ///
  /// In en, this message translates to:
  /// **'Security answer'**
  String get securityAnswerVerifyHint;

  /// Button on the sign-in screen that starts resetting a forgotten PIN. The Help screen quotes it.
  ///
  /// In en, this message translates to:
  /// **'Forgot PIN?'**
  String get forgotPin;

  /// Button that leaves PIN recovery and returns to the sign-in screen.
  ///
  /// In en, this message translates to:
  /// **'Back to Sign In'**
  String get backToSignIn;

  /// Button that finishes the first setup: the PIN, the security question, and its answer.
  ///
  /// In en, this message translates to:
  /// **'Create Security Profile'**
  String get createSecurityProfile;

  /// Button on the sign-in screen that starts erasing everything in the app. The Help screen quotes it.
  ///
  /// In en, this message translates to:
  /// **'Wipe All App Data'**
  String get wipeAllData;

  /// Screen reader label of the wipe button, warning that it is destructive.
  ///
  /// In en, this message translates to:
  /// **'Destructive action: wipe all app data'**
  String get wipeAllDataDestructiveLabel;

  /// Title of the dialog that confirms erasing all app data.
  ///
  /// In en, this message translates to:
  /// **'Delete all app data?'**
  String get wipeDataTitle;

  /// Warning in the dialog that confirms erasing all app data.
  ///
  /// In en, this message translates to:
  /// **'This permanently deletes all app data, financial records, PIN, and recovery settings. This cannot be undone.'**
  String get wipeDataWarning;

  /// Button that erases all app data, once the countdown ends.
  ///
  /// In en, this message translates to:
  /// **'Delete Everything'**
  String get wipeDataConfirm;

  /// The erase button while its countdown runs; the number is the seconds left.
  ///
  /// In en, this message translates to:
  /// **'Delete Everything ({seconds})'**
  String wipeDataConfirmCountdown(int seconds);

  /// The word the user types to confirm wiping all app data (security control CS-16). Don't translate it: the check compares the typed text with this exact word.
  ///
  /// In en, this message translates to:
  /// **'WIPE'**
  String get wipeDataConfirmationWord;

  /// Instruction above the field where the user types the confirmation word to wipe or restore. The word isn't translated.
  ///
  /// In en, this message translates to:
  /// **'Type {word} to confirm'**
  String wipeDataTypeToConfirm(String word);

  /// Screen reader label of the final erase button.
  ///
  /// In en, this message translates to:
  /// **'Destructive confirmation: delete all app data permanently'**
  String get wipeDataConfirmDestructiveLabel;

  /// Button that closes the wipe dialog without erasing anything.
  ///
  /// In en, this message translates to:
  /// **'Cancel'**
  String get wipeDataCancel;

  /// Error when a PIN isn't 6 to 12 digits.
  ///
  /// In en, this message translates to:
  /// **'PIN must be 6 to 12 digits.'**
  String get pinFormatError;

  /// Error when the two typed PINs differ.
  ///
  /// In en, this message translates to:
  /// **'PIN and confirmation must match.'**
  String get pinConfirmationMismatch;

  /// Error when the security question is left empty.
  ///
  /// In en, this message translates to:
  /// **'Security question is required.'**
  String get securityQuestionRequired;

  /// Error when the security answer is left empty.
  ///
  /// In en, this message translates to:
  /// **'Security answer is required.'**
  String get securityAnswerRequired;

  /// Error when the security question is longer than the limit, in characters.
  ///
  /// In en, this message translates to:
  /// **'Keep the security question to {maxLength} characters or fewer.'**
  String securityQuestionTooLong(int maxLength);

  /// Error when the security answer is shorter than the minimum, in characters.
  ///
  /// In en, this message translates to:
  /// **'The security answer needs at least {minLength} characters. Capital letters and extra spaces don\'t count.'**
  String securityAnswerTooShort(int minLength);

  /// Error when the security answer is longer than the limit, in characters.
  ///
  /// In en, this message translates to:
  /// **'Keep the security answer to {maxLength} characters or fewer.'**
  String securityAnswerTooLong(int maxLength);

  /// Error when the security answer appears in the question.
  ///
  /// In en, this message translates to:
  /// **'The answer can\'t appear in the question.'**
  String get securityAnswerInQuestion;

  /// Error when the security answer is the same as the PIN.
  ///
  /// In en, this message translates to:
  /// **'The answer can\'t be the same as your PIN.'**
  String get securityAnswerMatchesPin;

  /// Help text under the security answer field at setup; the number is the minimum length in characters.
  ///
  /// In en, this message translates to:
  /// **'At least {minLength} characters. Capital letters and extra spaces are ignored. Choose an answer others can\'t guess or look up.'**
  String securityAnswerRules(int minLength);

  /// Explanation shown once to users upgrading from an older version, who must set a new security question and answer.
  ///
  /// In en, this message translates to:
  /// **'Denarii Dolor now protects your data with your PIN and a stronger recovery answer. Set a security question and answer to finish signing in.'**
  String get upgradeRecoveryIntro;

  /// Button that saves the new security question and answer during an upgrade, then signs in.
  ///
  /// In en, this message translates to:
  /// **'Save and Continue'**
  String get saveAndContinue;

  /// Error when the device's secure storage can't be read, so the data can't be unlocked.
  ///
  /// In en, this message translates to:
  /// **'Secure storage on this device can\'t be read, so your data can\'t be unlocked. Try again, or wipe all app data to start over.'**
  String get storageErrorMessage;

  /// Button that retries after a storage error on the sign-in screen.
  ///
  /// In en, this message translates to:
  /// **'Try Again'**
  String get tryAgain;

  /// Confirmation after the first setup is saved.
  ///
  /// In en, this message translates to:
  /// **'Security profile created. Sign in with your PIN.'**
  String get profileSetupSuccess;

  /// Error when setup runs although a security profile already exists.
  ///
  /// In en, this message translates to:
  /// **'Security profile is already configured.'**
  String get profileAlreadyConfigured;

  /// Error when PIN recovery starts before any security profile exists.
  ///
  /// In en, this message translates to:
  /// **'Set up a security profile before recovering a PIN.'**
  String get profileNotConfigured;

  /// Error when the answer to the security question is wrong.
  ///
  /// In en, this message translates to:
  /// **'Security answer is incorrect.'**
  String get invalidSecurityAnswer;

  /// Confirmation after a forgotten PIN is replaced.
  ///
  /// In en, this message translates to:
  /// **'PIN reset successful. Sign in with your new PIN.'**
  String get pinRecoverySuccess;

  /// Confirmation after all app data is erased.
  ///
  /// In en, this message translates to:
  /// **'All app data was deleted. Set up a new security profile.'**
  String get wipeDataSuccess;

  /// Error when erasing the app data fails.
  ///
  /// In en, this message translates to:
  /// **'Unable to wipe app data. Please try again.'**
  String get wipeDataError;

  /// Shown in place of the security question when it can't be read.
  ///
  /// In en, this message translates to:
  /// **'Security question unavailable.'**
  String get securityQuestionUnavailable;

  /// Title of the screen for a new transaction, and the label of the + button.
  ///
  /// In en, this message translates to:
  /// **'Add Transaction'**
  String get addTransaction;

  /// Label of the description field in the transaction form.
  ///
  /// In en, this message translates to:
  /// **'Description'**
  String get transactionDescriptionHint;

  /// Label of the control that picks expense, income, or transfer.
  ///
  /// In en, this message translates to:
  /// **'Transaction type'**
  String get transactionTypeLabel;

  /// Label of the amount field in the transaction form.
  ///
  /// In en, this message translates to:
  /// **'Amount'**
  String get transactionAmountHint;

  /// Error when a transaction refers to an account or category that can't be found.
  ///
  /// In en, this message translates to:
  /// **'Review the account and category IDs before saving this transaction.'**
  String get transactionReferenceRequiredMessage;

  /// Label of the date field in the transaction form; keep the YYYY-MM-DD format as it is.
  ///
  /// In en, this message translates to:
  /// **'Date (YYYY-MM-DD)'**
  String get dateHint;

  /// Button that saves a new transaction.
  ///
  /// In en, this message translates to:
  /// **'Save Transaction'**
  String get saveTransaction;

  /// The expense transaction type.
  ///
  /// In en, this message translates to:
  /// **'Expense'**
  String get transactionTypeExpense;

  /// The income transaction type.
  ///
  /// In en, this message translates to:
  /// **'Income'**
  String get transactionTypeIncome;

  /// The transfer transaction type, which moves money between two accounts.
  ///
  /// In en, this message translates to:
  /// **'Transfer'**
  String get transactionTypeTransfer;

  /// Title of the Search screen and its navigation item.
  ///
  /// In en, this message translates to:
  /// **'Search'**
  String get search;

  /// Label of the search field that matches transaction descriptions.
  ///
  /// In en, this message translates to:
  /// **'Description'**
  String get searchDescriptionHint;

  /// Label of the search filter for the smallest amount.
  ///
  /// In en, this message translates to:
  /// **'Min amount'**
  String get searchMinAmountHint;

  /// Label of the search filter for the largest amount.
  ///
  /// In en, this message translates to:
  /// **'Max amount'**
  String get searchMaxAmountHint;

  /// Label of the search filter for the first date; keep the YYYY-MM-DD format as it is.
  ///
  /// In en, this message translates to:
  /// **'Start date (YYYY-MM-DD)'**
  String get searchStartDateHint;

  /// Label of the search filter for the last date; keep the YYYY-MM-DD format as it is.
  ///
  /// In en, this message translates to:
  /// **'End date (YYYY-MM-DD)'**
  String get searchEndDateHint;

  /// Error when a typed date isn't in the YYYY-MM-DD format.
  ///
  /// In en, this message translates to:
  /// **'Use the YYYY-MM-DD date format.'**
  String get invalidDateMessage;

  /// Error when the search filters can't be read.
  ///
  /// In en, this message translates to:
  /// **'Check your search dates and numeric filters, then try again.'**
  String get invalidSearchFiltersMessage;

  /// Error when a search's amount or date range is invalid.
  ///
  /// In en, this message translates to:
  /// **'Enter a valid amount and date range.'**
  String get invalidSearchRangeMessage;

  /// Title of the Reports screen and its navigation item. The Help screen quotes it.
  ///
  /// In en, this message translates to:
  /// **'Reports'**
  String get reports;

  /// Title of the Settings screen and its navigation item.
  ///
  /// In en, this message translates to:
  /// **'Settings'**
  String get settings;

  /// Settings line that says when the app signs the user out; the number is minutes.
  ///
  /// In en, this message translates to:
  /// **'Session timeout: {minutes} minutes of inactivity'**
  String settingsSessionTimeout(int minutes);

  /// Settings line shown when a PIN is set up.
  ///
  /// In en, this message translates to:
  /// **'PIN is configured for quick sign-in.'**
  String get settingsPinConfigured;

  /// Settings line shown when no PIN is set up.
  ///
  /// In en, this message translates to:
  /// **'No PIN is configured yet.'**
  String get settingsPinNotConfigured;

  /// Button that signs out and locks the app's data.
  ///
  /// In en, this message translates to:
  /// **'Sign Out'**
  String get signOut;

  /// Button that saves a new PIN during PIN recovery.
  ///
  /// In en, this message translates to:
  /// **'Reset PIN'**
  String get resetPin;

  /// Settings switch for the dark theme.
  ///
  /// In en, this message translates to:
  /// **'Dark mode'**
  String get settingsDarkMode;

  /// Settings switch for signing in with a fingerprint or face. The Help screen quotes it.
  ///
  /// In en, this message translates to:
  /// **'Biometric sign-in'**
  String get settingsBiometricSignIn;

  /// Settings item that opens the currency choice. The Help screen quotes it.
  ///
  /// In en, this message translates to:
  /// **'Currency'**
  String get settingsCurrency;

  /// The chosen currency under the Currency setting: its name, then its symbol in parentheses.
  ///
  /// In en, this message translates to:
  /// **'{currencyName} ({symbol})'**
  String settingsCurrencyValue(String currencyName, String symbol);

  /// Title of the dialog that lists the currencies.
  ///
  /// In en, this message translates to:
  /// **'Choose a currency'**
  String get settingsCurrencyTitle;

  /// Note in the currency dialog: changing the currency doesn't convert amounts.
  ///
  /// In en, this message translates to:
  /// **'Only the symbol changes. Amounts you\'ve already entered aren\'t converted.'**
  String get settingsCurrencyNote;

  /// Settings item and screen title for backups. The Help screen quotes it.
  ///
  /// In en, this message translates to:
  /// **'Backup and restore'**
  String get settingsBackupRestore;

  /// Settings item and screen title for the privacy policy.
  ///
  /// In en, this message translates to:
  /// **'Privacy policy'**
  String get settingsPrivacyPolicy;

  /// Settings item and screen title for the open-source licenses.
  ///
  /// In en, this message translates to:
  /// **'Open-source licenses'**
  String get settingsLicenses;

  /// Explanation at the top of the backup screen of what a backup holds.
  ///
  /// In en, this message translates to:
  /// **'A backup file holds your accounts, categories, budgets, transactions and currency setting, encrypted with a passphrase you choose. It doesn\'t hold your PIN, security question or biometric sign-in: on a new phone, set up the app first, then restore the backup here.'**
  String get backupExplanation;

  /// Warning on the backup screen that a lost passphrase can't be recovered.
  ///
  /// In en, this message translates to:
  /// **'Keep the passphrase safe. Without it the backup can\'t be opened, and nobody can recover it.'**
  String get backupPassphraseWarning;

  /// Button that starts creating a backup file.
  ///
  /// In en, this message translates to:
  /// **'Create backup'**
  String get backupCreate;

  /// Button that starts restoring from a backup file.
  ///
  /// In en, this message translates to:
  /// **'Restore from backup'**
  String get backupRestore;

  /// Title of the dialog where the user chooses a passphrase for a new backup.
  ///
  /// In en, this message translates to:
  /// **'Choose a passphrase'**
  String get backupCreateTitle;

  /// Title of the dialog where the user types a backup's passphrase to restore it.
  ///
  /// In en, this message translates to:
  /// **'Backup passphrase'**
  String get backupRestoreTitle;

  /// Label of the backup passphrase field.
  ///
  /// In en, this message translates to:
  /// **'Passphrase'**
  String get backupPassphrase;

  /// Label of the field where the backup passphrase is typed a second time.
  ///
  /// In en, this message translates to:
  /// **'Confirm passphrase'**
  String get backupPassphraseConfirm;

  /// Help text under the backup passphrase field; the number is the minimum length in characters.
  ///
  /// In en, this message translates to:
  /// **'At least {minLength} characters. A few unrelated words work well.'**
  String backupPassphraseRules(int minLength);

  /// Button that continues after the passphrase is typed.
  ///
  /// In en, this message translates to:
  /// **'Continue'**
  String get backupContinue;

  /// Shown while a backup is created or restored.
  ///
  /// In en, this message translates to:
  /// **'Working…'**
  String get backupWorking;

  /// Confirmation after a backup file is saved.
  ///
  /// In en, this message translates to:
  /// **'Backup saved.'**
  String get backupSaved;

  /// Error when the backup file couldn't be saved.
  ///
  /// In en, this message translates to:
  /// **'Couldn\'t save the backup.'**
  String get backupSaveFailed;

  /// Title of the dialog that confirms replacing all data with a backup.
  ///
  /// In en, this message translates to:
  /// **'Replace all data?'**
  String get backupRestoreConfirmTitle;

  /// What the chosen backup holds, one item per line: when it was created, then the number of accounts, categories, budgets, and transactions. Keep the line breaks.
  ///
  /// In en, this message translates to:
  /// **'Created: {created}\nAccounts: {accounts}\nCategories: {categories}\nBudgets: {budgets}\nTransactions: {transactions}'**
  String backupRestoreContents(
    String created,
    int accounts,
    int categories,
    int budgets,
    int transactions,
  );

  /// Warning in the restore dialog that the data on the phone will be replaced.
  ///
  /// In en, this message translates to:
  /// **'Restoring erases everything now on this phone and replaces it with the backup. This can\'t be undone.'**
  String get backupRestoreWarning;

  /// The word the user types to confirm restoring a backup over all data. Don't translate it: the check compares the typed text with this exact word.
  ///
  /// In en, this message translates to:
  /// **'RESTORE'**
  String get backupRestoreConfirmationWord;

  /// Button that replaces all data with the backup.
  ///
  /// In en, this message translates to:
  /// **'Replace data'**
  String get backupRestoreAction;

  /// Confirmation after a backup is restored.
  ///
  /// In en, this message translates to:
  /// **'Backup restored.'**
  String get backupRestored;

  /// Error when a restore fails; the data on the phone is unchanged.
  ///
  /// In en, this message translates to:
  /// **'Couldn\'t restore the backup. Your data hasn\'t changed.'**
  String get backupRestoreFailed;

  /// Error when the backup passphrase is too short or too long, in characters.
  ///
  /// In en, this message translates to:
  /// **'The passphrase must be {minLength} to {maxLength} characters.'**
  String backupErrorPassphraseLength(int minLength, int maxLength);

  /// Error when the two typed backup passphrases differ.
  ///
  /// In en, this message translates to:
  /// **'The passphrases don\'t match.'**
  String get backupErrorPassphraseMismatch;

  /// Error when the chosen file isn't a backup made by this app.
  ///
  /// In en, this message translates to:
  /// **'This file isn\'t a Denarii Dolor backup.'**
  String get backupErrorNotBackup;

  /// Error when the backup was made by a newer version of the app.
  ///
  /// In en, this message translates to:
  /// **'This backup was made by a newer version of Denarii Dolor. Update the app, then try again.'**
  String get backupErrorNewerVersion;

  /// Error when the backup can't be opened with the typed passphrase.
  ///
  /// In en, this message translates to:
  /// **'Wrong passphrase, or the file is damaged.'**
  String get backupErrorWrongPassphrase;

  /// Error when the backup file is damaged.
  ///
  /// In en, this message translates to:
  /// **'The backup is damaged and can\'t be restored.'**
  String get backupErrorDamaged;

  /// Error when the chosen file can't be read.
  ///
  /// In en, this message translates to:
  /// **'Couldn\'t read the file.'**
  String get backupErrorRead;

  /// Error when the privacy policy or the licenses can't be opened.
  ///
  /// In en, this message translates to:
  /// **'Couldn\'t open this document.'**
  String get legalLoadFailed;

  /// Note above a legal document that exists only in English.
  ///
  /// In en, this message translates to:
  /// **'This document is available in English only.'**
  String get legalEnglishOnly;

  /// Label of the category picker in the transaction form and in search.
  ///
  /// In en, this message translates to:
  /// **'Category'**
  String get transactionCategoryLabel;

  /// Label of the account picker in the transaction form.
  ///
  /// In en, this message translates to:
  /// **'Account'**
  String get transactionAccountLabel;

  /// Label of the picker for the account a transfer moves money to.
  ///
  /// In en, this message translates to:
  /// **'Transfer to account'**
  String get transactionTransferAccountLabel;

  /// Title of the screen that edits an existing transaction.
  ///
  /// In en, this message translates to:
  /// **'Edit Transaction'**
  String get editTransaction;

  /// Button that saves changes to an existing transaction.
  ///
  /// In en, this message translates to:
  /// **'Update Transaction'**
  String get updateTransaction;

  /// Confirmation after a transaction is saved.
  ///
  /// In en, this message translates to:
  /// **'Transaction saved.'**
  String get transactionSaved;

  /// Confirmation after a transaction is deleted.
  ///
  /// In en, this message translates to:
  /// **'Transaction deleted.'**
  String get transactionDeleted;

  /// Error when the transaction being edited no longer exists.
  ///
  /// In en, this message translates to:
  /// **'Transaction not found.'**
  String get transactionNotFound;

  /// Heading of the list of recent transactions on the Dashboard.
  ///
  /// In en, this message translates to:
  /// **'Recent transactions'**
  String get recentTransactions;

  /// Shown on the Dashboard before any transaction exists; + is the add button.
  ///
  /// In en, this message translates to:
  /// **'No transactions yet. Tap + to add one.'**
  String get noTransactions;

  /// Heading of the account balances on the Dashboard.
  ///
  /// In en, this message translates to:
  /// **'Account balances'**
  String get accountBalances;

  /// Title of the dialog that confirms deleting a transaction.
  ///
  /// In en, this message translates to:
  /// **'Delete transaction?'**
  String get deleteTransactionTitle;

  /// Message in the dialog that confirms deleting a transaction; the text in quotes is the transaction's description.
  ///
  /// In en, this message translates to:
  /// **'Delete \"{description}\"? Account balances will be adjusted.'**
  String deleteTransactionMessage(String description);

  /// Title of the dialog that confirms deleting an account, a category, or a budget; the name is the item's.
  ///
  /// In en, this message translates to:
  /// **'Delete {name}?'**
  String deleteItemTitle(String name);

  /// Message in the dialog that confirms deleting an account, a category, or a budget.
  ///
  /// In en, this message translates to:
  /// **'This cannot be undone.'**
  String get deleteItemMessage;

  /// Shown in a picker before anything is chosen.
  ///
  /// In en, this message translates to:
  /// **'Select…'**
  String get pickerPlaceholder;

  /// Button that opens an item for editing.
  ///
  /// In en, this message translates to:
  /// **'Edit'**
  String get edit;

  /// Button that deletes an item.
  ///
  /// In en, this message translates to:
  /// **'Delete'**
  String get delete;

  /// Button that closes a dialog without changing anything.
  ///
  /// In en, this message translates to:
  /// **'Cancel'**
  String get cancel;

  /// Button that saves a form.
  ///
  /// In en, this message translates to:
  /// **'Save'**
  String get save;

  /// Button that goes back to the previous screen.
  ///
  /// In en, this message translates to:
  /// **'Back'**
  String get back;

  /// Error shown when something unexpected fails.
  ///
  /// In en, this message translates to:
  /// **'Something went wrong. Please try again.'**
  String get genericError;

  /// Settings item and screen title for adding, editing, and deleting categories.
  ///
  /// In en, this message translates to:
  /// **'Manage Categories'**
  String get manageCategories;

  /// Settings item and screen title for adding, renaming, and deleting accounts.
  ///
  /// In en, this message translates to:
  /// **'Manage Accounts'**
  String get manageAccounts;

  /// Settings item and screen title for budgets. The Help screen quotes it.
  ///
  /// In en, this message translates to:
  /// **'Manage Budgets'**
  String get manageBudgets;

  /// Button and dialog title for a new category.
  ///
  /// In en, this message translates to:
  /// **'Add Category'**
  String get addCategory;

  /// Label of the category name field.
  ///
  /// In en, this message translates to:
  /// **'Category name'**
  String get categoryName;

  /// Confirmation after a category is saved.
  ///
  /// In en, this message translates to:
  /// **'Category saved.'**
  String get categorySaved;

  /// Confirmation after a category is deleted.
  ///
  /// In en, this message translates to:
  /// **'Category deleted.'**
  String get categoryDeleted;

  /// Button and dialog title for a new account.
  ///
  /// In en, this message translates to:
  /// **'Add Account'**
  String get addAccount;

  /// Dialog title for renaming an account.
  ///
  /// In en, this message translates to:
  /// **'Rename Account'**
  String get renameAccount;

  /// Label of the account name field.
  ///
  /// In en, this message translates to:
  /// **'Account name'**
  String get accountName;

  /// Label of the field for a new account's starting balance.
  ///
  /// In en, this message translates to:
  /// **'Opening balance'**
  String get openingBalance;

  /// An account's balance in the account list; the value is a formatted amount.
  ///
  /// In en, this message translates to:
  /// **'Balance: {balance}'**
  String accountBalance(String balance);

  /// Confirmation after an account is saved.
  ///
  /// In en, this message translates to:
  /// **'Account saved.'**
  String get accountSaved;

  /// Confirmation after an account is deleted.
  ///
  /// In en, this message translates to:
  /// **'Account deleted.'**
  String get accountDeleted;

  /// Error when the opening balance isn't a valid amount.
  ///
  /// In en, this message translates to:
  /// **'Enter a valid opening balance (at most 2 decimals).'**
  String get invalidOpeningBalance;

  /// Button and dialog title for setting a category's monthly budget.
  ///
  /// In en, this message translates to:
  /// **'Set Budget'**
  String get setBudget;

  /// Title of a category's budget; the name is the category's.
  ///
  /// In en, this message translates to:
  /// **'Budget: {category}'**
  String budgetForCategory(String category);

  /// Label of the field for a budget's monthly limit.
  ///
  /// In en, this message translates to:
  /// **'Monthly limit'**
  String get budgetLimit;

  /// Label of the field for the percentage of the limit at which the budget warns.
  ///
  /// In en, this message translates to:
  /// **'Warning threshold (percent of limit)'**
  String get budgetWarningPercent;

  /// A budget in the budget list: its monthly limit, then the percentage at which it warns.
  ///
  /// In en, this message translates to:
  /// **'Limit {limit} · warn at {percent}%'**
  String budgetSummary(String limit, int percent);

  /// Shown for a category that has no budget.
  ///
  /// In en, this message translates to:
  /// **'No budget set'**
  String get noBudget;

  /// Button that removes a category's budget.
  ///
  /// In en, this message translates to:
  /// **'Remove'**
  String get remove;

  /// Confirmation after a budget is saved.
  ///
  /// In en, this message translates to:
  /// **'Budget saved.'**
  String get budgetSaved;

  /// Confirmation after a budget is removed.
  ///
  /// In en, this message translates to:
  /// **'Budget removed.'**
  String get budgetDeleted;

  /// Error when the budget limit or warning percentage is invalid.
  ///
  /// In en, this message translates to:
  /// **'Enter a limit with at most 2 decimals and a warning percentage from 1 to 100.'**
  String get invalidBudgetInput;

  /// Error after a wrong PIN; the number is how many attempts remain before a temporary lockout.
  ///
  /// In en, this message translates to:
  /// **'Invalid PIN. {attempts} attempt(s) left before a temporary lockout.'**
  String invalidPinAttemptsLeft(int attempts);

  /// Error during a lockout after too many wrong attempts; the number is the seconds left.
  ///
  /// In en, this message translates to:
  /// **'Too many failed attempts. Try again in {seconds} seconds.'**
  String pinLockedOut(int seconds);

  /// Search filter choice that includes every category.
  ///
  /// In en, this message translates to:
  /// **'All categories'**
  String get searchAllCategories;

  /// Button that clears the search filters.
  ///
  /// In en, this message translates to:
  /// **'Clear'**
  String get searchClear;

  /// How many transactions a search found.
  ///
  /// In en, this message translates to:
  /// **'{count} result(s)'**
  String searchResultCount(int count);

  /// Shown when a search finds nothing.
  ///
  /// In en, this message translates to:
  /// **'No matching transactions.'**
  String get searchNoResults;

  /// Button that shows the previous month's report. Keep the ‹ character as it is; right-to-left text mirrors it.
  ///
  /// In en, this message translates to:
  /// **'‹ Prev'**
  String get previousMonth;

  /// Button that shows the next month's report. Keep the › character as it is; right-to-left text mirrors it.
  ///
  /// In en, this message translates to:
  /// **'Next ›'**
  String get nextMonth;

  /// Title of the monthly report, on screen and in the PDF.
  ///
  /// In en, this message translates to:
  /// **'Monthly Spending Report'**
  String get reportTitle;

  /// When the report was made, on screen and in the PDF; the value is a formatted date and time.
  ///
  /// In en, this message translates to:
  /// **'Generated {generated}'**
  String reportGenerated(String generated);

  /// Page number in the PDF report's footer.
  ///
  /// In en, this message translates to:
  /// **'Page {page}'**
  String reportPage(int page);

  /// The month's totals, on screen and in the PDF: income, expenses, and net, each a formatted amount.
  ///
  /// In en, this message translates to:
  /// **'Income {income} · Expense {expense} · Net {net}'**
  String reportTotals(String income, String expense, String net);

  /// Column heading in the report and the PDF.
  ///
  /// In en, this message translates to:
  /// **'Date'**
  String get reportColDate;

  /// Column heading in the report and the PDF: expense, income, or transfer.
  ///
  /// In en, this message translates to:
  /// **'Type'**
  String get reportColType;

  /// Column heading in the report and the PDF.
  ///
  /// In en, this message translates to:
  /// **'Category'**
  String get reportColCategory;

  /// Column heading in the report and the PDF.
  ///
  /// In en, this message translates to:
  /// **'Description'**
  String get reportColDescription;

  /// Column heading in the report and the PDF.
  ///
  /// In en, this message translates to:
  /// **'Amount'**
  String get reportColAmount;

  /// Column heading in the report and the PDF: the account the money came from.
  ///
  /// In en, this message translates to:
  /// **'Payment Method'**
  String get reportColPayment;

  /// Shown in the report, and in the PDF, when the month has no transactions.
  ///
  /// In en, this message translates to:
  /// **'No transactions in this month.'**
  String get reportEmpty;

  /// Error when the report can't be made.
  ///
  /// In en, this message translates to:
  /// **'Couldn\'t generate the report. Try again.'**
  String get reportLoadFailed;

  /// Button that saves the report as a CSV file.
  ///
  /// In en, this message translates to:
  /// **'Save CSV'**
  String get saveCsv;

  /// Button that saves the report as a PDF file.
  ///
  /// In en, this message translates to:
  /// **'Save PDF'**
  String get savePdf;

  /// Button that shares the report as a CSV file.
  ///
  /// In en, this message translates to:
  /// **'Share CSV'**
  String get shareCsv;

  /// Button that shares the report as a PDF file.
  ///
  /// In en, this message translates to:
  /// **'Share PDF'**
  String get sharePdf;

  /// Title of the system share sheet for a report.
  ///
  /// In en, this message translates to:
  /// **'Share report'**
  String get shareReport;

  /// Confirmation after a report file is saved.
  ///
  /// In en, this message translates to:
  /// **'Report saved.'**
  String get reportSaved;

  /// Error when the report file can't be saved or shared.
  ///
  /// In en, this message translates to:
  /// **'Couldn\'t export the report.'**
  String get reportExportFailed;

  /// Dashboard heading: income minus expenses for the current month.
  ///
  /// In en, this message translates to:
  /// **'Net this month'**
  String get netThisMonth;

  /// Dashboard label of the current month's income.
  ///
  /// In en, this message translates to:
  /// **'Income'**
  String get incomeThisMonth;

  /// Dashboard label of the current month's expenses.
  ///
  /// In en, this message translates to:
  /// **'Expenses'**
  String get expenseThisMonth;

  /// Heading of the Dashboard chart of spending by category.
  ///
  /// In en, this message translates to:
  /// **'Spending by category'**
  String get spendingByCategory;

  /// Shown in place of the spending chart when the month has no expenses.
  ///
  /// In en, this message translates to:
  /// **'No expenses this month.'**
  String get spendingNone;

  /// Chart slice that groups the smallest categories.
  ///
  /// In en, this message translates to:
  /// **'Other'**
  String get spendingOther;

  /// Heading of the budget progress on the Dashboard.
  ///
  /// In en, this message translates to:
  /// **'Budgets this month'**
  String get budgetsThisMonth;

  /// A budget's progress: the amount spent, then the monthly limit, each a formatted amount.
  ///
  /// In en, this message translates to:
  /// **'{spent} of {limit}'**
  String budgetSpentOfLimit(String spent, String limit);

  /// A budget's status word, then the percentage of the limit used.
  ///
  /// In en, this message translates to:
  /// **'{status} · {percent}% used'**
  String budgetStatusLine(String status, int percent);

  /// Budget status: under the warning threshold.
  ///
  /// In en, this message translates to:
  /// **'On track'**
  String get budgetStatusOk;

  /// Budget status: at or above the warning threshold.
  ///
  /// In en, this message translates to:
  /// **'Near limit'**
  String get budgetStatusWarning;

  /// Budget status: over the limit.
  ///
  /// In en, this message translates to:
  /// **'Over budget'**
  String get budgetStatusOver;

  /// Dashboard alert: how many budgets are near or over their limit.
  ///
  /// In en, this message translates to:
  /// **'{count, plural, one{{count} budget is near or over its limit.} other{{count} budgets are near or over their limits.}}'**
  String budgetAlerts(int count);

  /// Dialog title for editing a category.
  ///
  /// In en, this message translates to:
  /// **'Edit Category'**
  String get editCategory;

  /// Heading of the icon grid in the category dialog, naming the chosen icon.
  ///
  /// In en, this message translates to:
  /// **'Icon: {icon}'**
  String categoryIconLabel(String icon);

  /// Name of a category icon, read by screen readers and shown in the category dialog.
  ///
  /// In en, this message translates to:
  /// **'General'**
  String get categoryIconGeneral;

  /// Name of a category icon, read by screen readers and shown in the category dialog.
  ///
  /// In en, this message translates to:
  /// **'Income'**
  String get categoryIconIncome;

  /// Name of a category icon, read by screen readers and shown in the category dialog.
  ///
  /// In en, this message translates to:
  /// **'Transfer'**
  String get categoryIconTransfer;

  /// Name of a category icon, read by screen readers and shown in the category dialog.
  ///
  /// In en, this message translates to:
  /// **'Groceries'**
  String get categoryIconGroceries;

  /// Name of a category icon, read by screen readers and shown in the category dialog.
  ///
  /// In en, this message translates to:
  /// **'Dining'**
  String get categoryIconDining;

  /// Name of a category icon, read by screen readers and shown in the category dialog.
  ///
  /// In en, this message translates to:
  /// **'Coffee'**
  String get categoryIconCoffee;

  /// Name of a category icon, read by screen readers and shown in the category dialog.
  ///
  /// In en, this message translates to:
  /// **'Housing'**
  String get categoryIconHousing;

  /// Name of a category icon, read by screen readers and shown in the category dialog.
  ///
  /// In en, this message translates to:
  /// **'Utilities'**
  String get categoryIconUtilities;

  /// Name of a category icon, read by screen readers and shown in the category dialog.
  ///
  /// In en, this message translates to:
  /// **'Internet'**
  String get categoryIconInternet;

  /// Name of a category icon, read by screen readers and shown in the category dialog.
  ///
  /// In en, this message translates to:
  /// **'Phone'**
  String get categoryIconPhone;

  /// Name of a category icon, read by screen readers and shown in the category dialog.
  ///
  /// In en, this message translates to:
  /// **'Car'**
  String get categoryIconCar;

  /// Name of a category icon, read by screen readers and shown in the category dialog.
  ///
  /// In en, this message translates to:
  /// **'Fuel'**
  String get categoryIconFuel;

  /// Name of a category icon, read by screen readers and shown in the category dialog.
  ///
  /// In en, this message translates to:
  /// **'Transit'**
  String get categoryIconTransit;

  /// Name of a category icon, read by screen readers and shown in the category dialog.
  ///
  /// In en, this message translates to:
  /// **'Travel'**
  String get categoryIconTravel;

  /// Name of a category icon, read by screen readers and shown in the category dialog.
  ///
  /// In en, this message translates to:
  /// **'Health'**
  String get categoryIconHealth;

  /// Name of a category icon, read by screen readers and shown in the category dialog.
  ///
  /// In en, this message translates to:
  /// **'Fitness'**
  String get categoryIconFitness;

  /// Name of a category icon, read by screen readers and shown in the category dialog.
  ///
  /// In en, this message translates to:
  /// **'Shopping'**
  String get categoryIconShopping;

  /// Name of a category icon, read by screen readers and shown in the category dialog.
  ///
  /// In en, this message translates to:
  /// **'Clothing'**
  String get categoryIconClothing;

  /// Name of a category icon, read by screen readers and shown in the category dialog.
  ///
  /// In en, this message translates to:
  /// **'Entertainment'**
  String get categoryIconEntertainment;

  /// Name of a category icon, read by screen readers and shown in the category dialog.
  ///
  /// In en, this message translates to:
  /// **'Education'**
  String get categoryIconEducation;

  /// Name of a category icon, read by screen readers and shown in the category dialog.
  ///
  /// In en, this message translates to:
  /// **'Pets'**
  String get categoryIconPets;

  /// Name of a category icon, read by screen readers and shown in the category dialog.
  ///
  /// In en, this message translates to:
  /// **'Gifts'**
  String get categoryIconGifts;

  /// Name of a category icon, read by screen readers and shown in the category dialog.
  ///
  /// In en, this message translates to:
  /// **'Savings'**
  String get categoryIconSavings;

  /// Name of a category icon, read by screen readers and shown in the category dialog.
  ///
  /// In en, this message translates to:
  /// **'Bank & fees'**
  String get categoryIconBank;

  /// Error when the amount isn't a positive number with at most 2 decimals.
  ///
  /// In en, this message translates to:
  /// **'Enter an amount greater than 0 with at most 2 decimals.'**
  String get invalidAmountMessage;

  /// Error when a screen opens without the item it should show.
  ///
  /// In en, this message translates to:
  /// **'This item could not be found. Go back and try again.'**
  String get errorIdRequired;

  /// Error when the item no longer exists.
  ///
  /// In en, this message translates to:
  /// **'This item no longer exists.'**
  String get errorItemNotFound;

  /// Error when the transaction description is empty.
  ///
  /// In en, this message translates to:
  /// **'Enter a description.'**
  String get errorBlankDescription;

  /// Error when the description is longer than the limit, in characters.
  ///
  /// In en, this message translates to:
  /// **'Description must be at most {maxLength} characters.'**
  String errorDescriptionTooLong(int maxLength);

  /// Error when the transaction date is invalid.
  ///
  /// In en, this message translates to:
  /// **'Choose a valid date.'**
  String get errorInvalidDate;

  /// Error when a transfer has no account to move money to.
  ///
  /// In en, this message translates to:
  /// **'Choose the account to transfer to.'**
  String get errorTransferDestinationRequired;

  /// Error when a transfer's two accounts are the same.
  ///
  /// In en, this message translates to:
  /// **'Choose a different account to transfer to.'**
  String get errorTransferDestinationSame;

  /// Error when the chosen category was deleted.
  ///
  /// In en, this message translates to:
  /// **'That category no longer exists. Choose another.'**
  String get errorCategoryNotFound;

  /// Error when the chosen account was deleted.
  ///
  /// In en, this message translates to:
  /// **'That account no longer exists. Choose another.'**
  String get errorAccountNotFound;

  /// Error when an expense would go over its category's monthly budget (code BUDGET_EXCEEDED).
  ///
  /// In en, this message translates to:
  /// **'This expense would put the category over its monthly budget.'**
  String get errorBudgetExceeded;

  /// Title of the dialog that asks before saving an expense that goes over its budget.
  ///
  /// In en, this message translates to:
  /// **'Over budget'**
  String get overBudgetTitle;

  /// Message in the over-budget dialog: the category, the amount it goes over by, and the monthly limit. The amounts are formatted.
  ///
  /// In en, this message translates to:
  /// **'This expense puts {category} {overBy} over its monthly budget of {limit}.'**
  String overBudgetMessage(String category, String overBy, String limit);

  /// Button in the over-budget dialog that saves the expense anyway.
  ///
  /// In en, this message translates to:
  /// **'Save anyway'**
  String get overBudgetConfirm;

  /// Error when a name is empty or longer than the limit, in characters.
  ///
  /// In en, this message translates to:
  /// **'Name must be 1 to {maxLength} characters.'**
  String errorInvalidName(int maxLength);

  /// Error when another category already has the name.
  ///
  /// In en, this message translates to:
  /// **'A category named \"{name}\" already exists.'**
  String errorDuplicateCategoryName(String name);

  /// Error when another account already has the name.
  ///
  /// In en, this message translates to:
  /// **'An account named \"{name}\" already exists.'**
  String errorDuplicateAccountName(String name);

  /// Error when the user tries to delete a default category.
  ///
  /// In en, this message translates to:
  /// **'Default categories can\'t be deleted.'**
  String get errorDefaultCategory;

  /// Error when the user tries to delete a default account.
  ///
  /// In en, this message translates to:
  /// **'Default accounts can\'t be deleted.'**
  String get errorDefaultAccount;

  /// Error when the user tries to delete a category that transactions still use.
  ///
  /// In en, this message translates to:
  /// **'{count, plural, one{This category is used by {count} transaction.} other{This category is used by {count} transactions.}}'**
  String errorCategoryInUse(int count);

  /// Error when the user tries to delete an account that transactions still use.
  ///
  /// In en, this message translates to:
  /// **'{count, plural, one{This account is used by {count} transaction.} other{This account is used by {count} transactions.}}'**
  String errorAccountInUse(int count);

  /// Error when the warning percentage isn't from 1 to 100.
  ///
  /// In en, this message translates to:
  /// **'Warning threshold must be from 1 to 100 percent.'**
  String get errorInvalidWarningPercent;

  /// Name of a default account, saved in the user's language when the app is set up.
  ///
  /// In en, this message translates to:
  /// **'Cash'**
  String get defaultAccountCash;

  /// Name of a default account, saved in the user's language when the app is set up.
  ///
  /// In en, this message translates to:
  /// **'Savings'**
  String get defaultAccountSavings;

  /// Name of the default expense category, saved in the user's language when the app is set up.
  ///
  /// In en, this message translates to:
  /// **'General Expense'**
  String get defaultCategoryExpense;

  /// Name of the default income category, saved in the user's language when the app is set up.
  ///
  /// In en, this message translates to:
  /// **'General Income'**
  String get defaultCategoryIncome;

  /// Name of the default transfer category, saved in the user's language when the app is set up.
  ///
  /// In en, this message translates to:
  /// **'Transfer'**
  String get defaultCategoryTransfer;

  /// Error when the Dashboard can't load the transactions.
  ///
  /// In en, this message translates to:
  /// **'Your transactions couldn\'t be loaded. Leave the app and come back to try again.'**
  String get dashboardLoadFailed;

  /// Welcome tour, page 1 title.
  ///
  /// In en, this message translates to:
  /// **'Your money, kept private'**
  String get tourPrivateTitle;

  /// Welcome tour, page 1 text.
  ///
  /// In en, this message translates to:
  /// **'Track expenses, income and transfers between your accounts. Everything stays on this phone, encrypted and locked with your PIN. No account, no ads, no internet.'**
  String get tourPrivateBody;

  /// Welcome tour, page 2 title.
  ///
  /// In en, this message translates to:
  /// **'Add in seconds'**
  String get tourAddTitle;

  /// Welcome tour, page 2 text; + is the add button.
  ///
  /// In en, this message translates to:
  /// **'Tap + to record an expense, income or transfer. Pick an account and a category, and your balances update on their own.'**
  String get tourAddBody;

  /// Welcome tour, page 3 title.
  ///
  /// In en, this message translates to:
  /// **'Stay on budget'**
  String get tourBudgetTitle;

  /// Welcome tour, page 3 text.
  ///
  /// In en, this message translates to:
  /// **'Give a category a monthly limit. The Dashboard shows how much is left and warns you before you go over.'**
  String get tourBudgetBody;

  /// Welcome tour, page 4 title.
  ///
  /// In en, this message translates to:
  /// **'Reports and backups'**
  String get tourReportsTitle;

  /// Welcome tour, page 4 text.
  ///
  /// In en, this message translates to:
  /// **'Save or share a monthly report as a PDF or CSV file, and keep an encrypted backup so you never lose your records.'**
  String get tourReportsBody;

  /// Button that closes the welcome tour.
  ///
  /// In en, this message translates to:
  /// **'Skip'**
  String get tourSkip;

  /// Button that shows the next page of the welcome tour.
  ///
  /// In en, this message translates to:
  /// **'Next'**
  String get tourNext;

  /// Button on the last page of the welcome tour that closes it.
  ///
  /// In en, this message translates to:
  /// **'Get started'**
  String get tourGetStarted;

  /// Screen reader text of the welcome tour's page dots.
  ///
  /// In en, this message translates to:
  /// **'Page {page} of {pageCount}'**
  String tourPageIndicator(int page, int pageCount);

  /// Heading of the getting-started checklist on the Dashboard.
  ///
  /// In en, this message translates to:
  /// **'Get started'**
  String get gettingStartedTitle;

  /// How many checklist steps are done, out of the total.
  ///
  /// In en, this message translates to:
  /// **'{done} of {total} done'**
  String gettingStartedProgress(int done, int total);

  /// Checklist step.
  ///
  /// In en, this message translates to:
  /// **'Add your bank or card account'**
  String get gettingStartedAddAccount;

  /// Checklist step.
  ///
  /// In en, this message translates to:
  /// **'Record your first transaction'**
  String get gettingStartedAddTransaction;

  /// Checklist step.
  ///
  /// In en, this message translates to:
  /// **'Set a monthly budget'**
  String get gettingStartedSetBudget;

  /// Screen reader state of a finished checklist step.
  ///
  /// In en, this message translates to:
  /// **'Done'**
  String get gettingStartedStepDone;

  /// Screen reader state of an unfinished checklist step.
  ///
  /// In en, this message translates to:
  /// **'Not done yet'**
  String get gettingStartedStepTodo;

  /// Screen reader label of the button that hides the checklist.
  ///
  /// In en, this message translates to:
  /// **'Hide the getting-started checklist'**
  String get gettingStartedHide;

  /// Heading of the help and support section in Settings.
  ///
  /// In en, this message translates to:
  /// **'Help and support'**
  String get settingsHelpSection;

  /// Settings item that opens the Help screen.
  ///
  /// In en, this message translates to:
  /// **'Help & FAQ'**
  String get settingsHelpFaq;

  /// Settings item that shows the welcome tour again.
  ///
  /// In en, this message translates to:
  /// **'Show the welcome tour'**
  String get settingsShowTour;

  /// Settings item that writes an email to support. The Help screen quotes it.
  ///
  /// In en, this message translates to:
  /// **'Send feedback'**
  String get settingsSendFeedback;

  /// Settings item that opens the app's Google Play page.
  ///
  /// In en, this message translates to:
  /// **'Rate this app'**
  String get settingsRateApp;

  /// Settings item that shares a link to the app.
  ///
  /// In en, this message translates to:
  /// **'Share this app'**
  String get settingsShareApp;

  /// Title of the Help screen.
  ///
  /// In en, this message translates to:
  /// **'Help & FAQ'**
  String get helpTitle;

  /// Screen reader state of an open question on the Help screen.
  ///
  /// In en, this message translates to:
  /// **'Expanded'**
  String get helpExpanded;

  /// Screen reader state of a closed question on the Help screen.
  ///
  /// In en, this message translates to:
  /// **'Collapsed'**
  String get helpCollapsed;

  /// Heading of the contact card at the end of the Help screen.
  ///
  /// In en, this message translates to:
  /// **'Still need help?'**
  String get helpContactTitle;

  /// Text of the contact card at the end of the Help screen.
  ///
  /// In en, this message translates to:
  /// **'Send a question, a problem or an idea. Please don\'t include your PIN, a backup passphrase or your financial records.'**
  String get helpContactBody;

  /// Help screen question.
  ///
  /// In en, this message translates to:
  /// **'Where is my data stored?'**
  String get faqDataQ;

  /// Help screen answer.
  ///
  /// In en, this message translates to:
  /// **'Only on this phone. Denarii Dolor has no account or server and can\'t connect to the internet. Your records are kept in an encrypted database that only your PIN, your security answer or, if you turn it on, your fingerprint or face can unlock. Android\'s cloud backup is turned off for the app.'**
  String get faqDataA;

  /// Help screen question.
  ///
  /// In en, this message translates to:
  /// **'I forgot my PIN. What can I do?'**
  String get faqForgotPinQ;

  /// Help screen answer. The quoted names are the sign-in screen's Forgot PIN and Wipe All App Data buttons, each filled in from its own translated string.
  ///
  /// In en, this message translates to:
  /// **'On the sign-in screen, tap “{forgotPin}” and answer your security question to choose a new PIN. If you can\'t answer it either, your data can\'t be unlocked, because nobody else has the key. The only way forward then is “{wipeAllData}” on the sign-in screen, which erases everything so you can start over.'**
  String faqForgotPinA(String forgotPin, String wipeAllData);

  /// Help screen question.
  ///
  /// In en, this message translates to:
  /// **'Why was I signed out?'**
  String get faqSignedOutQ;

  /// Help screen answer: the number of minutes before the app signs out, and the name of the biometric setting, filled in from its own translated string.
  ///
  /// In en, this message translates to:
  /// **'To protect your data, the app signs you out after {minutes} minutes without use. Sign in again with your PIN, or turn on “{biometricSetting}” in Settings to sign in with your fingerprint or face.'**
  String faqSignedOutA(int minutes, String biometricSetting);

  /// Help screen question.
  ///
  /// In en, this message translates to:
  /// **'How do I back up my data or move it to a new phone?'**
  String get faqBackupQ;

  /// Help screen answer. The quoted name is the Backup and restore setting, filled in from its own translated string.
  ///
  /// In en, this message translates to:
  /// **'In Settings, open “{backupSetting}” and create a backup protected by a passphrase. Save the file somewhere safe, such as a cloud drive. On the new phone, install Denarii Dolor, set it up, then restore the file with the same passphrase. Nobody can recover a forgotten passphrase.'**
  String faqBackupA(String backupSetting);

  /// Help screen question.
  ///
  /// In en, this message translates to:
  /// **'How do budgets work?'**
  String get faqBudgetsQ;

  /// Help screen answer. The quoted name is the Manage Budgets screen, filled in from its own translated string.
  ///
  /// In en, this message translates to:
  /// **'In Settings, open “{budgetsScreen}” to give a category a monthly limit and a warning threshold. The Dashboard shows how much of each budget you\'ve used this month. If an expense would go over its budget, the app asks before saving it.'**
  String faqBudgetsA(String budgetsScreen);

  /// Help screen question.
  ///
  /// In en, this message translates to:
  /// **'What is a transfer?'**
  String get faqTransferQ;

  /// Help screen answer.
  ///
  /// In en, this message translates to:
  /// **'A transfer moves money between two of your accounts, for example from Savings to Cash. It changes both balances but doesn\'t count as income or as an expense.'**
  String get faqTransferA;

  /// Help screen question.
  ///
  /// In en, this message translates to:
  /// **'Why can\'t I delete a category or an account?'**
  String get faqDeleteQ;

  /// Help screen answer.
  ///
  /// In en, this message translates to:
  /// **'The default categories and accounts can\'t be deleted, and neither can one that transactions still use. Change or delete those transactions first, then try again.'**
  String get faqDeleteA;

  /// Help screen question.
  ///
  /// In en, this message translates to:
  /// **'How do I change the currency?'**
  String get faqCurrencyQ;

  /// Help screen answer. The quoted name is the Currency setting, filled in from its own translated string.
  ///
  /// In en, this message translates to:
  /// **'In Settings, tap “{currencySetting}” and pick one. Only the symbol changes: amounts you already entered aren\'t converted.'**
  String faqCurrencyA(String currencySetting);

  /// Help screen question.
  ///
  /// In en, this message translates to:
  /// **'How do I export a report?'**
  String get faqReportsQ;

  /// Help screen answer. The quoted name is the Reports screen, filled in from its own translated string.
  ///
  /// In en, this message translates to:
  /// **'Open “{reportsScreen}”, choose a month, then save or share it as a PDF or CSV file. Exported files aren\'t encrypted, so keep them somewhere private.'**
  String faqReportsA(String reportsScreen);

  /// Help screen question.
  ///
  /// In en, this message translates to:
  /// **'Why did fingerprint or face sign-in turn off?'**
  String get faqBiometricQ;

  /// Help screen answer.
  ///
  /// In en, this message translates to:
  /// **'It turns off when the fingerprints or faces enrolled on the phone change, so that a newly added one can\'t open your data. Your PIN still works, and you can turn biometric sign-in back on in Settings.'**
  String get faqBiometricA;

  /// Title of the dialog that asks the user to rate the app.
  ///
  /// In en, this message translates to:
  /// **'Enjoying Denarii Dolor?'**
  String get ratePromptTitle;

  /// Message of the dialog that asks the user to rate the app.
  ///
  /// In en, this message translates to:
  /// **'A quick rating on Google Play helps other people find the app. It only takes a moment.'**
  String get ratePromptMessage;

  /// Button that opens the app's Google Play page to rate it.
  ///
  /// In en, this message translates to:
  /// **'Rate now'**
  String get ratePromptRate;

  /// Button that closes the rating dialog for now.
  ///
  /// In en, this message translates to:
  /// **'Not now'**
  String get ratePromptLater;

  /// Button that closes the rating dialog for good.
  ///
  /// In en, this message translates to:
  /// **'Don\'t ask again'**
  String get ratePromptNever;

  /// Error when Google Play can't be opened.
  ///
  /// In en, this message translates to:
  /// **'Google Play couldn\'t be opened on this device.'**
  String get rateUnavailable;

  /// Title of the system share sheet for sharing the app.
  ///
  /// In en, this message translates to:
  /// **'Share Denarii Dolor'**
  String get shareAppChooser;

  /// Text the user shares to recommend the app, ending with its Google Play link.
  ///
  /// In en, this message translates to:
  /// **'I track my spending and budgets with Denarii Dolor. Everything stays private on the phone. Get it on Google Play: {storeLink}'**
  String shareAppMessage(String storeLink);

  /// Subject of the feedback email; the value is the app's version.
  ///
  /// In en, this message translates to:
  /// **'Denarii Dolor feedback (version {appVersion})'**
  String feedbackEmailSubject(String appVersion);

  /// Body of the feedback email: two blank lines for the user's message, then a separator, the app's version, and the Android version. Keep the line breaks and the separator.
  ///
  /// In en, this message translates to:
  /// **'\n\n———\nApp version: {appVersion}\nAndroid version: {androidVersion}'**
  String feedbackEmailBody(String appVersion, String androidVersion);

  /// Error when the phone has no email app; the value is the support email address.
  ///
  /// In en, this message translates to:
  /// **'No email app was found. You can write to {emailAddress}.'**
  String feedbackNoEmailApp(String emailAddress);

  /// Sign-in placeholder only, until M3.5: the whole number of cents the Rust core read from the typed amount.
  ///
  /// In en, this message translates to:
  /// **'{cents} cents'**
  String placeholderAmountInCents(int cents);

  /// Sign-in placeholder only, until M3.5: the Rust core rejected the typed amount; the value is the error's code name, which isn't translated.
  ///
  /// In en, this message translates to:
  /// **'Rejected: {code}'**
  String placeholderAmountRejected(String code);
}

class _AppLocalizationsDelegate
    extends LocalizationsDelegate<AppLocalizations> {
  const _AppLocalizationsDelegate();

  @override
  Future<AppLocalizations> load(Locale locale) {
    return SynchronousFuture<AppLocalizations>(lookupAppLocalizations(locale));
  }

  @override
  bool isSupported(Locale locale) => <String>[
    'ar',
    'en',
    'es',
    'fr',
    'hi',
    'pt',
    'zh',
  ].contains(locale.languageCode);

  @override
  bool shouldReload(_AppLocalizationsDelegate old) => false;
}

AppLocalizations lookupAppLocalizations(Locale locale) {
  // Lookup logic when only language code is specified.
  switch (locale.languageCode) {
    case 'ar':
      return AppLocalizationsAr();
    case 'en':
      return AppLocalizationsEn();
    case 'es':
      return AppLocalizationsEs();
    case 'fr':
      return AppLocalizationsFr();
    case 'hi':
      return AppLocalizationsHi();
    case 'pt':
      return AppLocalizationsPt();
    case 'zh':
      return AppLocalizationsZh();
  }

  throw FlutterError(
    'AppLocalizations.delegate failed to load unsupported locale "$locale". This is likely '
    'an issue with the localizations generation tool. Please file an issue '
    'on GitHub with a reproducible sample app and the gen-l10n configuration '
    'that was used.',
  );
}
