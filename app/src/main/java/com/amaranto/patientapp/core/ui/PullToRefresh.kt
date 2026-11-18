package com.amaranto.patientapp.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * "Desliza hacia abajo para actualizar" para cualquier pantalla.
 *
 * Funciona si el contenido es desplazable (LazyColumn, verticalScroll). Para estados que no lo son
 * (vacío, error) envuélvelos en [RefreshableFill].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmarantoPullToRefresh(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
        content = content
    )
}

/**
 * Hace desplazable un contenido que ocupa toda la pantalla (EmptyState, ErrorState) sin cambiar
 * cómo se ve, para que el gesto de deslizar llegue al PullToRefreshBox.
 */
@Composable
fun RefreshableFill(content: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val alto = maxHeight
        Box(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Box(Modifier.fillMaxWidth().height(alto)) { content() }
        }
    }
}
