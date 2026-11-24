// ignore: unused_import
import 'package:intl/intl.dart' as intl;

import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for Arabic (`ar`).
class AppLocalizationsAr extends AppLocalizations {
  AppLocalizationsAr([String locale = 'ar']) : super(locale);

  @override
  String get appName => 'Denarii Dolor';

  @override
  String get dashboard => 'لوحة المعلومات';

  @override
  String get signIn => 'تسجيل الدخول';

  @override
  String get signInWithBiometrics => 'تسجيل الدخول بالمقاييس الحيوية';

  @override
  String get biometricSignInAccessibilityLabel =>
      'تسجيل الدخول بالمقاييس الحيوية';

  @override
  String get biometricSignInTitle => 'فتح قفل Denarii Dolor';

  @override
  String get biometricSignInSubtitle =>
      'استخدم المقاييس الحيوية المسجَّلة للمتابعة';

  @override
  String get biometricSignInFailed =>
      'تعذّرت المصادقة بالمقاييس الحيوية. أعد المحاولة أو استخدم رمز PIN.';

  @override
  String get biometricSignInError =>
      'تسجيل الدخول بالمقاييس الحيوية غير متاح حاليًا. استخدم رمز PIN بدلاً من ذلك.';

  @override
  String get usePinInstead => 'استخدام رمز PIN';

  @override
  String get biometricSignInInvalidated =>
      'تم إيقاف تسجيل الدخول بالمقاييس الحيوية لأن بيانات المقاييس الحيوية على هذا الجهاز قد تغيّرت. سجّل الدخول برمز PIN، ثم فعّله مجددًا من الإعدادات.';

  @override
  String get biometricEnrollTitle => 'تفعيل تسجيل الدخول بالمقاييس الحيوية';

  @override
  String get biometricEnrollSubtitle => 'أكِّد باستخدام بصمة إصبعك أو وجهك';

  @override
  String get biometricEnabled => 'تم تفعيل تسجيل الدخول بالمقاييس الحيوية.';

  @override
  String get biometricEnableFailed =>
      'تعذّر تفعيل تسجيل الدخول بالمقاييس الحيوية.';

  @override
  String get pinHint => 'رمز PIN';

  @override
  String get showPin => 'إظهار';

  @override
  String get hidePin => 'إخفاء';

  @override
  String get newPinHint => 'رمز PIN الجديد';

  @override
  String get confirmPinHint => 'تأكيد رمز PIN';

  @override
  String get securityQuestionHint => 'سؤال الأمان';

  @override
  String get securityAnswerHint => 'إجابة سؤال الأمان';

  @override
  String get securityAnswerVerifyHint => 'إجابة سؤال الأمان';

  @override
  String get forgotPin => 'هل نسيت رمز PIN؟';

  @override
  String get backToSignIn => 'العودة إلى تسجيل الدخول';

  @override
  String get createSecurityProfile => 'إنشاء ملف الأمان';

  @override
  String get wipeAllData => 'محو جميع بيانات التطبيق';

  @override
  String get wipeAllDataDestructiveLabel =>
      'إجراء خطير: محو جميع بيانات التطبيق';

  @override
  String get wipeDataTitle => 'هل تريد حذف جميع بيانات التطبيق؟';

  @override
  String get wipeDataWarning =>
      'سيؤدي هذا إلى حذف جميع بيانات التطبيق والسجلات المالية ورمز PIN وإعدادات الاسترداد نهائيًا. لا يمكن التراجع عن هذا الإجراء.';

  @override
  String get wipeDataConfirm => 'حذف كل شيء';

  @override
  String wipeDataConfirmCountdown(int seconds) {
    return 'حذف كل شيء ($seconds)';
  }

  @override
  String get wipeDataConfirmationWord => 'WIPE';

  @override
  String wipeDataTypeToConfirm(String word) {
    return 'اكتب $word للتأكيد';
  }

  @override
  String get wipeDataConfirmDestructiveLabel =>
      'تأكيد إجراء خطير: حذف جميع بيانات التطبيق نهائيًا';

  @override
  String get wipeDataCancel => 'إلغاء';

  @override
  String get pinFormatError => 'يجب أن يتكوّن رمز PIN من 6 إلى 12 رقمًا.';

  @override
  String get pinConfirmationMismatch => 'يجب أن يتطابق رمز PIN مع تأكيده.';

  @override
  String get securityQuestionRequired => 'سؤال الأمان مطلوب.';

  @override
  String get securityAnswerRequired => 'إجابة سؤال الأمان مطلوبة.';

  @override
  String securityQuestionTooLong(int maxLength) {
    return 'يجب ألا يزيد عدد أحرف سؤال الأمان على $maxLength.';
  }

  @override
  String securityAnswerTooShort(int minLength) {
    return 'يجب ألا يقل عدد أحرف إجابة سؤال الأمان عن $minLength. لا يُفرَّق بين الأحرف الكبيرة والصغيرة، ولا تُحتسب المسافات الزائدة.';
  }

  @override
  String securityAnswerTooLong(int maxLength) {
    return 'يجب ألا يزيد عدد أحرف إجابة سؤال الأمان على $maxLength.';
  }

  @override
  String get securityAnswerInQuestion =>
      'لا يمكن أن تكون الإجابة جزءًا من السؤال.';

  @override
  String get securityAnswerMatchesPin =>
      'لا يمكن أن تكون الإجابة مطابقة لرمز PIN الخاص بك.';

  @override
  String securityAnswerRules(int minLength) {
    return 'الحد الأدنى لعدد الأحرف: $minLength. لا يُفرَّق بين الأحرف الكبيرة والصغيرة، وتُتجاهَل المسافات الزائدة. اختر إجابة لا يستطيع الآخرون تخمينها أو العثور عليها بالبحث.';
  }

  @override
  String get upgradeRecoveryIntro =>
      'أصبح Denarii Dolor يحمي بياناتك برمز PIN وإجابة استرداد أقوى. عيِّن سؤال أمان وإجابته لإكمال تسجيل الدخول.';

  @override
  String get saveAndContinue => 'حفظ ومتابعة';

  @override
  String get storageErrorMessage =>
      'تتعذّر قراءة وحدة التخزين الآمنة على هذا الجهاز، لذا لا يمكن فتح قفل بياناتك. أعد المحاولة، أو احذف جميع بيانات التطبيق للبدء من جديد.';

  @override
  String get tryAgain => 'إعادة المحاولة';

  @override
  String get profileSetupSuccess =>
      'تم إنشاء ملف الأمان. سجّل الدخول باستخدام رمز PIN الخاص بك.';

  @override
  String get profileAlreadyConfigured => 'تم إعداد ملف الأمان مسبقًا.';

  @override
  String get profileNotConfigured => 'أنشئ ملف الأمان قبل استرداد رمز PIN.';

  @override
  String get invalidSecurityAnswer => 'إجابة سؤال الأمان غير صحيحة.';

  @override
  String get pinRecoverySuccess =>
      'تمت إعادة تعيين رمز PIN بنجاح. سجّل الدخول باستخدام رمز PIN الجديد.';

  @override
  String get wipeDataSuccess =>
      'تم حذف جميع بيانات التطبيق. أنشئ ملف أمان جديدًا.';

  @override
  String get wipeDataError => 'تعذّر محو بيانات التطبيق. يُرجى إعادة المحاولة.';

  @override
  String get securityQuestionUnavailable => 'سؤال الأمان غير متاح.';

  @override
  String get addTransaction => 'إضافة معاملة';

  @override
  String get transactionDescriptionHint => 'الوصف';

  @override
  String get transactionTypeLabel => 'نوع المعاملة';

  @override
  String get transactionAmountHint => 'المبلغ';

  @override
  String get transactionReferenceRequiredMessage =>
      'راجِع معرِّفات الحساب والفئة قبل حفظ هذه المعاملة.';

  @override
  String get dateHint => 'التاريخ (YYYY-MM-DD)';

  @override
  String get saveTransaction => 'حفظ المعاملة';

  @override
  String get transactionTypeExpense => 'مصروف';

  @override
  String get transactionTypeIncome => 'دخل';

  @override
  String get transactionTypeTransfer => 'تحويل';

  @override
  String get search => 'بحث';

  @override
  String get searchDescriptionHint => 'الوصف';

  @override
  String get searchMinAmountHint => 'الحد الأدنى للمبلغ';

  @override
  String get searchMaxAmountHint => 'الحد الأقصى للمبلغ';

  @override
  String get searchStartDateHint => 'تاريخ البدء (YYYY-MM-DD)';

  @override
  String get searchEndDateHint => 'تاريخ الانتهاء (YYYY-MM-DD)';

  @override
  String get invalidDateMessage => 'استخدم تنسيق التاريخ YYYY-MM-DD.';

  @override
  String get invalidSearchFiltersMessage =>
      'تحقَّق من تواريخ البحث وعوامل التصفية الرقمية، ثم أعد المحاولة.';

  @override
  String get invalidSearchRangeMessage => 'أدخل نطاقًا صالحًا للمبلغ والتاريخ.';

  @override
  String get reports => 'التقارير';

  @override
  String get settings => 'الإعدادات';

  @override
  String settingsSessionTimeout(int minutes) {
    return 'مهلة الجلسة عند عدم النشاط (بالدقائق): $minutes';
  }

  @override
  String get settingsPinConfigured => 'تم إعداد رمز PIN لتسجيل الدخول السريع.';

  @override
  String get settingsPinNotConfigured => 'لم يتم إعداد رمز PIN بعد.';

  @override
  String get signOut => 'تسجيل الخروج';

  @override
  String get resetPin => 'إعادة تعيين رمز PIN';

  @override
  String get settingsDarkMode => 'الوضع الداكن';

  @override
  String get settingsBiometricSignIn => 'تسجيل الدخول بالمقاييس الحيوية';

  @override
  String get settingsCurrency => 'العملة';

  @override
  String settingsCurrencyValue(String currencyName, String symbol) {
    return '$currencyName ($symbol)';
  }

  @override
  String get settingsCurrencyTitle => 'اختيار العملة';

  @override
  String get settingsCurrencyNote =>
      'يتغيّر رمز العملة فقط، ولا تُحوَّل المبالغ التي أدخلتها سابقًا.';

  @override
  String get settingsBackupRestore => 'النسخ الاحتياطي والاستعادة';

  @override
  String get settingsPrivacyPolicy => 'سياسة الخصوصية';

  @override
  String get settingsLicenses => 'تراخيص البرامج مفتوحة المصدر';

  @override
  String get backupExplanation =>
      'يحتوي ملف النسخة الاحتياطية على حساباتك وفئاتك وميزانياتك ومعاملاتك وإعداد العملة، مشفّرةً بعبارة مرور تختارها. ولا يحتوي على رمز PIN أو سؤال الأمان أو تسجيل الدخول بالمقاييس الحيوية: على الهاتف الجديد، اضبط التطبيق أولًا، ثم استعِد النسخة الاحتياطية من هنا.';

  @override
  String get backupPassphraseWarning =>
      'احتفظ بعبارة المرور في مكان آمن. فبدونها لا يمكن فتح النسخة الاحتياطية، ولا يستطيع أحد استعادتها.';

  @override
  String get backupCreate => 'إنشاء نسخة احتياطية';

  @override
  String get backupRestore => 'الاستعادة من نسخة احتياطية';

  @override
  String get backupCreateTitle => 'اختر عبارة مرور';

  @override
  String get backupRestoreTitle => 'عبارة مرور النسخة الاحتياطية';

  @override
  String get backupPassphrase => 'عبارة المرور';

  @override
  String get backupPassphraseConfirm => 'تأكيد عبارة المرور';

  @override
  String backupPassphraseRules(int minLength) {
    return '$minLength حرفًا على الأقل. تصلح بضع كلمات لا علاقة بينها.';
  }

  @override
  String get backupContinue => 'متابعة';

  @override
  String get backupWorking => 'جارٍ التنفيذ…';

  @override
  String get backupSaved => 'تم حفظ النسخة الاحتياطية.';

  @override
  String get backupSaveFailed => 'تعذّر حفظ النسخة الاحتياطية.';

  @override
  String get backupRestoreConfirmTitle => 'هل تريد استبدال جميع البيانات؟';

  @override
  String backupRestoreContents(
    String created,
    int accounts,
    int categories,
    int budgets,
    int transactions,
  ) {
    return 'تاريخ الإنشاء: $created\nالحسابات: $accounts\nالفئات: $categories\nالميزانيات: $budgets\nالمعاملات: $transactions';
  }

  @override
  String get backupRestoreWarning =>
      'تمسح الاستعادة كل ما على هذا الهاتف الآن وتستبدله بالنسخة الاحتياطية. ولا يمكن التراجع عن ذلك.';

  @override
  String get backupRestoreConfirmationWord => 'RESTORE';

  @override
  String get backupRestoreAction => 'استبدال البيانات';

  @override
  String get backupRestored => 'تمت استعادة النسخة الاحتياطية.';

  @override
  String get backupRestoreFailed =>
      'تعذّرت استعادة النسخة الاحتياطية. لم تتغير بياناتك.';

  @override
  String backupErrorPassphraseLength(int minLength, int maxLength) {
    return 'يجب أن تتكوّن عبارة المرور من $minLength إلى $maxLength حرفًا.';
  }

  @override
  String get backupErrorPassphraseMismatch => 'عبارتا المرور غير متطابقتين.';

  @override
  String get backupErrorNotBackup =>
      'هذا الملف ليس نسخة احتياطية من Denarii Dolor.';

  @override
  String get backupErrorNewerVersion =>
      'أُنشئت هذه النسخة الاحتياطية بإصدار أحدث من Denarii Dolor. حدّث التطبيق، ثم أعد المحاولة.';

  @override
  String get backupErrorWrongPassphrase =>
      'عبارة المرور غير صحيحة، أو الملف تالف.';

  @override
  String get backupErrorDamaged =>
      'النسخة الاحتياطية تالفة ولا يمكن استعادتها.';

  @override
  String get backupErrorRead => 'تعذّرت قراءة الملف.';

  @override
  String get legalLoadFailed => 'تعذّر فتح هذا المستند.';

  @override
  String get legalEnglishOnly => 'هذا المستند متاح باللغة الإنجليزية فقط.';

  @override
  String get transactionCategoryLabel => 'الفئة';

  @override
  String get transactionAccountLabel => 'الحساب';

  @override
  String get transactionTransferAccountLabel => 'التحويل إلى حساب';

  @override
  String get editTransaction => 'تعديل المعاملة';

  @override
  String get updateTransaction => 'تحديث المعاملة';

  @override
  String get transactionSaved => 'تم حفظ المعاملة.';

  @override
  String get transactionDeleted => 'تم حذف المعاملة.';

  @override
  String get transactionNotFound => 'لم يتم العثور على المعاملة.';

  @override
  String get recentTransactions => 'المعاملات الأخيرة';

  @override
  String get noTransactions => 'لا توجد معاملات بعد. انقر على + لإضافة معاملة.';

  @override
  String get accountBalances => 'أرصدة الحسابات';

  @override
  String get deleteTransactionTitle => 'هل تريد حذف المعاملة؟';

  @override
  String deleteTransactionMessage(String description) {
    return 'هل تريد حذف «$description»؟ سيتم تعديل أرصدة الحسابات.';
  }

  @override
  String deleteItemTitle(String name) {
    return 'هل تريد حذف «$name»؟';
  }

  @override
  String get deleteItemMessage => 'لا يمكن التراجع عن هذا الإجراء.';

  @override
  String get pickerPlaceholder => 'اختر…';

  @override
  String get edit => 'تعديل';

  @override
  String get delete => 'حذف';

  @override
  String get cancel => 'إلغاء';

  @override
  String get save => 'حفظ';

  @override
  String get back => 'رجوع';

  @override
  String get genericError => 'حدث خطأ ما. يُرجى إعادة المحاولة.';

  @override
  String get manageCategories => 'إدارة الفئات';

  @override
  String get manageAccounts => 'إدارة الحسابات';

  @override
  String get manageBudgets => 'إدارة الميزانيات';

  @override
  String get addCategory => 'إضافة فئة';

  @override
  String get categoryName => 'اسم الفئة';

  @override
  String get categorySaved => 'تم حفظ الفئة.';

  @override
  String get categoryDeleted => 'تم حذف الفئة.';

  @override
  String get addAccount => 'إضافة حساب';

  @override
  String get renameAccount => 'إعادة تسمية الحساب';

  @override
  String get accountName => 'اسم الحساب';

  @override
  String get openingBalance => 'الرصيد الافتتاحي';

  @override
  String accountBalance(String balance) {
    return 'الرصيد: $balance';
  }

  @override
  String get accountSaved => 'تم حفظ الحساب.';

  @override
  String get accountDeleted => 'تم حذف الحساب.';

  @override
  String get invalidOpeningBalance =>
      'أدخل رصيدًا افتتاحيًا صالحًا (بمنزلتين عشريتين كحد أقصى).';

  @override
  String get setBudget => 'تعيين الميزانية';

  @override
  String budgetForCategory(String category) {
    return 'الميزانية: $category';
  }

  @override
  String get budgetLimit => 'الحد الشهري';

  @override
  String get budgetWarningPercent => 'عتبة التحذير (نسبة مئوية من الحد)';

  @override
  String budgetSummary(String limit, int percent) {
    return 'الحد $limit · التحذير عند $percent%';
  }

  @override
  String get noBudget => 'لم يتم تعيين ميزانية';

  @override
  String get remove => 'إزالة';

  @override
  String get budgetSaved => 'تم حفظ الميزانية.';

  @override
  String get budgetDeleted => 'تمت إزالة الميزانية.';

  @override
  String get invalidBudgetInput =>
      'أدخل حدًّا بمنزلتين عشريتين كحد أقصى، ونسبة تحذير مئوية من 1 إلى 100.';

  @override
  String invalidPinAttemptsLeft(int attempts) {
    return 'رمز PIN غير صحيح. عدد المحاولات المتبقية قبل القفل المؤقت: $attempts.';
  }

  @override
  String pinLockedOut(int seconds) {
    return 'محاولات فاشلة كثيرة جدًا. الوقت المتبقي قبل إعادة المحاولة (بالثواني): $seconds.';
  }

  @override
  String get searchAllCategories => 'جميع الفئات';

  @override
  String get searchClear => 'مسح';

  @override
  String searchResultCount(int count) {
    return 'عدد النتائج: $count';
  }

  @override
  String get searchNoResults => 'لا توجد معاملات مطابقة.';

  @override
  String get previousMonth => '‹ السابق';

  @override
  String get nextMonth => 'التالي ›';

  @override
  String get reportTitle => 'تقرير الإنفاق الشهري';

  @override
  String reportGenerated(String generated) {
    return 'تاريخ الإنشاء: $generated';
  }

  @override
  String reportPage(int page) {
    return 'الصفحة $page';
  }

  @override
  String reportTotals(String income, String expense, String net) {
    return 'الدخل $income · المصروفات $expense · الصافي $net';
  }

  @override
  String get reportColDate => 'التاريخ';

  @override
  String get reportColType => 'النوع';

  @override
  String get reportColCategory => 'الفئة';

  @override
  String get reportColDescription => 'الوصف';

  @override
  String get reportColAmount => 'المبلغ';

  @override
  String get reportColPayment => 'طريقة الدفع';

  @override
  String get reportEmpty => 'لا توجد معاملات في هذا الشهر.';

  @override
  String get reportLoadFailed => 'تعذّر إنشاء التقرير. أعد المحاولة.';

  @override
  String get saveCsv => 'حفظ بتنسيق CSV';

  @override
  String get savePdf => 'حفظ بتنسيق PDF';

  @override
  String get shareCsv => 'مشاركة بتنسيق CSV';

  @override
  String get sharePdf => 'مشاركة بتنسيق PDF';

  @override
  String get shareReport => 'مشاركة التقرير';

  @override
  String get reportSaved => 'تم حفظ التقرير.';

  @override
  String get reportExportFailed => 'تعذّر تصدير التقرير.';

  @override
  String get netThisMonth => 'الصافي هذا الشهر';

  @override
  String get incomeThisMonth => 'الدخل';

  @override
  String get expenseThisMonth => 'المصروفات';

  @override
  String get spendingByCategory => 'الإنفاق حسب الفئة';

  @override
  String get spendingNone => 'لا توجد مصروفات هذا الشهر.';

  @override
  String get spendingOther => 'أخرى';

  @override
  String get budgetsThisMonth => 'ميزانيات هذا الشهر';

  @override
  String budgetSpentOfLimit(String spent, String limit) {
    return '$spent من $limit';
  }

  @override
  String budgetStatusLine(String status, int percent) {
    return '$status · نسبة الاستخدام $percent%';
  }

  @override
  String get budgetStatusOk => 'ضمن الميزانية';

  @override
  String get budgetStatusWarning => 'قرب الحد';

  @override
  String get budgetStatusOver => 'تجاوز الميزانية';

  @override
  String budgetAlerts(int count) {
    String _temp0 = intl.Intl.pluralLogic(
      count,
      locale: localeName,
      other: '$count ميزانية قريبة من حدودها أو تجاوزتها.',
      many: '$count ميزانية قريبة من حدودها أو تجاوزتها.',
      few: '$count ميزانيات قريبة من حدودها أو تجاوزتها.',
      two: '$count ميزانيتان قريبتان من حدَّيهما أو تجاوزتاهما.',
      one: '$count ميزانية قريبة من حدها أو تجاوزته.',
      zero: '$count ميزانيات قريبة من حدودها أو تجاوزتها.',
    );
    return '$_temp0';
  }

  @override
  String get editCategory => 'تعديل الفئة';

  @override
  String categoryIconLabel(String icon) {
    return 'الأيقونة: $icon';
  }

  @override
  String get categoryIconGeneral => 'عام';

  @override
  String get categoryIconIncome => 'الدخل';

  @override
  String get categoryIconTransfer => 'التحويل';

  @override
  String get categoryIconGroceries => 'البقالة';

  @override
  String get categoryIconDining => 'المطاعم';

  @override
  String get categoryIconCoffee => 'القهوة';

  @override
  String get categoryIconHousing => 'السكن';

  @override
  String get categoryIconUtilities => 'المرافق';

  @override
  String get categoryIconInternet => 'الإنترنت';

  @override
  String get categoryIconPhone => 'الهاتف';

  @override
  String get categoryIconCar => 'السيارة';

  @override
  String get categoryIconFuel => 'الوقود';

  @override
  String get categoryIconTransit => 'النقل العام';

  @override
  String get categoryIconTravel => 'السفر';

  @override
  String get categoryIconHealth => 'الصحة';

  @override
  String get categoryIconFitness => 'اللياقة البدنية';

  @override
  String get categoryIconShopping => 'التسوّق';

  @override
  String get categoryIconClothing => 'الملابس';

  @override
  String get categoryIconEntertainment => 'الترفيه';

  @override
  String get categoryIconEducation => 'التعليم';

  @override
  String get categoryIconPets => 'الحيوانات الأليفة';

  @override
  String get categoryIconGifts => 'الهدايا';

  @override
  String get categoryIconSavings => 'المدخرات';

  @override
  String get categoryIconBank => 'البنوك والرسوم';

  @override
  String get invalidAmountMessage =>
      'أدخل مبلغًا أكبر من 0 بمنزلتين عشريتين كحد أقصى.';

  @override
  String get errorIdRequired =>
      'تعذّر العثور على هذا العنصر. ارجع وأعد المحاولة.';

  @override
  String get errorItemNotFound => 'هذا العنصر لم يعد موجودًا.';

  @override
  String get errorBlankDescription => 'أدخل وصفًا.';

  @override
  String errorDescriptionTooLong(int maxLength) {
    return 'يجب ألا يزيد عدد أحرف الوصف على $maxLength.';
  }

  @override
  String get errorInvalidDate => 'اختر تاريخًا صالحًا.';

  @override
  String get errorTransferDestinationRequired =>
      'اختر الحساب الذي تريد التحويل إليه.';

  @override
  String get errorTransferDestinationSame =>
      'اختر حسابًا مختلفًا للتحويل إليه.';

  @override
  String get errorCategoryNotFound => 'هذه الفئة لم تعد موجودة. اختر فئة أخرى.';

  @override
  String get errorAccountNotFound =>
      'هذا الحساب لم يعد موجودًا. اختر حسابًا آخر.';

  @override
  String get errorBudgetExceeded =>
      'سيؤدي هذا المصروف إلى تجاوز الفئة ميزانيتها الشهرية.';

  @override
  String get overBudgetTitle => 'تجاوز الميزانية';

  @override
  String overBudgetMessage(String category, String overBy, String limit) {
    return 'سيؤدي هذا المصروف إلى تجاوز الفئة «$category» ميزانيتها الشهرية البالغة $limit بمقدار $overBy.';
  }

  @override
  String get overBudgetConfirm => 'حفظ على أي حال';

  @override
  String errorInvalidName(int maxLength) {
    return 'يجب أن يتكوّن الاسم من حرف واحد على الأقل، وألا يزيد عدد أحرفه على $maxLength.';
  }

  @override
  String errorDuplicateCategoryName(String name) {
    return 'توجد فئة باسم «$name» بالفعل.';
  }

  @override
  String errorDuplicateAccountName(String name) {
    return 'يوجد حساب باسم «$name» بالفعل.';
  }

  @override
  String get errorDefaultCategory => 'لا يمكن حذف الفئات الافتراضية.';

  @override
  String get errorDefaultAccount => 'لا يمكن حذف الحسابات الافتراضية.';

  @override
  String errorCategoryInUse(int count) {
    String _temp0 = intl.Intl.pluralLogic(
      count,
      locale: localeName,
      other: 'هذه الفئة مستخدمة في $count معاملة.',
      many: 'هذه الفئة مستخدمة في $count معاملة.',
      few: 'هذه الفئة مستخدمة في $count معاملات.',
      two: 'هذه الفئة مستخدمة في $count معاملتين.',
      one: 'هذه الفئة مستخدمة في $count معاملة.',
      zero: 'هذه الفئة مستخدمة في $count معاملات.',
    );
    return '$_temp0';
  }

  @override
  String errorAccountInUse(int count) {
    String _temp0 = intl.Intl.pluralLogic(
      count,
      locale: localeName,
      other: 'هذا الحساب مستخدم في $count معاملة.',
      many: 'هذا الحساب مستخدم في $count معاملة.',
      few: 'هذا الحساب مستخدم في $count معاملات.',
      two: 'هذا الحساب مستخدم في $count معاملتين.',
      one: 'هذا الحساب مستخدم في $count معاملة.',
      zero: 'هذا الحساب مستخدم في $count معاملات.',
    );
    return '$_temp0';
  }

  @override
  String get errorInvalidWarningPercent =>
      'يجب أن تتراوح عتبة التحذير بين 1 و100 بالمئة.';

  @override
  String get defaultAccountCash => 'النقد';

  @override
  String get defaultAccountSavings => 'المدخرات';

  @override
  String get defaultCategoryExpense => 'مصروفات عامة';

  @override
  String get defaultCategoryIncome => 'دخل عام';

  @override
  String get defaultCategoryTransfer => 'تحويل';

  @override
  String get dashboardLoadFailed =>
      'تعذّر تحميل معاملاتك. اخرج من التطبيق ثم عُد إليه لإعادة المحاولة.';

  @override
  String get tourPrivateTitle => 'أموالك، بخصوصية تامة';

  @override
  String get tourPrivateBody =>
      'تتبّع مصروفاتك ودخلك والتحويلات بين حساباتك. يبقى كل شيء على هذا الهاتف، مشفّرًا ومقفلًا برمز PIN الخاص بك. بلا تسجيل، ولا إعلانات، ولا اتصال بالإنترنت.';

  @override
  String get tourAddTitle => 'أضِف في ثوانٍ';

  @override
  String get tourAddBody =>
      'انقر على + لتسجيل مصروف أو دخل أو تحويل. اختر حسابًا وفئة، وستتحدّث أرصدتك تلقائيًا.';

  @override
  String get tourBudgetTitle => 'التزم بميزانيتك';

  @override
  String get tourBudgetBody =>
      'عيِّن حدًّا شهريًا لأي فئة. تعرض لوحة المعلومات المبلغ المتبقي، وتنبّهك قبل أن تتجاوزه.';

  @override
  String get tourReportsTitle => 'التقارير والنسخ الاحتياطية';

  @override
  String get tourReportsBody =>
      'احفظ تقريرًا شهريًا أو شاركه بتنسيق PDF أو CSV، واحتفظ بنسخة احتياطية مشفّرة حتى لا تفقد سجلاتك أبدًا.';

  @override
  String get tourSkip => 'تخطٍّ';

  @override
  String get tourNext => 'التالي';

  @override
  String get tourGetStarted => 'لنبدأ';

  @override
  String tourPageIndicator(int page, int pageCount) {
    return 'الصفحة $page من $pageCount';
  }

  @override
  String get gettingStartedTitle => 'خطوات البدء';

  @override
  String gettingStartedProgress(int done, int total) {
    return 'اكتمل $done من $total';
  }

  @override
  String get gettingStartedAddAccount => 'أضِف حسابك المصرفي أو حساب بطاقتك';

  @override
  String get gettingStartedAddTransaction => 'سجِّل أول معاملة';

  @override
  String get gettingStartedSetBudget => 'عيِّن ميزانية شهرية';

  @override
  String get gettingStartedStepDone => 'تم';

  @override
  String get gettingStartedStepTodo => 'لم يكتمل بعد';

  @override
  String get gettingStartedHide => 'إخفاء قائمة خطوات البدء';

  @override
  String get settingsHelpSection => 'المساعدة والدعم';

  @override
  String get settingsHelpFaq => 'المساعدة والأسئلة الشائعة';

  @override
  String get settingsShowTour => 'عرض الجولة الترحيبية';

  @override
  String get settingsSendFeedback => 'إرسال ملاحظات';

  @override
  String get settingsRateApp => 'تقييم التطبيق';

  @override
  String get settingsShareApp => 'مشاركة التطبيق';

  @override
  String get helpTitle => 'المساعدة والأسئلة الشائعة';

  @override
  String get helpExpanded => 'موسَّع';

  @override
  String get helpCollapsed => 'مطويّ';

  @override
  String get helpContactTitle => 'هل ما زلت بحاجة إلى مساعدة؟';

  @override
  String get helpContactBody =>
      'أرسل سؤالًا أو مشكلة أو فكرة. يُرجى عدم تضمين رمز PIN أو عبارة مرور النسخة الاحتياطية أو سجلاتك المالية.';

  @override
  String get faqDataQ => 'أين تُخزَّن بياناتي؟';

  @override
  String get faqDataA =>
      'على هذا الهاتف فقط. لا يتطلّب Denarii Dolor إنشاء حساب، وليس له خادم، ولا يمكنه الاتصال بالإنترنت. تُحفَظ سجلاتك في قاعدة بيانات مشفّرة لا يفتح قفلها إلا رمز PIN أو إجابة سؤال الأمان أو، إذا فعّلتها، بصمة إصبعك أو وجهك. والنسخ الاحتياطي السحابي في Android متوقف لهذا التطبيق.';

  @override
  String get faqForgotPinQ => 'نسيت رمز PIN. ماذا أفعل؟';

  @override
  String faqForgotPinA(String forgotPin, String wipeAllData) {
    return 'في شاشة تسجيل الدخول، انقر على «$forgotPin» وأجب عن سؤال الأمان لاختيار رمز PIN جديد. وإذا لم تتمكن من الإجابة عنه أيضًا، فلا يمكن فتح قفل بياناتك، لأن المفتاح ليس بحوزة أي شخص آخر. وعندها يكون الحل الوحيد هو «$wipeAllData» من شاشة تسجيل الدخول، ما يمحو كل شيء لتبدأ من جديد.';
  }

  @override
  String get faqSignedOutQ => 'لماذا تم تسجيل خروجي؟';

  @override
  String faqSignedOutA(int minutes, String biometricSetting) {
    return 'لحماية بياناتك، يسجّل التطبيق خروجك بعد $minutes دقائق دون استخدام. سجّل الدخول مجددًا برمز PIN، أو فعّل «$biometricSetting» من الإعدادات لتسجيل الدخول ببصمة إصبعك أو وجهك.';
  }

  @override
  String get faqBackupQ =>
      'كيف أنسخ بياناتي احتياطيًا أو أنقلها إلى هاتف جديد؟';

  @override
  String faqBackupA(String backupSetting) {
    return 'في الإعدادات، افتح «$backupSetting» وأنشئ نسخة احتياطية محمية بعبارة مرور. احفظ الملف في مكان آمن، مثل خدمة تخزين سحابي. على الهاتف الجديد، ثبّت Denarii Dolor واضبطه، ثم استعِد الملف باستخدام عبارة المرور نفسها. لا يستطيع أحد استعادة عبارة مرور منسيّة.';
  }

  @override
  String get faqBudgetsQ => 'كيف تعمل الميزانيات؟';

  @override
  String faqBudgetsA(String budgetsScreen) {
    return 'في الإعدادات، افتح «$budgetsScreen» لتعيين حد شهري وعتبة تحذير لأي فئة. تعرض لوحة المعلومات مقدار ما استخدمته من كل ميزانية هذا الشهر. وإذا كان مصروف ما سيتجاوز ميزانيته، يسألك التطبيق قبل حفظه.';
  }

  @override
  String get faqTransferQ => 'ما هو التحويل؟';

  @override
  String get faqTransferA =>
      'ينقل التحويل المال بين حسابين من حساباتك، مثلًا من «المدخرات» إلى «النقد». فهو يغيّر الرصيدين معًا، لكنه لا يُحتسب دخلًا ولا مصروفًا.';

  @override
  String get faqDeleteQ => 'لماذا لا يمكنني حذف فئة أو حساب؟';

  @override
  String get faqDeleteA =>
      'لا يمكن حذف الفئات والحسابات الافتراضية، ولا أي فئة أو حساب لا تزال معاملات تستخدمه. عدِّل تلك المعاملات أو احذفها أولًا، ثم أعد المحاولة.';

  @override
  String get faqCurrencyQ => 'كيف أغيّر العملة؟';

  @override
  String faqCurrencyA(String currencySetting) {
    return 'في الإعدادات، انقر على «$currencySetting» واختر عملة. يتغيّر رمز العملة فقط، ولا تُحوَّل المبالغ التي أدخلتها سابقًا.';
  }

  @override
  String get faqReportsQ => 'كيف أصدّر تقريرًا؟';

  @override
  String faqReportsA(String reportsScreen) {
    return 'افتح «$reportsScreen»، واختر شهرًا، ثم احفظ التقرير أو شاركه بتنسيق PDF أو CSV. الملفات المصدَّرة غير مشفّرة، لذا احتفظ بها في مكان خاص.';
  }

  @override
  String get faqBiometricQ => 'لماذا توقّف تسجيل الدخول ببصمة الإصبع أو الوجه؟';

  @override
  String get faqBiometricA =>
      'يتوقف عند تغيّر بصمات الأصابع أو الوجوه المسجَّلة على الهاتف، حتى لا تتمكن بصمة أو وجه أُضيف حديثًا من فتح بياناتك. يظل رمز PIN يعمل، ويمكنك إعادة تفعيل تسجيل الدخول بالمقاييس الحيوية من الإعدادات.';

  @override
  String get ratePromptTitle => 'هل يعجبك Denarii Dolor؟';

  @override
  String get ratePromptMessage =>
      'يساعد تقييم سريع على Google Play الآخرين في العثور على التطبيق. لن يستغرق الأمر سوى لحظة.';

  @override
  String get ratePromptRate => 'قيّم الآن';

  @override
  String get ratePromptLater => 'ليس الآن';

  @override
  String get ratePromptNever => 'لا تسألني مجددًا';

  @override
  String get rateUnavailable => 'تعذّر فتح Google Play على هذا الجهاز.';

  @override
  String get shareAppChooser => 'مشاركة Denarii Dolor';

  @override
  String shareAppMessage(String storeLink) {
    return 'أتتبّع إنفاقي وميزانياتي باستخدام Denarii Dolor، وتبقى كل البيانات خاصة على الهاتف. نزّله من Google Play: $storeLink';
  }

  @override
  String feedbackEmailSubject(String appVersion) {
    return 'ملاحظات حول Denarii Dolor (الإصدار $appVersion)';
  }

  @override
  String feedbackEmailBody(String appVersion, String androidVersion) {
    return '\n\n———\nإصدار التطبيق: $appVersion\nإصدار Android: $androidVersion';
  }

  @override
  String feedbackNoEmailApp(String emailAddress) {
    return 'لم يتم العثور على تطبيق بريد إلكتروني. يمكنك الكتابة إلى $emailAddress.';
  }

  @override
  String placeholderAmountInCents(int cents) {
    return '$cents سنت';
  }

  @override
  String placeholderAmountRejected(String code) {
    return 'مرفوض: $code';
  }
}
