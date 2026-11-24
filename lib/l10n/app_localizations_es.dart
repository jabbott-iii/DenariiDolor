// ignore: unused_import
import 'package:intl/intl.dart' as intl;

import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for Spanish Castilian (`es`).
class AppLocalizationsEs extends AppLocalizations {
  AppLocalizationsEs([String locale = 'es']) : super(locale);

  @override
  String get appName => 'Denarii Dolor';

  @override
  String get dashboard => 'Panel';

  @override
  String get signIn => 'Iniciar sesión';

  @override
  String get signInWithBiometrics => 'Iniciar sesión con biometría';

  @override
  String get biometricSignInAccessibilityLabel =>
      'Iniciar sesión con biometría';

  @override
  String get biometricSignInTitle => 'Desbloquear Denarii Dolor';

  @override
  String get biometricSignInSubtitle =>
      'Usa tus datos biométricos registrados para continuar';

  @override
  String get biometricSignInFailed =>
      'Falló la autenticación biométrica. Vuelve a intentarlo o usa tu PIN.';

  @override
  String get biometricSignInError =>
      'El inicio de sesión con biometría no está disponible en este momento. Usa tu PIN.';

  @override
  String get usePinInstead => 'Usar PIN';

  @override
  String get biometricSignInInvalidated =>
      'Se desactivó el inicio de sesión con biometría porque cambiaron los datos biométricos de este dispositivo. Inicia sesión con tu PIN y vuelve a activarlo en Configuración.';

  @override
  String get biometricEnrollTitle => 'Activar inicio de sesión con biometría';

  @override
  String get biometricEnrollSubtitle =>
      'Confirma con tu huella digital o tu rostro';

  @override
  String get biometricEnabled =>
      'El inicio de sesión con biometría está activado.';

  @override
  String get biometricEnableFailed =>
      'No se pudo activar el inicio de sesión con biometría.';

  @override
  String get pinHint => 'PIN';

  @override
  String get showPin => 'Mostrar';

  @override
  String get hidePin => 'Ocultar';

  @override
  String get newPinHint => 'PIN nuevo';

  @override
  String get confirmPinHint => 'Confirmar PIN';

  @override
  String get securityQuestionHint => 'Pregunta de seguridad';

  @override
  String get securityAnswerHint => 'Respuesta de seguridad';

  @override
  String get securityAnswerVerifyHint => 'Respuesta de seguridad';

  @override
  String get forgotPin => '¿Olvidaste tu PIN?';

  @override
  String get backToSignIn => 'Volver al inicio de sesión';

  @override
  String get createSecurityProfile => 'Crear perfil de seguridad';

  @override
  String get wipeAllData => 'Borrar todos los datos de la app';

  @override
  String get wipeAllDataDestructiveLabel =>
      'Acción destructiva: borrar todos los datos de la app';

  @override
  String get wipeDataTitle => '¿Eliminar todos los datos de la app?';

  @override
  String get wipeDataWarning =>
      'Esto elimina de forma permanente todos los datos de la app, los registros financieros, el PIN y la configuración de recuperación. Esta acción no se puede deshacer.';

  @override
  String get wipeDataConfirm => 'Eliminar todo';

  @override
  String wipeDataConfirmCountdown(int seconds) {
    return 'Eliminar todo ($seconds)';
  }

  @override
  String get wipeDataConfirmationWord => 'WIPE';

  @override
  String wipeDataTypeToConfirm(String word) {
    return 'Escribe $word para confirmar';
  }

  @override
  String get wipeDataConfirmDestructiveLabel =>
      'Confirmación destructiva: eliminar de forma permanente todos los datos de la app';

  @override
  String get wipeDataCancel => 'Cancelar';

  @override
  String get pinFormatError => 'El PIN debe tener entre 6 y 12 dígitos.';

  @override
  String get pinConfirmationMismatch =>
      'El PIN y la confirmación deben coincidir.';

  @override
  String get securityQuestionRequired =>
      'La pregunta de seguridad es obligatoria.';

  @override
  String get securityAnswerRequired =>
      'La respuesta de seguridad es obligatoria.';

  @override
  String securityQuestionTooLong(int maxLength) {
    return 'La pregunta de seguridad debe tener $maxLength caracteres como máximo.';
  }

  @override
  String securityAnswerTooShort(int minLength) {
    return 'La respuesta de seguridad debe tener al menos $minLength caracteres. No se distinguen mayúsculas de minúsculas, y los espacios adicionales no cuentan.';
  }

  @override
  String securityAnswerTooLong(int maxLength) {
    return 'La respuesta de seguridad debe tener $maxLength caracteres como máximo.';
  }

  @override
  String get securityAnswerInQuestion =>
      'La respuesta no puede aparecer en la pregunta.';

  @override
  String get securityAnswerMatchesPin =>
      'La respuesta no puede ser igual a tu PIN.';

  @override
  String securityAnswerRules(int minLength) {
    return 'Al menos $minLength caracteres. No se distinguen mayúsculas de minúsculas, y se ignoran los espacios adicionales. Elige una respuesta que otras personas no puedan adivinar ni buscar.';
  }

  @override
  String get upgradeRecoveryIntro =>
      'Denarii Dolor ahora protege tus datos con tu PIN y una respuesta de recuperación más segura. Configura una pregunta y una respuesta de seguridad para terminar de iniciar sesión.';

  @override
  String get saveAndContinue => 'Guardar y continuar';

  @override
  String get storageErrorMessage =>
      'No se puede leer el almacenamiento seguro de este dispositivo, por lo que no es posible desbloquear tus datos. Vuelve a intentarlo o borra todos los datos de la app para empezar de nuevo.';

  @override
  String get tryAgain => 'Reintentar';

  @override
  String get profileSetupSuccess =>
      'Se creó el perfil de seguridad. Inicia sesión con tu PIN.';

  @override
  String get profileAlreadyConfigured =>
      'El perfil de seguridad ya está configurado.';

  @override
  String get profileNotConfigured =>
      'Configura un perfil de seguridad antes de recuperar un PIN.';

  @override
  String get invalidSecurityAnswer =>
      'La respuesta de seguridad es incorrecta.';

  @override
  String get pinRecoverySuccess =>
      'Se restableció el PIN. Inicia sesión con tu PIN nuevo.';

  @override
  String get wipeDataSuccess =>
      'Se eliminaron todos los datos de la app. Configura un nuevo perfil de seguridad.';

  @override
  String get wipeDataError =>
      'No se pudieron borrar los datos de la app. Vuelve a intentarlo.';

  @override
  String get securityQuestionUnavailable =>
      'La pregunta de seguridad no está disponible.';

  @override
  String get addTransaction => 'Agregar transacción';

  @override
  String get transactionDescriptionHint => 'Descripción';

  @override
  String get transactionTypeLabel => 'Tipo de transacción';

  @override
  String get transactionAmountHint => 'Monto';

  @override
  String get transactionReferenceRequiredMessage =>
      'Revisa los ID de la cuenta y de la categoría antes de guardar esta transacción.';

  @override
  String get dateHint => 'Fecha (AAAA-MM-DD)';

  @override
  String get saveTransaction => 'Guardar transacción';

  @override
  String get transactionTypeExpense => 'Gasto';

  @override
  String get transactionTypeIncome => 'Ingreso';

  @override
  String get transactionTypeTransfer => 'Transferencia';

  @override
  String get search => 'Buscar';

  @override
  String get searchDescriptionHint => 'Descripción';

  @override
  String get searchMinAmountHint => 'Monto mínimo';

  @override
  String get searchMaxAmountHint => 'Monto máximo';

  @override
  String get searchStartDateHint => 'Fecha inicial (AAAA-MM-DD)';

  @override
  String get searchEndDateHint => 'Fecha final (AAAA-MM-DD)';

  @override
  String get invalidDateMessage => 'Usa el formato de fecha AAAA-MM-DD.';

  @override
  String get invalidSearchFiltersMessage =>
      'Revisa las fechas y los filtros numéricos de la búsqueda, y vuelve a intentarlo.';

  @override
  String get invalidSearchRangeMessage =>
      'Ingresa un monto y un rango de fechas válidos.';

  @override
  String get reports => 'Informes';

  @override
  String get settings => 'Configuración';

  @override
  String settingsSessionTimeout(int minutes) {
    return 'Cierre de sesión automático: $minutes minutos de inactividad';
  }

  @override
  String get settingsPinConfigured =>
      'El PIN está configurado para iniciar sesión rápidamente.';

  @override
  String get settingsPinNotConfigured => 'Aún no se configuró ningún PIN.';

  @override
  String get signOut => 'Cerrar sesión';

  @override
  String get resetPin => 'Restablecer PIN';

  @override
  String get settingsDarkMode => 'Modo oscuro';

  @override
  String get settingsBiometricSignIn => 'Inicio de sesión con biometría';

  @override
  String get settingsCurrency => 'Moneda';

  @override
  String settingsCurrencyValue(String currencyName, String symbol) {
    return '$currencyName ($symbol)';
  }

  @override
  String get settingsCurrencyTitle => 'Elige una moneda';

  @override
  String get settingsCurrencyNote =>
      'Solo cambia el símbolo. Los montos que ya ingresaste no se convierten.';

  @override
  String get settingsBackupRestore => 'Copia de seguridad y restauración';

  @override
  String get settingsPrivacyPolicy => 'Política de privacidad';

  @override
  String get settingsLicenses => 'Licencias de código abierto';

  @override
  String get backupExplanation =>
      'Una copia de seguridad guarda tus cuentas, categorías, presupuestos, transacciones y la moneda elegida, cifrados con una frase de contraseña que tú eliges. No guarda tu PIN, tu pregunta de seguridad ni el inicio de sesión con biometría: en un teléfono nuevo, primero configura la app y luego restaura la copia aquí.';

  @override
  String get backupPassphraseWarning =>
      'Guarda bien la frase de contraseña. Sin ella no se puede abrir la copia, y nadie puede recuperarla.';

  @override
  String get backupCreate => 'Crear copia de seguridad';

  @override
  String get backupRestore => 'Restaurar una copia';

  @override
  String get backupCreateTitle => 'Elige una frase de contraseña';

  @override
  String get backupRestoreTitle => 'Frase de contraseña de la copia';

  @override
  String get backupPassphrase => 'Frase de contraseña';

  @override
  String get backupPassphraseConfirm => 'Confirma la frase de contraseña';

  @override
  String backupPassphraseRules(int minLength) {
    return 'Al menos $minLength caracteres. Unas cuantas palabras sin relación entre sí funcionan bien.';
  }

  @override
  String get backupContinue => 'Continuar';

  @override
  String get backupWorking => 'Procesando…';

  @override
  String get backupSaved => 'Copia de seguridad guardada.';

  @override
  String get backupSaveFailed => 'No se pudo guardar la copia de seguridad.';

  @override
  String get backupRestoreConfirmTitle => '¿Reemplazar todos los datos?';

  @override
  String backupRestoreContents(
    String created,
    int accounts,
    int categories,
    int budgets,
    int transactions,
  ) {
    return 'Creada: $created\nCuentas: $accounts\nCategorías: $categories\nPresupuestos: $budgets\nTransacciones: $transactions';
  }

  @override
  String get backupRestoreWarning =>
      'Al restaurar, se borra todo lo que hay ahora en este teléfono y se reemplaza con la copia. No se puede deshacer.';

  @override
  String get backupRestoreConfirmationWord => 'RESTORE';

  @override
  String get backupRestoreAction => 'Reemplazar datos';

  @override
  String get backupRestored => 'Copia de seguridad restaurada.';

  @override
  String get backupRestoreFailed =>
      'No se pudo restaurar la copia. Tus datos no cambiaron.';

  @override
  String backupErrorPassphraseLength(int minLength, int maxLength) {
    return 'La frase de contraseña debe tener entre $minLength y $maxLength caracteres.';
  }

  @override
  String get backupErrorPassphraseMismatch =>
      'Las frases de contraseña no coinciden.';

  @override
  String get backupErrorNotBackup =>
      'Este archivo no es una copia de seguridad de Denarii Dolor.';

  @override
  String get backupErrorNewerVersion =>
      'Esta copia se creó con una versión más reciente de Denarii Dolor. Actualiza la app y vuelve a intentarlo.';

  @override
  String get backupErrorWrongPassphrase =>
      'La frase de contraseña es incorrecta o el archivo está dañado.';

  @override
  String get backupErrorDamaged =>
      'La copia está dañada y no se puede restaurar.';

  @override
  String get backupErrorRead => 'No se pudo leer el archivo.';

  @override
  String get legalLoadFailed => 'No se pudo abrir este documento.';

  @override
  String get legalEnglishOnly =>
      'Este documento solo está disponible en inglés.';

  @override
  String get transactionCategoryLabel => 'Categoría';

  @override
  String get transactionAccountLabel => 'Cuenta';

  @override
  String get transactionTransferAccountLabel => 'Cuenta de destino';

  @override
  String get editTransaction => 'Editar transacción';

  @override
  String get updateTransaction => 'Actualizar transacción';

  @override
  String get transactionSaved => 'Transacción guardada.';

  @override
  String get transactionDeleted => 'Transacción eliminada.';

  @override
  String get transactionNotFound => 'No se encontró la transacción.';

  @override
  String get recentTransactions => 'Transacciones recientes';

  @override
  String get noTransactions =>
      'Aún no hay transacciones. Toca + para agregar una.';

  @override
  String get accountBalances => 'Saldos de las cuentas';

  @override
  String get deleteTransactionTitle => '¿Eliminar la transacción?';

  @override
  String deleteTransactionMessage(String description) {
    return '¿Eliminar \"$description\"? Se ajustarán los saldos de las cuentas.';
  }

  @override
  String deleteItemTitle(String name) {
    return '¿Eliminar $name?';
  }

  @override
  String get deleteItemMessage => 'Esta acción no se puede deshacer.';

  @override
  String get pickerPlaceholder => 'Seleccionar…';

  @override
  String get edit => 'Editar';

  @override
  String get delete => 'Eliminar';

  @override
  String get cancel => 'Cancelar';

  @override
  String get save => 'Guardar';

  @override
  String get back => 'Atrás';

  @override
  String get genericError => 'Algo salió mal. Vuelve a intentarlo.';

  @override
  String get manageCategories => 'Administrar categorías';

  @override
  String get manageAccounts => 'Administrar cuentas';

  @override
  String get manageBudgets => 'Administrar presupuestos';

  @override
  String get addCategory => 'Agregar categoría';

  @override
  String get categoryName => 'Nombre de la categoría';

  @override
  String get categorySaved => 'Categoría guardada.';

  @override
  String get categoryDeleted => 'Categoría eliminada.';

  @override
  String get addAccount => 'Agregar cuenta';

  @override
  String get renameAccount => 'Cambiar nombre de la cuenta';

  @override
  String get accountName => 'Nombre de la cuenta';

  @override
  String get openingBalance => 'Saldo inicial';

  @override
  String accountBalance(String balance) {
    return 'Saldo: $balance';
  }

  @override
  String get accountSaved => 'Cuenta guardada.';

  @override
  String get accountDeleted => 'Cuenta eliminada.';

  @override
  String get invalidOpeningBalance =>
      'Ingresa un saldo inicial válido (2 decimales como máximo).';

  @override
  String get setBudget => 'Definir presupuesto';

  @override
  String budgetForCategory(String category) {
    return 'Presupuesto: $category';
  }

  @override
  String get budgetLimit => 'Límite mensual';

  @override
  String get budgetWarningPercent => 'Umbral de alerta (porcentaje del límite)';

  @override
  String budgetSummary(String limit, int percent) {
    return 'Límite $limit · alerta al $percent%';
  }

  @override
  String get noBudget => 'Sin presupuesto definido';

  @override
  String get remove => 'Quitar';

  @override
  String get budgetSaved => 'Presupuesto guardado.';

  @override
  String get budgetDeleted => 'Se quitó el presupuesto.';

  @override
  String get invalidBudgetInput =>
      'Ingresa un límite con 2 decimales como máximo y un porcentaje de alerta entre 1 y 100.';

  @override
  String invalidPinAttemptsLeft(int attempts) {
    return 'PIN incorrecto. Intentos restantes antes de un bloqueo temporal: $attempts.';
  }

  @override
  String pinLockedOut(int seconds) {
    return 'Demasiados intentos fallidos. Vuelve a intentarlo en $seconds segundos.';
  }

  @override
  String get searchAllCategories => 'Todas las categorías';

  @override
  String get searchClear => 'Limpiar';

  @override
  String searchResultCount(int count) {
    return 'Resultados: $count';
  }

  @override
  String get searchNoResults => 'No hay transacciones que coincidan.';

  @override
  String get previousMonth => '‹ Anterior';

  @override
  String get nextMonth => 'Siguiente ›';

  @override
  String get reportTitle => 'Informe mensual de gastos';

  @override
  String reportGenerated(String generated) {
    return 'Generado: $generated';
  }

  @override
  String reportPage(int page) {
    return 'Página $page';
  }

  @override
  String reportTotals(String income, String expense, String net) {
    return 'Ingresos $income · Gastos $expense · Neto $net';
  }

  @override
  String get reportColDate => 'Fecha';

  @override
  String get reportColType => 'Tipo';

  @override
  String get reportColCategory => 'Categoría';

  @override
  String get reportColDescription => 'Descripción';

  @override
  String get reportColAmount => 'Monto';

  @override
  String get reportColPayment => 'Método de pago';

  @override
  String get reportEmpty => 'No hay transacciones en este mes.';

  @override
  String get reportLoadFailed =>
      'No se pudo generar el informe. Vuelve a intentarlo.';

  @override
  String get saveCsv => 'Guardar CSV';

  @override
  String get savePdf => 'Guardar PDF';

  @override
  String get shareCsv => 'Compartir CSV';

  @override
  String get sharePdf => 'Compartir PDF';

  @override
  String get shareReport => 'Compartir informe';

  @override
  String get reportSaved => 'Informe guardado.';

  @override
  String get reportExportFailed => 'No se pudo exportar el informe.';

  @override
  String get netThisMonth => 'Neto de este mes';

  @override
  String get incomeThisMonth => 'Ingresos';

  @override
  String get expenseThisMonth => 'Gastos';

  @override
  String get spendingByCategory => 'Gastos por categoría';

  @override
  String get spendingNone => 'No hay gastos este mes.';

  @override
  String get spendingOther => 'Otros';

  @override
  String get budgetsThisMonth => 'Presupuestos de este mes';

  @override
  String budgetSpentOfLimit(String spent, String limit) {
    return '$spent de $limit';
  }

  @override
  String budgetStatusLine(String status, int percent) {
    return '$status · $percent% usado';
  }

  @override
  String get budgetStatusOk => 'Dentro del límite';

  @override
  String get budgetStatusWarning => 'Cerca del límite';

  @override
  String get budgetStatusOver => 'Presupuesto excedido';

  @override
  String budgetAlerts(int count) {
    String _temp0 = intl.Intl.pluralLogic(
      count,
      locale: localeName,
      other: '$count presupuestos están cerca de su límite o lo exceden.',
      many: '$count presupuestos están cerca de su límite o lo exceden.',
      one: '$count presupuesto está cerca de su límite o lo excede.',
    );
    return '$_temp0';
  }

  @override
  String get editCategory => 'Editar categoría';

  @override
  String categoryIconLabel(String icon) {
    return 'Ícono: $icon';
  }

  @override
  String get categoryIconGeneral => 'General';

  @override
  String get categoryIconIncome => 'Ingresos';

  @override
  String get categoryIconTransfer => 'Transferencia';

  @override
  String get categoryIconGroceries => 'Supermercado';

  @override
  String get categoryIconDining => 'Restaurantes';

  @override
  String get categoryIconCoffee => 'Café';

  @override
  String get categoryIconHousing => 'Vivienda';

  @override
  String get categoryIconUtilities => 'Servicios';

  @override
  String get categoryIconInternet => 'Internet';

  @override
  String get categoryIconPhone => 'Teléfono';

  @override
  String get categoryIconCar => 'Auto';

  @override
  String get categoryIconFuel => 'Combustible';

  @override
  String get categoryIconTransit => 'Transporte público';

  @override
  String get categoryIconTravel => 'Viajes';

  @override
  String get categoryIconHealth => 'Salud';

  @override
  String get categoryIconFitness => 'Ejercicio';

  @override
  String get categoryIconShopping => 'Compras';

  @override
  String get categoryIconClothing => 'Ropa';

  @override
  String get categoryIconEntertainment => 'Entretenimiento';

  @override
  String get categoryIconEducation => 'Educación';

  @override
  String get categoryIconPets => 'Mascotas';

  @override
  String get categoryIconGifts => 'Regalos';

  @override
  String get categoryIconSavings => 'Ahorros';

  @override
  String get categoryIconBank => 'Banco y comisiones';

  @override
  String get invalidAmountMessage =>
      'Ingresa un monto mayor que 0 con 2 decimales como máximo.';

  @override
  String get errorIdRequired =>
      'No se encontró este elemento. Vuelve atrás e inténtalo de nuevo.';

  @override
  String get errorItemNotFound => 'Este elemento ya no existe.';

  @override
  String get errorBlankDescription => 'Ingresa una descripción.';

  @override
  String errorDescriptionTooLong(int maxLength) {
    return 'La descripción debe tener $maxLength caracteres como máximo.';
  }

  @override
  String get errorInvalidDate => 'Elige una fecha válida.';

  @override
  String get errorTransferDestinationRequired =>
      'Elige la cuenta de destino de la transferencia.';

  @override
  String get errorTransferDestinationSame =>
      'Elige una cuenta de destino diferente.';

  @override
  String get errorCategoryNotFound => 'Esa categoría ya no existe. Elige otra.';

  @override
  String get errorAccountNotFound => 'Esa cuenta ya no existe. Elige otra.';

  @override
  String get errorBudgetExceeded =>
      'Este gasto haría que la categoría supere su presupuesto mensual.';

  @override
  String get overBudgetTitle => 'Presupuesto excedido';

  @override
  String overBudgetMessage(String category, String overBy, String limit) {
    return 'Con este gasto, $category supera en $overBy su presupuesto mensual de $limit.';
  }

  @override
  String get overBudgetConfirm => 'Guardar de todos modos';

  @override
  String errorInvalidName(int maxLength) {
    return 'El nombre debe tener entre 1 y $maxLength caracteres.';
  }

  @override
  String errorDuplicateCategoryName(String name) {
    return 'Ya existe una categoría llamada \"$name\".';
  }

  @override
  String errorDuplicateAccountName(String name) {
    return 'Ya existe una cuenta llamada \"$name\".';
  }

  @override
  String get errorDefaultCategory =>
      'Las categorías predeterminadas no se pueden eliminar.';

  @override
  String get errorDefaultAccount =>
      'Las cuentas predeterminadas no se pueden eliminar.';

  @override
  String errorCategoryInUse(int count) {
    String _temp0 = intl.Intl.pluralLogic(
      count,
      locale: localeName,
      other: 'Esta categoría se usa en $count transacciones.',
      many: 'Esta categoría se usa en $count transacciones.',
      one: 'Esta categoría se usa en $count transacción.',
    );
    return '$_temp0';
  }

  @override
  String errorAccountInUse(int count) {
    String _temp0 = intl.Intl.pluralLogic(
      count,
      locale: localeName,
      other: 'Esta cuenta se usa en $count transacciones.',
      many: 'Esta cuenta se usa en $count transacciones.',
      one: 'Esta cuenta se usa en $count transacción.',
    );
    return '$_temp0';
  }

  @override
  String get errorInvalidWarningPercent =>
      'El umbral de alerta debe estar entre 1 y 100 por ciento.';

  @override
  String get defaultAccountCash => 'Efectivo';

  @override
  String get defaultAccountSavings => 'Ahorros';

  @override
  String get defaultCategoryExpense => 'Gastos generales';

  @override
  String get defaultCategoryIncome => 'Ingresos generales';

  @override
  String get defaultCategoryTransfer => 'Transferencia';

  @override
  String get dashboardLoadFailed =>
      'No se pudieron cargar tus transacciones. Sal de la app y vuelve a entrar para intentarlo de nuevo.';

  @override
  String get tourPrivateTitle => 'Tu dinero, en privado';

  @override
  String get tourPrivateBody =>
      'Registra gastos, ingresos y transferencias entre tus cuentas. Todo se queda en este teléfono, cifrado y protegido con tu PIN. Sin registrarte, sin anuncios y sin internet.';

  @override
  String get tourAddTitle => 'Agrega transacciones en segundos';

  @override
  String get tourAddBody =>
      'Toca + para registrar un gasto, un ingreso o una transferencia. Elige una cuenta y una categoría, y tus saldos se actualizan solos.';

  @override
  String get tourBudgetTitle => 'Cuida tu presupuesto';

  @override
  String get tourBudgetBody =>
      'Asigna un límite mensual a una categoría. El Panel te muestra cuánto te queda y te avisa antes de que lo superes.';

  @override
  String get tourReportsTitle => 'Informes y copias de seguridad';

  @override
  String get tourReportsBody =>
      'Guarda o comparte un informe mensual como archivo PDF o CSV, y crea una copia de seguridad cifrada para no perder nunca tus registros.';

  @override
  String get tourSkip => 'Omitir';

  @override
  String get tourNext => 'Siguiente';

  @override
  String get tourGetStarted => 'Comenzar';

  @override
  String tourPageIndicator(int page, int pageCount) {
    return 'Página $page de $pageCount';
  }

  @override
  String get gettingStartedTitle => 'Primeros pasos';

  @override
  String gettingStartedProgress(int done, int total) {
    return '$done de $total completados';
  }

  @override
  String get gettingStartedAddAccount =>
      'Agrega tu cuenta bancaria o de tarjeta';

  @override
  String get gettingStartedAddTransaction => 'Registra tu primera transacción';

  @override
  String get gettingStartedSetBudget => 'Define un presupuesto mensual';

  @override
  String get gettingStartedStepDone => 'Completado';

  @override
  String get gettingStartedStepTodo => 'Pendiente';

  @override
  String get gettingStartedHide => 'Ocultar la lista de primeros pasos';

  @override
  String get settingsHelpSection => 'Ayuda y soporte';

  @override
  String get settingsHelpFaq => 'Ayuda y preguntas frecuentes';

  @override
  String get settingsShowTour => 'Ver el recorrido de bienvenida';

  @override
  String get settingsSendFeedback => 'Enviar comentarios';

  @override
  String get settingsRateApp => 'Calificar esta app';

  @override
  String get settingsShareApp => 'Compartir esta app';

  @override
  String get helpTitle => 'Ayuda y preguntas frecuentes';

  @override
  String get helpExpanded => 'Expandido';

  @override
  String get helpCollapsed => 'Contraído';

  @override
  String get helpContactTitle => '¿Aún necesitas ayuda?';

  @override
  String get helpContactBody =>
      'Envíanos una pregunta, un problema o una idea. No incluyas tu PIN, la frase de contraseña de una copia de seguridad ni tus registros financieros.';

  @override
  String get faqDataQ => '¿Dónde se guardan mis datos?';

  @override
  String get faqDataA =>
      'Solo en este teléfono. Denarii Dolor no requiere una cuenta de usuario, no tiene servidor y no puede conectarse a internet. Tus registros se guardan en una base de datos cifrada que solo se puede desbloquear con tu PIN, tu respuesta de seguridad o, si lo activas, tu huella digital o tu rostro. La copia de seguridad en la nube de Android está desactivada para la app.';

  @override
  String get faqForgotPinQ => 'Olvidé mi PIN. ¿Qué puedo hacer?';

  @override
  String faqForgotPinA(String forgotPin, String wipeAllData) {
    return 'En la pantalla de inicio de sesión, toca “$forgotPin” y responde tu pregunta de seguridad para elegir un PIN nuevo. Si tampoco puedes responderla, no es posible desbloquear tus datos, porque nadie más tiene la clave. En ese caso, la única opción es “$wipeAllData” en la pantalla de inicio de sesión, que borra todo para que puedas empezar de nuevo.';
  }

  @override
  String get faqSignedOutQ => '¿Por qué se cerró mi sesión?';

  @override
  String faqSignedOutA(int minutes, String biometricSetting) {
    return 'Para proteger tus datos, la app cierra la sesión después de $minutes minutos sin uso. Vuelve a iniciar sesión con tu PIN, o activa “$biometricSetting” en Configuración para iniciar sesión con tu huella digital o tu rostro.';
  }

  @override
  String get faqBackupQ =>
      '¿Cómo hago una copia de seguridad de mis datos o los paso a un teléfono nuevo?';

  @override
  String faqBackupA(String backupSetting) {
    return 'En Configuración, abre “$backupSetting” y crea una copia de seguridad protegida con una frase de contraseña. Guarda el archivo en un lugar seguro, como un servicio de almacenamiento en la nube. En el teléfono nuevo, instala Denarii Dolor, configura la app y luego restaura el archivo con la misma frase de contraseña. Nadie puede recuperar una frase de contraseña olvidada.';
  }

  @override
  String get faqBudgetsQ => '¿Cómo funcionan los presupuestos?';

  @override
  String faqBudgetsA(String budgetsScreen) {
    return 'En Configuración, abre “$budgetsScreen” para asignar a una categoría un límite mensual y un umbral de alerta. El Panel muestra cuánto llevas usado de cada presupuesto este mes. Si un gasto va a superar su presupuesto, la app te pregunta antes de guardarlo.';
  }

  @override
  String get faqTransferQ => '¿Qué es una transferencia?';

  @override
  String get faqTransferA =>
      'Una transferencia mueve dinero entre dos de tus cuentas, por ejemplo, de Ahorros a Efectivo. Cambia ambos saldos, pero no cuenta como ingreso ni como gasto.';

  @override
  String get faqDeleteQ =>
      '¿Por qué no puedo eliminar una categoría o una cuenta?';

  @override
  String get faqDeleteA =>
      'Las categorías y cuentas predeterminadas no se pueden eliminar, y tampoco las que todavía se usan en transacciones. Primero cambia o elimina esas transacciones y vuelve a intentarlo.';

  @override
  String get faqCurrencyQ => '¿Cómo cambio la moneda?';

  @override
  String faqCurrencyA(String currencySetting) {
    return 'En Configuración, toca “$currencySetting” y elige una. Solo cambia el símbolo: los montos que ya ingresaste no se convierten.';
  }

  @override
  String get faqReportsQ => '¿Cómo exporto un informe?';

  @override
  String faqReportsA(String reportsScreen) {
    return 'Abre “$reportsScreen”, elige un mes y guarda o comparte el informe como archivo PDF o CSV. Los archivos exportados no están cifrados, así que guárdalos en un lugar privado.';
  }

  @override
  String get faqBiometricQ =>
      '¿Por qué se desactivó el inicio de sesión con huella digital o rostro?';

  @override
  String get faqBiometricA =>
      'Se desactiva cuando cambian las huellas digitales o los rostros registrados en el teléfono, para que uno agregado recientemente no pueda abrir tus datos. Tu PIN sigue funcionando, y puedes volver a activar el inicio de sesión con biometría en Configuración.';

  @override
  String get ratePromptTitle => '¿Te gusta Denarii Dolor?';

  @override
  String get ratePromptMessage =>
      'Una calificación rápida en Google Play ayuda a que otras personas encuentren la app. Solo toma un momento.';

  @override
  String get ratePromptRate => 'Calificar';

  @override
  String get ratePromptLater => 'Ahora no';

  @override
  String get ratePromptNever => 'No volver a preguntar';

  @override
  String get rateUnavailable =>
      'No se pudo abrir Google Play en este dispositivo.';

  @override
  String get shareAppChooser => 'Compartir Denarii Dolor';

  @override
  String shareAppMessage(String storeLink) {
    return 'Llevo el control de mis gastos y presupuestos con Denarii Dolor. Todo queda en privado en el teléfono. Disponible en Google Play: $storeLink';
  }

  @override
  String feedbackEmailSubject(String appVersion) {
    return 'Comentarios sobre Denarii Dolor (versión $appVersion)';
  }

  @override
  String feedbackEmailBody(String appVersion, String androidVersion) {
    return '\n\n———\nVersión de la app: $appVersion\nVersión de Android: $androidVersion';
  }

  @override
  String feedbackNoEmailApp(String emailAddress) {
    return 'No se encontró ninguna app de correo. Puedes escribir a $emailAddress.';
  }

  @override
  String placeholderAmountInCents(int cents) {
    return '$cents centavos';
  }

  @override
  String placeholderAmountRejected(String code) {
    return 'Rechazado: $code';
  }
}
