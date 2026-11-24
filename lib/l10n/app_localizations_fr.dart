// ignore: unused_import
import 'package:intl/intl.dart' as intl;

import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for French (`fr`).
class AppLocalizationsFr extends AppLocalizations {
  AppLocalizationsFr([String locale = 'fr']) : super(locale);

  @override
  String get appName => 'Denarii Dolor';

  @override
  String get dashboard => 'Tableau de bord';

  @override
  String get signIn => 'Se connecter';

  @override
  String get signInWithBiometrics => 'Se connecter avec la biométrie';

  @override
  String get biometricSignInAccessibilityLabel =>
      'Se connecter avec la biométrie';

  @override
  String get biometricSignInTitle => 'Déverrouiller Denarii Dolor';

  @override
  String get biometricSignInSubtitle =>
      'Utilisez la biométrie enregistrée pour continuer';

  @override
  String get biometricSignInFailed =>
      'Échec de l’authentification biométrique. Réessayez ou utilisez votre code PIN.';

  @override
  String get biometricSignInError =>
      'La connexion biométrique est actuellement indisponible. Utilisez plutôt votre code PIN.';

  @override
  String get usePinInstead => 'Utiliser le code PIN';

  @override
  String get biometricSignInInvalidated =>
      'La connexion biométrique a été désactivée, car les données biométriques de cet appareil ont changé. Connectez-vous avec votre code PIN, puis réactivez-la dans les paramètres.';

  @override
  String get biometricEnrollTitle => 'Activer la connexion biométrique';

  @override
  String get biometricEnrollSubtitle =>
      'Confirmez avec votre empreinte digitale ou votre visage';

  @override
  String get biometricEnabled => 'La connexion biométrique est activée.';

  @override
  String get biometricEnableFailed =>
      'Impossible d’activer la connexion biométrique.';

  @override
  String get pinHint => 'Code PIN';

  @override
  String get showPin => 'Afficher';

  @override
  String get hidePin => 'Masquer';

  @override
  String get newPinHint => 'Nouveau code PIN';

  @override
  String get confirmPinHint => 'Confirmer le code PIN';

  @override
  String get securityQuestionHint => 'Question de sécurité';

  @override
  String get securityAnswerHint => 'Réponse de sécurité';

  @override
  String get securityAnswerVerifyHint => 'Réponse de sécurité';

  @override
  String get forgotPin => 'Code PIN oublié ?';

  @override
  String get backToSignIn => 'Retour à la connexion';

  @override
  String get createSecurityProfile => 'Créer un profil de sécurité';

  @override
  String get wipeAllData => 'Effacer toutes les données';

  @override
  String get wipeAllDataDestructiveLabel =>
      'Action irréversible : effacer toutes les données de l’application';

  @override
  String get wipeDataTitle => 'Supprimer toutes les données de l’application ?';

  @override
  String get wipeDataWarning =>
      'Toutes les données de l’application, les données financières, le code PIN et les paramètres de récupération seront définitivement supprimés. Cette action est irréversible.';

  @override
  String get wipeDataConfirm => 'Tout supprimer';

  @override
  String wipeDataConfirmCountdown(int seconds) {
    return 'Tout supprimer ($seconds)';
  }

  @override
  String get wipeDataConfirmationWord => 'WIPE';

  @override
  String wipeDataTypeToConfirm(String word) {
    return 'Saisissez $word pour confirmer';
  }

  @override
  String get wipeDataConfirmDestructiveLabel =>
      'Confirmation irréversible : supprimer définitivement toutes les données de l’application';

  @override
  String get wipeDataCancel => 'Annuler';

  @override
  String get pinFormatError =>
      'Le code PIN doit comporter entre 6 et 12 chiffres.';

  @override
  String get pinConfirmationMismatch =>
      'Le code PIN et sa confirmation doivent être identiques.';

  @override
  String get securityQuestionRequired =>
      'La question de sécurité est obligatoire.';

  @override
  String get securityAnswerRequired =>
      'La réponse de sécurité est obligatoire.';

  @override
  String securityQuestionTooLong(int maxLength) {
    return 'La question de sécurité ne doit pas dépasser $maxLength caractères.';
  }

  @override
  String securityAnswerTooShort(int minLength) {
    return 'La réponse de sécurité doit comporter au moins $minLength caractères. Les majuscules et les espaces superflus ne comptent pas.';
  }

  @override
  String securityAnswerTooLong(int maxLength) {
    return 'La réponse de sécurité ne doit pas dépasser $maxLength caractères.';
  }

  @override
  String get securityAnswerInQuestion =>
      'La réponse ne peut pas figurer dans la question.';

  @override
  String get securityAnswerMatchesPin =>
      'La réponse ne peut pas être identique à votre code PIN.';

  @override
  String securityAnswerRules(int minLength) {
    return 'Au moins $minLength caractères. Les majuscules et les espaces superflus sont ignorés. Choisissez une réponse que personne d’autre ne peut deviner ni retrouver.';
  }

  @override
  String get upgradeRecoveryIntro =>
      'Denarii Dolor protège désormais vos données avec votre code PIN et une réponse de récupération plus robuste. Définissez une question et une réponse de sécurité pour terminer la connexion.';

  @override
  String get saveAndContinue => 'Enregistrer et continuer';

  @override
  String get storageErrorMessage =>
      'Impossible de lire le stockage sécurisé de cet appareil ; vos données ne peuvent donc pas être déverrouillées. Réessayez ou effacez toutes les données de l’application pour repartir de zéro.';

  @override
  String get tryAgain => 'Réessayer';

  @override
  String get profileSetupSuccess =>
      'Profil de sécurité créé. Connectez-vous avec votre code PIN.';

  @override
  String get profileAlreadyConfigured =>
      'Le profil de sécurité est déjà configuré.';

  @override
  String get profileNotConfigured =>
      'Configurez un profil de sécurité avant de récupérer un code PIN.';

  @override
  String get invalidSecurityAnswer => 'La réponse de sécurité est incorrecte.';

  @override
  String get pinRecoverySuccess =>
      'Code PIN réinitialisé. Connectez-vous avec votre nouveau code PIN.';

  @override
  String get wipeDataSuccess =>
      'Toutes les données de l’application ont été supprimées. Configurez un nouveau profil de sécurité.';

  @override
  String get wipeDataError =>
      'Impossible d’effacer les données de l’application. Veuillez réessayer.';

  @override
  String get securityQuestionUnavailable =>
      'Question de sécurité indisponible.';

  @override
  String get addTransaction => 'Ajouter une transaction';

  @override
  String get transactionDescriptionHint => 'Description';

  @override
  String get transactionTypeLabel => 'Type de transaction';

  @override
  String get transactionAmountHint => 'Montant';

  @override
  String get transactionReferenceRequiredMessage =>
      'Vérifiez les identifiants du compte et de la catégorie avant d’enregistrer cette transaction.';

  @override
  String get dateHint => 'Date (AAAA-MM-JJ)';

  @override
  String get saveTransaction => 'Enregistrer la transaction';

  @override
  String get transactionTypeExpense => 'Dépense';

  @override
  String get transactionTypeIncome => 'Revenu';

  @override
  String get transactionTypeTransfer => 'Virement';

  @override
  String get search => 'Rechercher';

  @override
  String get searchDescriptionHint => 'Description';

  @override
  String get searchMinAmountHint => 'Montant min.';

  @override
  String get searchMaxAmountHint => 'Montant max.';

  @override
  String get searchStartDateHint => 'Date de début (AAAA-MM-JJ)';

  @override
  String get searchEndDateHint => 'Date de fin (AAAA-MM-JJ)';

  @override
  String get invalidDateMessage => 'Utilisez le format de date AAAA-MM-JJ.';

  @override
  String get invalidSearchFiltersMessage =>
      'Vérifiez les dates et les filtres numériques de votre recherche, puis réessayez.';

  @override
  String get invalidSearchRangeMessage =>
      'Saisissez un montant et une plage de dates valides.';

  @override
  String get reports => 'Rapports';

  @override
  String get settings => 'Paramètres';

  @override
  String settingsSessionTimeout(int minutes) {
    return 'Expiration de la session : $minutes minutes d’inactivité';
  }

  @override
  String get settingsPinConfigured =>
      'Un code PIN est configuré pour la connexion rapide.';

  @override
  String get settingsPinNotConfigured =>
      'Aucun code PIN n’est encore configuré.';

  @override
  String get signOut => 'Se déconnecter';

  @override
  String get resetPin => 'Réinitialiser le code PIN';

  @override
  String get settingsDarkMode => 'Mode sombre';

  @override
  String get settingsBiometricSignIn => 'Connexion biométrique';

  @override
  String get settingsCurrency => 'Devise';

  @override
  String settingsCurrencyValue(String currencyName, String symbol) {
    return '$currencyName ($symbol)';
  }

  @override
  String get settingsCurrencyTitle => 'Choisir une devise';

  @override
  String get settingsCurrencyNote =>
      'Seul le symbole change. Les montants déjà saisis ne sont pas convertis.';

  @override
  String get settingsBackupRestore => 'Sauvegarde et restauration';

  @override
  String get settingsPrivacyPolicy => 'Politique de confidentialité';

  @override
  String get settingsLicenses => 'Licences open source';

  @override
  String get backupExplanation =>
      'Une sauvegarde contient vos comptes, catégories, budgets, transactions et votre devise, chiffrés avec une phrase secrète de votre choix. Elle ne contient ni votre code PIN, ni votre question de sécurité, ni la connexion biométrique : sur un nouveau téléphone, configurez d’abord l’application, puis restaurez la sauvegarde ici.';

  @override
  String get backupPassphraseWarning =>
      'Conservez votre phrase secrète en lieu sûr. Sans elle, la sauvegarde ne peut pas être ouverte, et personne ne peut la récupérer.';

  @override
  String get backupCreate => 'Créer une sauvegarde';

  @override
  String get backupRestore => 'Restaurer une sauvegarde';

  @override
  String get backupCreateTitle => 'Choisissez une phrase secrète';

  @override
  String get backupRestoreTitle => 'Phrase secrète de la sauvegarde';

  @override
  String get backupPassphrase => 'Phrase secrète';

  @override
  String get backupPassphraseConfirm => 'Confirmez la phrase secrète';

  @override
  String backupPassphraseRules(int minLength) {
    return 'Au moins $minLength caractères. Quelques mots sans rapport entre eux conviennent très bien.';
  }

  @override
  String get backupContinue => 'Continuer';

  @override
  String get backupWorking => 'Traitement en cours…';

  @override
  String get backupSaved => 'Sauvegarde enregistrée.';

  @override
  String get backupSaveFailed => 'Impossible d’enregistrer la sauvegarde.';

  @override
  String get backupRestoreConfirmTitle => 'Remplacer toutes les données ?';

  @override
  String backupRestoreContents(
    String created,
    int accounts,
    int categories,
    int budgets,
    int transactions,
  ) {
    return 'Date de création : $created\nComptes : $accounts\nCatégories : $categories\nBudgets : $budgets\nTransactions : $transactions';
  }

  @override
  String get backupRestoreWarning =>
      'La restauration efface tout ce qui se trouve actuellement sur ce téléphone et le remplace par la sauvegarde. Cette action est irréversible.';

  @override
  String get backupRestoreConfirmationWord => 'RESTORE';

  @override
  String get backupRestoreAction => 'Remplacer les données';

  @override
  String get backupRestored => 'Sauvegarde restaurée.';

  @override
  String get backupRestoreFailed =>
      'Impossible de restaurer la sauvegarde. Vos données n’ont pas été modifiées.';

  @override
  String backupErrorPassphraseLength(int minLength, int maxLength) {
    return 'La phrase secrète doit comporter entre $minLength et $maxLength caractères.';
  }

  @override
  String get backupErrorPassphraseMismatch =>
      'Les phrases secrètes ne correspondent pas.';

  @override
  String get backupErrorNotBackup =>
      'Ce fichier n’est pas une sauvegarde Denarii Dolor.';

  @override
  String get backupErrorNewerVersion =>
      'Cette sauvegarde a été créée par une version plus récente de Denarii Dolor. Mettez l’application à jour, puis réessayez.';

  @override
  String get backupErrorWrongPassphrase =>
      'Phrase secrète incorrecte, ou fichier endommagé.';

  @override
  String get backupErrorDamaged =>
      'La sauvegarde est endommagée et ne peut pas être restaurée.';

  @override
  String get backupErrorRead => 'Impossible de lire le fichier.';

  @override
  String get legalLoadFailed => 'Impossible d’ouvrir ce document.';

  @override
  String get legalEnglishOnly => 'Ce document n’est disponible qu’en anglais.';

  @override
  String get transactionCategoryLabel => 'Catégorie';

  @override
  String get transactionAccountLabel => 'Compte';

  @override
  String get transactionTransferAccountLabel => 'Compte de destination';

  @override
  String get editTransaction => 'Modifier la transaction';

  @override
  String get updateTransaction => 'Mettre à jour la transaction';

  @override
  String get transactionSaved => 'Transaction enregistrée.';

  @override
  String get transactionDeleted => 'Transaction supprimée.';

  @override
  String get transactionNotFound => 'Transaction introuvable.';

  @override
  String get recentTransactions => 'Transactions récentes';

  @override
  String get noTransactions =>
      'Aucune transaction pour le moment. Appuyez sur + pour en ajouter une.';

  @override
  String get accountBalances => 'Soldes des comptes';

  @override
  String get deleteTransactionTitle => 'Supprimer la transaction ?';

  @override
  String deleteTransactionMessage(String description) {
    return 'Supprimer « $description » ? Les soldes des comptes seront ajustés.';
  }

  @override
  String deleteItemTitle(String name) {
    return 'Supprimer $name ?';
  }

  @override
  String get deleteItemMessage => 'Cette action est irréversible.';

  @override
  String get pickerPlaceholder => 'Sélectionner…';

  @override
  String get edit => 'Modifier';

  @override
  String get delete => 'Supprimer';

  @override
  String get cancel => 'Annuler';

  @override
  String get save => 'Enregistrer';

  @override
  String get back => 'Retour';

  @override
  String get genericError => 'Une erreur s’est produite. Veuillez réessayer.';

  @override
  String get manageCategories => 'Gérer les catégories';

  @override
  String get manageAccounts => 'Gérer les comptes';

  @override
  String get manageBudgets => 'Gérer les budgets';

  @override
  String get addCategory => 'Ajouter une catégorie';

  @override
  String get categoryName => 'Nom de la catégorie';

  @override
  String get categorySaved => 'Catégorie enregistrée.';

  @override
  String get categoryDeleted => 'Catégorie supprimée.';

  @override
  String get addAccount => 'Ajouter un compte';

  @override
  String get renameAccount => 'Renommer le compte';

  @override
  String get accountName => 'Nom du compte';

  @override
  String get openingBalance => 'Solde initial';

  @override
  String accountBalance(String balance) {
    return 'Solde : $balance';
  }

  @override
  String get accountSaved => 'Compte enregistré.';

  @override
  String get accountDeleted => 'Compte supprimé.';

  @override
  String get invalidOpeningBalance =>
      'Saisissez un solde initial valide (2 décimales maximum).';

  @override
  String get setBudget => 'Définir un budget';

  @override
  String budgetForCategory(String category) {
    return 'Budget : $category';
  }

  @override
  String get budgetLimit => 'Plafond mensuel';

  @override
  String get budgetWarningPercent => 'Seuil d’alerte (pourcentage du plafond)';

  @override
  String budgetSummary(String limit, int percent) {
    return 'Plafond $limit · alerte à $percent %';
  }

  @override
  String get noBudget => 'Aucun budget défini';

  @override
  String get remove => 'Retirer';

  @override
  String get budgetSaved => 'Budget enregistré.';

  @override
  String get budgetDeleted => 'Budget retiré.';

  @override
  String get invalidBudgetInput =>
      'Saisissez un plafond avec 2 décimales maximum et un pourcentage d’alerte compris entre 1 et 100.';

  @override
  String invalidPinAttemptsLeft(int attempts) {
    return 'Code PIN incorrect. Il vous reste $attempts tentative(s) avant un verrouillage temporaire.';
  }

  @override
  String pinLockedOut(int seconds) {
    return 'Trop de tentatives infructueuses. Réessayez dans $seconds secondes.';
  }

  @override
  String get searchAllCategories => 'Toutes les catégories';

  @override
  String get searchClear => 'Effacer';

  @override
  String searchResultCount(int count) {
    return '$count résultat(s)';
  }

  @override
  String get searchNoResults => 'Aucune transaction correspondante.';

  @override
  String get previousMonth => '‹ Préc.';

  @override
  String get nextMonth => 'Suiv. ›';

  @override
  String get reportTitle => 'Rapport mensuel des dépenses';

  @override
  String reportGenerated(String generated) {
    return 'Généré le $generated';
  }

  @override
  String reportPage(int page) {
    return 'Page $page';
  }

  @override
  String reportTotals(String income, String expense, String net) {
    return 'Revenus $income · Dépenses $expense · Net $net';
  }

  @override
  String get reportColDate => 'Date';

  @override
  String get reportColType => 'Type';

  @override
  String get reportColCategory => 'Catégorie';

  @override
  String get reportColDescription => 'Description';

  @override
  String get reportColAmount => 'Montant';

  @override
  String get reportColPayment => 'Moyen de paiement';

  @override
  String get reportEmpty => 'Aucune transaction ce mois-ci.';

  @override
  String get reportLoadFailed => 'Impossible de générer le rapport. Réessayez.';

  @override
  String get saveCsv => 'Enregistrer en CSV';

  @override
  String get savePdf => 'Enregistrer en PDF';

  @override
  String get shareCsv => 'Partager en CSV';

  @override
  String get sharePdf => 'Partager en PDF';

  @override
  String get shareReport => 'Partager le rapport';

  @override
  String get reportSaved => 'Rapport enregistré.';

  @override
  String get reportExportFailed => 'Impossible d’exporter le rapport.';

  @override
  String get netThisMonth => 'Solde net du mois';

  @override
  String get incomeThisMonth => 'Revenus';

  @override
  String get expenseThisMonth => 'Dépenses';

  @override
  String get spendingByCategory => 'Dépenses par catégorie';

  @override
  String get spendingNone => 'Aucune dépense ce mois-ci.';

  @override
  String get spendingOther => 'Autres';

  @override
  String get budgetsThisMonth => 'Budgets du mois';

  @override
  String budgetSpentOfLimit(String spent, String limit) {
    return '$spent sur $limit';
  }

  @override
  String budgetStatusLine(String status, int percent) {
    return '$status · $percent % du budget utilisé';
  }

  @override
  String get budgetStatusOk => 'Dans le budget';

  @override
  String get budgetStatusWarning => 'Proche du plafond';

  @override
  String get budgetStatusOver => 'Budget dépassé';

  @override
  String budgetAlerts(int count) {
    String _temp0 = intl.Intl.pluralLogic(
      count,
      locale: localeName,
      other: '$count budgets sont proches de leur plafond ou le dépassent.',
      many: '$count budgets sont proches de leur plafond ou le dépassent.',
      one: '$count budget est proche de son plafond ou le dépasse.',
    );
    return '$_temp0';
  }

  @override
  String get editCategory => 'Modifier la catégorie';

  @override
  String categoryIconLabel(String icon) {
    return 'Icône : $icon';
  }

  @override
  String get categoryIconGeneral => 'Général';

  @override
  String get categoryIconIncome => 'Revenus';

  @override
  String get categoryIconTransfer => 'Virement';

  @override
  String get categoryIconGroceries => 'Courses';

  @override
  String get categoryIconDining => 'Restaurants';

  @override
  String get categoryIconCoffee => 'Café';

  @override
  String get categoryIconHousing => 'Logement';

  @override
  String get categoryIconUtilities => 'Charges';

  @override
  String get categoryIconInternet => 'Internet';

  @override
  String get categoryIconPhone => 'Téléphone';

  @override
  String get categoryIconCar => 'Voiture';

  @override
  String get categoryIconFuel => 'Carburant';

  @override
  String get categoryIconTransit => 'Transports';

  @override
  String get categoryIconTravel => 'Voyages';

  @override
  String get categoryIconHealth => 'Santé';

  @override
  String get categoryIconFitness => 'Sport';

  @override
  String get categoryIconShopping => 'Achats';

  @override
  String get categoryIconClothing => 'Vêtements';

  @override
  String get categoryIconEntertainment => 'Loisirs';

  @override
  String get categoryIconEducation => 'Éducation';

  @override
  String get categoryIconPets => 'Animaux';

  @override
  String get categoryIconGifts => 'Cadeaux';

  @override
  String get categoryIconSavings => 'Épargne';

  @override
  String get categoryIconBank => 'Banque et frais';

  @override
  String get invalidAmountMessage =>
      'Saisissez un montant supérieur à 0 avec 2 décimales maximum.';

  @override
  String get errorIdRequired =>
      'Cet élément est introuvable. Revenez en arrière et réessayez.';

  @override
  String get errorItemNotFound => 'Cet élément n’existe plus.';

  @override
  String get errorBlankDescription => 'Saisissez une description.';

  @override
  String errorDescriptionTooLong(int maxLength) {
    return 'La description ne doit pas dépasser $maxLength caractères.';
  }

  @override
  String get errorInvalidDate => 'Choisissez une date valide.';

  @override
  String get errorTransferDestinationRequired =>
      'Choisissez le compte de destination du virement.';

  @override
  String get errorTransferDestinationSame =>
      'Choisissez un autre compte de destination.';

  @override
  String get errorCategoryNotFound =>
      'Cette catégorie n’existe plus. Choisissez-en une autre.';

  @override
  String get errorAccountNotFound =>
      'Ce compte n’existe plus. Choisissez-en un autre.';

  @override
  String get errorBudgetExceeded =>
      'Avec cette dépense, la catégorie dépasserait son budget mensuel.';

  @override
  String get overBudgetTitle => 'Budget dépassé';

  @override
  String overBudgetMessage(String category, String overBy, String limit) {
    return 'Avec cette dépense, $category dépasse de $overBy son budget mensuel de $limit.';
  }

  @override
  String get overBudgetConfirm => 'Enregistrer quand même';

  @override
  String errorInvalidName(int maxLength) {
    return 'Le nom doit comporter entre 1 et $maxLength caractères.';
  }

  @override
  String errorDuplicateCategoryName(String name) {
    return 'Une catégorie nommée « $name » existe déjà.';
  }

  @override
  String errorDuplicateAccountName(String name) {
    return 'Un compte nommé « $name » existe déjà.';
  }

  @override
  String get errorDefaultCategory =>
      'Les catégories par défaut ne peuvent pas être supprimées.';

  @override
  String get errorDefaultAccount =>
      'Les comptes par défaut ne peuvent pas être supprimés.';

  @override
  String errorCategoryInUse(int count) {
    String _temp0 = intl.Intl.pluralLogic(
      count,
      locale: localeName,
      other: 'Cette catégorie est utilisée par $count transactions.',
      many: 'Cette catégorie est utilisée par $count transactions.',
      one: 'Cette catégorie est utilisée par $count transaction.',
    );
    return '$_temp0';
  }

  @override
  String errorAccountInUse(int count) {
    String _temp0 = intl.Intl.pluralLogic(
      count,
      locale: localeName,
      other: 'Ce compte est utilisé par $count transactions.',
      many: 'Ce compte est utilisé par $count transactions.',
      one: 'Ce compte est utilisé par $count transaction.',
    );
    return '$_temp0';
  }

  @override
  String get errorInvalidWarningPercent =>
      'Le seuil d’alerte doit être un pourcentage compris entre 1 et 100.';

  @override
  String get defaultAccountCash => 'Espèces';

  @override
  String get defaultAccountSavings => 'Épargne';

  @override
  String get defaultCategoryExpense => 'Dépenses générales';

  @override
  String get defaultCategoryIncome => 'Revenus généraux';

  @override
  String get defaultCategoryTransfer => 'Virement';

  @override
  String get dashboardLoadFailed =>
      'Impossible de charger vos transactions. Quittez l’application et revenez-y pour réessayer.';

  @override
  String get tourPrivateTitle => 'Votre argent, en toute confidentialité';

  @override
  String get tourPrivateBody =>
      'Suivez vos dépenses, vos revenus et les virements entre vos comptes. Tout reste sur ce téléphone, chiffré et verrouillé par votre code PIN. Sans inscription, sans publicité, sans Internet.';

  @override
  String get tourAddTitle => 'Ajoutez en quelques secondes';

  @override
  String get tourAddBody =>
      'Appuyez sur + pour enregistrer une dépense, un revenu ou un virement. Choisissez un compte et une catégorie : vos soldes se mettent à jour tout seuls.';

  @override
  String get tourBudgetTitle => 'Respectez votre budget';

  @override
  String get tourBudgetBody =>
      'Fixez un plafond mensuel pour une catégorie. Le tableau de bord affiche ce qu’il vous reste et vous prévient avant tout dépassement.';

  @override
  String get tourReportsTitle => 'Rapports et sauvegardes';

  @override
  String get tourReportsBody =>
      'Enregistrez ou partagez un rapport mensuel au format PDF ou CSV, et gardez une sauvegarde chiffrée pour ne jamais perdre vos données.';

  @override
  String get tourSkip => 'Passer';

  @override
  String get tourNext => 'Suivant';

  @override
  String get tourGetStarted => 'Commencer';

  @override
  String tourPageIndicator(int page, int pageCount) {
    return 'Page $page sur $pageCount';
  }

  @override
  String get gettingStartedTitle => 'Premiers pas';

  @override
  String gettingStartedProgress(int done, int total) {
    return 'Étapes terminées : $done sur $total';
  }

  @override
  String get gettingStartedAddAccount =>
      'Ajoutez votre compte bancaire ou votre carte';

  @override
  String get gettingStartedAddTransaction =>
      'Enregistrez votre première transaction';

  @override
  String get gettingStartedSetBudget => 'Définissez un budget mensuel';

  @override
  String get gettingStartedStepDone => 'Terminé';

  @override
  String get gettingStartedStepTodo => 'À faire';

  @override
  String get gettingStartedHide => 'Masquer la liste des premiers pas';

  @override
  String get settingsHelpSection => 'Aide et assistance';

  @override
  String get settingsHelpFaq => 'Aide et FAQ';

  @override
  String get settingsShowTour => 'Revoir la visite guidée';

  @override
  String get settingsSendFeedback => 'Envoyer des commentaires';

  @override
  String get settingsRateApp => 'Noter l’application';

  @override
  String get settingsShareApp => 'Partager l’application';

  @override
  String get helpTitle => 'Aide et FAQ';

  @override
  String get helpExpanded => 'Développé';

  @override
  String get helpCollapsed => 'Réduit';

  @override
  String get helpContactTitle => 'Besoin d’aide supplémentaire ?';

  @override
  String get helpContactBody =>
      'Envoyez-nous une question, un problème ou une idée. Merci de ne pas y inclure votre code PIN, la phrase secrète d’une sauvegarde ni vos données financières.';

  @override
  String get faqDataQ => 'Où mes données sont-elles stockées ?';

  @override
  String get faqDataA =>
      'Uniquement sur ce téléphone. Denarii Dolor ne demande aucune inscription, n’utilise aucun serveur et ne peut pas se connecter à Internet. Vos données sont conservées dans une base chiffrée que seuls votre code PIN, votre réponse de sécurité ou, si vous l’activez, votre empreinte digitale ou votre visage peuvent déverrouiller. La sauvegarde dans le cloud d’Android est désactivée pour l’application.';

  @override
  String get faqForgotPinQ => 'J’ai oublié mon code PIN. Que faire ?';

  @override
  String faqForgotPinA(String forgotPin, String wipeAllData) {
    return 'Sur l’écran de connexion, appuyez sur « $forgotPin » et répondez à votre question de sécurité pour choisir un nouveau code PIN. Si vous ne connaissez pas non plus la réponse, vos données ne peuvent pas être déverrouillées, car personne d’autre n’en possède la clé. La seule solution est alors « $wipeAllData » sur l’écran de connexion, qui efface tout pour vous permettre de repartir de zéro.';
  }

  @override
  String get faqSignedOutQ => 'Pourquoi ma session s’est-elle fermée ?';

  @override
  String faqSignedOutA(int minutes, String biometricSetting) {
    return 'Pour protéger vos données, l’application vous déconnecte après $minutes minutes d’inactivité. Reconnectez-vous avec votre code PIN, ou activez « $biometricSetting » dans les paramètres pour vous connecter avec votre empreinte digitale ou votre visage.';
  }

  @override
  String get faqBackupQ =>
      'Comment sauvegarder mes données ou les transférer sur un nouveau téléphone ?';

  @override
  String faqBackupA(String backupSetting) {
    return 'Dans les paramètres, ouvrez « $backupSetting » et créez une sauvegarde protégée par une phrase secrète. Enregistrez le fichier en lieu sûr, par exemple sur un service de stockage en ligne. Sur le nouveau téléphone, installez Denarii Dolor, configurez l’application, puis restaurez le fichier avec la même phrase secrète. Personne ne peut récupérer une phrase secrète oubliée.';
  }

  @override
  String get faqBudgetsQ => 'Comment fonctionnent les budgets ?';

  @override
  String faqBudgetsA(String budgetsScreen) {
    return 'Dans les paramètres, ouvrez « $budgetsScreen » pour fixer un plafond mensuel et un seuil d’alerte pour une catégorie. Le tableau de bord indique la part de chaque budget déjà utilisée ce mois-ci. Si une dépense risque de dépasser son budget, l’application vous demande confirmation avant de l’enregistrer.';
  }

  @override
  String get faqTransferQ => 'Qu’est-ce qu’un virement ?';

  @override
  String get faqTransferA =>
      'Un virement déplace de l’argent entre deux de vos comptes, par exemple du compte Épargne vers le compte Espèces. Il modifie les deux soldes, mais n’est compté ni comme un revenu ni comme une dépense.';

  @override
  String get faqDeleteQ =>
      'Pourquoi ne puis-je pas supprimer une catégorie ou un compte ?';

  @override
  String get faqDeleteA =>
      'Les catégories et les comptes par défaut ne peuvent pas être supprimés, pas plus que ceux encore utilisés par des transactions. Modifiez ou supprimez d’abord ces transactions, puis réessayez.';

  @override
  String get faqCurrencyQ => 'Comment changer de devise ?';

  @override
  String faqCurrencyA(String currencySetting) {
    return 'Dans les paramètres, appuyez sur « $currencySetting » et choisissez-en une. Seul le symbole change : les montants déjà saisis ne sont pas convertis.';
  }

  @override
  String get faqReportsQ => 'Comment exporter un rapport ?';

  @override
  String faqReportsA(String reportsScreen) {
    return 'Ouvrez « $reportsScreen », choisissez un mois, puis enregistrez ou partagez le rapport au format PDF ou CSV. Les fichiers exportés ne sont pas chiffrés : conservez-les dans un endroit privé.';
  }

  @override
  String get faqBiometricQ =>
      'Pourquoi la connexion par empreinte digitale ou par visage s’est-elle désactivée ?';

  @override
  String get faqBiometricA =>
      'Elle se désactive lorsque les empreintes digitales ou les visages enregistrés sur le téléphone changent, afin qu’une empreinte ou un visage ajouté récemment ne puisse pas ouvrir vos données. Votre code PIN fonctionne toujours, et vous pouvez réactiver la connexion biométrique dans les paramètres.';

  @override
  String get ratePromptTitle => 'Vous aimez Denarii Dolor ?';

  @override
  String get ratePromptMessage =>
      'Une petite note sur Google Play aide d’autres personnes à découvrir l’application. Cela ne prend qu’un instant.';

  @override
  String get ratePromptRate => 'Noter';

  @override
  String get ratePromptLater => 'Plus tard';

  @override
  String get ratePromptNever => 'Ne plus demander';

  @override
  String get rateUnavailable =>
      'Impossible d’ouvrir Google Play sur cet appareil.';

  @override
  String get shareAppChooser => 'Partager Denarii Dolor';

  @override
  String shareAppMessage(String storeLink) {
    return 'Je suis mes dépenses et mes budgets avec Denarii Dolor. Tout reste privé, sur le téléphone. Disponible sur Google Play : $storeLink';
  }

  @override
  String feedbackEmailSubject(String appVersion) {
    return 'Commentaires sur Denarii Dolor (version $appVersion)';
  }

  @override
  String feedbackEmailBody(String appVersion, String androidVersion) {
    return '\n\n———\nVersion de l’application : $appVersion\nVersion d’Android : $androidVersion';
  }

  @override
  String feedbackNoEmailApp(String emailAddress) {
    return 'Aucune application de messagerie trouvée. Vous pouvez écrire à $emailAddress.';
  }

  @override
  String placeholderAmountInCents(int cents) {
    return '$cents centimes';
  }

  @override
  String placeholderAmountRejected(String code) {
    return 'Refusé : $code';
  }
}
