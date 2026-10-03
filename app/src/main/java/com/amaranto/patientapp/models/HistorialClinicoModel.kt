package com.amaranto.patientapp.models

import com.google.gson.annotations.SerializedName

/**
 * Elemento individual del arreglo 'diagnostico_nutricional' en MongoDB.
 */
data class DiagnosticoNutricionalItem(
    @SerializedName("diagnostico") val diagnostico: String? = null,
    @SerializedName("meta") val meta: String? = null,
    @SerializedName("seguimiento") val seguimiento: String? = null,
    @SerializedName("tratamiento") val tratamiento: String? = null
)

/**
 * Objeto 'habitos_alimentarios' registrado en MongoDB.
 */
data class HabitosAlimentarios(
    @SerializedName("cantidad_agua_dia") val cantidadAguaDia: String? = null,
    @SerializedName("notas") val notas: String? = null,
    @SerializedName("habitos") val habitosTexto: String? = null
)

/**
 * Modelo de datos para mapear la respuesta del Expediente o Historial Clínico desde MongoDB/Laravel.
 * REGLA DE NEGOCIO: La información del historial clínico es de SOLO LECTURA para la App de Paciente.
 */
data class HistorialClinicoResponse(
    @SerializedName("_id") val id: String? = null,
    @SerializedName("paciente_id") val pacienteId: String? = null,

    // Campos planos tradicionales
    @SerializedName("diagnostico") val diagnosticoPlano: String? = null,
    @SerializedName("meta") val metaPlana: String? = null,
    @SerializedName("habitos") val habitosPlano: Any? = null,
    @SerializedName("observaciones") val observacionesPlanas: String? = null,
    @SerializedName("fecha_actualizacion") val fechaActualizacion: String? = null,

    // Estructura anidada proveniente del backend en Laravel/MongoDB
    @SerializedName("diagnostico_nutricional") val diagnosticoNutricionalList: List<DiagnosticoNutricionalItem>? = null,
    @SerializedName("habitos_alimentarios") val habitosAlimentariosObj: HabitosAlimentarios? = null
) {

    /**
     * Devuelve el diagnóstico extraído de 'diagnostico_nutricional' o del campo plano.
     */
    fun getDiagnosticoFormateado(): String {
        val item = diagnosticoNutricionalList?.firstOrNull()
        if (item != null && !item.diagnostico.isNullOrBlank()) {
            return item.diagnostico
        }
        return diagnosticoPlano ?: "Sin diagnóstico registrado por el médico."
    }

    /**
     * Devuelve la meta / tratamiento extrayéndola de 'diagnostico_nutricional' o del campo plano.
     */
    fun getMetaFormateada(): String {
        val item = diagnosticoNutricionalList?.firstOrNull()
        if (item != null) {
            val parts = mutableListOf<String>()
            if (!item.meta.isNullOrBlank()) parts.add("• Meta: ${item.meta}")
            if (!item.tratamiento.isNullOrBlank()) parts.add("• Tratamiento: ${item.tratamiento}")
            if (!item.seguimiento.isNullOrBlank()) parts.add("• Seguimiento: ${item.seguimiento}")

            if (parts.isNotEmpty()) {
                return parts.joinToString("\n")
            }
        }
        return metaPlana ?: "Sin meta de tratamiento asignada."
    }

    /**
     * Devuelve los hábitos extrayéndolos de 'habitos_alimentarios' o del campo plano.
     */
    fun getHabitosFormateados(): String {
        if (habitosAlimentariosObj != null) {
            val parts = mutableListOf<String>()
            if (!habitosAlimentariosObj.cantidadAguaDia.isNullOrBlank()) {
                parts.add("• Consumo de agua: ${habitosAlimentariosObj.cantidadAguaDia}")
            }
            if (!habitosAlimentariosObj.notas.isNullOrBlank()) {
                parts.add("• Recomendaciones/Notas: ${habitosAlimentariosObj.notas}")
            }
            if (!habitosAlimentariosObj.habitosTexto.isNullOrBlank()) {
                parts.add("• Hábitos: ${habitosAlimentariosObj.habitosTexto}")
            }

            if (parts.isNotEmpty()) {
                return parts.joinToString("\n")
            }
        }

        // Fallback a campo plano habitos
        return when (val h = habitosPlano) {
            is List<*> -> h.joinToString(separator = "\n• ", prefix = "• ") { it.toString() }
            is String -> if (h.isNotBlank()) h else "Sin hábitos registrados."
            else -> "Sin hábitos registrados."
        }
    }

    /**
     * Devuelve las observaciones generales.
     */
    fun getObservacionesFormateadas(): String {
        if (!observacionesPlanas.isNullOrBlank()) {
            return observacionesPlanas
        }
        val item = diagnosticoNutricionalList?.firstOrNull()
        if (item != null && !item.seguimiento.isNullOrBlank()) {
            return "Seguimiento médico programado: ${item.seguimiento}"
        }
        return "Sin observaciones registradas."
    }
}
