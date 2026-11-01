package com.driverapp.repartidor.domain.model

enum class EstadoPedido(val raw: String) {
    PENDIENTE("PENDIENTE"),
    ACEPTADO("ACEPTADO"),
    EN_CAMINO("EN_CAMINO"),
    ENTREGADO("ENTREGADO"),
    RECHAZADO("RECHAZADO"),
    CANCELADO("CANCELADO"),
    DESCONOCIDO("DESCONOCIDO");

    companion object {
        fun from(raw: String?): EstadoPedido =
            values().firstOrNull { it.raw == raw } ?: DESCONOCIDO
    }
}
