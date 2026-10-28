package com.amaranto.patientapp.core.ui

import androidx.annotation.StringRes
import com.amaranto.patientapp.R
import com.amaranto.patientapp.core.network.AppError

@StringRes
fun AppError.messageRes(): Int = when (this) {
    AppError.Network -> R.string.error_network
    AppError.Unauthorized -> R.string.error_unauthorized
    AppError.Forbidden -> R.string.error_forbidden
    AppError.NotFound -> R.string.error_not_found
    AppError.Conflict -> R.string.error_conflict
    AppError.Validation -> R.string.error_validation
    AppError.Server -> R.string.error_server
    AppError.Unknown -> R.string.error_unknown
}