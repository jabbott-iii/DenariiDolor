/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.presentation.ui.common

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import com.denariidolor.R

// Android's words for the keys shared code uses: each key reads the resource of its lowercase name, in all seven languages.
// StringKeyResourcesTest checks every entry, so a key added to StringKeys.kt fails there until it is mapped here.

@get:StringRes
val StringKey.resId: Int get() = STRING_RESOURCES.getValue(this)

@get:PluralsRes
val PluralKey.resId: Int get() = PLURAL_RESOURCES.getValue(this)

private val STRING_RESOURCES: Map<StringKey, Int> = mapOf(
    StringKey.ACCOUNT_DELETED to R.string.account_deleted,
    StringKey.ACCOUNT_SAVED to R.string.account_saved,
    StringKey.BACKUP_ERROR_READ to R.string.backup_error_read,
    StringKey.BACKUP_RESTORE_FAILED to R.string.backup_restore_failed,
    StringKey.BACKUP_SAVE_FAILED to R.string.backup_save_failed,
    StringKey.BACKUP_SAVED to R.string.backup_saved,
    StringKey.BIOMETRIC_ENABLE_FAILED to R.string.biometric_enable_failed,
    StringKey.BIOMETRIC_ENABLED to R.string.biometric_enabled,
    StringKey.BIOMETRIC_SIGN_IN_INVALIDATED to R.string.biometric_sign_in_invalidated,
    StringKey.BUDGET_DELETED to R.string.budget_deleted,
    StringKey.BUDGET_SAVED to R.string.budget_saved,
    StringKey.CATEGORY_DELETED to R.string.category_deleted,
    StringKey.CATEGORY_SAVED to R.string.category_saved,
    StringKey.ERROR_ACCOUNT_NOT_FOUND to R.string.error_account_not_found,
    StringKey.ERROR_BLANK_DESCRIPTION to R.string.error_blank_description,
    StringKey.ERROR_BUDGET_EXCEEDED to R.string.error_budget_exceeded,
    StringKey.ERROR_CATEGORY_NOT_FOUND to R.string.error_category_not_found,
    StringKey.ERROR_DEFAULT_ACCOUNT to R.string.error_default_account,
    StringKey.ERROR_DEFAULT_CATEGORY to R.string.error_default_category,
    StringKey.ERROR_DESCRIPTION_TOO_LONG to R.string.error_description_too_long,
    StringKey.ERROR_DUPLICATE_ACCOUNT_NAME to R.string.error_duplicate_account_name,
    StringKey.ERROR_DUPLICATE_CATEGORY_NAME to R.string.error_duplicate_category_name,
    StringKey.ERROR_ID_REQUIRED to R.string.error_id_required,
    StringKey.ERROR_INVALID_DATE to R.string.error_invalid_date,
    StringKey.ERROR_INVALID_NAME to R.string.error_invalid_name,
    StringKey.ERROR_INVALID_WARNING_PERCENT to R.string.error_invalid_warning_percent,
    StringKey.ERROR_ITEM_NOT_FOUND to R.string.error_item_not_found,
    StringKey.ERROR_TRANSFER_DESTINATION_REQUIRED to R.string.error_transfer_destination_required,
    StringKey.ERROR_TRANSFER_DESTINATION_SAME to R.string.error_transfer_destination_same,
    StringKey.GENERIC_ERROR to R.string.generic_error,
    StringKey.INVALID_AMOUNT_MESSAGE to R.string.invalid_amount_message,
    StringKey.INVALID_BUDGET_INPUT to R.string.invalid_budget_input,
    StringKey.INVALID_OPENING_BALANCE to R.string.invalid_opening_balance,
    StringKey.INVALID_PIN_ATTEMPTS_LEFT to R.string.invalid_pin_attempts_left,
    StringKey.INVALID_SEARCH_RANGE_MESSAGE to R.string.invalid_search_range_message,
    StringKey.INVALID_SECURITY_ANSWER to R.string.invalid_security_answer,
    StringKey.PIN_CONFIRMATION_MISMATCH to R.string.pin_confirmation_mismatch,
    StringKey.PIN_FORMAT_ERROR to R.string.pin_format_error,
    StringKey.PIN_LOCKED_OUT to R.string.pin_locked_out,
    StringKey.PIN_RECOVERY_SUCCESS to R.string.pin_recovery_success,
    StringKey.PROFILE_ALREADY_CONFIGURED to R.string.profile_already_configured,
    StringKey.PROFILE_NOT_CONFIGURED to R.string.profile_not_configured,
    StringKey.PROFILE_SETUP_SUCCESS to R.string.profile_setup_success,
    StringKey.REPORT_EXPORT_FAILED to R.string.report_export_failed,
    StringKey.REPORT_SAVED to R.string.report_saved,
    StringKey.SECURITY_ANSWER_IN_QUESTION to R.string.security_answer_in_question,
    StringKey.SECURITY_ANSWER_MATCHES_PIN to R.string.security_answer_matches_pin,
    StringKey.SECURITY_ANSWER_REQUIRED to R.string.security_answer_required,
    StringKey.SECURITY_ANSWER_TOO_LONG to R.string.security_answer_too_long,
    StringKey.SECURITY_ANSWER_TOO_SHORT to R.string.security_answer_too_short,
    StringKey.SECURITY_QUESTION_REQUIRED to R.string.security_question_required,
    StringKey.SECURITY_QUESTION_TOO_LONG to R.string.security_question_too_long,
    StringKey.TRANSACTION_DELETED to R.string.transaction_deleted,
    StringKey.TRANSACTION_REFERENCE_REQUIRED_MESSAGE to R.string.transaction_reference_required_message,
    StringKey.WIPE_DATA_ERROR to R.string.wipe_data_error,
    StringKey.WIPE_DATA_SUCCESS to R.string.wipe_data_success
)

private val PLURAL_RESOURCES: Map<PluralKey, Int> = mapOf(
    PluralKey.ERROR_ACCOUNT_IN_USE to R.plurals.error_account_in_use,
    PluralKey.ERROR_CATEGORY_IN_USE to R.plurals.error_category_in_use
)
