package com.driverapp.repartidor.domain.model

data class Pedido(
    val id: Int,
    val restaurantId: Int,
    val driverId: Int? = null,
    val clientName: String,
    val clientAddress: String,
    val destLat: Double,
    val destLng: Double,
    val estado: EstadoPedido,
    val total: Double,
    val deliveryFee: Double = 0.0,
    val paymentMethod: String = "Efectivo",
    val distanceKm: Double = 0.0,
    val estTimeMin: Int = 20,
    val restaurant: Restaurante? = null,
    val driver: ConductorBrief? = null,
    val items: List<PedidoItem> = emptyList()
) {
    val isPast: Boolean
        get() = estado == EstadoPedido.ENTREGADO || estado == EstadoPedido.RECHAZADO
}
