// ignore: unused_import
import 'package:intl/intl.dart' as intl;

import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for Chinese (`zh`).
class AppLocalizationsZh extends AppLocalizations {
  AppLocalizationsZh([String locale = 'zh']) : super(locale);

  @override
  String get appName => 'Denarii Dolor';

  @override
  String get dashboard => '概览';

  @override
  String get signIn => '登录';

  @override
  String get signInWithBiometrics => '使用生物识别登录';

  @override
  String get biometricSignInAccessibilityLabel => '使用生物识别登录';

  @override
  String get biometricSignInTitle => '解锁 Denarii Dolor';

  @override
  String get biometricSignInSubtitle => '使用已录入的生物识别信息继续';

  @override
  String get biometricSignInFailed => '生物识别验证失败。请重试或使用 PIN 码。';

  @override
  String get biometricSignInError => '生物识别登录暂不可用。请改用 PIN 码。';

  @override
  String get usePinInstead => '改用 PIN 码';

  @override
  String get biometricSignInInvalidated =>
      '由于此设备上的生物识别信息已更改，生物识别登录已关闭。请使用 PIN 码登录，然后在“设置”中重新开启。';

  @override
  String get biometricEnrollTitle => '开启生物识别登录';

  @override
  String get biometricEnrollSubtitle => '使用指纹或人脸进行确认';

  @override
  String get biometricEnabled => '生物识别登录已开启。';

  @override
  String get biometricEnableFailed => '无法开启生物识别登录。';

  @override
  String get pinHint => 'PIN 码';

  @override
  String get showPin => '显示';

  @override
  String get hidePin => '隐藏';

  @override
  String get newPinHint => '新 PIN 码';

  @override
  String get confirmPinHint => '确认 PIN 码';

  @override
  String get securityQuestionHint => '安全问题';

  @override
  String get securityAnswerHint => '安全问题答案';

  @override
  String get securityAnswerVerifyHint => '安全问题答案';

  @override
  String get forgotPin => '忘记 PIN 码？';

  @override
  String get backToSignIn => '返回登录';

  @override
  String get createSecurityProfile => '创建安全档案';

  @override
  String get wipeAllData => '清除所有应用数据';

  @override
  String get wipeAllDataDestructiveLabel => '危险操作：清除所有应用数据';

  @override
  String get wipeDataTitle => '删除所有应用数据？';

  @override
  String get wipeDataWarning => '此操作将永久删除所有应用数据、财务记录、PIN 码及恢复设置，且无法撤销。';

  @override
  String get wipeDataConfirm => '全部删除';

  @override
  String wipeDataConfirmCountdown(int seconds) {
    return '全部删除（$seconds）';
  }

  @override
  String get wipeDataConfirmationWord => 'WIPE';

  @override
  String wipeDataTypeToConfirm(String word) {
    return '输入 $word 以确认';
  }

  @override
  String get wipeDataConfirmDestructiveLabel => '危险确认：永久删除所有应用数据';

  @override
  String get wipeDataCancel => '取消';

  @override
  String get pinFormatError => 'PIN 码必须为 6 至 12 位数字。';

  @override
  String get pinConfirmationMismatch => '两次输入的 PIN 码必须一致。';

  @override
  String get securityQuestionRequired => '请填写安全问题。';

  @override
  String get securityAnswerRequired => '请填写安全问题答案。';

  @override
  String securityQuestionTooLong(int maxLength) {
    return '安全问题不能超过 $maxLength 个字符。';
  }

  @override
  String securityAnswerTooShort(int minLength) {
    return '安全问题答案至少需要 $minLength 个字符。不区分大小写，多余的空格不计入。';
  }

  @override
  String securityAnswerTooLong(int maxLength) {
    return '安全问题答案不能超过 $maxLength 个字符。';
  }

  @override
  String get securityAnswerInQuestion => '答案不能出现在问题中。';

  @override
  String get securityAnswerMatchesPin => '答案不能与 PIN 码相同。';

  @override
  String securityAnswerRules(int minLength) {
    return '至少 $minLength 个字符。不区分大小写，多余的空格将被忽略。请选择他人无法猜到或查到的答案。';
  }

  @override
  String get upgradeRecoveryIntro =>
      'Denarii Dolor 现已使用 PIN 码和更强的恢复答案保护数据。请设置安全问题及答案以完成登录。';

  @override
  String get saveAndContinue => '保存并继续';

  @override
  String get storageErrorMessage =>
      '无法读取此设备上的安全存储，因此无法解锁数据。请重试，或清除所有应用数据以重新开始。';

  @override
  String get tryAgain => '重试';

  @override
  String get profileSetupSuccess => '安全档案已创建。请使用 PIN 码登录。';

  @override
  String get profileAlreadyConfigured => '安全档案已配置。';

  @override
  String get profileNotConfigured => '请先设置安全档案，然后再恢复 PIN 码。';

  @override
  String get invalidSecurityAnswer => '安全问题答案不正确。';

  @override
  String get pinRecoverySuccess => 'PIN 码重置成功。请使用新 PIN 码登录。';

  @override
  String get wipeDataSuccess => '所有应用数据已删除。请设置新的安全档案。';

  @override
  String get wipeDataError => '无法清除应用数据。请重试。';

  @override
  String get securityQuestionUnavailable => '安全问题不可用。';

  @override
  String get addTransaction => '添加交易';

  @override
  String get transactionDescriptionHint => '描述';

  @override
  String get transactionTypeLabel => '交易类型';

  @override
  String get transactionAmountHint => '金额';

  @override
  String get transactionReferenceRequiredMessage => '保存此交易前，请检查账户和分类 ID。';

  @override
  String get dateHint => '日期（YYYY-MM-DD）';

  @override
  String get saveTransaction => '保存交易';

  @override
  String get transactionTypeExpense => '支出';

  @override
  String get transactionTypeIncome => '收入';

  @override
  String get transactionTypeTransfer => '转账';

  @override
  String get search => '搜索';

  @override
  String get searchDescriptionHint => '描述';

  @override
  String get searchMinAmountHint => '最低金额';

  @override
  String get searchMaxAmountHint => '最高金额';

  @override
  String get searchStartDateHint => '开始日期（YYYY-MM-DD）';

  @override
  String get searchEndDateHint => '结束日期（YYYY-MM-DD）';

  @override
  String get invalidDateMessage => '请使用 YYYY-MM-DD 日期格式。';

  @override
  String get invalidSearchFiltersMessage => '请检查搜索日期和数值筛选条件，然后重试。';

  @override
  String get invalidSearchRangeMessage => '请输入有效的金额和日期范围。';

  @override
  String get reports => '报表';

  @override
  String get settings => '设置';

  @override
  String settingsSessionTimeout(int minutes) {
    return '会话超时：闲置 $minutes 分钟';
  }

  @override
  String get settingsPinConfigured => '已设置 PIN 码，可快速登录。';

  @override
  String get settingsPinNotConfigured => '尚未设置 PIN 码。';

  @override
  String get signOut => '退出登录';

  @override
  String get resetPin => '重置 PIN 码';

  @override
  String get settingsDarkMode => '深色模式';

  @override
  String get settingsBiometricSignIn => '生物识别登录';

  @override
  String get settingsCurrency => '货币';

  @override
  String settingsCurrencyValue(String currencyName, String symbol) {
    return '$currencyName（$symbol）';
  }

  @override
  String get settingsCurrencyTitle => '选择货币';

  @override
  String get settingsCurrencyNote => '仅更改货币符号，已输入的金额不会换算。';

  @override
  String get settingsBackupRestore => '备份与恢复';

  @override
  String get settingsPrivacyPolicy => '隐私政策';

  @override
  String get settingsLicenses => '开源许可';

  @override
  String get backupExplanation =>
      '备份文件包含你的账户、分类、预算、交易记录和货币设置，并使用你设定的密码短语加密。备份不包含 PIN 码、安全问题或生物识别登录：在新手机上，请先设置应用，再在这里恢复备份。';

  @override
  String get backupPassphraseWarning => '请妥善保管密码短语。没有它就无法打开备份，也没有任何人能找回它。';

  @override
  String get backupCreate => '创建备份';

  @override
  String get backupRestore => '从备份恢复';

  @override
  String get backupCreateTitle => '设置密码短语';

  @override
  String get backupRestoreTitle => '备份密码短语';

  @override
  String get backupPassphrase => '密码短语';

  @override
  String get backupPassphraseConfirm => '确认密码短语';

  @override
  String backupPassphraseRules(int minLength) {
    return '至少 $minLength 个字符。几个互不相关的词组合起来效果很好。';
  }

  @override
  String get backupContinue => '继续';

  @override
  String get backupWorking => '正在处理…';

  @override
  String get backupSaved => '备份已保存。';

  @override
  String get backupSaveFailed => '无法保存备份。';

  @override
  String get backupRestoreConfirmTitle => '替换所有数据？';

  @override
  String backupRestoreContents(
    String created,
    int accounts,
    int categories,
    int budgets,
    int transactions,
  ) {
    return '创建时间：$created\n账户：$accounts\n分类：$categories\n预算：$budgets\n交易：$transactions';
  }

  @override
  String get backupRestoreWarning => '恢复会清除此手机上现有的全部数据，并替换为备份中的数据。此操作无法撤消。';

  @override
  String get backupRestoreConfirmationWord => 'RESTORE';

  @override
  String get backupRestoreAction => '替换数据';

  @override
  String get backupRestored => '备份已恢复。';

  @override
  String get backupRestoreFailed => '无法恢复备份。你的数据未作任何更改。';

  @override
  String backupErrorPassphraseLength(int minLength, int maxLength) {
    return '密码短语必须为 $minLength 到 $maxLength 个字符。';
  }

  @override
  String get backupErrorPassphraseMismatch => '两次输入的密码短语不一致。';

  @override
  String get backupErrorNotBackup => '此文件不是 Denarii Dolor 备份。';

  @override
  String get backupErrorNewerVersion => '此备份由更新版本的 Denarii Dolor 创建。请更新应用后重试。';

  @override
  String get backupErrorWrongPassphrase => '密码短语错误，或文件已损坏。';

  @override
  String get backupErrorDamaged => '备份已损坏，无法恢复。';

  @override
  String get backupErrorRead => '无法读取文件。';

  @override
  String get legalLoadFailed => '无法打开此文档。';

  @override
  String get legalEnglishOnly => '此文档仅提供英文版本。';

  @override
  String get transactionCategoryLabel => '分类';

  @override
  String get transactionAccountLabel => '账户';

  @override
  String get transactionTransferAccountLabel => '转入账户';

  @override
  String get editTransaction => '编辑交易';

  @override
  String get updateTransaction => '更新交易';

  @override
  String get transactionSaved => '交易已保存。';

  @override
  String get transactionDeleted => '交易已删除。';

  @override
  String get transactionNotFound => '未找到该交易。';

  @override
  String get recentTransactions => '最近交易';

  @override
  String get noTransactions => '暂无交易。点击 + 添加一笔。';

  @override
  String get accountBalances => '账户余额';

  @override
  String get deleteTransactionTitle => '删除交易？';

  @override
  String deleteTransactionMessage(String description) {
    return '删除“$description”？账户余额将相应调整。';
  }

  @override
  String deleteItemTitle(String name) {
    return '删除“$name”？';
  }

  @override
  String get deleteItemMessage => '此操作无法撤销。';

  @override
  String get pickerPlaceholder => '请选择…';

  @override
  String get edit => '编辑';

  @override
  String get delete => '删除';

  @override
  String get cancel => '取消';

  @override
  String get save => '保存';

  @override
  String get back => '返回';

  @override
  String get genericError => '出错了，请重试。';

  @override
  String get manageCategories => '管理分类';

  @override
  String get manageAccounts => '管理账户';

  @override
  String get manageBudgets => '管理预算';

  @override
  String get addCategory => '添加分类';

  @override
  String get categoryName => '分类名称';

  @override
  String get categorySaved => '分类已保存。';

  @override
  String get categoryDeleted => '分类已删除。';

  @override
  String get addAccount => '添加账户';

  @override
  String get renameAccount => '重命名账户';

  @override
  String get accountName => '账户名称';

  @override
  String get openingBalance => '初始余额';

  @override
  String accountBalance(String balance) {
    return '余额：$balance';
  }

  @override
  String get accountSaved => '账户已保存。';

  @override
  String get accountDeleted => '账户已删除。';

  @override
  String get invalidOpeningBalance => '请输入有效的初始余额（最多 2 位小数）。';

  @override
  String get setBudget => '设置预算';

  @override
  String budgetForCategory(String category) {
    return '预算：$category';
  }

  @override
  String get budgetLimit => '每月限额';

  @override
  String get budgetWarningPercent => '预警阈值（占限额的百分比）';

  @override
  String budgetSummary(String limit, int percent) {
    return '限额 $limit · 达到 $percent% 时预警';
  }

  @override
  String get noBudget => '未设置预算';

  @override
  String get remove => '移除';

  @override
  String get budgetSaved => '预算已保存。';

  @override
  String get budgetDeleted => '预算已移除。';

  @override
  String get invalidBudgetInput => '请输入最多 2 位小数的限额，以及 1 至 100 之间的预警百分比。';

  @override
  String invalidPinAttemptsLeft(int attempts) {
    return 'PIN 码错误。还剩 $attempts 次尝试机会，之后将暂时锁定。';
  }

  @override
  String pinLockedOut(int seconds) {
    return '失败次数过多。请在 $seconds 秒后重试。';
  }

  @override
  String get searchAllCategories => '全部分类';

  @override
  String get searchClear => '清除';

  @override
  String searchResultCount(int count) {
    return '$count 条结果';
  }

  @override
  String get searchNoResults => '没有匹配的交易。';

  @override
  String get previousMonth => '‹ 上月';

  @override
  String get nextMonth => '下月 ›';

  @override
  String get reportTitle => '月度支出报表';

  @override
  String reportGenerated(String generated) {
    return '生成于 $generated';
  }

  @override
  String reportPage(int page) {
    return '第 $page 页';
  }

  @override
  String reportTotals(String income, String expense, String net) {
    return '收入 $income · 支出 $expense · 结余 $net';
  }

  @override
  String get reportColDate => '日期';

  @override
  String get reportColType => '类型';

  @override
  String get reportColCategory => '分类';

  @override
  String get reportColDescription => '描述';

  @override
  String get reportColAmount => '金额';

  @override
  String get reportColPayment => '支付方式';

  @override
  String get reportEmpty => '本月无交易。';

  @override
  String get reportLoadFailed => '无法生成报表。请重试。';

  @override
  String get saveCsv => '保存 CSV';

  @override
  String get savePdf => '保存 PDF';

  @override
  String get shareCsv => '分享 CSV';

  @override
  String get sharePdf => '分享 PDF';

  @override
  String get shareReport => '分享报表';

  @override
  String get reportSaved => '报表已保存。';

  @override
  String get reportExportFailed => '无法导出报表。';

  @override
  String get netThisMonth => '本月结余';

  @override
  String get incomeThisMonth => '收入';

  @override
  String get expenseThisMonth => '支出';

  @override
  String get spendingByCategory => '分类支出';

  @override
  String get spendingNone => '本月无支出。';

  @override
  String get spendingOther => '其他';

  @override
  String get budgetsThisMonth => '本月预算';

  @override
  String budgetSpentOfLimit(String spent, String limit) {
    return '$spent / $limit';
  }

  @override
  String budgetStatusLine(String status, int percent) {
    return '$status · 已用 $percent%';
  }

  @override
  String get budgetStatusOk => '正常';

  @override
  String get budgetStatusWarning => '接近限额';

  @override
  String get budgetStatusOver => '超出预算';

  @override
  String budgetAlerts(int count) {
    String _temp0 = intl.Intl.pluralLogic(
      count,
      locale: localeName,
      other: '$count 项预算已接近或超出限额。',
    );
    return '$_temp0';
  }

  @override
  String get editCategory => '编辑分类';

  @override
  String categoryIconLabel(String icon) {
    return '图标：$icon';
  }

  @override
  String get categoryIconGeneral => '通用';

  @override
  String get categoryIconIncome => '收入';

  @override
  String get categoryIconTransfer => '转账';

  @override
  String get categoryIconGroceries => '食品杂货';

  @override
  String get categoryIconDining => '餐饮';

  @override
  String get categoryIconCoffee => '咖啡';

  @override
  String get categoryIconHousing => '住房';

  @override
  String get categoryIconUtilities => '水电燃气';

  @override
  String get categoryIconInternet => '网络';

  @override
  String get categoryIconPhone => '通讯';

  @override
  String get categoryIconCar => '汽车';

  @override
  String get categoryIconFuel => '加油';

  @override
  String get categoryIconTransit => '公共交通';

  @override
  String get categoryIconTravel => '旅行';

  @override
  String get categoryIconHealth => '医疗';

  @override
  String get categoryIconFitness => '健身';

  @override
  String get categoryIconShopping => '购物';

  @override
  String get categoryIconClothing => '服饰';

  @override
  String get categoryIconEntertainment => '娱乐';

  @override
  String get categoryIconEducation => '教育';

  @override
  String get categoryIconPets => '宠物';

  @override
  String get categoryIconGifts => '礼物';

  @override
  String get categoryIconSavings => '储蓄';

  @override
  String get categoryIconBank => '银行与手续费';

  @override
  String get invalidAmountMessage => '请输入大于 0 且最多 2 位小数的金额。';

  @override
  String get errorIdRequired => '找不到此项目。请返回后重试。';

  @override
  String get errorItemNotFound => '此项目已不存在。';

  @override
  String get errorBlankDescription => '请输入描述。';

  @override
  String errorDescriptionTooLong(int maxLength) {
    return '描述不能超过 $maxLength 个字符。';
  }

  @override
  String get errorInvalidDate => '请选择有效日期。';

  @override
  String get errorTransferDestinationRequired => '请选择转入账户。';

  @override
  String get errorTransferDestinationSame => '请选择另一个转入账户。';

  @override
  String get errorCategoryNotFound => '该分类已不存在，请另选一个。';

  @override
  String get errorAccountNotFound => '该账户已不存在，请另选一个。';

  @override
  String get errorBudgetExceeded => '此笔支出将使该分类超出每月预算。';

  @override
  String get overBudgetTitle => '超出预算';

  @override
  String overBudgetMessage(String category, String overBy, String limit) {
    return '此笔支出将使“$category”超出每月预算（$limit）$overBy。';
  }

  @override
  String get overBudgetConfirm => '仍然保存';

  @override
  String errorInvalidName(int maxLength) {
    return '名称长度须为 1 至 $maxLength 个字符。';
  }

  @override
  String errorDuplicateCategoryName(String name) {
    return '已存在名为“$name”的分类。';
  }

  @override
  String errorDuplicateAccountName(String name) {
    return '已存在名为“$name”的账户。';
  }

  @override
  String get errorDefaultCategory => '默认分类无法删除。';

  @override
  String get errorDefaultAccount => '默认账户无法删除。';

  @override
  String errorCategoryInUse(int count) {
    String _temp0 = intl.Intl.pluralLogic(
      count,
      locale: localeName,
      other: '有 $count 笔交易正在使用此分类。',
    );
    return '$_temp0';
  }

  @override
  String errorAccountInUse(int count) {
    String _temp0 = intl.Intl.pluralLogic(
      count,
      locale: localeName,
      other: '有 $count 笔交易正在使用此账户。',
    );
    return '$_temp0';
  }

  @override
  String get errorInvalidWarningPercent => '预警阈值必须为 1 至 100 之间的百分比。';

  @override
  String get defaultAccountCash => '现金';

  @override
  String get defaultAccountSavings => '储蓄';

  @override
  String get defaultCategoryExpense => '一般支出';

  @override
  String get defaultCategoryIncome => '一般收入';

  @override
  String get defaultCategoryTransfer => '转账';

  @override
  String get dashboardLoadFailed => '无法加载交易。请退出应用后重新进入以重试。';

  @override
  String get tourPrivateTitle => '你的财务，私密保管';

  @override
  String get tourPrivateBody =>
      '记录支出、收入以及账户之间的转账。所有数据都保存在这部手机上，经过加密，并由你的 PIN 码锁定。无需注册，没有广告，也不联网。';

  @override
  String get tourAddTitle => '几秒完成记账';

  @override
  String get tourAddBody => '点击 + 记录一笔支出、收入或转账。选好账户和分类，余额就会自动更新。';

  @override
  String get tourBudgetTitle => '守住预算';

  @override
  String get tourBudgetBody => '为分类设置每月限额。“概览”会显示还剩多少，并在你超支之前提醒你。';

  @override
  String get tourReportsTitle => '报表与备份';

  @override
  String get tourReportsBody => '将月度报表保存或分享为 PDF 或 CSV 文件，并保留一份加密备份，让记录永不丢失。';

  @override
  String get tourSkip => '跳过';

  @override
  String get tourNext => '下一步';

  @override
  String get tourGetStarted => '开始使用';

  @override
  String tourPageIndicator(int page, int pageCount) {
    return '第 $page 页，共 $pageCount 页';
  }

  @override
  String get gettingStartedTitle => '快速上手';

  @override
  String gettingStartedProgress(int done, int total) {
    return '已完成 $done 项，共 $total 项';
  }

  @override
  String get gettingStartedAddAccount => '添加你的银行或银行卡账户';

  @override
  String get gettingStartedAddTransaction => '记录第一笔交易';

  @override
  String get gettingStartedSetBudget => '设置每月预算';

  @override
  String get gettingStartedStepDone => '已完成';

  @override
  String get gettingStartedStepTodo => '尚未完成';

  @override
  String get gettingStartedHide => '隐藏快速上手清单';

  @override
  String get settingsHelpSection => '帮助与支持';

  @override
  String get settingsHelpFaq => '帮助与常见问题';

  @override
  String get settingsShowTour => '查看欢迎导览';

  @override
  String get settingsSendFeedback => '发送反馈';

  @override
  String get settingsRateApp => '为此应用评分';

  @override
  String get settingsShareApp => '分享此应用';

  @override
  String get helpTitle => '帮助与常见问题';

  @override
  String get helpExpanded => '已展开';

  @override
  String get helpCollapsed => '已收起';

  @override
  String get helpContactTitle => '还需要帮助？';

  @override
  String get helpContactBody => '欢迎发送问题、故障或建议。请勿包含你的 PIN 码、备份密码短语或财务记录。';

  @override
  String get faqDataQ => '我的数据存储在哪里？';

  @override
  String get faqDataA =>
      '只存储在这部手机上。Denarii Dolor 无需注册账号，也没有服务器，而且无法连接互联网。你的记录保存在加密数据库中，只有你的 PIN 码、安全问题答案，或者（如果你已开启）你的指纹或人脸才能解锁。此应用已关闭 Android 云备份。';

  @override
  String get faqForgotPinQ => '忘记了 PIN 码，怎么办？';

  @override
  String faqForgotPinA(String forgotPin, String wipeAllData) {
    return '在登录界面点击“$forgotPin”，然后回答安全问题，即可设置新的 PIN 码。如果安全问题也答不上来，你的数据将无法解锁，因为没有其他人持有密钥。此时唯一的办法是在登录界面点击“$wipeAllData”，清除全部数据后重新开始。';
  }

  @override
  String get faqSignedOutQ => '为什么我被退出登录了？';

  @override
  String faqSignedOutA(int minutes, String biometricSetting) {
    return '为保护你的数据，应用在闲置 $minutes 分钟后会自动退出登录。请使用 PIN 码重新登录，或在“设置”中开启“$biometricSetting”，以便使用指纹或人脸登录。';
  }

  @override
  String get faqBackupQ => '如何备份数据，或将数据迁移到新手机？';

  @override
  String faqBackupA(String backupSetting) {
    return '在“设置”中打开“$backupSetting”，创建一个由密码短语保护的备份。将文件保存在安全的地方，例如云盘。在新手机上安装并设置 Denarii Dolor，然后使用相同的密码短语恢复该文件。密码短语一旦忘记，任何人都无法找回。';
  }

  @override
  String get faqBudgetsQ => '预算是如何运作的？';

  @override
  String faqBudgetsA(String budgetsScreen) {
    return '在“设置”中打开“$budgetsScreen”，为分类设置每月限额和预警阈值。“概览”会显示本月每项预算已使用了多少。如果某笔支出会超出预算，应用会在保存前先询问你。';
  }

  @override
  String get faqTransferQ => '什么是转账？';

  @override
  String get faqTransferA =>
      '转账是在你的两个账户之间转移资金，例如从“储蓄”转到“现金”。它会同时改变两个账户的余额，但不计为收入或支出。';

  @override
  String get faqDeleteQ => '为什么无法删除分类或账户？';

  @override
  String get faqDeleteA => '默认分类和账户无法删除，仍被交易使用的分类或账户也无法删除。请先修改或删除这些交易，然后重试。';

  @override
  String get faqCurrencyQ => '如何更改货币？';

  @override
  String faqCurrencyA(String currencySetting) {
    return '在“设置”中点击“$currencySetting”并选择一种货币。仅更改货币符号，已输入的金额不会换算。';
  }

  @override
  String get faqReportsQ => '如何导出报表？';

  @override
  String faqReportsA(String reportsScreen) {
    return '打开“$reportsScreen”，选择月份，然后将报表保存或分享为 PDF 或 CSV 文件。导出的文件未加密，请存放在私密的地方。';
  }

  @override
  String get faqBiometricQ => '为什么指纹或人脸登录被关闭了？';

  @override
  String get faqBiometricA =>
      '当手机上录入的指纹或人脸发生变化时，此功能会自动关闭，以防新添加的指纹或人脸打开你的数据。你的 PIN 码仍然可用，也可以在“设置”中重新开启生物识别登录。';

  @override
  String get ratePromptTitle => '喜欢 Denarii Dolor 吗？';

  @override
  String get ratePromptMessage => '在 Google Play 上快速评个分，就能帮助更多人发现这款应用。只需片刻。';

  @override
  String get ratePromptRate => '去评分';

  @override
  String get ratePromptLater => '以后再说';

  @override
  String get ratePromptNever => '不再询问';

  @override
  String get rateUnavailable => '无法在此设备上打开 Google Play。';

  @override
  String get shareAppChooser => '分享 Denarii Dolor';

  @override
  String shareAppMessage(String storeLink) {
    return '我用 Denarii Dolor 记录支出和预算，所有数据都私密地保存在手机上。在 Google Play 上下载：$storeLink';
  }

  @override
  String feedbackEmailSubject(String appVersion) {
    return 'Denarii Dolor 反馈（版本 $appVersion）';
  }

  @override
  String feedbackEmailBody(String appVersion, String androidVersion) {
    return '\n\n———\n应用版本：$appVersion\nAndroid 版本：$androidVersion';
  }

  @override
  String feedbackNoEmailApp(String emailAddress) {
    return '未找到电子邮件应用。你可以发送邮件至 $emailAddress。';
  }

  @override
  String placeholderAmountInCents(int cents) {
    return '$cents 分';
  }

  @override
  String placeholderAmountRejected(String code) {
    return '已拒绝：$code';
  }
}
