package com.driverapp.repartidor

import com.driverapp.repartidor.data.remote.dto.PedidoDto
import com.driverapp.repartidor.domain.model.EstadoPedido
import com.driverapp.repartidor.domain.model.Pedido
import com.driverapp.repartidor.domain.model.User

fun sampleUser() = User(
    id = 2,
    email = "repartidor1@driverapp.com",
    name = "Juan Delgado",
    phone = "+52 555 123 4567",
    role = "REPARTIDOR",
    vehicle = "Moto",
    online = true,
    active = true
)

fun samplePedido() = Pedido(
    id = 10,
    restaurantId = 1,
    driverId = null,
    clientName = "Ana López",
    clientAddress = "Calle Falsa 123",
    destLat = 19.4326,
    destLng = -99.1332,
    estado = EstadoPedido.PENDIENTE,
    total = 150.0,
    deliveryFee = 25.0
)

fun samplePedidoDto() = PedidoDto(
    id = 10,
    restaurantId = 1,
    driverId = null,
    clientName = "Ana López",
    clientAddress = "Calle Falsa 123",
    destLat = 19.4326,
    destLng = -99.1332,
    status = "PENDIENTE",
    total = 150.0,
    deliveryFee = 25.0
)
