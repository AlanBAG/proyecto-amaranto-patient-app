package com.amaranto.patientapp.feature.suscripciones.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amaranto.patientapp.R
import com.amaranto.patientapp.core.ui.ErrorState
import com.amaranto.patientapp.core.ui.LoadingState
import com.amaranto.patientapp.core.ui.theme.BadgeReadonlyBg
import com.amaranto.patientapp.core.ui.theme.BadgeReadonlyText
import com.amaranto.patientapp.feature.suscripciones.domain.model.Plan
import com.amaranto.patientapp.feature.suscripciones.domain.model.Suscripcion

@Composable
fun PagoRoute(
    onClose: () -> Unit,
    viewModel: PagoViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    when {
        state.suscripcionCreada != null -> PagoExito(
            suscripcion = state.suscripcionCreada!!,
            plan = state.plan,
            onListo = onClose
        )
        state.isLoadingPlan -> LoadingState()
        state.planError != null -> ErrorState(error = state.planError!!, onRetry = viewModel::loadPlan)
        state.plan != null -> PagoForm(
            plan = state.plan!!,
            state = state,
            onTitularChange = viewModel::onTitularChange,
            onNumeroChange = viewModel::onNumeroChange,
            onVencimientoChange = viewModel::onVencimientoChange,
            onCvvChange = viewModel::onCvvChange,
            onPagar = viewModel::onPagar
        )
    }
}

@Composable
private fun PagoForm(
    plan: Plan,
    state: PagoUiState,
    onTitularChange: (String) -> Unit,
    onNumeroChange: (String) -> Unit,
    onVencimientoChange: (String) -> Unit,
    onCvvChange: (String) -> Unit,
    onPagar: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = plan.nombre,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = stringResource(R.string.pago_total, plan.precio.toCurrencyText()),
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = stringResource(R.string.pago_simulado_aviso),
            style = MaterialTheme.typography.bodySmall,
            color = BadgeReadonlyText,
            modifier = Modifier
                .fillMaxWidth()
                .background(BadgeReadonlyBg, RoundedCornerShape(8.dp))
                .padding(12.dp)
        )

        OutlinedTextField(
            value = state.titular,
            onValueChange = onTitularChange,
            label = { Text(stringResource(R.string.pago_titular)) },
            isError = state.titularError,
            supportingText = {
                if (state.titularError) Text(stringResource(R.string.pago_titular_error))
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = state.numero,
            onValueChange = onNumeroChange,
            label = { Text(stringResource(R.string.pago_numero)) },
            isError = state.numeroError,
            supportingText = {
                if (state.numeroError) Text(stringResource(R.string.pago_numero_error))
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = state.vencimiento,
                onValueChange = onVencimientoChange,
                label = { Text(stringResource(R.string.pago_vencimiento)) },
                isError = state.vencimientoError,
                supportingText = {
                    if (state.vencimientoError) Text(stringResource(R.string.pago_vencimiento_error))
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = state.cvv,
                onValueChange = onCvvChange,
                label = { Text(stringResource(R.string.pago_cvv)) },
                isError = state.cvvError,
                supportingText = {
                    if (state.cvvError) Text(stringResource(R.string.pago_cvv_error))
                },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier.weight(1f)
            )
        }

        state.submitErrorRes?.let {
            Text(
                text = stringResource(it),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }

        Button(
            onClick = onPagar,
            enabled = !state.isProcessing,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isProcessing) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text(stringResource(R.string.pago_pagar, plan.precio.toCurrencyText()))
            }
        }
    }
}

@Composable
private fun PagoExito(suscripcion: Suscripcion, plan: Plan?, onListo: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.pago_exito_titulo),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.secondary
        )
        Text(
            text = stringResource(
                R.string.pago_exito_detalle,
                plan?.nombre.orEmpty(),
                suscripcion.fechaFin.toDateText()
            ),
            style = MaterialTheme.typography.bodyLarge
        )
        Button(onClick = onListo) { Text(stringResource(R.string.pago_listo)) }
    }
}