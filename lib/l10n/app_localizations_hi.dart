// ignore: unused_import
import 'package:intl/intl.dart' as intl;

import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for Hindi (`hi`).
class AppLocalizationsHi extends AppLocalizations {
  AppLocalizationsHi([String locale = 'hi']) : super(locale);

  @override
  String get appName => 'Denarii Dolor';

  @override
  String get dashboard => 'डैशबोर्ड';

  @override
  String get signIn => 'साइन इन करें';

  @override
  String get signInWithBiometrics => 'बायोमेट्रिक से साइन इन करें';

  @override
  String get biometricSignInAccessibilityLabel => 'बायोमेट्रिक से साइन इन करें';

  @override
  String get biometricSignInTitle => 'Denarii Dolor अनलॉक करें';

  @override
  String get biometricSignInSubtitle =>
      'जारी रखने के लिए अपने रजिस्टर किए गए बायोमेट्रिक का इस्तेमाल करें';

  @override
  String get biometricSignInFailed =>
      'बायोमेट्रिक से पुष्टि नहीं हो सकी। फिर से कोशिश करें या अपना PIN इस्तेमाल करें।';

  @override
  String get biometricSignInError =>
      'बायोमेट्रिक साइन-इन अभी उपलब्ध नहीं है। इसके बजाय अपना PIN इस्तेमाल करें।';

  @override
  String get usePinInstead => 'इसके बजाय PIN डालें';

  @override
  String get biometricSignInInvalidated =>
      'इस डिवाइस की बायोमेट्रिक जानकारी बदल गई है, इसलिए बायोमेट्रिक साइन-इन बंद कर दिया गया। अपने PIN से साइन इन करें, फिर सेटिंग में इसे दोबारा चालू करें।';

  @override
  String get biometricEnrollTitle => 'बायोमेट्रिक साइन-इन चालू करें';

  @override
  String get biometricEnrollSubtitle =>
      'अपने फ़िंगरप्रिंट या चेहरे से पुष्टि करें';

  @override
  String get biometricEnabled => 'बायोमेट्रिक साइन-इन चालू है।';

  @override
  String get biometricEnableFailed =>
      'बायोमेट्रिक साइन-इन चालू नहीं किया जा सका।';

  @override
  String get pinHint => 'PIN';

  @override
  String get showPin => 'दिखाएं';

  @override
  String get hidePin => 'छिपाएं';

  @override
  String get newPinHint => 'नया PIN';

  @override
  String get confirmPinHint => 'PIN की पुष्टि करें';

  @override
  String get securityQuestionHint => 'सुरक्षा प्रश्न';

  @override
  String get securityAnswerHint => 'सुरक्षा उत्तर';

  @override
  String get securityAnswerVerifyHint => 'सुरक्षा उत्तर';

  @override
  String get forgotPin => 'PIN भूल गए?';

  @override
  String get backToSignIn => 'साइन इन पर वापस जाएं';

  @override
  String get createSecurityProfile => 'सुरक्षा प्रोफ़ाइल बनाएं';

  @override
  String get wipeAllData => 'ऐप का सारा डेटा मिटाएं';

  @override
  String get wipeAllDataDestructiveLabel =>
      'खतरनाक कार्रवाई: ऐप का सारा डेटा मिटाएं';

  @override
  String get wipeDataTitle => 'ऐप का सारा डेटा मिटाएं?';

  @override
  String get wipeDataWarning =>
      'इससे ऐप का सारा डेटा, वित्तीय रिकॉर्ड, PIN और रिकवरी सेटिंग हमेशा के लिए मिट जाएंगे। इसे पहले जैसा नहीं किया जा सकता।';

  @override
  String get wipeDataConfirm => 'सब कुछ मिटाएं';

  @override
  String wipeDataConfirmCountdown(int seconds) {
    return 'सब कुछ मिटाएं ($seconds)';
  }

  @override
  String get wipeDataConfirmationWord => 'WIPE';

  @override
  String wipeDataTypeToConfirm(String word) {
    return 'पुष्टि करने के लिए $word टाइप करें';
  }

  @override
  String get wipeDataConfirmDestructiveLabel =>
      'खतरनाक कार्रवाई की पुष्टि: ऐप का सारा डेटा हमेशा के लिए मिटाएं';

  @override
  String get wipeDataCancel => 'रद्द करें';

  @override
  String get pinFormatError => 'PIN में 6 से 12 अंक होने चाहिए।';

  @override
  String get pinConfirmationMismatch =>
      'PIN और पुष्टि वाला PIN एक जैसे होने चाहिए।';

  @override
  String get securityQuestionRequired => 'सुरक्षा प्रश्न डालना ज़रूरी है।';

  @override
  String get securityAnswerRequired => 'सुरक्षा उत्तर डालना ज़रूरी है।';

  @override
  String securityQuestionTooLong(int maxLength) {
    return 'सुरक्षा प्रश्न में ज़्यादा से ज़्यादा $maxLength वर्ण हो सकते हैं।';
  }

  @override
  String securityAnswerTooShort(int minLength) {
    return 'सुरक्षा उत्तर में कम से कम $minLength वर्ण होने चाहिए। कैपिटल लेटर और अतिरिक्त स्पेस नहीं गिने जाते।';
  }

  @override
  String securityAnswerTooLong(int maxLength) {
    return 'सुरक्षा उत्तर में ज़्यादा से ज़्यादा $maxLength वर्ण हो सकते हैं।';
  }

  @override
  String get securityAnswerInQuestion =>
      'उत्तर, प्रश्न में शामिल नहीं हो सकता।';

  @override
  String get securityAnswerMatchesPin => 'उत्तर आपके PIN जैसा नहीं हो सकता।';

  @override
  String securityAnswerRules(int minLength) {
    return 'कम से कम $minLength वर्ण। कैपिटल लेटर और अतिरिक्त स्पेस को अनदेखा किया जाता है। ऐसा उत्तर चुनें जिसका दूसरे लोग न तो अंदाज़ा लगा सकें और न ही कहीं से पता लगा सकें।';
  }

  @override
  String get upgradeRecoveryIntro =>
      'Denarii Dolor अब आपके PIN और ज़्यादा मज़बूत रिकवरी उत्तर से आपके डेटा को सुरक्षित रखता है। साइन इन पूरा करने के लिए सुरक्षा प्रश्न और उत्तर सेट करें।';

  @override
  String get saveAndContinue => 'सेव करें और जारी रखें';

  @override
  String get storageErrorMessage =>
      'इस डिवाइस का सुरक्षित स्टोरेज पढ़ा नहीं जा सकता, इसलिए आपका डेटा अनलॉक नहीं हो सकता। फिर से कोशिश करें, या नए सिरे से शुरू करने के लिए ऐप का सारा डेटा मिटाएं।';

  @override
  String get tryAgain => 'फिर से कोशिश करें';

  @override
  String get profileSetupSuccess =>
      'सुरक्षा प्रोफ़ाइल बन गई। अपने PIN से साइन इन करें।';

  @override
  String get profileAlreadyConfigured => 'सुरक्षा प्रोफ़ाइल पहले से सेट है।';

  @override
  String get profileNotConfigured =>
      'PIN रिकवर करने से पहले सुरक्षा प्रोफ़ाइल सेट करें।';

  @override
  String get invalidSecurityAnswer => 'सुरक्षा उत्तर गलत है।';

  @override
  String get pinRecoverySuccess =>
      'PIN रीसेट हो गया। अपने नए PIN से साइन इन करें।';

  @override
  String get wipeDataSuccess =>
      'ऐप का सारा डेटा मिटा दिया गया। नई सुरक्षा प्रोफ़ाइल सेट करें।';

  @override
  String get wipeDataError =>
      'ऐप का डेटा मिटाया नहीं जा सका। कृपया फिर से कोशिश करें।';

  @override
  String get securityQuestionUnavailable => 'सुरक्षा प्रश्न उपलब्ध नहीं है।';

  @override
  String get addTransaction => 'लेन-देन जोड़ें';

  @override
  String get transactionDescriptionHint => 'विवरण';

  @override
  String get transactionTypeLabel => 'लेन-देन का प्रकार';

  @override
  String get transactionAmountHint => 'राशि';

  @override
  String get transactionReferenceRequiredMessage =>
      'इस लेन-देन को सेव करने से पहले खाते और कैटगरी की ID जांच लें।';

  @override
  String get dateHint => 'तारीख (YYYY-MM-DD)';

  @override
  String get saveTransaction => 'लेन-देन सेव करें';

  @override
  String get transactionTypeExpense => 'खर्च';

  @override
  String get transactionTypeIncome => 'आय';

  @override
  String get transactionTypeTransfer => 'ट्रांसफ़र';

  @override
  String get search => 'खोजें';

  @override
  String get searchDescriptionHint => 'विवरण';

  @override
  String get searchMinAmountHint => 'न्यूनतम राशि';

  @override
  String get searchMaxAmountHint => 'अधिकतम राशि';

  @override
  String get searchStartDateHint => 'शुरुआती तारीख (YYYY-MM-DD)';

  @override
  String get searchEndDateHint => 'आखिरी तारीख (YYYY-MM-DD)';

  @override
  String get invalidDateMessage => 'तारीख YYYY-MM-DD फ़ॉर्मैट में डालें।';

  @override
  String get invalidSearchFiltersMessage =>
      'खोज की तारीखें और संख्या वाले फ़िल्टर जांचें, फिर दोबारा कोशिश करें।';

  @override
  String get invalidSearchRangeMessage => 'सही राशि और तारीख की रेंज डालें।';

  @override
  String get reports => 'रिपोर्ट';

  @override
  String get settings => 'सेटिंग';

  @override
  String settingsSessionTimeout(int minutes) {
    return 'सेशन टाइमआउट: $minutes मिनट तक इस्तेमाल न होने पर';
  }

  @override
  String get settingsPinConfigured => 'तुरंत साइन इन के लिए PIN सेट है।';

  @override
  String get settingsPinNotConfigured => 'अभी तक कोई PIN सेट नहीं है।';

  @override
  String get signOut => 'साइन आउट करें';

  @override
  String get resetPin => 'PIN रीसेट करें';

  @override
  String get settingsDarkMode => 'डार्क मोड';

  @override
  String get settingsBiometricSignIn => 'बायोमेट्रिक साइन-इन';

  @override
  String get settingsCurrency => 'मुद्रा';

  @override
  String settingsCurrencyValue(String currencyName, String symbol) {
    return '$currencyName ($symbol)';
  }

  @override
  String get settingsCurrencyTitle => 'मुद्रा चुनें';

  @override
  String get settingsCurrencyNote =>
      'सिर्फ़ मुद्रा का चिह्न बदलेगा। आपने जो राशियां पहले से डाली हैं, वे कन्वर्ट नहीं होंगी।';

  @override
  String get settingsBackupRestore => 'बैकअप और रीस्टोर';

  @override
  String get settingsPrivacyPolicy => 'निजता नीति';

  @override
  String get settingsLicenses => 'ओपन-सोर्स लाइसेंस';

  @override
  String get backupExplanation =>
      'बैकअप फ़ाइल में आपके खाते, कैटगरी, बजट, लेन-देन और मुद्रा की सेटिंग होती है, जिसे आपके चुने हुए पासफ़्रेज़ से एन्क्रिप्ट किया जाता है। इसमें आपका PIN, सुरक्षा प्रश्न या बायोमेट्रिक साइन-इन नहीं होता: नए फ़ोन पर पहले ऐप सेट अप करें, फिर यहां बैकअप रीस्टोर करें।';

  @override
  String get backupPassphraseWarning =>
      'पासफ़्रेज़ संभालकर रखें। इसके बिना बैकअप नहीं खुल सकता, और कोई भी इसे वापस नहीं ला सकता।';

  @override
  String get backupCreate => 'बैकअप बनाएं';

  @override
  String get backupRestore => 'बैकअप से रीस्टोर करें';

  @override
  String get backupCreateTitle => 'पासफ़्रेज़ चुनें';

  @override
  String get backupRestoreTitle => 'बैकअप का पासफ़्रेज़';

  @override
  String get backupPassphrase => 'पासफ़्रेज़';

  @override
  String get backupPassphraseConfirm => 'पासफ़्रेज़ की पुष्टि करें';

  @override
  String backupPassphraseRules(int minLength) {
    return 'कम से कम $minLength अक्षर। आपस में असंबंधित कुछ शब्द अच्छे रहते हैं।';
  }

  @override
  String get backupContinue => 'जारी रखें';

  @override
  String get backupWorking => 'काम चल रहा है…';

  @override
  String get backupSaved => 'बैकअप सेव हो गया।';

  @override
  String get backupSaveFailed => 'बैकअप सेव नहीं हो सका।';

  @override
  String get backupRestoreConfirmTitle => 'सारा डेटा बदलें?';

  @override
  String backupRestoreContents(
    String created,
    int accounts,
    int categories,
    int budgets,
    int transactions,
  ) {
    return 'बनाया गया: $created\nखाते: $accounts\nकैटगरी: $categories\nबजट: $budgets\nलेन-देन: $transactions';
  }

  @override
  String get backupRestoreWarning =>
      'रीस्टोर करने से इस फ़ोन पर अभी मौजूद सब कुछ मिट जाएगा और उसकी जगह बैकअप आ जाएगा। इसे पहले जैसा नहीं किया जा सकता।';

  @override
  String get backupRestoreConfirmationWord => 'RESTORE';

  @override
  String get backupRestoreAction => 'डेटा बदलें';

  @override
  String get backupRestored => 'बैकअप रीस्टोर हो गया।';

  @override
  String get backupRestoreFailed =>
      'बैकअप रीस्टोर नहीं हो सका। आपका डेटा नहीं बदला।';

  @override
  String backupErrorPassphraseLength(int minLength, int maxLength) {
    return 'पासफ़्रेज़ $minLength से $maxLength अक्षरों का होना चाहिए।';
  }

  @override
  String get backupErrorPassphraseMismatch => 'दोनों पासफ़्रेज़ मेल नहीं खाते।';

  @override
  String get backupErrorNotBackup => 'यह फ़ाइल Denarii Dolor का बैकअप नहीं है।';

  @override
  String get backupErrorNewerVersion =>
      'यह बैकअप Denarii Dolor के नए वर्शन से बनाया गया था। ऐप अपडेट करें, फिर से कोशिश करें।';

  @override
  String get backupErrorWrongPassphrase =>
      'पासफ़्रेज़ गलत है, या फ़ाइल खराब है।';

  @override
  String get backupErrorDamaged => 'बैकअप खराब है और रीस्टोर नहीं हो सकता।';

  @override
  String get backupErrorRead => 'फ़ाइल पढ़ी नहीं जा सकी।';

  @override
  String get legalLoadFailed => 'यह दस्तावेज़ खुल नहीं सका।';

  @override
  String get legalEnglishOnly => 'यह दस्तावेज़ सिर्फ़ अंग्रेज़ी में उपलब्ध है।';

  @override
  String get transactionCategoryLabel => 'कैटगरी';

  @override
  String get transactionAccountLabel => 'खाता';

  @override
  String get transactionTransferAccountLabel =>
      'किस खाते में ट्रांसफ़र करना है';

  @override
  String get editTransaction => 'लेन-देन में बदलाव करें';

  @override
  String get updateTransaction => 'लेन-देन अपडेट करें';

  @override
  String get transactionSaved => 'लेन-देन सेव हो गया।';

  @override
  String get transactionDeleted => 'लेन-देन मिटा दिया गया।';

  @override
  String get transactionNotFound => 'लेन-देन नहीं मिला।';

  @override
  String get recentTransactions => 'हाल ही के लेन-देन';

  @override
  String get noTransactions =>
      'अभी तक कोई लेन-देन नहीं है। जोड़ने के लिए + पर टैप करें।';

  @override
  String get accountBalances => 'खातों का बैलेंस';

  @override
  String get deleteTransactionTitle => 'लेन-देन मिटाएं?';

  @override
  String deleteTransactionMessage(String description) {
    return '\"$description\" मिटाएं? खातों का बैलेंस उसी हिसाब से बदल जाएगा।';
  }

  @override
  String deleteItemTitle(String name) {
    return '$name मिटाएं?';
  }

  @override
  String get deleteItemMessage => 'इसे पहले जैसा नहीं किया जा सकता।';

  @override
  String get pickerPlaceholder => 'चुनें…';

  @override
  String get edit => 'बदलाव करें';

  @override
  String get delete => 'मिटाएं';

  @override
  String get cancel => 'रद्द करें';

  @override
  String get save => 'सेव करें';

  @override
  String get back => 'वापस जाएं';

  @override
  String get genericError => 'कुछ गड़बड़ हो गई। कृपया फिर से कोशिश करें।';

  @override
  String get manageCategories => 'कैटगरी मैनेज करें';

  @override
  String get manageAccounts => 'खाते मैनेज करें';

  @override
  String get manageBudgets => 'बजट मैनेज करें';

  @override
  String get addCategory => 'कैटगरी जोड़ें';

  @override
  String get categoryName => 'कैटगरी का नाम';

  @override
  String get categorySaved => 'कैटगरी सेव हो गई।';

  @override
  String get categoryDeleted => 'कैटगरी मिटा दी गई।';

  @override
  String get addAccount => 'खाता जोड़ें';

  @override
  String get renameAccount => 'खाते का नाम बदलें';

  @override
  String get accountName => 'खाते का नाम';

  @override
  String get openingBalance => 'शुरुआती बैलेंस';

  @override
  String accountBalance(String balance) {
    return 'बैलेंस: $balance';
  }

  @override
  String get accountSaved => 'खाता सेव हो गया।';

  @override
  String get accountDeleted => 'खाता मिटा दिया गया।';

  @override
  String get invalidOpeningBalance =>
      'सही शुरुआती बैलेंस डालें (दशमलव के बाद ज़्यादा से ज़्यादा 2 अंक)।';

  @override
  String get setBudget => 'बजट सेट करें';

  @override
  String budgetForCategory(String category) {
    return 'बजट: $category';
  }

  @override
  String get budgetLimit => 'मासिक सीमा';

  @override
  String get budgetWarningPercent => 'चेतावनी सीमा (कुल सीमा का प्रतिशत)';

  @override
  String budgetSummary(String limit, int percent) {
    return 'सीमा $limit · $percent% पर चेतावनी';
  }

  @override
  String get noBudget => 'कोई बजट सेट नहीं है';

  @override
  String get remove => 'हटाएं';

  @override
  String get budgetSaved => 'बजट सेव हो गया।';

  @override
  String get budgetDeleted => 'बजट हटा दिया गया।';

  @override
  String get invalidBudgetInput =>
      'दशमलव के बाद ज़्यादा से ज़्यादा 2 अंकों वाली सीमा और 1 से 100 के बीच चेतावनी प्रतिशत डालें।';

  @override
  String invalidPinAttemptsLeft(int attempts) {
    return 'गलत PIN। कुछ समय के लिए लॉक होने से पहले बची कोशिशें: $attempts';
  }

  @override
  String pinLockedOut(int seconds) {
    return 'बहुत ज़्यादा बार गलत कोशिश की गई। $seconds सेकंड बाद फिर से कोशिश करें।';
  }

  @override
  String get searchAllCategories => 'सभी कैटगरी';

  @override
  String get searchClear => 'साफ़ करें';

  @override
  String searchResultCount(int count) {
    return '$count परिणाम';
  }

  @override
  String get searchNoResults => 'कोई मिलता-जुलता लेन-देन नहीं मिला।';

  @override
  String get previousMonth => '‹ पिछला';

  @override
  String get nextMonth => 'अगला ›';

  @override
  String get reportTitle => 'मासिक खर्च रिपोर्ट';

  @override
  String reportGenerated(String generated) {
    return 'बनाई गई: $generated';
  }

  @override
  String reportPage(int page) {
    return 'पेज $page';
  }

  @override
  String reportTotals(String income, String expense, String net) {
    return 'आय $income · खर्च $expense · नेट $net';
  }

  @override
  String get reportColDate => 'तारीख';

  @override
  String get reportColType => 'प्रकार';

  @override
  String get reportColCategory => 'कैटगरी';

  @override
  String get reportColDescription => 'विवरण';

  @override
  String get reportColAmount => 'राशि';

  @override
  String get reportColPayment => 'पेमेंट का तरीका';

  @override
  String get reportEmpty => 'इस महीने कोई लेन-देन नहीं है।';

  @override
  String get reportLoadFailed => 'रिपोर्ट नहीं बन सकी। फिर से कोशिश करें।';

  @override
  String get saveCsv => 'CSV सेव करें';

  @override
  String get savePdf => 'PDF सेव करें';

  @override
  String get shareCsv => 'CSV शेयर करें';

  @override
  String get sharePdf => 'PDF शेयर करें';

  @override
  String get shareReport => 'रिपोर्ट शेयर करें';

  @override
  String get reportSaved => 'रिपोर्ट सेव हो गई।';

  @override
  String get reportExportFailed => 'रिपोर्ट एक्सपोर्ट नहीं हो सकी।';

  @override
  String get netThisMonth => 'इस महीने का नेट';

  @override
  String get incomeThisMonth => 'आय';

  @override
  String get expenseThisMonth => 'खर्च';

  @override
  String get spendingByCategory => 'कैटगरी के हिसाब से खर्च';

  @override
  String get spendingNone => 'इस महीने कोई खर्च नहीं हुआ।';

  @override
  String get spendingOther => 'अन्य';

  @override
  String get budgetsThisMonth => 'इस महीने के बजट';

  @override
  String budgetSpentOfLimit(String spent, String limit) {
    return '$limit में से $spent';
  }

  @override
  String budgetStatusLine(String status, int percent) {
    return '$status · $percent% इस्तेमाल हुआ';
  }

  @override
  String get budgetStatusOk => 'बजट के अंदर';

  @override
  String get budgetStatusWarning => 'सीमा के करीब';

  @override
  String get budgetStatusOver => 'बजट से ज़्यादा';

  @override
  String budgetAlerts(int count) {
    String _temp0 = intl.Intl.pluralLogic(
      count,
      locale: localeName,
      other: '$count बजट अपनी सीमा के करीब हैं या उससे ज़्यादा हो गए हैं।',
      one: '$count बजट अपनी सीमा के करीब है या उससे ज़्यादा हो गया है।',
    );
    return '$_temp0';
  }

  @override
  String get editCategory => 'कैटगरी में बदलाव करें';

  @override
  String categoryIconLabel(String icon) {
    return 'आइकॉन: $icon';
  }

  @override
  String get categoryIconGeneral => 'सामान्य';

  @override
  String get categoryIconIncome => 'आय';

  @override
  String get categoryIconTransfer => 'ट्रांसफ़र';

  @override
  String get categoryIconGroceries => 'किराना';

  @override
  String get categoryIconDining => 'खाना-पीना';

  @override
  String get categoryIconCoffee => 'कॉफ़ी';

  @override
  String get categoryIconHousing => 'घर';

  @override
  String get categoryIconUtilities => 'बिजली-पानी';

  @override
  String get categoryIconInternet => 'इंटरनेट';

  @override
  String get categoryIconPhone => 'फ़ोन';

  @override
  String get categoryIconCar => 'गाड़ी';

  @override
  String get categoryIconFuel => 'ईंधन';

  @override
  String get categoryIconTransit => 'लोकल ट्रांसपोर्ट';

  @override
  String get categoryIconTravel => 'यात्रा';

  @override
  String get categoryIconHealth => 'सेहत';

  @override
  String get categoryIconFitness => 'फ़िटनेस';

  @override
  String get categoryIconShopping => 'शॉपिंग';

  @override
  String get categoryIconClothing => 'कपड़े';

  @override
  String get categoryIconEntertainment => 'मनोरंजन';

  @override
  String get categoryIconEducation => 'शिक्षा';

  @override
  String get categoryIconPets => 'पालतू जानवर';

  @override
  String get categoryIconGifts => 'तोहफ़े';

  @override
  String get categoryIconSavings => 'बचत';

  @override
  String get categoryIconBank => 'बैंक और शुल्क';

  @override
  String get invalidAmountMessage =>
      '0 से ज़्यादा राशि डालें, जिसमें दशमलव के बाद ज़्यादा से ज़्यादा 2 अंक हों।';

  @override
  String get errorIdRequired =>
      'यह आइटम नहीं मिला। वापस जाकर फिर से कोशिश करें।';

  @override
  String get errorItemNotFound => 'यह आइटम अब मौजूद नहीं है।';

  @override
  String get errorBlankDescription => 'विवरण डालें।';

  @override
  String errorDescriptionTooLong(int maxLength) {
    return 'विवरण में ज़्यादा से ज़्यादा $maxLength वर्ण हो सकते हैं।';
  }

  @override
  String get errorInvalidDate => 'सही तारीख चुनें।';

  @override
  String get errorTransferDestinationRequired =>
      'वह खाता चुनें जिसमें ट्रांसफ़र करना है।';

  @override
  String get errorTransferDestinationSame =>
      'ट्रांसफ़र के लिए कोई दूसरा खाता चुनें।';

  @override
  String get errorCategoryNotFound =>
      'वह कैटगरी अब मौजूद नहीं है। कोई दूसरी कैटगरी चुनें।';

  @override
  String get errorAccountNotFound =>
      'वह खाता अब मौजूद नहीं है। कोई दूसरा खाता चुनें।';

  @override
  String get errorBudgetExceeded =>
      'इस खर्च से यह कैटगरी अपने मासिक बजट से ज़्यादा हो जाएगी।';

  @override
  String get overBudgetTitle => 'बजट से ज़्यादा';

  @override
  String overBudgetMessage(String category, String overBy, String limit) {
    return 'इस खर्च के बाद $category का खर्च, $limit के मासिक बजट से $overBy ज़्यादा हो जाएगा।';
  }

  @override
  String get overBudgetConfirm => 'फिर भी सेव करें';

  @override
  String errorInvalidName(int maxLength) {
    return 'नाम में 1 से $maxLength वर्ण होने चाहिए।';
  }

  @override
  String errorDuplicateCategoryName(String name) {
    return '\"$name\" नाम की कैटगरी पहले से मौजूद है।';
  }

  @override
  String errorDuplicateAccountName(String name) {
    return '\"$name\" नाम का खाता पहले से मौजूद है।';
  }

  @override
  String get errorDefaultCategory => 'डिफ़ॉल्ट कैटगरी मिटाई नहीं जा सकतीं।';

  @override
  String get errorDefaultAccount => 'डिफ़ॉल्ट खाते मिटाए नहीं जा सकते।';

  @override
  String errorCategoryInUse(int count) {
    String _temp0 = intl.Intl.pluralLogic(
      count,
      locale: localeName,
      other: 'इस कैटगरी का इस्तेमाल $count लेन-देन में हुआ है।',
      one: 'इस कैटगरी का इस्तेमाल $count लेन-देन में हुआ है।',
    );
    return '$_temp0';
  }

  @override
  String errorAccountInUse(int count) {
    String _temp0 = intl.Intl.pluralLogic(
      count,
      locale: localeName,
      other: 'इस खाते का इस्तेमाल $count लेन-देन में हुआ है।',
      one: 'इस खाते का इस्तेमाल $count लेन-देन में हुआ है।',
    );
    return '$_temp0';
  }

  @override
  String get errorInvalidWarningPercent =>
      'चेतावनी सीमा 1 से 100 प्रतिशत के बीच होनी चाहिए।';

  @override
  String get defaultAccountCash => 'नकद';

  @override
  String get defaultAccountSavings => 'बचत';

  @override
  String get defaultCategoryExpense => 'सामान्य खर्च';

  @override
  String get defaultCategoryIncome => 'सामान्य आय';

  @override
  String get defaultCategoryTransfer => 'ट्रांसफ़र';

  @override
  String get dashboardLoadFailed =>
      'आपके लेन-देन लोड नहीं हो सके। फिर से कोशिश करने के लिए ऐप से बाहर निकलकर वापस आएं।';

  @override
  String get tourPrivateTitle => 'आपका पैसा, पूरी तरह निजी';

  @override
  String get tourPrivateBody =>
      'अपने खर्च, आय और खातों के बीच ट्रांसफ़र का हिसाब रखें। सब कुछ इसी फ़ोन पर रहता है, एन्क्रिप्ट किया हुआ और आपके PIN से लॉक। न साइन-अप, न विज्ञापन, न इंटरनेट।';

  @override
  String get tourAddTitle => 'कुछ ही सेकंड में जोड़ें';

  @override
  String get tourAddBody =>
      'खर्च, आय या ट्रांसफ़र दर्ज करने के लिए + पर टैप करें। खाता और कैटगरी चुनें, आपका बैलेंस अपने-आप अपडेट हो जाएगा।';

  @override
  String get tourBudgetTitle => 'बजट में रहें';

  @override
  String get tourBudgetBody =>
      'किसी कैटगरी के लिए मासिक सीमा तय करें। डैशबोर्ड दिखाता है कि कितना बचा है, और सीमा पार होने से पहले आपको चेतावनी देता है।';

  @override
  String get tourReportsTitle => 'रिपोर्ट और बैकअप';

  @override
  String get tourReportsBody =>
      'मासिक रिपोर्ट को PDF या CSV फ़ाइल के रूप में सेव या शेयर करें, और एन्क्रिप्ट किया हुआ बैकअप रखें ताकि आपके रिकॉर्ड कभी न खोएं।';

  @override
  String get tourSkip => 'छोड़ें';

  @override
  String get tourNext => 'आगे';

  @override
  String get tourGetStarted => 'शुरू करें';

  @override
  String tourPageIndicator(int page, int pageCount) {
    return '$pageCount में से पेज $page';
  }

  @override
  String get gettingStartedTitle => 'शुरुआत करें';

  @override
  String gettingStartedProgress(int done, int total) {
    return '$total में से $done पूरे हुए';
  }

  @override
  String get gettingStartedAddAccount => 'अपना बैंक या कार्ड खाता जोड़ें';

  @override
  String get gettingStartedAddTransaction => 'अपना पहला लेन-देन दर्ज करें';

  @override
  String get gettingStartedSetBudget => 'मासिक बजट सेट करें';

  @override
  String get gettingStartedStepDone => 'पूरा हुआ';

  @override
  String get gettingStartedStepTodo => 'अभी बाकी है';

  @override
  String get gettingStartedHide => 'शुरुआती चेकलिस्ट छिपाएं';

  @override
  String get settingsHelpSection => 'मदद और सहायता';

  @override
  String get settingsHelpFaq => 'मदद और FAQ';

  @override
  String get settingsShowTour => 'वेलकम टूर दिखाएं';

  @override
  String get settingsSendFeedback => 'फ़ीडबैक भेजें';

  @override
  String get settingsRateApp => 'इस ऐप को रेटिंग दें';

  @override
  String get settingsShareApp => 'इस ऐप को शेयर करें';

  @override
  String get helpTitle => 'मदद और FAQ';

  @override
  String get helpExpanded => 'खुला है';

  @override
  String get helpCollapsed => 'बंद है';

  @override
  String get helpContactTitle => 'अब भी मदद चाहिए?';

  @override
  String get helpContactBody =>
      'कोई सवाल, समस्या या सुझाव भेजें। कृपया अपना PIN, बैकअप का पासफ़्रेज़ या वित्तीय रिकॉर्ड शामिल न करें।';

  @override
  String get faqDataQ => 'मेरा डेटा कहां सेव होता है?';

  @override
  String get faqDataA =>
      'सिर्फ़ इसी फ़ोन पर। Denarii Dolor में न कोई अकाउंट बनाना पड़ता है, न कोई सर्वर है, और यह इंटरनेट से कनेक्ट नहीं हो सकता। आपके रिकॉर्ड एक एन्क्रिप्ट किए गए डेटाबेस में रखे जाते हैं, जिसे सिर्फ़ आपका PIN, आपका सुरक्षा उत्तर या, अगर आपने चालू किया है तो, आपका फ़िंगरप्रिंट या चेहरा ही अनलॉक कर सकता है। इस ऐप के लिए Android का क्लाउड बैकअप बंद है।';

  @override
  String get faqForgotPinQ => 'अपना PIN भूल गए हैं? अब क्या करें?';

  @override
  String faqForgotPinA(String forgotPin, String wipeAllData) {
    return 'साइन इन स्क्रीन पर “$forgotPin” पर टैप करें और नया PIN चुनने के लिए अपने सुरक्षा प्रश्न का उत्तर दें। अगर आप उसका उत्तर भी नहीं दे सकते, तो आपका डेटा अनलॉक नहीं हो सकता, क्योंकि इसकी चाबी किसी और के पास नहीं है। ऐसे में आगे बढ़ने का एक ही तरीका है: साइन इन स्क्रीन पर “$wipeAllData”, जिससे सब कुछ मिट जाता है ताकि आप नए सिरे से शुरू कर सकें।';
  }

  @override
  String get faqSignedOutQ => 'मुझे साइन आउट क्यों कर दिया गया?';

  @override
  String faqSignedOutA(int minutes, String biometricSetting) {
    return 'आपके डेटा की सुरक्षा के लिए, $minutes मिनट तक इस्तेमाल न होने पर ऐप आपको साइन आउट कर देता है। अपने PIN से फिर से साइन इन करें, या अपने फ़िंगरप्रिंट या चेहरे से साइन इन करने के लिए सेटिंग में “$biometricSetting” चालू करें।';
  }

  @override
  String get faqBackupQ =>
      'अपने डेटा का बैकअप कैसे लें या उसे नए फ़ोन पर कैसे ले जाएं?';

  @override
  String faqBackupA(String backupSetting) {
    return 'सेटिंग में “$backupSetting” खोलें और पासफ़्रेज़ से सुरक्षित बैकअप बनाएं। फ़ाइल को किसी सुरक्षित जगह, जैसे क्लाउड ड्राइव में सेव करें। नए फ़ोन पर Denarii Dolor इंस्टॉल करके सेट अप करें, फिर उसी पासफ़्रेज़ से फ़ाइल रीस्टोर करें। भूला हुआ पासफ़्रेज़ कोई भी वापस नहीं ला सकता।';
  }

  @override
  String get faqBudgetsQ => 'बजट कैसे काम करते हैं?';

  @override
  String faqBudgetsA(String budgetsScreen) {
    return 'किसी कैटगरी के लिए मासिक सीमा और चेतावनी सीमा तय करने के लिए सेटिंग में “$budgetsScreen” खोलें। डैशबोर्ड दिखाता है कि इस महीने आपने हर बजट का कितना हिस्सा इस्तेमाल किया है। अगर कोई खर्च अपने बजट से ज़्यादा हो जाएगा, तो ऐप उसे सेव करने से पहले आपसे पूछता है।';
  }

  @override
  String get faqTransferQ => 'ट्रांसफ़र क्या है?';

  @override
  String get faqTransferA =>
      'ट्रांसफ़र से आपके दो खातों के बीच पैसा जाता है, जैसे बचत से नकद में। इससे दोनों खातों का बैलेंस बदलता है, लेकिन इसे आय या खर्च नहीं गिना जाता।';

  @override
  String get faqDeleteQ => 'कोई कैटगरी या खाता मिटाया क्यों नहीं जा सकता?';

  @override
  String get faqDeleteA =>
      'डिफ़ॉल्ट कैटगरी और खाते मिटाए नहीं जा सकते, और न ही वे जिनका इस्तेमाल अब भी किसी लेन-देन में हो रहा है। पहले उन लेन-देन में बदलाव करें या उन्हें मिटाएं, फिर दोबारा कोशिश करें।';

  @override
  String get faqCurrencyQ => 'मुद्रा कैसे बदलें?';

  @override
  String faqCurrencyA(String currencySetting) {
    return 'सेटिंग में “$currencySetting” पर टैप करें और कोई मुद्रा चुनें। सिर्फ़ मुद्रा का चिह्न बदलता है: आपने जो राशियां पहले से डाली हैं, वे कन्वर्ट नहीं होतीं।';
  }

  @override
  String get faqReportsQ => 'रिपोर्ट एक्सपोर्ट कैसे करें?';

  @override
  String faqReportsA(String reportsScreen) {
    return '“$reportsScreen” खोलें, कोई महीना चुनें, फिर रिपोर्ट को PDF या CSV फ़ाइल के रूप में सेव या शेयर करें। एक्सपोर्ट की गई फ़ाइलें एन्क्रिप्ट नहीं होतीं, इसलिए उन्हें किसी निजी जगह पर रखें।';
  }

  @override
  String get faqBiometricQ =>
      'फ़िंगरप्रिंट या चेहरे से साइन-इन बंद क्यों हो गया?';

  @override
  String get faqBiometricA =>
      'फ़ोन में रजिस्टर किए गए फ़िंगरप्रिंट या चेहरे बदलने पर यह बंद हो जाता है, ताकि नया जोड़ा गया फ़िंगरप्रिंट या चेहरा आपका डेटा न खोल सके। आपका PIN अब भी काम करता है, और आप सेटिंग में बायोमेट्रिक साइन-इन फिर से चालू कर सकते हैं।';

  @override
  String get ratePromptTitle => 'Denarii Dolor पसंद आ रहा है?';

  @override
  String get ratePromptMessage =>
      'Google Play पर एक छोटी-सी रेटिंग से दूसरे लोगों को यह ऐप ढूंढने में मदद मिलती है। इसमें बस एक पल लगता है।';

  @override
  String get ratePromptRate => 'अभी रेटिंग दें';

  @override
  String get ratePromptLater => 'अभी नहीं';

  @override
  String get ratePromptNever => 'फिर से न पूछें';

  @override
  String get rateUnavailable => 'इस डिवाइस पर Google Play नहीं खुल सका।';

  @override
  String get shareAppChooser => 'Denarii Dolor शेयर करें';

  @override
  String shareAppMessage(String storeLink) {
    return 'मेरे खर्च और बजट का हिसाब Denarii Dolor में रहता है। सारा डेटा फ़ोन पर ही निजी रहता है। इसे Google Play से पाएं: $storeLink';
  }

  @override
  String feedbackEmailSubject(String appVersion) {
    return 'Denarii Dolor फ़ीडबैक (वर्शन $appVersion)';
  }

  @override
  String feedbackEmailBody(String appVersion, String androidVersion) {
    return '\n\n———\nऐप वर्शन: $appVersion\nAndroid वर्शन: $androidVersion';
  }

  @override
  String feedbackNoEmailApp(String emailAddress) {
    return 'कोई ईमेल ऐप नहीं मिला। आप $emailAddress पर लिख सकते हैं।';
  }

  @override
  String placeholderAmountInCents(int cents) {
    return '$cents सेंट';
  }

  @override
  String placeholderAmountRejected(String code) {
    return 'अस्वीकृत: $code';
  }
}
