package com.denariidolor.domain.usecase

import com.denariidolor.data.local.db.entity.AccountEntity
import com.denariidolor.data.repository.AccountRepository
import com.denariidolor.data.repository.TransactionRepository
import com.denariidolor.util.Constants
import com.denariidolor.util.Validators
import com.denariidolor.util.runSuspendCatching
import javax.inject.Inject

class AccountUseCases @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository
) {
    suspend fun add(name: String, openingBalance: Double = 0.0): Result<Long> = runSuspendCatching {
        require(Validators.isValidBalance(openingBalance)) { "Opening balance must be a valid number" }
        val validName = requireValidName(name, excludeId = 0L)
        accountRepository.add(AccountEntity(name = validName, balance = openingBalance))
    }

    suspend fun rename(id: Long, name: String): Result<Unit> = runSuspendCatching {
        require(id > 0L) { "Account ID is required" }
        val validName = requireValidName(name, excludeId = id)
        if (!accountRepository.rename(id, validName)) throw NoSuchElementException("Account not found")
    }

    suspend fun delete(id: Long): Result<Unit> = runSuspendCatching {
        check(id !in PROTECTED_IDS) { "Default accounts cannot be deleted" }
        val usage = transactionRepository.countByAccount(id)
        check(usage == 0) { "Account is used by $usage transaction(s)" }
        if (!accountRepository.delete(id)) throw NoSuchElementException("Account not found")
    }

    private suspend fun requireValidName(name: String, excludeId: Long): String {
        val trimmed = name.trim()
        require(Validators.isValidName(trimmed)) { "Name must be 1-${Validators.MAX_NAME_LENGTH} characters" }
        require(!accountRepository.isDuplicateName(trimmed, excludeId)) { "An account named \"$trimmed\" already exists" }
        return trimmed
    }

    private companion object {
        val PROTECTED_IDS = Constants.DEFAULT_ACCOUNTS.map { it.id }.toSet()
    }
}
