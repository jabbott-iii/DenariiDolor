// ignore: unused_import
import 'package:intl/intl.dart' as intl;

import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for Portuguese (`pt`).
class AppLocalizationsPt extends AppLocalizations {
  AppLocalizationsPt([String locale = 'pt']) : super(locale);

  @override
  String get appName => 'Denarii Dolor';

  @override
  String get dashboard => 'Painel';

  @override
  String get signIn => 'Entrar';

  @override
  String get signInWithBiometrics => 'Entrar com biometria';

  @override
  String get biometricSignInAccessibilityLabel => 'Entrar com biometria';

  @override
  String get biometricSignInTitle => 'Desbloquear o Denarii Dolor';

  @override
  String get biometricSignInSubtitle =>
      'Use a biometria cadastrada para continuar';

  @override
  String get biometricSignInFailed =>
      'Falha na autenticação por biometria. Tente novamente ou use seu PIN.';

  @override
  String get biometricSignInError =>
      'O login com biometria está indisponível no momento. Use seu PIN.';

  @override
  String get usePinInstead => 'Usar PIN';

  @override
  String get biometricSignInInvalidated =>
      'O login com biometria foi desativado porque a biometria deste celular mudou. Entre com seu PIN e ative-o de novo em Configurações.';

  @override
  String get biometricEnrollTitle => 'Ativar login com biometria';

  @override
  String get biometricEnrollSubtitle =>
      'Confirme com sua impressão digital ou seu rosto';

  @override
  String get biometricEnabled => 'O login com biometria está ativado.';

  @override
  String get biometricEnableFailed =>
      'Não foi possível ativar o login com biometria.';

  @override
  String get pinHint => 'PIN';

  @override
  String get showPin => 'Mostrar';

  @override
  String get hidePin => 'Ocultar';

  @override
  String get newPinHint => 'Novo PIN';

  @override
  String get confirmPinHint => 'Confirmar PIN';

  @override
  String get securityQuestionHint => 'Pergunta de segurança';

  @override
  String get securityAnswerHint => 'Resposta de segurança';

  @override
  String get securityAnswerVerifyHint => 'Resposta de segurança';

  @override
  String get forgotPin => 'Esqueceu o PIN?';

  @override
  String get backToSignIn => 'Voltar ao login';

  @override
  String get createSecurityProfile => 'Criar perfil de segurança';

  @override
  String get wipeAllData => 'Apagar todos os dados do app';

  @override
  String get wipeAllDataDestructiveLabel =>
      'Ação destrutiva: apagar todos os dados do app';

  @override
  String get wipeDataTitle => 'Excluir todos os dados do app?';

  @override
  String get wipeDataWarning =>
      'Esta ação exclui permanentemente todos os dados do app, registros financeiros, PIN e configurações de recuperação. Não é possível desfazer.';

  @override
  String get wipeDataConfirm => 'Excluir tudo';

  @override
  String wipeDataConfirmCountdown(int seconds) {
    return 'Excluir tudo ($seconds)';
  }

  @override
  String get wipeDataConfirmationWord => 'WIPE';

  @override
  String wipeDataTypeToConfirm(String word) {
    return 'Digite $word para confirmar';
  }

  @override
  String get wipeDataConfirmDestructiveLabel =>
      'Confirmação destrutiva: excluir permanentemente todos os dados do app';

  @override
  String get wipeDataCancel => 'Cancelar';

  @override
  String get pinFormatError => 'O PIN deve ter de 6 a 12 dígitos.';

  @override
  String get pinConfirmationMismatch =>
      'O PIN e a confirmação precisam ser iguais.';

  @override
  String get securityQuestionRequired =>
      'A pergunta de segurança é obrigatória.';

  @override
  String get securityAnswerRequired => 'A resposta de segurança é obrigatória.';

  @override
  String securityQuestionTooLong(int maxLength) {
    return 'A pergunta de segurança deve ter no máximo $maxLength caracteres.';
  }

  @override
  String securityAnswerTooShort(int minLength) {
    return 'A resposta de segurança precisa ter pelo menos $minLength caracteres. Letras maiúsculas e espaços extras não contam.';
  }

  @override
  String securityAnswerTooLong(int maxLength) {
    return 'A resposta de segurança deve ter no máximo $maxLength caracteres.';
  }

  @override
  String get securityAnswerInQuestion =>
      'A resposta não pode aparecer na pergunta.';

  @override
  String get securityAnswerMatchesPin =>
      'A resposta não pode ser igual ao seu PIN.';

  @override
  String securityAnswerRules(int minLength) {
    return 'Pelo menos $minLength caracteres. Letras maiúsculas e espaços extras são ignorados. Escolha uma resposta que outras pessoas não consigam adivinhar nem pesquisar.';
  }

  @override
  String get upgradeRecoveryIntro =>
      'O Denarii Dolor agora protege seus dados com seu PIN e uma resposta de recuperação mais forte. Defina uma pergunta e uma resposta de segurança para concluir o login.';

  @override
  String get saveAndContinue => 'Salvar e continuar';

  @override
  String get storageErrorMessage =>
      'Não é possível ler o armazenamento seguro deste celular, então seus dados não podem ser desbloqueados. Tente novamente ou apague todos os dados do app para começar do zero.';

  @override
  String get tryAgain => 'Tentar novamente';

  @override
  String get profileSetupSuccess =>
      'Perfil de segurança criado. Entre com seu PIN.';

  @override
  String get profileAlreadyConfigured =>
      'O perfil de segurança já está configurado.';

  @override
  String get profileNotConfigured =>
      'Configure um perfil de segurança antes de recuperar um PIN.';

  @override
  String get invalidSecurityAnswer => 'A resposta de segurança está incorreta.';

  @override
  String get pinRecoverySuccess => 'PIN redefinido. Entre com seu novo PIN.';

  @override
  String get wipeDataSuccess =>
      'Todos os dados do app foram excluídos. Configure um novo perfil de segurança.';

  @override
  String get wipeDataError =>
      'Não foi possível apagar os dados do app. Tente novamente.';

  @override
  String get securityQuestionUnavailable =>
      'Pergunta de segurança indisponível.';

  @override
  String get addTransaction => 'Adicionar transação';

  @override
  String get transactionDescriptionHint => 'Descrição';

  @override
  String get transactionTypeLabel => 'Tipo de transação';

  @override
  String get transactionAmountHint => 'Valor';

  @override
  String get transactionReferenceRequiredMessage =>
      'Revise os IDs da conta e da categoria antes de salvar esta transação.';

  @override
  String get dateHint => 'Data (AAAA-MM-DD)';

  @override
  String get saveTransaction => 'Salvar transação';

  @override
  String get transactionTypeExpense => 'Despesa';

  @override
  String get transactionTypeIncome => 'Receita';

  @override
  String get transactionTypeTransfer => 'Transferência';

  @override
  String get search => 'Pesquisar';

  @override
  String get searchDescriptionHint => 'Descrição';

  @override
  String get searchMinAmountHint => 'Valor mínimo';

  @override
  String get searchMaxAmountHint => 'Valor máximo';

  @override
  String get searchStartDateHint => 'Data inicial (AAAA-MM-DD)';

  @override
  String get searchEndDateHint => 'Data final (AAAA-MM-DD)';

  @override
  String get invalidDateMessage => 'Use o formato de data AAAA-MM-DD.';

  @override
  String get invalidSearchFiltersMessage =>
      'Verifique as datas e os filtros numéricos da pesquisa e tente novamente.';

  @override
  String get invalidSearchRangeMessage =>
      'Insira um intervalo de valores e de datas válido.';

  @override
  String get reports => 'Relatórios';

  @override
  String get settings => 'Configurações';

  @override
  String settingsSessionTimeout(int minutes) {
    return 'Tempo limite da sessão: $minutes minutos de inatividade';
  }

  @override
  String get settingsPinConfigured =>
      'O PIN está configurado para login rápido.';

  @override
  String get settingsPinNotConfigured => 'Nenhum PIN configurado ainda.';

  @override
  String get signOut => 'Sair';

  @override
  String get resetPin => 'Redefinir PIN';

  @override
  String get settingsDarkMode => 'Modo escuro';

  @override
  String get settingsBiometricSignIn => 'Login com biometria';

  @override
  String get settingsCurrency => 'Moeda';

  @override
  String settingsCurrencyValue(String currencyName, String symbol) {
    return '$currencyName ($symbol)';
  }

  @override
  String get settingsCurrencyTitle => 'Escolha uma moeda';

  @override
  String get settingsCurrencyNote =>
      'Só o símbolo muda. Os valores que você já inseriu não são convertidos.';

  @override
  String get settingsBackupRestore => 'Backup e restauração';

  @override
  String get settingsPrivacyPolicy => 'Política de privacidade';

  @override
  String get settingsLicenses => 'Licenças de código aberto';

  @override
  String get backupExplanation =>
      'Um backup guarda suas contas, categorias, orçamentos, transações e a moeda escolhida, criptografados com uma frase secreta que você escolhe. Ele não guarda seu PIN, sua pergunta de segurança nem o login com biometria: em um celular novo, configure o app primeiro e depois restaure o backup aqui.';

  @override
  String get backupPassphraseWarning =>
      'Guarde bem a frase secreta. Sem ela, o backup não pode ser aberto, e ninguém consegue recuperá-la.';

  @override
  String get backupCreate => 'Criar backup';

  @override
  String get backupRestore => 'Restaurar um backup';

  @override
  String get backupCreateTitle => 'Escolha uma frase secreta';

  @override
  String get backupRestoreTitle => 'Frase secreta do backup';

  @override
  String get backupPassphrase => 'Frase secreta';

  @override
  String get backupPassphraseConfirm => 'Confirme a frase secreta';

  @override
  String backupPassphraseRules(int minLength) {
    return 'Pelo menos $minLength caracteres. Algumas palavras sem relação entre si funcionam bem.';
  }

  @override
  String get backupContinue => 'Continuar';

  @override
  String get backupWorking => 'Processando…';

  @override
  String get backupSaved => 'Backup salvo.';

  @override
  String get backupSaveFailed => 'Não foi possível salvar o backup.';

  @override
  String get backupRestoreConfirmTitle => 'Substituir todos os dados?';

  @override
  String backupRestoreContents(
    String created,
    int accounts,
    int categories,
    int budgets,
    int transactions,
  ) {
    return 'Criado em: $created\nContas: $accounts\nCategorias: $categories\nOrçamentos: $budgets\nTransações: $transactions';
  }

  @override
  String get backupRestoreWarning =>
      'A restauração apaga tudo o que está neste celular agora e substitui pelo backup. Não é possível desfazer.';

  @override
  String get backupRestoreConfirmationWord => 'RESTORE';

  @override
  String get backupRestoreAction => 'Substituir dados';

  @override
  String get backupRestored => 'Backup restaurado.';

  @override
  String get backupRestoreFailed =>
      'Não foi possível restaurar o backup. Seus dados não foram alterados.';

  @override
  String backupErrorPassphraseLength(int minLength, int maxLength) {
    return 'A frase secreta precisa ter de $minLength a $maxLength caracteres.';
  }

  @override
  String get backupErrorPassphraseMismatch =>
      'As frases secretas não coincidem.';

  @override
  String get backupErrorNotBackup =>
      'Este arquivo não é um backup do Denarii Dolor.';

  @override
  String get backupErrorNewerVersion =>
      'Este backup foi criado por uma versão mais recente do Denarii Dolor. Atualize o app e tente novamente.';

  @override
  String get backupErrorWrongPassphrase =>
      'Frase secreta incorreta, ou o arquivo está corrompido.';

  @override
  String get backupErrorDamaged =>
      'O backup está corrompido e não pode ser restaurado.';

  @override
  String get backupErrorRead => 'Não foi possível ler o arquivo.';

  @override
  String get legalLoadFailed => 'Não foi possível abrir este documento.';

  @override
  String get legalEnglishOnly =>
      'Este documento está disponível somente em inglês.';

  @override
  String get transactionCategoryLabel => 'Categoria';

  @override
  String get transactionAccountLabel => 'Conta';

  @override
  String get transactionTransferAccountLabel => 'Transferir para a conta';

  @override
  String get editTransaction => 'Editar transação';

  @override
  String get updateTransaction => 'Atualizar transação';

  @override
  String get transactionSaved => 'Transação salva.';

  @override
  String get transactionDeleted => 'Transação excluída.';

  @override
  String get transactionNotFound => 'Transação não encontrada.';

  @override
  String get recentTransactions => 'Transações recentes';

  @override
  String get noTransactions =>
      'Nenhuma transação ainda. Toque em + para adicionar.';

  @override
  String get accountBalances => 'Saldos das contas';

  @override
  String get deleteTransactionTitle => 'Excluir transação?';

  @override
  String deleteTransactionMessage(String description) {
    return 'Excluir \"$description\"? Os saldos das contas serão ajustados.';
  }

  @override
  String deleteItemTitle(String name) {
    return 'Excluir $name?';
  }

  @override
  String get deleteItemMessage => 'Não é possível desfazer esta ação.';

  @override
  String get pickerPlaceholder => 'Selecione…';

  @override
  String get edit => 'Editar';

  @override
  String get delete => 'Excluir';

  @override
  String get cancel => 'Cancelar';

  @override
  String get save => 'Salvar';

  @override
  String get back => 'Voltar';

  @override
  String get genericError => 'Algo deu errado. Tente novamente.';

  @override
  String get manageCategories => 'Gerenciar categorias';

  @override
  String get manageAccounts => 'Gerenciar contas';

  @override
  String get manageBudgets => 'Gerenciar orçamentos';

  @override
  String get addCategory => 'Adicionar categoria';

  @override
  String get categoryName => 'Nome da categoria';

  @override
  String get categorySaved => 'Categoria salva.';

  @override
  String get categoryDeleted => 'Categoria excluída.';

  @override
  String get addAccount => 'Adicionar conta';

  @override
  String get renameAccount => 'Renomear conta';

  @override
  String get accountName => 'Nome da conta';

  @override
  String get openingBalance => 'Saldo inicial';

  @override
  String accountBalance(String balance) {
    return 'Saldo: $balance';
  }

  @override
  String get accountSaved => 'Conta salva.';

  @override
  String get accountDeleted => 'Conta excluída.';

  @override
  String get invalidOpeningBalance =>
      'Insira um saldo inicial válido (no máximo 2 casas decimais).';

  @override
  String get setBudget => 'Definir orçamento';

  @override
  String budgetForCategory(String category) {
    return 'Orçamento: $category';
  }

  @override
  String get budgetLimit => 'Limite mensal';

  @override
  String get budgetWarningPercent => 'Nível de alerta (porcentagem do limite)';

  @override
  String budgetSummary(String limit, int percent) {
    return 'Limite $limit · alerta em $percent%';
  }

  @override
  String get noBudget => 'Nenhum orçamento definido';

  @override
  String get remove => 'Remover';

  @override
  String get budgetSaved => 'Orçamento salvo.';

  @override
  String get budgetDeleted => 'Orçamento removido.';

  @override
  String get invalidBudgetInput =>
      'Insira um limite com no máximo 2 casas decimais e uma porcentagem de alerta de 1 a 100.';

  @override
  String invalidPinAttemptsLeft(int attempts) {
    return 'PIN inválido. Você ainda tem $attempts tentativa(s) antes de um bloqueio temporário.';
  }

  @override
  String pinLockedOut(int seconds) {
    return 'Muitas tentativas sem sucesso. Tente novamente em $seconds segundos.';
  }

  @override
  String get searchAllCategories => 'Todas as categorias';

  @override
  String get searchClear => 'Limpar';

  @override
  String searchResultCount(int count) {
    return '$count resultado(s)';
  }

  @override
  String get searchNoResults => 'Nenhuma transação encontrada.';

  @override
  String get previousMonth => '‹ Anterior';

  @override
  String get nextMonth => 'Próximo ›';

  @override
  String get reportTitle => 'Relatório mensal de gastos';

  @override
  String reportGenerated(String generated) {
    return 'Gerado em $generated';
  }

  @override
  String reportPage(int page) {
    return 'Página $page';
  }

  @override
  String reportTotals(String income, String expense, String net) {
    return 'Receitas $income · Despesas $expense · Saldo $net';
  }

  @override
  String get reportColDate => 'Data';

  @override
  String get reportColType => 'Tipo';

  @override
  String get reportColCategory => 'Categoria';

  @override
  String get reportColDescription => 'Descrição';

  @override
  String get reportColAmount => 'Valor';

  @override
  String get reportColPayment => 'Forma de pagamento';

  @override
  String get reportEmpty => 'Nenhuma transação neste mês.';

  @override
  String get reportLoadFailed =>
      'Não foi possível gerar o relatório. Tente novamente.';

  @override
  String get saveCsv => 'Salvar CSV';

  @override
  String get savePdf => 'Salvar PDF';

  @override
  String get shareCsv => 'Compartilhar CSV';

  @override
  String get sharePdf => 'Compartilhar PDF';

  @override
  String get shareReport => 'Compartilhar relatório';

  @override
  String get reportSaved => 'Relatório salvo.';

  @override
  String get reportExportFailed => 'Não foi possível exportar o relatório.';

  @override
  String get netThisMonth => 'Saldo do mês';

  @override
  String get incomeThisMonth => 'Receitas';

  @override
  String get expenseThisMonth => 'Despesas';

  @override
  String get spendingByCategory => 'Gastos por categoria';

  @override
  String get spendingNone => 'Nenhuma despesa neste mês.';

  @override
  String get spendingOther => 'Outros';

  @override
  String get budgetsThisMonth => 'Orçamentos do mês';

  @override
  String budgetSpentOfLimit(String spent, String limit) {
    return '$spent de $limit';
  }

  @override
  String budgetStatusLine(String status, int percent) {
    return '$status · $percent% usado';
  }

  @override
  String get budgetStatusOk => 'Dentro do previsto';

  @override
  String get budgetStatusWarning => 'Perto do limite';

  @override
  String get budgetStatusOver => 'Acima do orçamento';

  @override
  String budgetAlerts(int count) {
    String _temp0 = intl.Intl.pluralLogic(
      count,
      locale: localeName,
      other: '$count orçamentos estão perto ou acima do limite.',
      many: '$count orçamentos estão perto ou acima do limite.',
      one: '$count orçamento está perto ou acima do limite.',
    );
    return '$_temp0';
  }

  @override
  String get editCategory => 'Editar categoria';

  @override
  String categoryIconLabel(String icon) {
    return 'Ícone: $icon';
  }

  @override
  String get categoryIconGeneral => 'Geral';

  @override
  String get categoryIconIncome => 'Receita';

  @override
  String get categoryIconTransfer => 'Transferência';

  @override
  String get categoryIconGroceries => 'Mercado';

  @override
  String get categoryIconDining => 'Restaurantes';

  @override
  String get categoryIconCoffee => 'Café';

  @override
  String get categoryIconHousing => 'Moradia';

  @override
  String get categoryIconUtilities => 'Contas da casa';

  @override
  String get categoryIconInternet => 'Internet';

  @override
  String get categoryIconPhone => 'Telefone';

  @override
  String get categoryIconCar => 'Carro';

  @override
  String get categoryIconFuel => 'Combustível';

  @override
  String get categoryIconTransit => 'Transporte público';

  @override
  String get categoryIconTravel => 'Viagem';

  @override
  String get categoryIconHealth => 'Saúde';

  @override
  String get categoryIconFitness => 'Academia';

  @override
  String get categoryIconShopping => 'Compras';

  @override
  String get categoryIconClothing => 'Roupas';

  @override
  String get categoryIconEntertainment => 'Lazer';

  @override
  String get categoryIconEducation => 'Educação';

  @override
  String get categoryIconPets => 'Pets';

  @override
  String get categoryIconGifts => 'Presentes';

  @override
  String get categoryIconSavings => 'Poupança';

  @override
  String get categoryIconBank => 'Banco e tarifas';

  @override
  String get invalidAmountMessage =>
      'Insira um valor maior que 0 com no máximo 2 casas decimais.';

  @override
  String get errorIdRequired =>
      'Não foi possível encontrar este item. Volte e tente novamente.';

  @override
  String get errorItemNotFound => 'Este item não existe mais.';

  @override
  String get errorBlankDescription => 'Insira uma descrição.';

  @override
  String errorDescriptionTooLong(int maxLength) {
    return 'A descrição deve ter no máximo $maxLength caracteres.';
  }

  @override
  String get errorInvalidDate => 'Escolha uma data válida.';

  @override
  String get errorTransferDestinationRequired =>
      'Escolha a conta de destino da transferência.';

  @override
  String get errorTransferDestinationSame =>
      'Escolha uma conta de destino diferente.';

  @override
  String get errorCategoryNotFound =>
      'Essa categoria não existe mais. Escolha outra.';

  @override
  String get errorAccountNotFound =>
      'Essa conta não existe mais. Escolha outra.';

  @override
  String get errorBudgetExceeded =>
      'Esta despesa deixaria a categoria acima do orçamento mensal.';

  @override
  String get overBudgetTitle => 'Acima do orçamento';

  @override
  String overBudgetMessage(String category, String overBy, String limit) {
    return 'Com esta despesa, $category fica $overBy acima do orçamento mensal de $limit.';
  }

  @override
  String get overBudgetConfirm => 'Salvar mesmo assim';

  @override
  String errorInvalidName(int maxLength) {
    return 'O nome deve ter de 1 a $maxLength caracteres.';
  }

  @override
  String errorDuplicateCategoryName(String name) {
    return 'Já existe uma categoria chamada \"$name\".';
  }

  @override
  String errorDuplicateAccountName(String name) {
    return 'Já existe uma conta chamada \"$name\".';
  }

  @override
  String get errorDefaultCategory =>
      'Não é possível excluir as categorias padrão.';

  @override
  String get errorDefaultAccount => 'Não é possível excluir as contas padrão.';

  @override
  String errorCategoryInUse(int count) {
    String _temp0 = intl.Intl.pluralLogic(
      count,
      locale: localeName,
      other: 'Esta categoria é usada em $count transações.',
      many: 'Esta categoria é usada em $count transações.',
      one: 'Esta categoria é usada em $count transação.',
    );
    return '$_temp0';
  }

  @override
  String errorAccountInUse(int count) {
    String _temp0 = intl.Intl.pluralLogic(
      count,
      locale: localeName,
      other: 'Esta conta é usada em $count transações.',
      many: 'Esta conta é usada em $count transações.',
      one: 'Esta conta é usada em $count transação.',
    );
    return '$_temp0';
  }

  @override
  String get errorInvalidWarningPercent =>
      'O nível de alerta deve ser de 1 a 100 por cento.';

  @override
  String get defaultAccountCash => 'Dinheiro';

  @override
  String get defaultAccountSavings => 'Poupança';

  @override
  String get defaultCategoryExpense => 'Despesas gerais';

  @override
  String get defaultCategoryIncome => 'Receitas gerais';

  @override
  String get defaultCategoryTransfer => 'Transferência';

  @override
  String get dashboardLoadFailed =>
      'Não foi possível carregar suas transações. Saia do app e volte para tentar novamente.';

  @override
  String get tourPrivateTitle => 'Seu dinheiro, com privacidade';

  @override
  String get tourPrivateBody =>
      'Registre despesas, receitas e transferências entre suas contas. Tudo fica neste celular, criptografado e protegido pelo seu PIN. Sem cadastro, sem anúncios, sem internet.';

  @override
  String get tourAddTitle => 'Adicione em segundos';

  @override
  String get tourAddBody =>
      'Toque em + para registrar uma despesa, uma receita ou uma transferência. Escolha uma conta e uma categoria, e seus saldos são atualizados automaticamente.';

  @override
  String get tourBudgetTitle => 'Fique dentro do orçamento';

  @override
  String get tourBudgetBody =>
      'Defina um limite mensal para uma categoria. O Painel mostra quanto ainda resta e avisa você antes de ultrapassar o limite.';

  @override
  String get tourReportsTitle => 'Relatórios e backups';

  @override
  String get tourReportsBody =>
      'Salve ou compartilhe um relatório mensal como arquivo PDF ou CSV e mantenha um backup criptografado para nunca perder seus registros.';

  @override
  String get tourSkip => 'Pular';

  @override
  String get tourNext => 'Próximo';

  @override
  String get tourGetStarted => 'Começar';

  @override
  String tourPageIndicator(int page, int pageCount) {
    return 'Página $page de $pageCount';
  }

  @override
  String get gettingStartedTitle => 'Primeiros passos';

  @override
  String gettingStartedProgress(int done, int total) {
    return '$done de $total concluídos';
  }

  @override
  String get gettingStartedAddAccount =>
      'Adicione sua conta bancária ou de cartão';

  @override
  String get gettingStartedAddTransaction => 'Registre sua primeira transação';

  @override
  String get gettingStartedSetBudget => 'Defina um orçamento mensal';

  @override
  String get gettingStartedStepDone => 'Concluído';

  @override
  String get gettingStartedStepTodo => 'Pendente';

  @override
  String get gettingStartedHide => 'Ocultar a lista de primeiros passos';

  @override
  String get settingsHelpSection => 'Ajuda e suporte';

  @override
  String get settingsHelpFaq => 'Ajuda e perguntas frequentes';

  @override
  String get settingsShowTour => 'Ver o tour de boas-vindas';

  @override
  String get settingsSendFeedback => 'Enviar feedback';

  @override
  String get settingsRateApp => 'Avaliar este app';

  @override
  String get settingsShareApp => 'Compartilhar este app';

  @override
  String get helpTitle => 'Ajuda e perguntas frequentes';

  @override
  String get helpExpanded => 'Expandido';

  @override
  String get helpCollapsed => 'Recolhido';

  @override
  String get helpContactTitle => 'Ainda precisa de ajuda?';

  @override
  String get helpContactBody =>
      'Envie uma dúvida, um problema ou uma ideia. Não inclua seu PIN, a frase secreta de um backup nem seus registros financeiros.';

  @override
  String get faqDataQ => 'Onde meus dados ficam armazenados?';

  @override
  String get faqDataA =>
      'Somente neste celular. O Denarii Dolor não tem cadastro nem servidor e não consegue se conectar à internet. Seus registros ficam em um banco de dados criptografado que só pode ser desbloqueado com seu PIN, sua resposta de segurança ou, se você ativar, sua impressão digital ou seu rosto. O backup em nuvem do Android fica desativado para o app.';

  @override
  String get faqForgotPinQ => 'Esqueci meu PIN. O que posso fazer?';

  @override
  String faqForgotPinA(String forgotPin, String wipeAllData) {
    return 'Na tela de login, toque em “$forgotPin” e responda à sua pergunta de segurança para escolher um novo PIN. Se você também não souber a resposta, seus dados não podem ser desbloqueados, porque ninguém mais tem a chave. Nesse caso, a única saída é “$wipeAllData” na tela de login, que apaga tudo para você começar do zero.';
  }

  @override
  String get faqSignedOutQ => 'Por que minha sessão foi encerrada?';

  @override
  String faqSignedOutA(int minutes, String biometricSetting) {
    return 'Para proteger seus dados, o app encerra a sessão depois de $minutes minutos sem uso. Entre de novo com seu PIN ou ative “$biometricSetting” em Configurações para entrar com sua impressão digital ou seu rosto.';
  }

  @override
  String get faqBackupQ =>
      'Como faço backup dos meus dados ou passo tudo para um celular novo?';

  @override
  String faqBackupA(String backupSetting) {
    return 'Em Configurações, abra “$backupSetting” e crie um backup protegido por uma frase secreta. Salve o arquivo em um lugar seguro, como um serviço de armazenamento em nuvem. No celular novo, instale o Denarii Dolor, configure o app e depois restaure o arquivo com a mesma frase secreta. Ninguém consegue recuperar uma frase secreta esquecida.';
  }

  @override
  String get faqBudgetsQ => 'Como funcionam os orçamentos?';

  @override
  String faqBudgetsA(String budgetsScreen) {
    return 'Em Configurações, abra “$budgetsScreen” para definir um limite mensal e um nível de alerta para uma categoria. O Painel mostra quanto de cada orçamento você já usou neste mês. Se uma despesa for deixar a categoria acima do orçamento, o app pergunta antes de salvar.';
  }

  @override
  String get faqTransferQ => 'O que é uma transferência?';

  @override
  String get faqTransferA =>
      'Uma transferência move dinheiro entre duas contas suas, por exemplo, de Poupança para Dinheiro. Ela altera os dois saldos, mas não conta como receita nem como despesa.';

  @override
  String get faqDeleteQ =>
      'Por que não consigo excluir uma categoria ou uma conta?';

  @override
  String get faqDeleteA =>
      'Não é possível excluir as categorias e contas padrão, nem as que ainda são usadas em transações. Altere ou exclua essas transações primeiro e tente novamente.';

  @override
  String get faqCurrencyQ => 'Como mudo a moeda?';

  @override
  String faqCurrencyA(String currencySetting) {
    return 'Em Configurações, toque em “$currencySetting” e escolha uma. Só o símbolo muda: os valores que você já inseriu não são convertidos.';
  }

  @override
  String get faqReportsQ => 'Como exporto um relatório?';

  @override
  String faqReportsA(String reportsScreen) {
    return 'Abra “$reportsScreen”, escolha um mês e salve ou compartilhe o relatório como arquivo PDF ou CSV. Os arquivos exportados não são criptografados, então guarde-os em um lugar privado.';
  }

  @override
  String get faqBiometricQ =>
      'Por que o login com impressão digital ou rosto foi desativado?';

  @override
  String get faqBiometricA =>
      'Ele é desativado quando as impressões digitais ou os rostos cadastrados no celular mudam, para que um novo cadastro não consiga abrir seus dados. Seu PIN continua funcionando, e você pode reativar o login com biometria em Configurações.';

  @override
  String get ratePromptTitle => 'Está gostando do Denarii Dolor?';

  @override
  String get ratePromptMessage =>
      'Uma avaliação rápida no Google Play ajuda outras pessoas a encontrar o app. Leva só um instante.';

  @override
  String get ratePromptRate => 'Avaliar';

  @override
  String get ratePromptLater => 'Agora não';

  @override
  String get ratePromptNever => 'Não perguntar de novo';

  @override
  String get rateUnavailable =>
      'Não foi possível abrir o Google Play neste dispositivo.';

  @override
  String get shareAppChooser => 'Compartilhar o Denarii Dolor';

  @override
  String shareAppMessage(String storeLink) {
    return 'Eu controlo meus gastos e orçamentos com o Denarii Dolor. Tudo fica privado no celular. Baixe no Google Play: $storeLink';
  }

  @override
  String feedbackEmailSubject(String appVersion) {
    return 'Feedback sobre o Denarii Dolor (versão $appVersion)';
  }

  @override
  String feedbackEmailBody(String appVersion, String androidVersion) {
    return '\n\n———\nVersão do app: $appVersion\nVersão do Android: $androidVersion';
  }

  @override
  String feedbackNoEmailApp(String emailAddress) {
    return 'Nenhum app de e-mail foi encontrado. Você pode escrever para $emailAddress.';
  }

  @override
  String placeholderAmountInCents(int cents) {
    return '$cents centavos';
  }

  @override
  String placeholderAmountRejected(String code) {
    return 'Rejeitado: $code';
  }
}
